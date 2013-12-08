package org.discovery.vivaldi.dto

import akka.actor.ActorRef

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

/**
 * Trait defining the common properties of a node
 */
trait nodeInfo {
  val node: ActorRef
  val systemInfo: SystemInfo
  val coordinates: Coordinates
}

/**
 * Class containing the information about a node during the RPS process
 * @param node
 * @param systemInfo
 * @param coordinates
 * @param ping
 */
case class RPSInfo(node: ActorRef, systemInfo: SystemInfo, coordinates: Coordinates, ping: Long) extends nodeInfo

/**
 * Class containing the information about a node and its distance from the current node.
 * @param node
 * @param systemInfo
 * @param coordinates
 * @param distanceFromSelf
 */
case class CloseNodeInfo(node: ActorRef, systemInfo: SystemInfo, coordinates: Coordinates, distanceFromSelf: Double) extends nodeInfo with Ordered[CloseNodeInfo] {
  override def compare(that: CloseNodeInfo) = (this.distanceFromSelf - that.distanceFromSelf).signum

  override def equals(obj: Any) = obj match {
    case that: CloseNodeInfo =>  this.node.path == that.node.path
    case _ => false
  }
}

/**
 * Class containing information about the machine on which the node is running
 * @param cores
 * @param memory
 */
case class SystemInfo(cores: Int, memory: Long)

case class Coordinates(x: Double, y: Double) {
  def times(multiplier: Double): Coordinates =  {
    Coordinates(this.x * multiplier, this.y * multiplier)
  }

  def add(that: Coordinates): Coordinates = {
    Coordinates(this.x + that.x, this.y + that.y)
  }
}

case class DoRPSRequest(newInfo:RPSInfo,numberOfNodesToContact: Int) // System-Network Message

case class FirstContact(node: ActorRef) //System-Network Message

case class UpdatedRPS(rps: Iterable[RPSInfo]) // Network-Vivaldi Message

case class UpdatedCoordinates(coordinates: Coordinates, RPSTable: Iterable[RPSInfo])  // Vivaldi-System Message

// API Messages
/**
 * Message to get the next closest Nodes to self
 * @param excluded nodes to exclude from the result. By default it is empty.
 * @param numberOfNodes number of nodes to return. 1 by default.
 */
case class NextNodesToSelf(excluded: Set[nodeInfo] = Set(), numberOfNodes: Int = 1)

/**
 * Message to get the next closest Nodes to origin
 * @param origin node from which you want the closest node from
 * @param excluded nodes to exclude from the result. By default it is empty.
 * @param numberOfNodes number of nodes to return. 1 by default.
 */
case class NextNodesFrom(origin: nodeInfo, excluded: Set[nodeInfo] = Set(), numberOfNodes: Int = 1)
