package org.discovery.vivaldi.local

import akka.actor.{ActorSystem, Props, ActorRef}
import org.discovery.vivaldi.dto.{FirstContact, RPSInfo, Coordinates}
import org.discovery.vivaldi.system.Main
import org.discovery.vivaldi.core.ComputingAlgorithm
import org.discovery.vivaldi.network.Communication


/**
 * Created with IntelliJ IDEA.
 * User: raphael
 * Date: 1/6/14
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */

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


