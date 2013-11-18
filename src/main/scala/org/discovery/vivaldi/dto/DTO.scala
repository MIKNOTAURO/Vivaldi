package org.discovery.vivaldi.dto

import akka.actor.ActorRef

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

case class RPSInfo(node: ActorRef, coordinates: Coordinates, ping: Long)

case class Coordinates(x: Long, y: Long)


case class DoRPSRequest(numberOfNodesToContact: Int) //Message Système-Réseau

case class FirstContact(node: ActorRef) //Message Système-Réseau (pour l'initialisation)

case class UpdatedRPS(rps: Iterable[RPSInfo]) //Message Réseau-Vivaldi

case class UpdatedCoordinates(coordinates: Coordinates, RPSTable: Iterable[RPSInfo])  //Message Vivaldi-Système
