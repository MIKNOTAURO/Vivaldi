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

/**
 * A trait used to tag messages that need to be forwarded to the Communication actor
 */
trait CommunicationMessage

object Communication{
  // Ping/Pong are used in the RPS update process, to measure ping and recover new RPSs
  case class Ping(sendTime: Long, selfInfo: RPSInfo) extends CommunicationMessage
  case class Pong(sendTime: Long,selfInfo: RPSInfo,rps: Iterable[RPSInfo]) extends CommunicationMessage
  //NewRPS is used to update the RPS (in the "mix RPS" phase)
  case class NewRPS(rps: Set[RPSInfo]) extends CommunicationMessage
}

class Communication(id: Long, vivaldiCore: ActorRef, main: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  var rps = Set[RPSInfo]()

  val rpsSize = context.system.settings.config.getConfig("vivaldi.system").getInt("communication.rpssize")

  //used when getting rps info
  implicit val pingTimeout = Timeout(5 seconds)

  //TODO set systemInfo
  var myInfo:RPSInfo = RPSInfo(id, self,Coordinates(0,0),23)//the ping in myInfo isn't used


  def receive = {

    case ping: Ping => receivePing(ping)
    case DoRPSRequest(newInfo: RPSInfo,numberOfNodesToContact) => {
      myInfo = newInfo  // we use RPSInfo to propagate new systemInfo and coordinates
      contactNodes(numberOfNodesToContact)
    }
    case FirstContact(node) => rps = Set(RPSInfo(id, node,Coordinates(11,11),1000000))
    case NewRPS(newRPS) => rps = newRPS
    case msg => {
      log.info(s"Unknown Message: $msg")
    }
  }

  def receivePing(ping: Ping) {
    //size check is necessary to make sure our rps grows at one point (this will make our rps initially very self-biased.
    rps = Random.shuffle(rps + ping.selfInfo).take(rpsSize)
    sender ! Pong(ping.sendTime, myInfo, rps)
  }

  def mixRPS(rpsList: Set[Pong]): Set[RPSInfo] = {
    val rpses = rpsList.flatMap {
      _.rps
    }
    Random.shuffle(rpses ++ rps).take(rpsSize)
  }

  //overwritten in fake ping class
  def calculatePing(sendTime:Long,otherInfo:RPSInfo):Long = {
    System.currentTimeMillis()-sendTime
  }

  def contactNodes(numberOfNodesToContact: Int) {
    log.debug(s"Order to contact $numberOfNodesToContact received")
    val toContact = rps.take(Math.min(rps.size, numberOfNodesToContact))
    val asks = toContact map askPing

    val updatedAsks: Iterable[Future[Option[Pong]]] = asks.map {
      ask =>
        ((for {
          result <- ask
          if result != null
          Pong(sendTime, otherInfo, otherRPS) = result
        } yield {
          val pingTime = calculatePing(sendTime,otherInfo)
          val newOtherInfo = otherInfo.copy(ping = pingTime)
          Some(result.copy(selfInfo = newOtherInfo)) //update info of ping'd guy
        }):Future[Option[Pong]]).recover({case _ => None }) // if None, then no response was recieved
    }

    val allAsks = Future sequence updatedAsks

    allAsks onComplete {
      case Success(newInfos: Set[Option[Pong]]) => {
        //filter out all the non-responses
        val filteredNewInfos=newInfos.filter(x => x.isDefined).map(x=>x.get)
        val newRPS = filteredNewInfos.map(_.selfInfo)
        vivaldiCore ! UpdatedRPS(newRPS)
        rps = mixRPS(filteredNewInfos)
      }
      case x => log.error("RPS request failed! "+x)
    }
  }

   //refactored this out to be able to easily create asks
  def singleAsk(info:RPSInfo):Future[Any] ={
    ask(info.node,Ping(System.currentTimeMillis(),myInfo))(10 seconds) fallbackTo Future(null)
  }

  def askPing(info:RPSInfo): Future[Pong]= {
    //we ask, if it fails (like in a Timeout, notably), we instead return null
    val future = singleAsk(info)
    future.map {
      result =>
        if (result.isInstanceOf[Pong]) {
          result.asInstanceOf[Pong]
        }
        else {

          log.error("Can't figure out response type ")
          main ! DeleteCloseNode(info)
          null
        }
    }
  }
}



