package org.discovery.vivaldi.core

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import scala.math._
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.UpdatedRPS
import org.discovery.vivaldi.dto.RPSInfo
import org.discovery.vivaldi.dto.UpdatedCoordinates
import scala.util.Random

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

 class ComputingAlgorithm(system: ActorRef, deltaConf: Double) extends Actor {

   val log = Logging(context.system, this)
   val delta = deltaConf
   var coordinates = Coordinates(0, 0)

   def receive = {
     case UpdatedRPS(rps) => compute(rps)
     case _ => log.info("Message Inconnu")
   }

   def compute(rps: Iterable[RPSInfo]): Coordinates = {
     //Vivaldi algorithm
     for (oneRps <- rps) {
       coordinates = coordinates.add(computeOne(oneRps))
     }
     system ! UpdatedCoordinates(coordinates, rps) //Envoi des coordonnées calculées à la brique Système
     coordinates
   }

   def computeOne(oneRps: RPSInfo): Coordinates = {

     val delta = 0.5
     //TODO see what value we assign to delta

     // Compute error of this sample. (1)
     val diffX = coordinates.x - oneRps.coordinates.x
     val diffY = coordinates.y - oneRps.coordinates.y
     val e = oneRps.ping - hypot(diffX, diffY)
     // Find the direction of the force the error is causing. (2)
     val dir = findDir(diffX, diffY)

     // The force vector is proportional to the error (3)
     val f = dir.times(e.toDouble)
     // Move a a small step in the direction of the force. (4)
     f.times(delta.toDouble)
   }

   def findDir(diffX: Double, diffY: Double): Coordinates = {
     if (diffX == 0 && diffY == 0) {
       val abs = new Random().nextDouble()
       val ord = new Random().nextDouble()
       val hyp = hypot(abs, ord)
       Coordinates(abs/hyp, ord/hyp)
     } else {
       val hyp = hypot(diffX, diffY)
       Coordinates(diffX/hyp, diffY/hyp)
     }
   }

}
