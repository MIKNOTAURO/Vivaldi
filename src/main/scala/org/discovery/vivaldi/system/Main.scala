package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, ActorSystem, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.dto.{CloseNodeInfo, RPSInfo, Coordinates, UpdatedCoordinates}
import org.discovery.vivaldi.network.Communication
import scala.math._

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
  var closeNodes: List[CloseNodeInfo] = List()

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
   * @param rps list of the RPS nodes that will be used to update the close node list
   */
  def updateCoordinates(newCoordinates: Coordinates, rps: Iterable[RPSInfo]) {
    log.debug(s"New coordinated received: $newCoordinates")
    coordinates = newCoordinates

    log.debug("Computing & updating distances")
    for (nodeRPS <- rps ; closeNode <- closeNodes){
      if (nodeRPS.node.equals(closeNode.node)){
        closeNode.copy(distanceFromSelf = computeDistanceToSelf(nodeRPS))
      }else {
        closeNodes::CloseNodeInfo(nodeRPS.node,computeDistanceToSelf(nodeRPS))
      }
    }

    log.debug("Ordering closest node List")
    closeNodes.sortWith(SortCloseNodeInfo)
  }

  /**
   * Method to compute the distance between the current node and the node in parameter
   * @param node to compute the distance from
   * @return
   */
  def computeDistanceToSelf(node: RPSInfo): Double = {
    hypot(coordinates.x-node.coordinates.x,coordinates.y-node.coordinates.y)
  }

  /**
   * Method to compare the distance between two nodes
   * @param node1
   * @param node2
   * @return true if node1 is closest from self than node2
   */
  def SortCloseNodeInfo(node1: CloseNodeInfo, node2: CloseNodeInfo): Boolean ={
    node1.distanceFromSelf < node2.distanceFromSelf
  }

  def initSystem(){

  }

}
