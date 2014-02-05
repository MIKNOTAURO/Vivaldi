package org.discovery.vivaldi.system

import akka.testkit.TestActorRef
import org.discovery.vivaldi.dto.{nodeInfo, CloseNodeInfo, RPSInfo, Coordinates}
import akka.actor.ActorSystem

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

object VivaldiActorSpec extends App {

  implicit val system = ActorSystem("testSystem")
  val testMainActor: TestActorRef[VivaldiActor] = TestActorRef[VivaldiActor](new VivaldiActor("0", 0))

  val coordinate: Coordinates = Coordinates(-16.82321256098785, -123.9839475699903)

  val rps: List[RPSInfo] = List(
    RPSInfo("parapide-4".hashCode, system.deadLetters, Coordinates(-24.880416064243878, -123.33776621282207), 2),
    RPSInfo("suno-7".hashCode, system.deadLetters, Coordinates(-1.4429409913939262, -128.02808677041514), 22),
    RPSInfo("taurus-2".hashCode, system.deadLetters, Coordinates(5.626330661826955, -119.51191208825271), 16),
    RPSInfo("suno-6".hashCode, system.deadLetters, Coordinates(-2.292592859591011, -129.2297869373219), 23),
    RPSInfo("edel-39".hashCode, system.deadLetters, Coordinates(-13.740941249180738, -148.4299233889106), 18),
    RPSInfo("suno-8".hashCode, system.deadLetters, Coordinates(-3.8799486374887984, -130.74454886344256), 23),
    RPSInfo("parapide-3".hashCode, system.deadLetters, Coordinates(-20.359955655731724, -123.33086194832438), 1),
    RPSInfo("taurus-2".hashCode, system.deadLetters, Coordinates(5.626330661826955, -119.51191208825271), 29),
    RPSInfo("taurus-7".hashCode, system.deadLetters, Coordinates(2.605412996206159, -121.18845644119841), 15),
    RPSInfo("suno-9".hashCode, system.deadLetters, Coordinates(1.571320255636714, -137.12951092425186), 22)
  )

  var closeNodes: List[CloseNodeInfo] = List(
//    CloseNodeInfo("parapide-8".hashCode, system.deadLetters, Coordinates(1.444402236560414, 5.20798219588905), 0.0),
//    CloseNodeInfo("parapide-8".hashCode, system.deadLetters, Coordinates(1.444402236560414, 5.20798219588905), 0.0),
    CloseNodeInfo("taurus-15".hashCode, system.deadLetters, Coordinates(14.050237543644672, -31.05732538392686), 0.0),
    CloseNodeInfo("suno-34".hashCode, system.deadLetters, Coordinates(-13.987179754073892, -163.19627851148306), 0.0),
    CloseNodeInfo("parapide-9".hashCode, system.deadLetters, Coordinates(-45.79765142294397, -115.91618218327183), 0.0),
    CloseNodeInfo("tsaurus-16".hashCode, system.deadLetters, Coordinates(-24.04342970189931, -134.3229371691431), 0.0),
    CloseNodeInfo("edel-40".hashCode, system.deadLetters, Coordinates(-24.89197085858956, -141.93547918562885), 0.0),
    CloseNodeInfo("taurus-4".hashCode, system.deadLetters, Coordinates(-21.20299163761686, -124.56129879035849), 0.0),
    CloseNodeInfo("parapide-3".hashCode, system.deadLetters, Coordinates(-20.359955655731724, -123.33086194832438), 3.596536187423287),
    CloseNodeInfo("parapide-4".hashCode, system.deadLetters, Coordinates(-24.880416064243878, -123.33776621282207), 8.083073588631553),
    CloseNodeInfo("suno-8".hashCode, system.deadLetters, Coordinates(-3.8799486374887984, -130.74454886344256), 14.602527549790153),
    CloseNodeInfo("suno-6".hashCode, system.deadLetters, Coordinates(-2.292592859591011, -129.2297869373219), 15.448551374626295),
    CloseNodeInfo("suno-7".hashCode, system.deadLetters, Coordinates(-1.4429409913939262, -128.02808677041514), 15.90307565934565),
    CloseNodeInfo("taurus-7".hashCode, system.deadLetters, Coordinates(2.605412996206159, -121.18845644119841), 19.62871013828485),
    CloseNodeInfo("suno-9".hashCode, system.deadLetters, Coordinates(1.571320255636714, -137.12951092425186), 22.60895117962756),
    CloseNodeInfo("taurus-2".hashCode, system.deadLetters, Coordinates(5.626330661826955, -119.51191208825271), 22.890633286629495),
    CloseNodeInfo("taurus-2".hashCode, system.deadLetters, Coordinates(5.626330661826955, -119.51191208825271), 22.890633286629495),
    CloseNodeInfo("edel-39".hashCode, system.deadLetters, Coordinates(-13.740941249180738, -148.4299233889106), 24.63952374090913)
  )


  val listOfIds = List(
    "parapide-4","suno-7","taurus-2","suno-6","edel-39","suno-8","parapide-3","taurus-2","taurus-7","suno-9",
    "parapide-8","parapide-8","taurus-15","parapide-6","suno-34","parapide-9","tsaurus-16","edel-40","taurus-4",
    "parapide-3","parapide-4","suno-8","suno-6","suno-7","taurus-7","suno-9","taurus-2","taurus-2","edel-39")


  // buggy version on G5K
  testMainActor.underlyingActor.coordinates = coordinate
  // corrected version
  closeNodes = closeNodes.map(cni => CloseNodeInfo(cni.id, cni.node, cni.coordinates, testMainActor.underlyingActor.computeDistanceToSelf(cni.coordinates)))

  testMainActor.underlyingActor.closeNodes = closeNodes

  val closest = testMainActor.underlyingActor.getCloseNodesToSelf(Set(new nodeInfo {
    override val id = "paradent-8".hashCode.toLong
    val node = system.deadLetters
    val coordinates = null
  }), 1)

  println(closeNodes.sorted.take(5))
  println(s"closest: $closest")

  listOfIds.foreach(id =>
    println(s"$id => ${id.hashCode}")
  )




}
