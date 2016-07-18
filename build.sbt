name := "akka-microservice-utils"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

val circe = Seq("core", "generic", "parser", "jawn")
  .map(v => "io.circe" %% s"circe-$v" % "0.4.1")

val slick = Seq("slick", "slick-codegen", "slick-hikaricp")
  .map(v => "com.typesafe.slick" %% v % "3.1.1")

val slickPG = Seq("slick-pg", "slick-pg_date2", "slick-pg_circe-json")
  .map(v => "com.github.tminglei" %% v % "0.14.1")

val akka = Seq("actor", "stream", "http-experimental", "slf4j")
  .map(v => "com.typesafe.akka" %% s"akka-$v" % "2.4.7")

val scalaz = Seq("core", "effect")
  .map(v => "org.scalaz" %% s"scalaz-$v" % "7.2.4")

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.5",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "com.casualmiracles" %% "treelog" % "1.3.0"
) ++ slick ++ slickPG ++ akka ++ scalaz ++ circe

sources in(Compile, doc) := Seq.empty
publishArtifact in(Compile, packageDoc) := false

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)