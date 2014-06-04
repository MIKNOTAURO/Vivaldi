package org.discovery.vivaldi

/* ============================================================
 * Discovery Project - DVMS
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

import akka.actor.{ActorSystem, Props}
import scala.concurrent.duration._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import collection.mutable
import org.discovery.vivaldi.system.VivaldiActor

object Main extends App {

  override def main(args: Array[String]) {

    println("Vivaldi - version 0.1 (alpha)")

    implicit val timeout = Timeout(1 seconds)
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

    var argumentHashMap = new mutable.HashMap[String, String]()

    args.foreach(arg => {
      val argArray = arg.split("=")
      argArray.size match {
        case 1 => argumentHashMap += (argArray(0) -> "")
        case 2 => argumentHashMap += (argArray(0) -> argArray(1))
        case 3 => throw new Exception(s"Invalid argument: $arg")
      }

    })

    val ip = argumentHashMap.contains("ip") match {
      case true => argumentHashMap("ip")
      case false => throw new Exception("please specify <ip> argument")
    }
    val portAsString = argumentHashMap.contains("port") match {
      case true => argumentHashMap("port")
      case false => throw new Exception("please specify <port> argument")
    }
    val debug = argumentHashMap.contains("debug") match {
      case true => argumentHashMap("debug")
      case false => "false"
    }


    val port: Int = Integer.parseInt(portAsString)


//    val location: NetworkLocation = NetworkLocation(ip, port, Some(NetworkLocation.getHostname))
//
    val system = ActorSystem(s"DvmsSystem", Configuration.generateNetworkActorConfiguration(
      ip,
      port,
      true,
      "http://localhost:9000/"
    ))

    system.actorOf(Props(new VivaldiActor("25769820553343", 25769820553343l)), "System")

    system.awaitTermination()
  }
}