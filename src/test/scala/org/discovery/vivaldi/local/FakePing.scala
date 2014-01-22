package org.discovery.vivaldi.local

import akka.actor.{ActorSystem, Props, ActorRef}
import org.discovery.vivaldi.dto.{FirstContact, RPSInfo, Coordinates}
import org.discovery.vivaldi.system.Main
import org.discovery.vivaldi.core.ComputingAlgorithm
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

 object FakePing {

  var pingTable : Array[Array[Long]] = Array()

  def createTable(coordinates:Seq[Coordinates]):Array[Array[Long]] =
    (for( coord1 <- coordinates)
      yield (for (coord2 <- coordinates)
        yield math.hypot(coord2.x - coord1.x, coord2.y - coord1.y).toLong).toArray).toArray


  def initActorSystem(coordinates:Seq[Coordinates]):Seq[ActorRef] = {
    pingTable = createTable(coordinates)
    val system = ActorSystem("testSystem")
    coordinates.zip(0 until coordinates.length).map({
      case (coordinate,id) => system.actorOf(Props(classOf[FakeMain], id.toString, id))
    })
  }

  def createLinks(actorRefs : Seq[ActorRef]) = {
    for (actorRef <- actorRefs) {
      actorRef ! FirstContact(actorRefs(0))
    }
  }

}


class FakePing(core:ActorRef,main:ActorRef,id:Integer) extends Communication(core,main,id) {

     override def calculatePing(sendTime:Long,otherInfo:RPSInfo):Long={
       FakePing.pingTable(this.id)(otherInfo.id)
     }
}

class FakeMain(name : String, id : Int) extends Main(name, id) {

  override val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self, deltaConf), "VivaldiCore"+id)
  override val network = context.actorOf(Props(classOf[FakePing], vivaldiCore, self, id), "Network"+id)

}


