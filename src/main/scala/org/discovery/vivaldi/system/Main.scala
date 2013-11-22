package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{Props, ActorSystem, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.dto._
import org.discovery.vivaldi.network.Communication
import scala.concurrent.duration._
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.RPSInfo
import org.discovery.vivaldi.dto.UpdatedCoordinates
import org.discovery.vivaldi.dto.DoRPSRequest

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



  /**
   *  First call made
  Used to init the system with a first node
   */
  val initScheduler = context.system.scheduler.scheduleOnce(0.millis){
     //network ! FirstContact()
  }


  /**
   *  Creates a scheduler
  Calls network with case class DoRPSRequest in argument
  First call will be made after 50 ms
  Calls made each 50 ms
   */
  val schedulerRPS = context.system.scheduler.schedule(50.millis, 50.millis){
    log.debug("Scheduler for RPS request called")
    network ! DoRPSRequest(10)
  }

}
