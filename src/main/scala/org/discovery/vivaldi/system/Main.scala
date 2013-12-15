package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import scala.concurrent.duration._
import org.discovery.vivaldi.dto._
import org.discovery.vivaldi.network.Communication
import scala.math._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.DoRPSRequest
import org.discovery.vivaldi.dto.CloseNodeInfo
import org.discovery.vivaldi.dto.RPSInfo
import org.discovery.vivaldi.dto.UpdatedCoordinates

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

class Main extends Actor {

  val log = Logging(context.system, this)

  //Creating child actors
  val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self), "VivaldiCore")
  val network = context.actorOf(Props(classOf[Communication], vivaldiCore, self), "Network")

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
    case NextNodesToSelf(excluded, numberOfNodes) => getCloseNodesToSelf(excluded, numberOfNodes)
    case NextNodesFrom(origin, excluded, numberOfNodes) => getCloseNodesFrom(origin, excluded, numberOfNodes)
    case UpdatedCoordinates(newCoordinates, rps) => updateCoordinates(newCoordinates, rps)
    case DeleteCloseNode(toDelete) => deleteCloseNode(toDelete)
    case unknownMessage => log.info("Unkown Message ",unknownMessage)
  }

  /**
   * Method that retrieves the closest nodes from self
   * @param excluded excluded nodes from the result by default nothing is excluded
   * @param numberOfNodes number of nodes to return by default we return one node
   * @return a Sequence of the closest nodes
   */
  def getCloseNodesToSelf(excluded: Set[nodeInfo], numberOfNodes: Int): Seq[nodeInfo] = {
    // we take as a reference the current node, we only have to retrieve the n first elements of the list without the excluded nodes
    closeNodes.filterNot(excluded contains).take(numberOfNodes)
  }

  /**
   * Method that retrieves the closest nodes from the origin
   * @param origin reference node
   * @param excluded excluded nodes from the result. By default nothing is excluded
   * @param numberOfNodes number of nodes to return. By default we return one node
   * @return a Sequence of the closest nodes
   */
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

    log.debug(s"New coordinated received: $newCoordinates")
    coordinates = newCoordinates

    log.debug("Computing & updating distances")
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

    log.debug("Ordering closest node List")
    closeNodes = closeNodes.sorted.take(numberOfCloseNodes)
  }

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

  val myInfo = RPSInfo(this.network, coordinates, 1067) // TODO Fix that

  /**
   *  First call made
  Used to init the system with a first node
   */
  val initScheduler = context.system.scheduler.scheduleOnce(firstCallTime seconds){
     network ! FirstContact(this.network)
  }

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
    log.debug("Scheduler for RPS request called")
    log.debug(s"$numberOfNodesCalled nodes will be called")
    val myInfo = RPSInfo(self, coordinates, 0)
    network ! DoRPSRequest(myInfo, numberOfNodesCalled)
  }

}
