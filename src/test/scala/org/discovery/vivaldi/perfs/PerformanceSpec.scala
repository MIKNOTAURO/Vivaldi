package org.discovery.vivaldi.perfs

import akka.testkit.{TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest._
import org.discovery.vivaldi.system.VivaldiActor
import org.discovery.vivaldi.dto.{CloseNodeInfo, Coordinates}

/**
 * Created with IntelliJ IDEA.
 * User: cyp
 * Date: 18/02/14
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
class PerformanceSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with MustMatchers {
  "The perfs actor " must {
    val mainActor = TestActorRef[VivaldiActor](new VivaldiActor("Stuff",1))

    val perfTest: TestActorRef[Performance] = TestActorRef(Props(classOf[Performance], mainActor), "TestPerfActor")

    val cn1 = CloseNodeInfo(1,mainActor, Coordinates(1,0), 1)
    val cn2 = CloseNodeInfo(2,mainActor, Coordinates(2,0), 2)
    val cn3 = CloseNodeInfo(3,mainActor, Coordinates(3,0), 3)
    val cn4 = CloseNodeInfo(4,mainActor, Coordinates(4,0), 4)
    val cn5 = CloseNodeInfo(5,mainActor, Coordinates(5,0), 5)
    val seq3 : Seq[CloseNodeInfo] = Seq(cn1, cn2, cn3)
    val seq4 : Seq[CloseNodeInfo] = Seq(cn2, cn1, cn3)
    val seq5 : Seq[CloseNodeInfo] = Seq(cn2, cn1, cn3, cn4)
    val seq6 : Seq[CloseNodeInfo] = Seq(cn1, cn2, cn3, cn4, cn5)

    "Add n last elements of Integer Seqs" in {
      val seq1 : Seq[Int] = Seq(1, 0, 2, 8)
      assertResult(10) {
        perfTest.underlyingActor.addNLast(3, seq1)
      }
    }

    "Add n last elements of Double Seqs" in {
      val seq2 : Seq[Double] = Seq(0.2, 1.0, 2.0, 8.5)
      assertResult(11.7) {
        perfTest.underlyingActor.addNLast(4, seq2)
      }
    }

    "Correctly weight the different types of changes in a seq of closenodes" in {
      //Console.println("Haha!" + seq4.contains(cn4)+" "+seq5.contains(cn4))

      assertResult(1) {
        perfTest.underlyingActor.change(cn1, seq3, seq4)
      }

      assertResult(2) {
        perfTest.underlyingActor.change(cn4, seq4, seq5)
      }

      assertResult(0) {
        perfTest.underlyingActor.change(cn3, seq5, seq6)
      }
    }

    "Find the right number of changes between two seq of close nodes" in {
      assertResult(0) {
        perfTest.underlyingActor.findChangesNumber(seq3, seq3)
      }

      assertResult(2) {
        perfTest.underlyingActor.findChangesNumber(seq3, seq4)
      }

      assertResult(2) {
        perfTest.underlyingActor.findChangesNumber(seq4, seq5)
      }

      assertResult(4) {
        perfTest.underlyingActor.findChangesNumber(seq5, seq6)
      }
    }

    "Compute the right total distance from node to close nodes" in {
      assertResult(6) {
        perfTest.underlyingActor.computeTotalDistance(seq3)
      }

      assertResult(10) {
        perfTest.underlyingActor.computeTotalDistance(seq5)
      }
    }
  }
}
