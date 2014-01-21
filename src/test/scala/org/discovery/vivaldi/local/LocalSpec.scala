package org.discovery.vivaldi.local

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto.{FirstContact, Coordinates}
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

 class LocalSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers{
  "An actor system " must {
    "work" in {
      /*
      To activate local test :
        -Uncomment the 2 lines below
        - Create your coordinates
       */
      var coordinates : Seq[(Coordinates, String)] = List(
        (new Coordinates(0,0), "Nantes"),
        (new Coordinates(0,1), "Rennes"),
        (new Coordinates(3,1), "Paris"),
        (new Coordinates(2,-3), "Lyon"),
        (new Coordinates(1,-5), "Toulouse"),
        (new Coordinates(5,2), "Reims"),
        (new Coordinates(-2,1), "Brest"))

      coordinates = FakePing.createClusters(coordinates)
      val actorRefs = FakePing.initActorSystem(coordinates)
      FakePing.createLinks(actorRefs)
    }
  }
}
