package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, ActorSystem, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.dto.{RPSInfo, Coordinates, UpdatedCoordinates}
import org.discovery.vivaldi.network.Communication

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

  def receive = {
    case UpdatedCoordinates(newCoordinates, rps) => updateCoordinates(newCoordinates, rps)
    case _ => log.info("Message Inconnu")
  }

  def updateCoordinates(newCoordinates: Coordinates, rps: Iterable[RPSInfo]) {
    log.debug(s"New coordinated received: $newCoordinates")
    coordinates = newCoordinates
    //Compute the new neighbours table
  }

  def initSystem(){

  }

}
