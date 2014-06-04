package org.discovery.vivaldi.system

import akka.event.Logging
import akka.actor.{ActorRef, Props, Actor}
import org.discovery.vivaldi.core.ComputingAlgorithm
import scala.concurrent.duration._
import org.discovery.vivaldi.dto._
import org.discovery.vivaldi.network.{CommunicationMessage, Communication}
import scala.math._
import scala.concurrent.{Await, ExecutionContext}
import ExecutionContext.Implicits.global
import org.discovery.vivaldi.dto._
import dispatch._
import scala.util.parsing.json.JSON
import org.discovery.vivaldi.system.VivaldiActor._
import akka.util.Timeout
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import net.liftweb.json.{JObject, JsonAST}
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer._

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


object VivaldiActor {

  case class AreYouAwake()

  case class IAmAwake(closeNodes: Seq[CloseNodeInfo])

}

class VivaldiActor(name: String, id: Long, outgoingActor: Option[ActorRef] = None) extends Actor {

  val log = Logging(context.system, this)

  //Creating child actors
  val deltaConf = context.system.settings.config.getDouble("vivaldi.system.vivaldi.delta")
  val vivaldiCore = context.actorOf(Props(classOf[ComputingAlgorithm], self, deltaConf), "VivaldiCore")
  val network = context.actorOf(Props(classOf[Communication], id, vivaldiCore, self), "Network")

  var coordinates: Coordinates = Coordinates(0,0)
  var closeNodes: Seq[CloseNodeInfo] = Seq()

  val config = context.system.settings.config.getConfig("vivaldi.system")
  val configInit = config.getConfig("init")
  var numberOfCloseNodes = config.getInt("closeNodes.size")

  //variables related to monitoring
  val monitoringActivated : Boolean = context.system.settings.config.getBoolean("vivaldi.system.monitoring.activated")
  val urlMonitoring : String = context.system.settings.config.getString("vivaldi.system.monitoring.url")
  val contentType = Map("content-type" -> "application/json")
  val networkName : String = context.system.settings.config.getString("vivaldi.system.monitoring.network")
  var networkId : Int = 0
  var idMonitoring : Int = 0

  /**
   * Called when the actor is created
   */
  override def preStart() {
    if (monitoringActivated){
      createNetwork
      initializeNode
    }
  }

//  def createNetwork = {
//
//    var networkExists = false
//
//    val getNetworks = url(urlMonitoring+"networks/").GET
//    val resultGetNetworks = Http(getNetworks OK as.String).either
//    var responseGetNetworks = ""
//    resultGetNetworks() match {
//      case Right(content)         => responseGetNetworks = content
//      case Left(StatusCode(404))  => log.error("Not found")
//      case Left(StatusCode(code)) => log.error("Some other code: " + code.toString)
//      case _ => log.error("Error")
//    }
//    val jsonList = JSON.parseFull(responseGetNetworks).get.asInstanceOf[List[Map[String, Any]]]
//    log.info(jsonList.toString())
//    for (item <- jsonList){
//      val name = item.get("networkName").get.asInstanceOf[String]
//      if (name == networkName){
//        networkExists = true
//        networkId = item.get("id").get.asInstanceOf[Double].toInt
//      }
//    }
//
//    if (!networkExists) {
//      val jsonSystem = ("networkName" -> networkName)
//      val bodySystem = compact(JsonAST.render(jsonSystem))
//      val responseNetwork = makePostRequest(bodySystem, "networks/")
//      networkId = JSON.parseFull(responseNetwork).get.asInstanceOf[Map[String, Any]]
//        .get("id").get.asInstanceOf[Double].toInt
//      log.info(s"Id network : $networkId")
//    }
//
//  }

