package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import scala.concurrent.duration._
import org.discovery.vivaldi.dto.DoRPSRequest
import org.discovery.vivaldi.dto.{CloseNodeInfo, RPSInfo, Coordinates, UpdatedCoordinates}
import org.discovery.vivaldi.network.Communication
import scala.math._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

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

class Main extends Actor{

  val log = Logging(context.system, this)

  //Creating child actors
  val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self), "VivaldiCore")
  val network = context.actorOf(Props(classOf[Communication], vivaldiCore), "Network")

  var coordinates: Coordinates = Coordinates(0,0)
  var closeNodes: Seq[CloseNodeInfo] = Seq()

  /**
   * Method that handles the oncoming messages
   * @return
   */
  def receive = {
    case UpdatedCoordinates(newCoordinates, rps) => updateCoordinates(newCoordinates, rps)
    case _ => log.info("Message Inconnu")
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
    val RPSCloseNodes = rps.map(node => CloseNodeInfo(node.node, node.systemInfo, node.coordinates,computeDistanceToSelf(node.coordinates)))

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
    closeNodes.sorted
    //TODO Troncate the list to only keep the n first element. We first have to define the configuration part
  }

  /**
   * Method to compute the distance between the current node and the node in parameter
   * @param externCoordinates to compute the distance from
   * @return
   */
  def computeDistanceToSelf(externCoordinates: Coordinates): Double = {
    hypot(coordinates.x-externCoordinates.x,coordinates.y-externCoordinates.y)
  }

  def initSystem(){

  }


  /**
   * Created by configuration file
   */
  val firstCallTime = context.system.settings.config.getString(
    "vivaldi.system.init.firstCallTime"
  ).toInt

  /**
   * Created by configuration file
   */
  val timeBetweenCalls = context.system.settings.config.getString(
    "vivaldi.system.init.timeBetweenCalls"
  ).toInt

  /**
   * Created by configuration file
   */
  val numberOfNodesCalled = context.system.settings.config.getString(
    "vivaldi.system.init.numberOfNodesCalled"
  ).toInt

  /**
   *  First call made
  Used to init the system with a first node
   */
  val initScheduler = context.system.scheduler.scheduleOnce(firstCallTime.millis){
     //network ! FirstContact()
  }


  /**
   *  Creates a scheduler
  Calls network with case class DoRPSRequest in argument
  First call will be made after 50 ms
  Calls made each 50 ms
   */
  val schedulerRPS = context.system.scheduler.schedule(timeBetweenCalls.millis, timeBetweenCalls.millis){
    log.debug("Scheduler for RPS request called")
    log.debug(s"$numberOfNodesCalled nodes will be called")
    network ! DoRPSRequest(numberOfNodesCalled)
  }

}
