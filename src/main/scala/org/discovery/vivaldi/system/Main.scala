package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, Actor}
import akka.pattern.{ask, AskTimeoutException}
import akka.util.Timeout
import org.discovery.vivaldi.core.ComputingAlgorithm
import scala.concurrent.duration._
import org.discovery.vivaldi.dto._
import org.discovery.vivaldi.network.Communication
import scala.math._
import scala.concurrent.{Await, Future, ExecutionContext}
import ExecutionContext.Implicits.global
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.DoRPSRequest
import org.discovery.vivaldi.dto.CloseNodeInfo
import org.discovery.vivaldi.dto.RPSInfo
import org.discovery.vivaldi.dto.UpdatedCoordinates
import org.discovery.vivaldi.network.Communication.Ping
import dispatch._
import org.discovery.vivaldi.system.Main.{IAmAwake, AreYouAwake}

/* ============================================================
 * Discovery Project - AkkaArc
 * http://beyondtheclouds.github.io/
 * ============================================================
 * Copyright 2013 Discovery Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============================================================ */


object Main {

  case class AreYouAwake()

  case class IAmAwake(closeNodes: Seq[CloseNodeInfo])

}


class Main(name : String,id:Int) extends Actor {

  val log = Logging(context.system, this)

  //Creating child actors
  val deltaConf = context.system.settings.config.getDouble("vivaldi.system.vivaldi.delta")
  val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self, deltaConf), "VivaldiCore")
  val network = context.actorOf(Props(classOf[Communication], vivaldiCore, self, 0), "Network")

  var coordinates: Coordinates = Coordinates(0,0)
  var closeNodes: Seq[CloseNodeInfo] = Seq()

  val config = context.system.settings.config.getConfig("vivaldi.system")
  val configInit = config.getConfig("init")
  val numberOfCloseNodes = config.getInt("closeNodes.size")

  /**
   * Method that handles the oncoming messages
   * @return
   */
  def receive = {
    case NextNodesToSelf(excluded, numberOfNodes) => sender ! getCloseNodesToSelf(excluded, numberOfNodes)
    case NextNodesFrom(origin, excluded, numberOfNodes) => sender ! getCloseNodesFrom(origin, excluded, numberOfNodes)
    case UpdatedCoordinates(newCoordinates, rps) => updateCoordinates(newCoordinates, rps)
    case DeleteCloseNode(toDelete) => deleteCloseNode(toDelete)
    case p: Ping => network forward p
    case f:FirstContact => network forward f
    case a: AreYouAwake => sender ! IAmAwake(closeNodes)
    case unknownMessage => log.info("Unknown Message "+unknownMessage)
  }

  /**
   * Check is a node is responding and if so retrieve its closeNodes table have more information
   * @param info Node to contact
   * @return true if the node is responding and false otherwise
   */
  def isAwake(info: nodeInfo): Boolean = {
    implicit val timeout = Timeout(5 seconds)
    val response = info.node ? AreYouAwake()
    try {
      Await.result(response, 6 seconds) match {
        case r: IAmAwake => {
          log.debug(s"Node $info is well, processing closeNode table")
          mergeCloseNodesTable(r.closeNodes)
          true
        }
      }
    } catch {
      case e: AskTimeoutException => {
        log.warning(s"Node $info is not responding")
        false
      }
      case exp: Throwable => {
        log.error("Unknown exception", exp)
        false
      }
    }
  }

  /**
   * Merges the table in parameter with the local closeNodes table
   * @param table closeNodes table to merge the local one with
   */
  def mergeCloseNodesTable(table: Seq[CloseNodeInfo]) {
    val updatedTable = table.map(node => node.copy(distanceFromSelf = computeDistanceBtw(this.coordinates, node.coordinates)))
    closeNodes = (closeNodes ++ updatedTable).sorted.take(numberOfCloseNodes)
  }

  /**
   * Method that retrieves the closest nodes from self
   * @param excluded excluded nodes from the result
   * @param numberOfNodes number of nodes to return
   * @return a Sequence of the closest nodes
   */
  def getCloseNodesToSelf(excluded: Set[nodeInfo], numberOfNodes: Int): Seq[nodeInfo] = {
    // we take as a reference the current node, we only have to retrieve the n first elements of the list without the excluded nodes
    val currentCloseNodes = closeNodes.filterNot(excluded contains)
    val partition = currentCloseNodes.splitAt(numberOfNodes)
    val test = isAwake(partition._1.head)
    var awakeCloseNodes = partition._1.filter(isAwake) // we test all the nodes, some dead note are possibly filtered
    var remainingCloseNodes = partition._2

    while (awakeCloseNodes.size < numberOfNodes && !remainingCloseNodes.isEmpty) { // while we don't have the correct number of nodes we add them for the second part of the closeNodes list
      val info = remainingCloseNodes.head
      remainingCloseNodes = remainingCloseNodes.tail
      if (isAwake(info)) {
        awakeCloseNodes = awakeCloseNodes :+ info
      }
    }

    awakeCloseNodes
  }

  /**
   * Method that retrieves the closest nodes from the origin
   * @param origin reference node
   * @param excluded excluded nodes from the result. By default nothing is excluded
   * @param numberOfNodes number of nodes to return. By default we return one node
   * @return a Sequence of the closest nodes
   */
  @deprecated
  def getCloseNodesFrom(origin: nodeInfo, excluded: Set[nodeInfo] , numberOfNodes: Int ): Seq[nodeInfo] = {
      // we just have to compute the distances between the reference and the nodes in memory, sort them, and send the n closest without excluded nodes
      val relativeDistancesSeq = closeNodes.map(node => node.copy(distanceFromSelf = computeDistanceBtw(origin.coordinates,this.coordinates)))
      relativeDistancesSeq.sorted.filterNot(excluded contains).take(numberOfNodes)
  }

  /**
   * Method to update the close node list coordinates.
   * @param newCoordinates updated coordinates of the current node just calculates
   * @param rpsIterable list of the RPS nodes that will be used to update the close node list
   */
  def updateCoordinates(newCoordinates: Coordinates, rpsIterable: Iterable[RPSInfo]) {

    val rps = rpsIterable.toSeq

    coordinates = newCoordinates
    updateMonitoring

    //Computing the distances from the RPS table
    val RPSCloseNodes = rps.map(node => CloseNodeInfo(node.node, node.coordinates,computeDistanceToSelf(node.coordinates)))

    //TODO Test contain with data and code the method equals for closeNodes
    //Retrieve nodes to update
    val RPSCloseNodesUpdates = RPSCloseNodes intersect closeNodes
    //Get rid of the nodes already in the list
    val RPSCloseNodesToAdd = RPSCloseNodes.filterNot(RPSCloseNodesUpdates contains)

    //Computing and updating distances for the existing nodes
    closeNodes = closeNodes.map(node => node.copy(distanceFromSelf = computeDistanceToSelf(this.coordinates)))
    //Updating RPS nodes already in closeNodes
    closeNodes =  RPSCloseNodesUpdates ++ closeNodes.filterNot(RPSCloseNodesUpdates contains)

    //Adding new Nodes
    closeNodes = RPSCloseNodesToAdd ++ closeNodes

    closeNodes = closeNodes.sorted.take(numberOfCloseNodes)
  }

  def updateMonitoring = {}
  /**
   * Method to compute the distance between the current node and the node in parameter
   * @param externCoordinates to compute the distance from
   * @return
   */
  def computeDistanceToSelf(externCoordinates: Coordinates): Double = {
    computeDistanceBtw(coordinates,externCoordinates)
  }

  /**
   * Compute the distance between two coordinates
   * @param a coordinates of the point a
   * @param b coordinates of the point b
   * @return distance between the two points
   */
  def computeDistanceBtw(a: Coordinates, b: Coordinates): Double = {
    hypot(a.x-b.x,a.y-b.y)
  }

  /**
   * Methods that deletes a node from the close node list when a ping isn't correct.
   * @param nodeToDelete to delete from the list
   */
  def deleteCloseNode(nodeToDelete: RPSInfo){
    closeNodes = closeNodes.filterNot(_.node.path == nodeToDelete.node.path)
  }

  def initSystem(){

  }

  case class CountCalls();

  val firstCallTime = configInit.getInt("firstCallTime")
  val timeBetweenCallsFirst = configInit.getInt("timeBetweenCallsFirst")
  val timeBetweenCallsThen = configInit.getInt("timeBetweenCallsThen")
  val numberOfNodesCalled = configInit.getInt("numberOfNodesCalled")
  val changeTime =  configInit.getInt("changeTime")

  var numberOfCalls = 0

  val myInfo = RPSInfo(this.network, coordinates, 1067, this.id) // TODO Fix that

  /**
   *  Creates a scheduler
  Calls network with case class DoRPSRequest in argument
  First call will be made after firstCallTime ms
  Calls made each timeBetweenCalls ms
   */
  val schedulerRPSFirst = context.system.scheduler.schedule(timeBetweenCallsFirst seconds, timeBetweenCallsFirst seconds){
    callNetwork()
  }

  val schedulerChangeFrequency = context.system.scheduler.scheduleOnce(changeTime seconds){
    schedulerRPSFirst.cancel()
  }

  val schedulerRPSThen = context.system.scheduler.schedule(changeTime seconds, timeBetweenCallsThen seconds){
     callNetwork()
  }

  def callNetwork() = {
    val myInfo = RPSInfo(self, coordinates, 0, this.id)
    network ! DoRPSRequest(myInfo, numberOfNodesCalled)
  }

}