  def createNetwork = {

    def getNetworkRequest(): Option[String] = {
      val getNetworks = url(urlMonitoring + "networks/").GET
      val resultGetNetworks = Http(getNetworks OK as.String).either
      resultGetNetworks() match {
        case Right(content) =>
          Some(content)
        case Left(StatusCode(404)) =>
          log.error("Not found")
          None
        case Left(StatusCode(code)) =>
          log.error("Some other code: " + code.toString)
          None

        case _ =>
          log.error("Error")
          None
      }
    }

    def sendCreateNetworkRequest(): Option[String] = {
      val jsonSystem = ("networkName" -> networkName)
      val bodySystem = compact(JsonAST.render(jsonSystem))
      val requestNetwork = url(urlMonitoring + "networks/").POST << bodySystem <:< contentType
      val resultNetwork = Http(requestNetwork OK as.String).either

      resultNetwork() match {
        case Right(content) =>
          Some(content)
        case Left(StatusCode(404)) =>
          log.error("Not found")
          None
        case Left(StatusCode(code)) =>
          log.error("Some other code: " + code.toString)
          None
        case _ =>
          log.error("Error")
          None
      }
    }

    var networkExists = false

    while (!networkExists) {

      getNetworkRequest() match {
        case Some(jsonString: String) =>
          JSON.parseFull(jsonString) match {
            case Some(jsonList: List[Map[String, Any]]) =>
              log.info(jsonList.toString())
              for (item <- jsonList) {
                (item.get("networkName"), item.get("id")) match {
                  case (Some(name: String), Some(id: Double)) if (name == networkName) =>
                    networkId = id.toInt
                    networkExists = true
                  case _ =>
                    log.info(s"createNetwork: network cannot be found: looping! (1), response is {$jsonString}")
                }
              }
            case _ =>
              log.info(s"createNetwork: network cannot be found: looping! (2), response is {$jsonString}")
          }
        case _ =>
          log.info(s"createNetwork: network cannot be found: looping! (3), response is {None}")
      }


      if (!networkExists) {
        log.info(s"createNetwork: as network cannot be found: waiting 200ms")
        Thread.sleep(200)
        sendCreateNetworkRequest() match {
          case Some(responseAsJson) =>
            log.info(s"createNetwork: sendCreateNetworkRequest() => $responseAsJson")
          case None =>
            log.info(s"createNetwork: sendCreateNetworkRequest() => None")
        }
      }
    }

    log.info( s"""createNetwork: {"networkId": ${networkId}""")
  }



  def initializeNode = {

    //call monitoring to create nodes
    val jsonRegister = ("nodeName" -> name) ~ ("networkId" -> networkId) ~ ("id" -> id.toLong)
    val bodyRegister = compact(JsonAST.render(jsonRegister))
    log.info(bodyRegister)
    makePostRequest(bodyRegister, "nodes/withId")

    //call monitoring to initialize node
    val jsonInit = ("nodeId" -> id)
    val bodyInit = compact(JsonAST.render(jsonInit))
    makePostRequest(bodyInit, "initTimes/")

  }

  /**
   * Calls monitoring to update coordinates
   */
  def updateMonitoring = {
    if (monitoringActivated) {
      val x = coordinates.x
      val y = coordinates.y
//      val id = 25769820553343l
      val jsonUpdate = ("nodeId" -> id) ~ ("x" -> x) ~ ("y" -> y)
      val bodyUpdate = compact(JsonAST.render(jsonUpdate))
      makePostRequest(bodyUpdate, "coordinates/")
    }
  }

  def updateCloseNodesMonitoring = {
    if (monitoringActivated) {
      var jsonList : List[JObject] = List()
      for (closeNode <- closeNodes) {
        jsonList ++= List(("localNodeId" -> id) ~ ("distantNodeId" -> closeNode.id) ~ ("distance" -> closeNode.distanceFromSelf))
      }
      val json = compact(JsonAST.render(jsonList))
      log.info(s"Close nodes json : $json")
      makePostRequest(json, "closeNodes/list")
    }
  }

  def makePostRequest(toSend : String, urlToSend : String): Option[String] = {
    val request = url(urlMonitoring+urlToSend).POST << toSend <:< contentType
    val result = Http(request OK as.String).either
    result() match {
      case Right(content)         =>
        Some(content)
      case Left(StatusCode(404))  =>
        log.error("Not found")
        None
      case Left(StatusCode(code)) =>
        log.error("Some other code: " + code.toString)
        None
      case _ =>
        log.error("Error")
        None
    }
  }

