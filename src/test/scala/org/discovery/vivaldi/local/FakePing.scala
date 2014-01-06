package org.discovery.vivaldi.local

import org.discovery.vivaldi.network.Communication
import akka.actor.ActorRef
import org.discovery.vivaldi.dto.Coordinates

/**
 * Created with IntelliJ IDEA.
 * User: raphael
 * Date: 1/6/14
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */

object FakePing {
  var pingTable : Array[Array[Long]] = Array()

  def createTable(coordinates:Seq[Coordinates]):Array[Array[Long]] = {
    null //TODO fix
  }

  def initActorSystem(coordinates:Seq[Coordinates]):Seq[ActorRef] = {
    null //TODO fix
  }

}


class FakePing(core:ActorRef,main:ActorRef) extends Communication(core,main) {
     override def receive = {
       case _ => super.receive //TODO check that this works
     }
}


