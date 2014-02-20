package org.discovery.vivaldi.perfs

import akka.actor.{Actor, ActorRef}
import org.discovery.vivaldi.dto._
import akka.event.Logging
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.CurrentCloseNodes
import org.discovery.vivaldi.dto.CloseNodeInfo

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
 * This actor gets information about the current state of the node and sends
 * performance information to the REST service at: http://vivaldi-monitoring-demo.herokuapp.com
 */

class Performance(system: ActorRef) extends Actor {

  var closeNodesOld : Seq[CloseNodeInfo] = Seq()
  var coordinatesOld : Coordinates = null

  //Differences between added distances from node to close nodes
  var closeNodesTotalMovements : Seq[Double] = Seq(500.0)
  //Value of movements of node
  var nodeMovements : Seq[Double] = Seq()
  //Number of movements inside the table of close nodes
  var closeNodesChanges : Seq[Int] = Seq()

  //Boolean variables that record whether the thresholds have been passed or not
  var movementThresholdPassed : Boolean = false
  var cnMovementsThresholdPassed : Boolean = false
  var cnChangesThresholdPassed : Boolean = false

  //Number of iterations on which we work
  val nbIterations = 3
  //Threshold that defines when the node doesn't move significantly anymore
  val thresholdPosition : Double = nbIterations * 1.0
  //Threshold that defines when the total of distances between the node and its close nodes doesn't move significantly anymore
  val cnThresholdMovements : Double = nbIterations * 1.0
  //Threshold that defines when there aren't much permutations in the close nodes table anymore
  val cnThresholdChanges : Int = nbIterations * 1

  val log = Logging(context.system, this)

  def receive = {
    case CurrentCloseNodes(cn) => processCloseNodesInfo(cn)
    case CurrentCoordinates(c) => processPosition(c)
    case _ => log.info("Message Inconnu")
  }

  def processCloseNodesInfo(closeNodes : Seq[CloseNodeInfo]) {
    if (closeNodesOld.length == 0) {
      closeNodesOld = closeNodes
    } else {
      if (!cnChangesThresholdPassed || !cnMovementsThresholdPassed) {
        if (!cnChangesThresholdPassed) {
          closeNodesChanges = closeNodesChanges :+ findChangesNumber(closeNodesOld, closeNodes)
          processCloseNodesChanges()
        }
        if (!cnMovementsThresholdPassed) {
          closeNodesTotalMovements = closeNodesTotalMovements :+ Math.abs(closeNodesTotalMovements.last - computeTotalDistance(closeNodes))
          processCloseNodesTotalMovements()
        }
        closeNodesOld = closeNodes
      }
    }
  }

  def processPosition(coordinates : Coordinates) {
    if (coordinatesOld != null || !coordinates.equals(Coordinates(0, 0))) {
      if (!movementThresholdPassed) {
        nodeMovements = nodeMovements :+ coordinatesOld.diff(coordinates)
        coordinatesOld = coordinates
        if (nodeMovements.length > nbIterations) {
          if (addNLast(nbIterations, nodeMovements) < thresholdPosition) {
            movementThresholdPassed = true
            //TODO send information to REST service
          }
        }
      }
    }
  }

  def processCloseNodesChanges() {
    if (closeNodesChanges.length > nbIterations) {
      if (addNLast(nbIterations, closeNodesChanges) < cnThresholdChanges) {
        cnChangesThresholdPassed = true
        //TODO send indormation to REST service
      }
    }
  }

  def processCloseNodesTotalMovements() {
    if (closeNodesTotalMovements.length > nbIterations) {
      if (addNLast(nbIterations, closeNodesTotalMovements) < cnThresholdMovements) {
        cnMovementsThresholdPassed = true
        //TODO send information to REST service
      }
    }
  }

  def computeTotalDistance(closeNodes: Seq[CloseNodeInfo]) : Double = {
    val distances : Seq[Double] = closeNodes.map(_.distanceFromSelf)
    distances.fold[Double](0)(_+_)
  }

  def findChangesNumber(closeNodesOld_ : Seq[CloseNodeInfo], closeNodes: Seq[CloseNodeInfo]) : Int = {
    val changes : Seq[Int] = closeNodes.map(change(_, closeNodes, closeNodesOld_))
    changes.fold[Int](0)(_+_)
  }

  def change(closeNode : CloseNodeInfo, closeNodes : Seq[CloseNodeInfo], closeNodesOld_ : Seq[CloseNodeInfo]) : Int = {
    if (closeNodesOld_.contains(closeNode) && closeNodes.contains(closeNode)) {
      if (closeNodes.indexOf(closeNode) == closeNodesOld_.indexOf(closeNode)) {
        0
      } else {
        1
      }
    } else {
      2
    }
  }

  def addNLast[T](n : Int, list : Seq[T])(implicit numeric: Numeric[T]) : T = {
    list.drop(list.length-n).fold(numeric.zero)(numeric.plus)
  }
}
