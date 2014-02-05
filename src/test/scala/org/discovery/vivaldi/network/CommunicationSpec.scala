package org.discovery.vivaldi.network

import akka.actor.{ActorSystem}
import akka.testkit.{TestActorRef, TestKit,ImplicitSender}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.discovery.vivaldi.network.Communication._
import org.discovery.vivaldi.system.{VivaldiActor}
import org.discovery.vivaldi.dto.{Coordinates, RPSInfo}
import akka.util.Timeout


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
      val actor = TestActorRef[VivaldiActor](new VivaldiActor("Stuff",1))
      val com   = TestActorRef[Communication](new Communication(1, actor.underlyingActor.vivaldiCore,actor))
      val PONG  = Pong(0,null,null).getClass
      "put pingers into RPS" in {
        com.receive(Ping(System.currentTimeMillis(),RPSInfo(1, self,Coordinates(0,0),12)),self)
        expectMsgClass(PONG)
        println(com.underlyingActor.rps)
        assert(com.underlyingActor.rps.exists { case RPSInfo(id,x,y,z) => id == 1 }) // id is the id given to the actor
      }

      implicit val timeout = Timeout(2000)
  }

}
