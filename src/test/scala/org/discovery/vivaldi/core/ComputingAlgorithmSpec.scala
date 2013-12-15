package org.discovery.vivaldi.core

import akka.testkit.{TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto._
import scala.math.sqrt
import org.discovery.vivaldi.system.Main
import scala.util.Random

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

class ComputingAlgorithmSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers {

  "The core actor for the computing algorithm" must {
    val mainActorOne = TestActorRef[Main]
    val mainActorTwo = TestActorRef[Main]
    val mainActorThree = TestActorRef[Main]

    val rpsOne = RPSInfo(mainActorOne.underlyingActor.network,SystemInfo(4,1024),Coordinates(3,-4),10)
    val rpsTwo = RPSInfo(mainActorTwo.underlyingActor.network,SystemInfo(2,512),Coordinates(3,-4),10)
    val rpsThree = RPSInfo(mainActorThree.underlyingActor.network,SystemInfo(8,2048),Coordinates(-4.5,6),10)
    val newRpsTable = Seq(rpsTwo,rpsThree)

    val algorithmTestOne: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActorOne), "TestCoreActorOne")
    val algorithmTestTwo: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActorTwo), "TestCoreActorTwo")
    val algorithmTestThree: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActorThree), "TestCoreActorThree")

    "Find the direction towards which it will move its coordinates" in {
      val a = 3.0
      val b = -4.0
      assertResult(Coordinates(0.6, -0.8)) {
        algorithmTestOne.underlyingActor.findDir(a, b)
      }
    }

    "Never give an empty direction even if the remote node is at the same location" in {
      val c = 0.0
      val d = 0.0
      assertResult(false) {
        algorithmTestOne.underlyingActor.findDir(c, d).equals(Coordinates(0.0, 0.0))
      }
    }

    "Always find an unit vector for this direction" in {
      val e = new Random().nextDouble()
      val f = new Random().nextDouble()
      assertResult(1) {
        Math.rint(algorithmTestOne.underlyingActor.findDir(e, f).length()*100)/100
      }
    }

    "Compute its new vivaldi coordinates for a single RPS info" in {
      assertResult(Coordinates(-1.5, 2.0)){
        algorithmTestTwo.underlyingActor.computeOne(rpsOne)
      }
    }

    "Compute its new vivaldi coordinates for multiple RPS info" in {
      // By default the coordinates of the actor is (0,0)
      assertResult(Coordinates(0, 0)){
        algorithmTestThree.underlyingActor.compute(newRpsTable)
      }
    }
  }
}