  /**
   * Method that handles the oncoming messages
   * @return
   */
  def receive = {
    case NextNodesToSelf(excluded, numberOfNodes) =>
      sender ! getCloseNodesToSelf(excluded, numberOfNodes)
    case NextNodesFrom(origin, excluded, numberOfNodes) =>
      sender ! getCloseNodesFrom(origin, excluded, numberOfNodes)
    case UpdatedCoordinates(newCoordinates, rps) => updateCoordinates(newCoordinates, rps)
    case DeleteCloseNode(toDelete) => deleteCloseNode(toDelete)

    /* routing messages to child actors */
    case c: CommunicationMessage => network forward c
    case a: AreYouAwake => sender ! IAmAwake(closeNodes)
    /* Unknown message => log */
    case msg =>
      outgoingActor match {
        case Some(ref) => ref.forward(msg)
        case None => log.info(s"Unknown Message: $msg")
      }
  }
  /**
   * Check is a node is responding and if so retrieve its closeNodes table have more information
   * @param info Node to contact
   * @return true if the node is responding and false otherwise
   */
  def isAwake(info: nodeInfo): Boolean = {
    implicit val timeout = Timeout(5 seconds)
    val response = info.node ? AreYouAwake()
    try {
      Await.result(response, 6 seconds) match {
        case r: IAmAwake => {
          log.debug(s"Node $info is well, processing closeNode table")
          mergeCloseNodesTable(r.closeNodes)
          true
        }
      }
    } catch {
      case e: AskTimeoutException => {
        log.warning(s"Node $info is not responding")
        false
      }
      case exp: Throwable => {
        log.error("Unknown exception", exp)
        false
      }
    }
  }

  /**
   * Merges the table in parameter with the local closeNodes table
   * @param table closeNodes table to merge the local one with
   */
  def mergeCloseNodesTable(table: Seq[CloseNodeInfo]) {
    val updatedTable = table.map(node => node.copy(distanceFromSelf = computeDistanceBtw(this.coordinates, node.coordinates)))
    closeNodes = (closeNodes ++ updatedTable).sorted.take(numberOfCloseNodes)
  }
  /**
   * Method that retrieves the closest nodes from self
   * @param excluded excluded nodes from the result 
   * @param numberOfNodes number of nodes to return 
   * @return a Sequence of the closest nodes. If the number of nodes required is bigger
   *         than the number of nodes in the sequence, the entire table is returned.
   *         The maximum number of nodes is then updated to the amount of nodes required
   */
  def getCloseNodesToSelf(excluded: Set[nodeInfo], numberOfNodes: Int): Seq[nodeInfo] = {
    //first we need to check the size we use
    if (numberOfNodes > numberOfCloseNodes){
      numberOfCloseNodes = numberOfNodes
      getCloseNodesToSelf(excluded,numberOfNodes)
    }else{
      // we take as a reference the current node, we only have to retrieve the n first elements of the list without the excluded nodes
      val currentCloseNodes = closeNodes.filterNot(excluded contains)
      val partition = currentCloseNodes.splitAt(numberOfNodes)
      val test = isAwake(partition._1.head)
      var awakeCloseNodes = partition._1.filter(isAwake) // we test all the nodes, some dead note are possibly filtered
      var remainingCloseNodes = partition._2

      while (awakeCloseNodes.size < numberOfNodes && !remainingCloseNodes.isEmpty) { // while we don't have the correct number of nodes we add them for the second part of the closeNodes list
        val info = remainingCloseNodes.head
        remainingCloseNodes = remainingCloseNodes.tail
        if (isAwake(info)) {
          awakeCloseNodes = awakeCloseNodes :+ info
        }
      }

      awakeCloseNodes
    }
  }

  /**
   * Method that retrieves the closest nodes from the origin
   * @param origin reference node
   * @param excluded excluded nodes from the result. By default nothing is excluded
   * @param numberOfNodes number of nodes to return. By default we return one node
   * @return a Sequence of the closest nodes
   */
  @deprecated
  def getCloseNodesFrom(origin: nodeInfo, excluded: Set[nodeInfo], numberOfNodes: Int): Seq[nodeInfo] = {
    // we just have to compute the distances between the reference and the nodes in memory, sort them, and send the n closest without excluded nodes
    val relativeDistancesSeq = closeNodes.map(node => node.copy(distanceFromSelf = computeDistanceBtw(origin.coordinates, this.coordinates)))

    if (numberOfNodes <= numberOfCloseNodes) {
      relativeDistancesSeq.sorted.filterNot(excluded contains).take(numberOfNodes)
    } else {
      val temp = numberOfCloseNodes
      numberOfCloseNodes = numberOfNodes
      relativeDistancesSeq.sorted.filterNot(excluded contains).take(temp)
    }
  }

