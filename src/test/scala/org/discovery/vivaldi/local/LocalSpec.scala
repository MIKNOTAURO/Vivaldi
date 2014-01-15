package org.discovery.vivaldi.local

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto.{FirstContact, Coordinates}

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
      val coordinates : Seq[Coordinates] = List(new Coordinates(0,1),new Coordinates(1,0), new Coordinates(0,0), new Coordinates(1,1), new Coordinates(0.5,0.5))
      //val actorRefs = FakePing.initActorSystem(coordinates)
      //FakePing.createLinks(actorRefs)
    }
  }
}
