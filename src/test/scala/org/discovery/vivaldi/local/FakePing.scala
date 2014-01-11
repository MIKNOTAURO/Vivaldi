package org.discovery.vivaldi.local

import org.discovery.vivaldi.network.Communication
import akka.actor.ActorRef
import org.discovery.vivaldi.dto.{RPSInfo, Coordinates}
import org.discovery.vivaldi.network.Communication.Ping


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
        yield math.hypot(coord2.x-coord1.x,coord2.y-coord1.y).toLong).toArray).toArray


  def initActorSystem(coordinates:Seq[Coordinates]):Seq[ActorRef] = {
    createTable(coordinates)
    coordinates.zip(0 until coordinates.length).map({case (coord,id) => null.asInstanceOf[ActorRef] })//TODO
  }

}


class FakePing(core:ActorRef,main:ActorRef,id:Integer) extends Communication(core,main,id) {
     override def calculatePing(sendTime:Long,otherInfo:RPSInfo):Long={
       FakePing.pingTable(this.id)(otherInfo.id)
     }
}



