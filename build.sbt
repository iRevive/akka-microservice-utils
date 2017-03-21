
val commonSettings = Seq(
  organization := "io.github.irevive",

  version := "0.0.1",

  scalaVersion := Version.scala,

  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "UTF-8"/*, "-Xplugin-require:macroparadise"*/),

  resolvers ++= Seq(Resolvers.sonatype, Resolvers.scalaz, Resolvers.irevive, Resolvers.scalaMeta),

  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

//  addCompilerPlugin(Library.scalaMetaParadise)
)

lazy val errors = (project in file("./errors"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-utils-errors",
    libraryDependencies ++= Dependencies.errors
  )

lazy val ftp = (project in file("./ftp"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-utils-ftp",
    libraryDependencies ++= Dependencies.ftp
  ).aggregate(errors).dependsOn(errors)

lazy val slick = (project in file("./slick"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-utils-slick",
    libraryDependencies ++= Dependencies.slick
  ).aggregate(errors).dependsOn(errors)

lazy val `slick-postgres` = (project in file("./slick-postgres"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-utils-slick-postgres",
    libraryDependencies ++= Dependencies.postgres
  ).aggregate(slick).dependsOn(slick)

lazy val common = (project in file("./common"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-utils-common",
    libraryDependencies ++= Dependencies.common
  ).aggregate(errors).dependsOn(errors)
