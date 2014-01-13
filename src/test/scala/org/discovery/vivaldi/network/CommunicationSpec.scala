package org.discovery.vivaldi.network


import akka.actor.{Props, ActorSystem, Actor}
import akka.testkit.{TestActor,TestActorRef, TestKit,ImplicitSender}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.discovery.vivaldi.network.Communication._
import org.discovery.vivaldi.system.Main
import org.discovery.vivaldi.dto.{Coordinates, RPSInfo}
import akka.event.Logging

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
      val actor = TestActorRef[Main]
      val com   = TestActorRef[Communication](new Communication(actor.underlyingActor.vivaldiCore,actor))
      val PONG  = Pong(0,null,null).getClass
      "put pingers into RPS" in {
        com.receive(Ping(System.currentTimeMillis(),RPSInfo(self,Coordinates(0,0),12)),self)
        expectMsgClass(PONG)
        println(com.underlyingActor.rps)
        assert(com.underlyingActor.rps.exists { case RPSInfo(id,x,y) => id == self })
      }

      "have an increasing RPS initially " in {
        val com1 = TestActorRef[Communication](new Communication(actor.underlyingActor.vivaldiCore,actor))
        val com2 = TestActorRef[Communication](new Communication(actor.underlyingActor.vivaldiCore,actor))
        com2.receive(Ping(System.currentTimeMillis(),RPSInfo(com1,Coordinates(10,10),17)),com1)
        assert(com2.underlyingActor.rps.size>1)
      }
  }
}
