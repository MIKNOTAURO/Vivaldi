package org.discovery.vivaldi.core

import akka.testkit.{TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto._
import scala.math.sqrt
import org.discovery.vivaldi.system.VivaldiActor
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
    val mainActor: TestActorRef[VivaldiActor] = TestActorRef[VivaldiActor](new VivaldiActor("0", 0))

    val rpsOne = RPSInfo(1, mainActor.underlyingActor.network,Coordinates(3,-4),10)
    val rpsTwo = RPSInfo(2, mainActor.underlyingActor.network,Coordinates(-4.5,6),10)
    val rpsTableOne = Seq(rpsOne, rpsTwo)
    val rpsTableTwo = Seq(rpsOne, rpsTwo, rpsOne)

    val deltaConf = 0.5
    val algorithmTestOne: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActor, deltaConf), "TestCoreActorOne")
    val algorithmTestTwo: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActor, deltaConf), "TestCoreActorTwo")
    val algorithmTestThree: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActor, deltaConf), "TestCoreActorThree")
    val algorithmTestFour: TestActorRef[ComputingAlgorithm] = TestActorRef(Props(classOf[ComputingAlgorithm], mainActor, deltaConf), "TestCoreActorFour")

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
        algorithmTestOne.underlyingActor.computeOne(rpsOne)
      }
    }

    "Compute its new vivaldi coordinates for multiple RPS info" in {
      // By default the coordinates of the actor is (0,0)
      assertResult(Coordinates(0, 0)){
        algorithmTestTwo.underlyingActor.compute(rpsTableOne)
      }
    }

    "Compute its new vivaldi coordinates for multiple RPS info sent by message" in {
      // By default the coordinates of the actor is (0,0)
      assertResult(true){
        algorithmTestThree.underlyingActor.receive(UpdatedRPS(rpsTableOne))
        algorithmTestThree.underlyingActor.coordinates.equals(Coordinates(0, 0))
      }
      mainActor.underlyingActor.schedulerRPSFirst.cancel()
      mainActor.underlyingActor.schedulerRPSThen.cancel()
    }

    "Send its new coordinates to the main actor" in {
      // By default the coordinates of the actor is (0,0)
      assertResult(true){
        algorithmTestFour.underlyingActor.receive(UpdatedRPS(rpsTableTwo))
        mainActor.underlyingActor.coordinates.equals(Coordinates(-1.5, 2))
      }
      mainActor.underlyingActor.schedulerRPSFirst.cancel()
      mainActor.underlyingActor.schedulerRPSThen.cancel()
    }
  }
}
