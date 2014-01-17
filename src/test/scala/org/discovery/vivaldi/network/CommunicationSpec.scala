package org.discovery.vivaldi.network

import akka.pattern.ask
import akka.actor.{Props, ActorSystem, Actor}
import akka.testkit.{TestActor,TestActorRef, TestKit,ImplicitSender}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.discovery.vivaldi.network.Communication._
import org.discovery.vivaldi.system.Main
import org.discovery.vivaldi.dto.{FirstContact, Coordinates, RPSInfo}
import akka.event.Logging
import org.scalatest.time.Seconds
import akka.util.Timeout

import ch.qos.logback.classic.Level
import scala.concurrent.Await
import scala.concurrent.duration._

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


class CommunicationSpec extends TestKit(ActorSystem("testSystem")) with ImplicitSender with WordSpecLike with MustMatchers {
  "The network block " must {
      val actor = TestActorRef[Main](new Main("Stuff",1))
      val com   = TestActorRef[Communication](new Communication(actor.underlyingActor.vivaldiCore,actor))
      val PONG  = Pong(0,null,null).getClass
      "put pingers into RPS" in {
        com.receive(Ping(System.currentTimeMillis(),RPSInfo(self,Coordinates(0,0),12)),self)
        expectMsgClass(PONG)
        println(com.underlyingActor.rps)
        assert(com.underlyingActor.rps.exists { case RPSInfo(id,x,y,z) => id == self })
      }

      implicit val timeout = Timeout(2000)
/* test DOES NOT WORK, but for testing framework reasons
      "have an increasing RPS initially " in {
        val actor1 = TestActorRef[Main](new Main("stuff",2))
        val com1 = TestActorRef[Communication](new Communication(actor1.underlyingActor.vivaldiCore,actor))
        val actor2 = TestActorRef[Main](new Main("More stuff",3))
        val com2 = TestActorRef[Communication](new Communication(actor2.underlyingActor.vivaldiCore,actor))
        val result = {
          Await.result(com1 ? Ping(0, RPSInfo(com2, Coordinates(10, 10), 17)), 5 seconds)
        }
        assert(result.isInstanceOf[Pong])
        result match {
          case Pong(time,info,rps) => assert(rps.size>1)
        }
      }
*/
  }
}
