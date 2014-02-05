package org.discovery.vivaldi

import akka.event.slf4j.Logger
import akka.actor.{Props, ActorSystem}
import org.discovery.vivaldi.system.VivaldiActor
import java.io.{InputStreamReader, BufferedReader}

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

object Vivaldi {

//  val log = Logger("Primary")

  def main(args: Array[String]) = {

    val shellCmd: Array[String] = Array(
      "/bin/sh",
      "-c",
      "ping -c 1 google.fr | grep -e 'time=.*ms' | sed 's/^.*time=//g' | sed 's/ ms//g'"
    );

    val runtime: Runtime = Runtime.getRuntime()
    val p:Process = runtime.exec(shellCmd)

    var in: BufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()))
    var result: String = in.readLine()
    println(result.toDouble)
  }

}
