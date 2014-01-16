package org.discovery.vivaldi.local

import akka.actor.{ActorSystem, Props, ActorRef}
import org.discovery.vivaldi.dto.{FirstContact, RPSInfo, Coordinates}
import org.discovery.vivaldi.system.Main
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.network.Communication
import dispatch._
import scala.concurrent.{ExecutionContext}
import ExecutionContext.Implicits.global
import akka.event.slf4j.Logger
import scala.util.parsing.json.JSON
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

object FakePing {

  val log = Logger("Primary")
  var pingTable : Array[Array[Double]] = Array()
  var idNetwork = 0

  /**
   * Creates the table of pings from given coordinates
   * @param coordinates
   * @return
   */
  def createTable(coordinates:Seq[Coordinates]):Array[Array[Double]] =
    (for(coord1 <- coordinates)
      yield (for (coord2 <- coordinates)
        yield math.hypot(coord2.x - coord1.x, coord2.y - coord1.y)*100).toArray).toArray

  /**
   * Creates the network and the different nodes for local test
   * @param coordinates
   * @return
   */
  def initActorSystem(coordinates:Seq[Coordinates]):Seq[ActorRef] = {

    //Call monitoring to create network
    pingTable = createTable(coordinates)
    val system = ActorSystem("testSystem")
    val requestNetwork = url("http://vivaldi-monitoring-demo.herokuapp.com/networks/").POST << """{"networkName": "localTest22"}""" <:< Map("content-type" -> "application/json")
    val resultNetwork = Http(requestNetwork OK as.String).either
    var responseNetwork = ""
    resultNetwork() match {
      case Right(content)         => responseNetwork = content
      case Left(StatusCode(404))  => log.error("Not found")
      case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
      case _ => log.error("Error")
    }
    idNetwork = JSON.parseFull(responseNetwork).get.asInstanceOf[Map[String, Any]]
      .get("id").get.asInstanceOf[Double].toInt
    log.info(s"Id network : $idNetwork")

    //Create nodes
    coordinates.zip(0 until coordinates.length).map({
      case (coordinate,id) => {
        //call monitoring to create nodes
        val requestRegister = url("http://vivaldi-monitoring-demo.herokuapp.com/nodes/").POST << s"""{"nodeName": "$id", "networkId": $idNetwork}""" <:< Map("content-type" -> "application/json")
        val resultRegister = Http(requestRegister OK as.String).either
        var responseRegister = ""
        resultRegister() match {
          case Right(content)         => responseRegister = content
          case Left(StatusCode(404))  => log.error("Not found")
          case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
          case _ => log.error("Error")
        }
        val idNode = JSON.parseFull(responseRegister).get.asInstanceOf[Map[String, Any]]
          .get("id").get.asInstanceOf[Double].toInt
        log.info(s"Id node : $idNode")

        //call monitoring to initialize node
        val requestInit = url("http://vivaldi-monitoring-demo.herokuapp.com/initTimes/").POST << s"""{"nodeId": $idNode}""" <:< Map("content-type" -> "application/json")
        val resultInit = Http(requestInit OK as.String).either
        resultInit() match {
          case Right(content)         => log.info(s"Node $idNode initialized")
          case Left(StatusCode(404))  => log.error("Not found")
          case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
          case _ => log.error("Error")
        }

        //create actorRef representing the node
        system.actorOf(Props(classOf[FakeMain], idNode.toString, id))
      }
    })
  }

  def createClusters(list : Seq[Coordinates]) : Seq[Coordinates] = {
    var newList : List[Coordinates] = List()
    for (node <- list){
      newList ::= node
      val x = node.x
      val y = node.y
      for(i <- 1 to 10){
        newList ::= new Coordinates(x+Math.pow(-1, i)*Random.nextDouble()/10, y+Math.pow(-1, i)*Random.nextDouble()/10)
      }
    }
    Random.shuffle(newList)
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

}


class FakePing(core:ActorRef,main:ActorRef,id:Integer) extends Communication(core,main,id) {

  /**
   * Gives the ping from the ping table created in the function createTable
   * @param sendTime
   * @param otherInfo
   * @return
   */
   override def calculatePing(sendTime:Long,otherInfo:RPSInfo):Long={
    var ping = FakePing.pingTable(this.id)(otherInfo.id)
    ping += Random.nextDouble()/10*Math.pow(-1, Random.nextInt(10))*ping
    ping.toLong
   }
}

class FakeMain(name : String, id : Int) extends Main(name, id) {

  /**
   * Calls monitoring to update coordinates
   */
  override def updateMonitoring = {
    val x = coordinates.x
    val y = coordinates.y
    val requestInit = url("http://vivaldi-monitoring-demo.herokuapp.com/coordinates/").POST << s"""{"nodeId": $name, "x": $x, "y": $y}""" <:< Map("content-type" -> "application/json")
    val resultInit = Http(requestInit OK as.String).either
    resultInit() match {
      case Right(content)         => log.info("Update coordinates on monitoring "+content)
      case Left(StatusCode(404))  => log.error("Not found")
      case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
      case _ => log.error("Error")
    }
  }

  override val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self, deltaConf), "VivaldiCore"+id)
  override val network = context.actorOf(Props(classOf[FakePing], vivaldiCore, self, id), "Network"+id)

}


