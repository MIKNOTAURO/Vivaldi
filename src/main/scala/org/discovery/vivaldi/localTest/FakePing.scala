package org.discovery.vivaldi.local

import akka.actor._
import org.discovery.vivaldi.system.{VivaldiActor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.network.Communication
import scala.concurrent.{ExecutionContext}
import akka.event.slf4j.Logger
import scala.util.Random
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.FirstContact
import org.discovery.vivaldi.dto.RPSInfo

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

  val log = Logger("Primary")
  var pingTable : Array[Array[Double]] = Array()
  var coordinatesSeq : Seq[(Coordinates, String)] = Seq()
  val system : ActorSystem = ActorSystem("testSystem")
  var actorRefs : Seq[ActorRef] = Seq()

  /**
   * Creates the table of pings from given coordinates
   * @param coordinates
   * @return
   */
  def createTable(coordinates:Seq[(Coordinates, String)]) : Array[Array[Double]] =
    (for(coord1 <- coordinates)
      yield (for (coord2 <- coordinates)
        yield math.hypot(coord2._1.x - coord1._1.x, coord2._1.y - coord1._1.y)*100).toArray).toArray

  /**
   * Creates the network and the different nodes for local test
   * @param coordinates
   * @return
   */
  def initActorSystem(coordinates:Seq[(Coordinates, String)]) : Seq[ActorRef] = {

    //Call monitoring to create network
    pingTable = createTable(coordinates)

    //Create nodes
    coordinates.zip(0 until coordinates.length).map({
      case (coordinate,id) => {
        //create actorRef representing the node
        system.actorOf(Props(classOf[FakeMain], coordinate._2, id.toLong))
      }
    })
  }

  def createClusters(list : Seq[(Coordinates, String)]) : Seq[(Coordinates, String)] = {
    var newList : List[(Coordinates, String)] = List()
    for (node <- list){
      newList ::= node
      for(i <- 1 to 5){
        newList ::= createRandomNode(node, i)
      }
    }
    coordinatesSeq = newList
    //Random.shuffle(newList)
    newList
  }

  /**
   * Creates a node near (+/- 10%) the given node
   * @param node
   * @param index
   * @return
   */
  def createRandomNode(node : (Coordinates, String), index : Int) : (Coordinates, String) = {
    val x = node._1.x
    val y = node._1.y
    val name = node._2
    (new Coordinates(x+Math.pow(-1, index)*Random.nextDouble()/10, y+Math.pow(-1, index)*Random.nextDouble()/10),
      name+index)
  }

  /**
   * Gives FirstContacts for the created nodes
   * @param actorRefs
   */
  def createLinks(actorRefs : Seq[ActorRef]) = {
    for (actorRef <- actorRefs) {
      actorRef ! FirstContact(actorRefs(0))
    }
  }

  def createNewNode(index : Int) = {
    val randomIndex = (Random.nextDouble()*(coordinatesSeq.length-1)).toInt
    var newNode = coordinatesSeq(randomIndex)
    newNode = (newNode._1, newNode._2 + s"New$index" )
    coordinatesSeq ++= List(createRandomNode(newNode, index))
    pingTable = createTable(coordinatesSeq)

    //create actorRef representing the node
    val newActorRef = system.actorOf(Props(classOf[FakeMain], newNode._2, (coordinatesSeq.length-1).toLong))
    actorRefs ++= List(newActorRef)
    newActorRef ! FirstContact(actorRefs(0))
  }

  def addAndDelete(nodes : Seq[ActorRef]) = {
    actorRefs = nodes
    Thread.sleep(10000)
    var index = 1
    while(index < 10){
      createNewNode(index)
      index += 1
      Thread.sleep(5000)
    }
    for(i <- 1 to 5) {
      nodes(i) ! PoisonPill
    }
    while(index < 20){
      createNewNode(index)
      index += 1
      Thread.sleep(10000)
    }
  }

}


class FakePing(id:Long, core:ActorRef, main:ActorRef) extends Communication(id, core, main) {

  /**
   * Gives the ping from the ping table created in the function createTable
   * @param sendTime
   * @param otherInfo
   * @return
   */
   override def calculatePing(sendTime:Long,otherInfo:RPSInfo):Long={
    var ping = FakePing.pingTable(this.id.toInt)(otherInfo.id.toInt)
    ping += Random.nextDouble()/10*Math.pow(-1, Random.nextInt(10))*ping
    ping.toLong
   }
}

class FakeMain(name : String, id : Long) extends VivaldiActor(name, id) {

  override val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self, deltaConf), "VivaldiCore"+id)
  override val network = context.actorOf(Props(classOf[FakePing], id, vivaldiCore, self), "Network"+id)

}


