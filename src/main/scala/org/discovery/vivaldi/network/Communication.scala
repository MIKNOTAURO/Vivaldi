package org.discovery.vivaldi.network

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import org.discovery.vivaldi.dto.{FirstContact, UpdatedRPS, RPSInfo, DoRPSRequest}
import scala.concurrent.Future
import akka.pattern.ask
import org.discovery.vivaldi.network.Communication.{NewRPS, Pong, Ping}
import scala.collection.mutable
import scala.util.{Success, Random}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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
  case class Ping(sendTime:Long)
  case class Pong(sendTime:Long,selfInfo:RPSInfo,rps:Iterable[RPSInfo])
  //NewRPS is used to update the RPS (in the "mix RPS" phase)
  case class NewRPS(rps:Iterable[RPSInfo])
}

class Communication(vivaldiCore: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  var rps: Iterable[RPSInfo] = Seq[RPSInfo]()

  //used when getting rps info
  implicit val pingTimeout = Timeout(5 seconds)

  var myInfo:RPSInfo= null

  def receive = {

    case Ping(sendTime) =>  sender ! Pong(sendTime,myInfo,rps)  // it's the reply to someone so you don't have to treat it
    case DoRPSRequest(newInfo:RPSInfo,numberOfNodesToContact) => {
      myInfo=newInfo
      contactNodes(numberOfNodesToContact)
    }
    case FirstContact(node) => rps =Seq(RPSInfo(node,null,null,1000000))// I don't know the system information here
    case NewRPS(newRPS) => rps = newRPS
    case _ => {
      log.info("Unknown message")
    }
  }

  /**
   * Shuffles our RPS with another RPS
   * modifies rps
   * @param other the other RPS to mix with
   * @return  the new RPS to give to the owner of the 'other' RPS
   */
  //TODO make this better
  def mixRPS(other:Iterable[RPSInfo]):Iterable[RPSInfo]={
    /**
     * This function takes 2 RPS lists r1 and r2 , of size n and m, and mixes together
     * this makes two new RPS lists (r1' and r2') of sizes floor((n+m)/2) and cieling((n+m)/2)
     */
    val returnRPS=mutable.Set[RPSInfo]()
    // we need to synchronize this huge block to make this thread-safe
    this.synchronized { //there's surely a more functional way to do this
      rps= Random.shuffle(Seq(myInfo)++rps)
      val thisSz = rps.size+1
      val otherSz= other.size
      val newSize= (thisSz+otherSz)/2
      val newRPS = mutable.Set[RPSInfo]()
      val thisIter = rps.iterator
      val otherIter= other.iterator
      for(i <- 0 until newSize){//we draw floor(n+m)/2 elements
        if(Math.random()<0.5){ //with equal probability
          if(thisIter.hasNext)
            newRPS.add(thisIter.next()) //fetch from r1
          else
            newRPS.add(otherIter.next())//(unless there're no more elements in r1)
        }else{
          if(otherIter.hasNext)
            newRPS.add(otherIter.next()) //or fetch from r2
          else
            newRPS.add(thisIter.next())
        }
      }
      rps=newRPS //udate r1
      //then put the rest of the elements into r2
      while(thisIter.hasNext){
        returnRPS.add(thisIter.next())
      }
      while(otherIter.hasNext){
        returnRPS.add(otherIter.next())
      }
    }
    returnRPS
  }



  def askPing(info:RPSInfo):Future[Any]= {
    //we ask, if it fails (like in a Timeout, notably), we instead return null
    ask(info.node,Ping(System.currentTimeMillis()))(10 seconds) fallbackTo Future(null)
  }

  def contactNodes(numberOfNodesToContact: Int) {
    log.debug(s"Order to contact $numberOfNodesToContact received")
    //seeing how the rps is shuffled on every update, no need to shuffle again
    val toContact:Iterable[RPSInfo]=rps.take(numberOfNodesToContact)

    //list of contacts we'll need to make to update the RPS
    val asks = toContact.map(askPing)

    //this is the callback to execute when recieving an answer from an RPS request
    val contactedNodes:Iterable[Future[RPSInfo]] = for{//this is an Iterable for-expression
      ask <- asks
    } yield {
      for{//this is a Future for-comprehension, we can't mix with the previous one
        result <- ask
        if result != null //the askPing method returns null when there's a contact failure, so we filter it out
        Success(Pong(sendTime,selfInfo,otherRPS)) = result //if it doesn't fail it returns a Pong
      } yield {
        val pingTime = System.currentTimeMillis()-sendTime
        val newSenderRPS = this.mixRPS(otherRPS)
        sender ! NewRPS(newSenderRPS)
        selfInfo.copy(ping = pingTime)//we give the same info, but update the diff
      }
    }


    val allAsks = Future.sequence(asks)
    allAsks onComplete {
      case scala.util.Success(newInfos:Iterable[RPSInfo]) => {
        log.debug("RPS request completed")
        log.debug("Sending new RPS to vivaldi core")
        vivaldiCore ! UpdatedRPS(newInfos.filter((selfInfo:RPSInfo) => selfInfo!=null)) //only send RPSInfo with good info
      }
      case _ => {
        //this should never run, since asks always succeed in theory
        log.error("RPS request failed! Code logic error")
      }
    }
  }
}



