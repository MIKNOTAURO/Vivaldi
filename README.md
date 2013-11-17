VivaldiGSI project
===========================

This project is an implementation of the Vivaldi algorithm.

À suivre une description plus détaillée.

Bonnes pratiques
================

Vu que nous sommes 5 à travailler sur le projet, je met en place quelques règles pour que tout le monde fasse à peu près la même chose :

Gestion de version
------------------
Chaque personne travaille toujours dans une branche et fait régulièrement des pull requests (mieux vaut qu'il n'y ait pas trop de code à relire, donc il faut en faire régulièrement)

Gestion des packages
--------------------
J'ai créé un modèle de classe Scala dans tous les packages, libre à vous de vous réorganiser à l'intérieur de ceux-ci. J'ai créé un package `dto` dans lequel je met tous les types de messages qui vont
transiter entre les modules. Utilisez ces objets là pour envoyer ou recevoir les messages comme ça tout le monde utilise la même chose.
Si vous avez besoin de rajouter ou de modifier des objets, faites un pull request.

Logging
-------
Je propose d'utiliser le logging d'Akka pour que nous puissions choisir où va la sortie (sur un fichier ou directement sur la console).
J'ai paramétré le logging pour qu'il affiche sur la console pour le début.
Cela veut dire que `println` est interdit ! On ne fait que des `log.info("message")` (ou autre niveau voir ci-dessous).

Il y a différents niveaux :
* `info` qui comme son nom l'indique est fait pour donner une information, par exemple : "Service x démarré"
* `debug` qui sert à donner des informations qui ne servent qu'à comprendre ce qui se passe en cas de problème, par exemple "x=7"
* `error` qui a un nom plutôt clair, pour des vraies erreurs, par exemple "Impossible de communiquer avec Roger"
* `warning` pour les petits problèmes qui ne gênent pas le bon fonctionnement du système, par exemple "Pas de nom donné, utilisation du nom par défaut"

Il est important de respecter ces niveaux et de ne pas utiliser `println` parce que le logging d'Akka permet aussi de filtrer le type de messages qu'on veut.

Dépendances
-----------
Les dépendances sont définies dans le fichier `build.sbt` (qui n'est pas très dur à comprendre). J'ai déjà mis Akka, dans le cas peu probable ou vous en avez besoin d'une autre,
renseignez-vous sur internet et faites une pull request.