  /**
   * Method to update the close node list coordinates.
   * @param newCoordinates updated coordinates of the current node just calculates
   * @param rpsIterable list of the RPS nodes that will be used to update the close node list
   */
  def updateCoordinates(newCoordinates: Coordinates, rpsIterable: Iterable[RPSInfo]) {

    val rps = rpsIterable.toSeq

    log.debug(s"New coordinated received: $newCoordinates")
    coordinates = newCoordinates
    updateMonitoring

    log.debug("Computing & updating distances")
    //Computing the distances from the RPS table
    val RPSCloseNodes = rps.map(node => CloseNodeInfo(node.id, node.node, node.coordinates,computeDistanceToSelf(node.coordinates)))

    //TODO Test contain with data and code the method equals for closeNodes
    //Retrieve nodes to update
//    val RPSCloseNodesUpdates = RPSCloseNodes intersect closeNodes
    val RPSCloseNodesUpdates = RPSCloseNodes.filter(n => closeNodes.exists(e => e.id == n.id))
    //Get rid of the nodes already in the list
    val RPSCloseNodesToAdd = RPSCloseNodes.filterNot(n => RPSCloseNodesUpdates.exists(e => e.id == n.id))

    //Computing and updating distances for the existing nodes
    closeNodes = closeNodes.map(node => node.copy(distanceFromSelf = computeDistanceToSelf(this.coordinates)))
    //Updating RPS nodes already in closeNodes
    closeNodes =  RPSCloseNodesUpdates ++ closeNodes.filterNot(n => RPSCloseNodesUpdates.exists(e => e.id == n.id))

    //Adding new Nodes
    closeNodes = RPSCloseNodesToAdd ++ closeNodes

    // Update distances
    closeNodes = closeNodes.map(cni => CloseNodeInfo(cni.id, cni.node, cni.coordinates, computeDistanceToSelf(cni.coordinates)))

    closeNodes = closeNodes.sorted.take(numberOfCloseNodes)

    updateCloseNodesMonitoring

    log.info(s"[TICK] coordinate: ${newCoordinates}, rps: ${rpsIterable.toList}, closeNodes: ${closeNodes.toList}")
  }

  /**
   * Method to compute the distance between the current node and the node in parameter
   * @param externCoordinates to compute the distance from
   * @return
   */
  def computeDistanceToSelf(externCoordinates: Coordinates): Double = {
    computeDistanceBtw(coordinates,externCoordinates)
  }

  /**
   * Compute the distance between two coordinates
   * @param a coordinates of the point a
   * @param b coordinates of the point b
   * @return distance between the two points
   */
  def computeDistanceBtw(a: Coordinates, b: Coordinates): Double = {
    hypot(a.x-b.x,a.y-b.y)
  }

  /**
   * Methods that deletes a node from the close node list when a ping isn't correct.
   * @param nodeToDelete to delete from the list
   */
  def deleteCloseNode(nodeToDelete: RPSInfo){
    closeNodes = closeNodes.filterNot(_.node.path == nodeToDelete.node.path)
  }


  val firstCallTime = configInit.getInt("firstCallTime")
  val timeBetweenCallsFirst = configInit.getInt("timeBetweenCallsFirst")
  val timeBetweenCallsThen = configInit.getInt("timeBetweenCallsThen")
  val numberOfNodesCalled = configInit.getInt("numberOfNodesCalled")
  val changeTime =  configInit.getInt("changeTime")


  /**
   *  Creates a scheduler
  Calls network with case class DoRPSRequest in argument
  First call will be made after firstCallTime ms
  Calls made each timeBetweenCalls ms
   */
  val schedulerRPSFirst = context.system.scheduler.schedule(timeBetweenCallsFirst seconds, timeBetweenCallsFirst seconds){
    callNetwork()
  }

  val schedulerChangeFrequency = context.system.scheduler.scheduleOnce(changeTime seconds){
    schedulerRPSFirst.cancel()
  }

  val schedulerRPSThen = context.system.scheduler.schedule(changeTime seconds, timeBetweenCallsThen seconds){
     callNetwork()
  }

  def callNetwork() = {
    log.debug("Scheduler for RPS request called")
    log.debug(s"$numberOfNodesCalled nodes will be called")
    val myInfo = RPSInfo(id, self, coordinates, 0)
    network ! DoRPSRequest(myInfo, numberOfNodesCalled)
  }

}
