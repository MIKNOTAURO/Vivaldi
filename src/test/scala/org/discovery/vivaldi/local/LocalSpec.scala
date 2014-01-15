package org.discovery.vivaldi.local

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{WordSpecLike, MustMatchers}
import org.discovery.vivaldi.dto.{FirstContact, Coordinates}

/**
 * Created with IntelliJ IDEA.
 * User: raphael
 * Date: 1/6/14
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
class LocalSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers{
  "An actor system " must {
    "work" in {
      val coordinates : Seq[Coordinates] = List(new Coordinates(0,1),new Coordinates(1,0), new Coordinates(0,0), new Coordinates(1,1), new Coordinates(0.5,0.5))
      //val actorRefs = FakePing.initActorSystem(coordinates)
      //FakePing.createLinks(actorRefs)
    }
  }
}
