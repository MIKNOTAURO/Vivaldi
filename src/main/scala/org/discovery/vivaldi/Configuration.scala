package org.discovery.vivaldi

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

/* ============================================================
 * Discovery Project - PeerActor
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

object Configuration {
  val id_size = 5 // TODO: for large set, put 40 here
  var firstId: Long = -1
  var numberOfCrashes = 0
  var debug: Boolean = false

  val mailBoxConfigurationFormat: String = """

//  akka.actor.default-mailbox {
//    mailbox-type = "org.discovery.peeractor.overlay.OverlayPriorityMailbox"
//  }

  """

  val vivaldiConfigurationFormat: String = """

# configuration file for the project

akka {

    # loggers = ["akka.event.slf4j.Slf4jLogger"]

    # Options: OFF, ERROR, WARNING, INFO, DEBUG
    loglevel = "DEBUG"


}

vivaldi {

    system {

        monitoring {
            activated = %s
            url = "%s"
            network = "Vivaldi"
        }

        init {

            firstCallTime = 0
            timeBetweenCallsFirst = 1
            timeBetweenCallsThen = 2
            numberOfNodesCalled = 30
            changeTime = 6

        }

        closeNodes {

           size = 100

        }

        vivaldi {

            delta = 0.5

        }

        communication {

            rpssize = 100

        }

    }

}


                                           """


  val networkConfigurationFormat: String = """

    akka {
//        actor {
//          provider = "akka.remote.RemoteActorRefProvider"
//        }
        remote {
          enabled-transports = ["akka.remote.netty.tcp"]
          netty.tcp {
            hostname = "%s"
            port = %d
          }
       }
    }

    #akka.remote.log-remote-lifecycle-events = on

                                           """

  def generateNetworkActorConfiguration(ip: String, port: Int, monitoringActivated: Boolean = false, monitoringUrl: String = "http://vivaldi-monitoring-demo.herokuapp.com/"): Config = {

    val mailBoxConfiguration: String = mailBoxConfigurationFormat
    val networkConfiguration: String = networkConfigurationFormat.format(
      ip,
      port,
      ip,
      port
    )

    val vivaldiConfiguration: String = vivaldiConfigurationFormat.format(
      monitoringActivated,
      monitoringUrl
    )

    val completeConfiguration: String = "%s\n%s\n%s".format(
      mailBoxConfiguration,
      networkConfiguration,
      vivaldiConfiguration
    )

    println(completeConfiguration)

    ConfigFactory.load(ConfigFactory.parseString(completeConfiguration))
  }

  def generateLocalActorConfiguration: Config = {
    val mailBoxConfiguration: String = mailBoxConfigurationFormat
    val vivaldiConfiguration: String = vivaldiConfigurationFormat

    val completeConfiguration: String = "%s\n%s".format(
      mailBoxConfiguration,
      vivaldiConfiguration
    )

    ConfigFactory.load(ConfigFactory.parseString(completeConfiguration))
  }

}