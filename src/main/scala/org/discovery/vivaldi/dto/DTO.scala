package org.discovery.vivaldi.dto

import akka.actor.ActorRef
import org.discovery.vivaldi.network.CommunicationMessage

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
  val id: Long
  val node: ActorRef
  val coordinates: Coordinates
}

/**
 * Class containing the information about a node during the RPS process
 * @param node
 * @param coordinates
 * @param ping
 */
case class RPSInfo(id: Long, node: ActorRef,  coordinates: Coordinates, ping: Long) extends nodeInfo

/**
 * Class containing the information about a node and its distance from the current node.
 * @param node
 * @param coordinates
 * @param distanceFromSelf
 */
case class CloseNodeInfo(id: Long, node: ActorRef, coordinates: Coordinates, distanceFromSelf: Double) extends nodeInfo with Ordered[CloseNodeInfo] {
  override def compare(that: CloseNodeInfo) = (this.distanceFromSelf - that.distanceFromSelf).signum

  override def equals(obj: Any) = obj match {
    case that: CloseNodeInfo =>  this.node.path == that.node.path
    case _ => false
  }
}

case class Coordinates(x: Double, y: Double) {
  def times(multiplier: Double): Coordinates =  {
    Coordinates(this.x * multiplier, this.y * multiplier)
  }

  def add(that: Coordinates): Coordinates = {
    Coordinates(this.x + that.x, this.y + that.y)
  }

  def length(): Double = {
    Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2))
  }

  override def equals(obj: Any) = obj match {
    case that: Coordinates =>  this.x == that.x && this.y == that.y
    case _ => false
  }
}

/**
  * Message to delete a node from the close node list when a pinged node in the network part
  * is not responding to ping
  * @param nodeToDelete
  */
case class DeleteCloseNode(nodeToDelete: RPSInfo)

case class DoRPSRequest(newInfo:RPSInfo,numberOfNodesToContact: Int) extends CommunicationMessage // System-Network Message

/**
  * Message to tell Vivaldi the first node to contact.
  * @param node ActorRef of the first node to contact.
  */
case class FirstContact(node: ActorRef) extends CommunicationMessage //System-Network Message

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
