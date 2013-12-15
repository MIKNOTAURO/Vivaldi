package org.discovery.vivaldi.system

import akka.testkit.{TestActorRef, TestKit}
import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto._
import org.discovery.vivaldi.dto.Coordinates
import org.discovery.vivaldi.dto.RPSInfo
import org.discovery.vivaldi.dto.UpdatedCoordinates
import org.discovery.vivaldi.dto.SystemInfo
import scala.math.sqrt

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

class MainSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers {

  "The system actor for the close nodes list generation and the API" must {
    // Creation of the TestActorRef
    val testMainActor = TestActorRef[Main]

    val mainActorOne = TestActorRef[Main]
    val mainActorTwo = TestActorRef[Main]
    val mainActorThree = TestActorRef[Main]

    val rpsOne = RPSInfo(mainActorOne.underlyingActor.network,SystemInfo(4,1024),Coordinates(1,0),10)
    val rpsTwo = RPSInfo(mainActorTwo.underlyingActor.network,SystemInfo(2,512),Coordinates(3,1),20)
    val rpsThree = RPSInfo(mainActorThree.underlyingActor.network,SystemInfo(8,2048),Coordinates(3,4),50)
    val newRpsTable = Seq(rpsOne,rpsTwo,rpsThree)

    val newCoordinates = Coordinates(1,1)

    val closeNodeOne = CloseNodeInfo(mainActorOne.underlyingActor.network,SystemInfo(4,1024),Coordinates(1,0),1)
    val closeNodeTwo = CloseNodeInfo(mainActorTwo.underlyingActor.network,SystemInfo(2,512),Coordinates(3,1),2)
    val closeNodeThree = CloseNodeInfo(mainActorThree.underlyingActor.network,SystemInfo(8,2048),Coordinates(3,4),sqrt(13))
    val closeNodesToBe = Seq(closeNodeOne,closeNodeTwo,closeNodeThree)

    "Compute the distance between two points" in {
      val a = Coordinates(1,1)
      val b = Coordinates(2,2)
      assertResult(sqrt(2)){
        testMainActor.underlyingActor.computeDistanceBtw(a,b)
      }
    }

    "Compute the distance to Self" in {
      // By default the coordinates of the actor is (0,0)
      val b = Coordinates(2,0)
      assertResult(2){
        testMainActor.underlyingActor.computeDistanceToSelf(b)
      }
    }

    "Receive and Handle \"UpdatedCoordinates\" messages " in {
      testMainActor.receive(UpdatedCoordinates(newCoordinates,newRpsTable))
      assert(testMainActor.underlyingActor.coordinates == Coordinates(1,1))
      assert(testMainActor.underlyingActor.closeNodes == closeNodesToBe)
    }

    "Be able to retrieve the closest node to self" in {
      assertResult(List(closeNodeOne)){
        testMainActor.underlyingActor.getCloseNodesToSelf(Set(),1)
      }
    }

    "Be able to retrieve the closest node to self excluding another one" in {
      assertResult(List(closeNodeTwo)){
        testMainActor.underlyingActor.getCloseNodesToSelf(Set(closeNodeOne),1)
      }
    }

    "Be able to retrieve close nodes to another node" in {
      assertResult(List(closeNodeTwo)){
        testMainActor.underlyingActor.getCloseNodesFrom(closeNodeThree,Set(closeNodeOne),1)
      }
    }

    "Be able to delete a node from the close node list" in {
      testMainActor.underlyingActor.deleteCloseNode(rpsThree)
      assert(Seq(closeNodeOne,closeNodeTwo) == testMainActor.underlyingActor.closeNodes)
    }
  }

  "The main actor for initialization" must {

  }

}
