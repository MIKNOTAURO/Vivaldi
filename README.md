VivaldiGSI project [![Build Status](https://travis-ci.org/BeyondTheClouds/VivaldiGSI.png?branch=master)](https://travis-ci.org/BeyondTheClouds/VivaldiGSI)
===========================

This project is an implementation of the Vivaldi algorithm.

This is the project architecture:
![VivaldiGSI Architecture](/reports/VivaldiArchitecture.png "Architecture")

System
------
The System part is both the orchestrator and the keeper of the close node list. It  parses the configuration file to handle initialisation of the node. It also keeps the list of close nodes up-to-date thanks to information received from the Vivaldi part. Finaly it also provides the API to retrieve the closest node to the current one.

### Initialization
As to Initialize the Vivaldi module, it needs to receive the AKKA Actor Reference of the first node to contact. This node will be used as an entering point in the Network for Vivaldi.

To send this node reference to Vivaldi, you just have to send it the following message :
```scala
/**
  * Message to tell Vivaldi the first node to contact.
  * @param node ActorRef of the first node to contact.
  */
case class FirstContact(node: ActorRef)
```

### API
As for now the API is accessible by sending messages to the system.
Here are the different messages available :

```scala
// API Messages
  /**
   * Method that retrieves the closest nodes from self
   * @param excluded excluded nodes from the result by default nothing is excluded
   * @param numberOfNodes number of nodes to return by default we return one node
   * @return a Sequence of the closest nodes if the number of nodes required is bigger
   *         than the number of nodes in the sequence, the entire table is returned.
   *         The maximum number of nodes is then updated to the amount of nodes required
   */
case class NextNodesToSelf(excluded: Set[nodeInfo] = Set(), numberOfNodes: Int = 1)

  /**
   * Method that retrieves the closest nodes from the origin
   * @param origin reference node
   * @param excluded excluded nodes from the result. By default nothing is excluded
   * @param numberOfNodes number of nodes to return. By default we return one node
   * @return a Sequence of the closest nodes. if the number of nodes required is bigger
   *         than the number of nodes in the sequence, the entire table is returned.
   *         The maximum number of nodes is then updated to the amount of nodes required
   */
case class NextNodesFrom(origin: nodeInfo, excluded: Set[nodeInfo] = Set(), numberOfNodes: Int = 1)
```

What is returned is a `Seq[nodeInfo]`

### Close Nodes
To maintain the close node list, here is what we do :

Every time the vivaldi algorithm computes the node's coordinates, it sends the coordinates and an RPS table to the system .
* We update the existing distances in the close node coordinates
* We compute the distances of the RPS nodes.
* We update the coordinates of the nodes in the close node list which are also in the RPS list. (We take the RPS list coordinates as the new ones)
* We combine the RPS list and the close node list
* We order them
* We keep the n first elements (n is user-defined)

### Core

The core actor recalculates the position of the node given a number of other nodes and using the distributed version of the vivaldi algorithm.

![VivaldiGSI Algorithm](/reports/VivaldiAlgorithm.png "Algorithm")

Basically, for each node given, it moves in the direction of the balance position by 50% of the distance between it and its current position.

How to use
==========
This project uses SBT. Please first install sbt on your computer (the version used is 0.13). I've added the `sbt-idea` plugin dependency to the project so we can use IntelliJ Idea.

To open the project in IntelliJ Idea for the first time, run `sbt gen-idea` in the project directory. You can then open the project in IntelliJ Idea (remember to install the Scala plugin).

To compile, `sbt compile`, to run `sbt run` and to test `sbt test`. Simple enough?

Best Practices
================

Version Control
------------------
Each of us will work in branches. As soon as there is something to be reviewed, we shall use pull-requests to ask for code validation. It is better if there is not too much code to read, so make them regular but not too much either.
Commits should be sized around 50-100 lines of code (excluding comments).

Package Control
--------------------
* The 'dto' package contains  all the messages types that will be exchanged between the actors.
* The 'core' package contains the Vivaldi Algorithm.
* The 'network' package contains the Networking aspects.
* The 'system' package contains the System part.

If you need to modify 'dto' make a pull request.

Logging
-------
We are using Akka's logging system so that we can easily choose the output (File or Console). We use the console by default.

How to use :

```scala
log.info("Information message")
```

There are different types of logging messages :
* `info` which is used to give information on the program state, for example "Service x Launched"
* `debug` which is used to give information to understand what is going on when the program fails. For example, "x=7"
* `error` which is used to print errors, for example "Impossible to speak with Roger"
* `warning` which is used to print little issues that are not bothering the system, for example "no name given, default name will be used"

Dependencies
-----------
These are defined in the 'build.sbt' file.

If you change this file, make a pull request for it.
