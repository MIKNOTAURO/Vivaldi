package org.discovery.vivaldi.network

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import org.discovery.vivaldi.dto._
import scala.concurrent.Future
import akka.pattern.ask
import scala.util.Random
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.discovery.vivaldi.network.Communication.Ping
import org.discovery.vivaldi.dto.DoRPSRequest
import org.discovery.vivaldi.network.Communication.Pong
import org.discovery.vivaldi.network.Communication.NewRPS
import scala.util.Success
import org.discovery.vivaldi.dto.FirstContact
import org.discovery.vivaldi.dto.UpdatedRPS
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


object Communication{
  // Ping/Pong are used in the RPS update process, to measure ping and recover new RPSs
  case class Ping(sendTime:Long, selfInfo: RPSInfo)
  case class Pong(sendTime:Long,selfInfo:RPSInfo,rps:Iterable[RPSInfo])
  //NewRPS is used to update the RPS (in the "mix RPS" phase)
  case class NewRPS(rps:Iterable[RPSInfo])
}

class Communication(vivaldiCore: ActorRef, main: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  var rps: Iterable[RPSInfo] = Seq[RPSInfo]()

  val rpsSize = 100 //TODO choose a number

  //used when getting rps info
  implicit val pingTimeout = Timeout(5 seconds)

  //TODO set systemInfo
  var myInfo:RPSInfo= RPSInfo(self,Coordinates(0,0),0)//the ping in myInfo isn't used


  def receive = {

    case ping: Ping => receivePing(ping)
    case DoRPSRequest(newInfo:RPSInfo,numberOfNodesToContact) => {
      myInfo=newInfo  // we use RPSInfo to propagate new systemInfo and coordinates
      contactNodes(numberOfNodesToContact)
    }
    case FirstContact(node) => rps =Seq(RPSInfo(node,null,1000000))// I don't know the system information here
    case NewRPS(newRPS) => rps = newRPS
    case _ => {
      log.info("Unknown message")
    }
  }

  def receivePing(ping: Ping) {
    sender ! Pong(ping.sendTime, myInfo, rps)
    rps = Random.shuffle(ping.selfInfo +: rps.tail.toSeq) //replacing the first element of the rps by the pinger and shuffling all that
  }

  def mixRPS(rpsList: Iterable[Pong]): Iterable[RPSInfo] = {
    Random.shuffle(rpsList.flatMap(_.rps)).take(rpsSize)
  }

  def contactNodes(numberOfNodesToContact: Int) {
    log.debug(s"Order to contact $numberOfNodesToContact received")
    val toContact = rps.take(numberOfNodesToContact)
    val asks= toContact map askPing

    val updatedAsks: Iterable[Future[Pong]] = asks.map { ask =>
      for {
        result <- ask
        if result != null
        Pong(sendTime,selfInfo,otherRPS) = result
        if selfInfo != null
      } yield {
        val pingTime = System.currentTimeMillis()-sendTime
        val newSelfInfo = selfInfo.copy(ping = pingTime)
        result.copy(selfInfo = newSelfInfo)
      }
    }

    val allAsks = Future sequence updatedAsks

    allAsks onComplete {
      case Success(newInfos: Iterable[Pong]) => {
        val newRPS = newInfos.map(_.selfInfo)
        vivaldiCore ! UpdatedRPS(newRPS)
        rps = mixRPS(newInfos)
      }
      case _ => log.error("RPS request failed!")
    }
  }


  def askPing(info:RPSInfo): Future[Pong]= {
    //we ask, if it fails (like in a Timeout, notably), we instead return null
    val future = ask(info.node,Ping(System.currentTimeMillis(), myInfo))(10 seconds) fallbackTo Future(null)
    future.map{result =>
      if (result.isInstanceOf[Pong]) {
        result.asInstanceOf[Pong]
      }
      else {
        log.error("Can't figure out response type")
        main ! DeleteCloseNode(info)
        null
      }
    }
  }
}



