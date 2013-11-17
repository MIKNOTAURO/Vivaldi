package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.Actor

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

class Main extends Actor{

  val log = Logging(context.system, this)

  def receive = {
    case "Test" => log.info("Message reçu")
    case _ => log.info("Message Inconnu")
  }

}
