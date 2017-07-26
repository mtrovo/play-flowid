name := "play-xflowid"

version := "0.0.1"

scalaVersion := "2.11.7"

lazy val root = (project in file("."))

libraryDependencies += "com.typesafe.play" %% "play-server" % "2.6.0"
libraryDependencies += "com.typesafe.play" %% "play-specs2" % "2.6.0" % Test

