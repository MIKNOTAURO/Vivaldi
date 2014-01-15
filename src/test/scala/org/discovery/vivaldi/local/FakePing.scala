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


/**
 * Created with IntelliJ IDEA.
 * User: raphael
 * Date: 1/6/14
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */

object FakePing {

  val log = Logger("Primary")
  var pingTable : Array[Array[Long]] = Array()
  var idNetwork = 0

  def createTable(coordinates:Seq[Coordinates]):Array[Array[Long]] =
    (for( coord1 <- coordinates)
      yield (for (coord2 <- coordinates)
        yield math.hypot(coord2.x - coord1.x, coord2.y - coord1.y).toLong).toArray).toArray


  def initActorSystem(coordinates:Seq[Coordinates]):Seq[ActorRef] = {
    pingTable = createTable(coordinates)
    val system = ActorSystem("testSystem")
    val myRequest = url("http://vivaldi-monitoring-demo.herokuapp.com/networks/").POST << """{"networkName": "localTest3"}""" <:< Map("content-type" -> "application/json")
    val result = Http(myRequest OK as.String).either
    var response = ""
    result() match {
      case Right(content)         => response = content
      case Left(StatusCode(404))  => log.error("Not found")
      case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
      case _ => log.error("Error")
    }
    idNetwork = JSON.parseFull(response).get.asInstanceOf[Map[String, Any]]
      .get("id").get.asInstanceOf[Double].toInt
    log.info(s"Id network : $idNetwork")
    coordinates.zip(0 until coordinates.length).map({
      case (coordinate,id) => {
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
        val requestInit = url("http://vivaldi-monitoring-demo.herokuapp.com/initTimes/").POST << s"""{"nodeId": $idNode}""" <:< Map("content-type" -> "application/json")
        val resultInit = Http(requestInit OK as.String).either
        resultInit() match {
          case Right(content)         => log.info(s"Node $idNode initialized")
          case Left(StatusCode(404))  => log.error("Not found")
          case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
          case _ => log.error("Error")
        }
        system.actorOf(Props(classOf[FakeMain], idNode.toString, id))
      }
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


