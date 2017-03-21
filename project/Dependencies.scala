import sbt._

object Version {
  val scala             = "2.11.8"

  val circe             = "0.7.0"
  val slick             = "3.2.0"
  val slick_pg          = "0.15.0-RC"
  val akka_http         = "10.0.5"
  val scalaz            = "7.2.10"
  val logless           = "0.1.1"
  val config            = "1.3.1"

  val postgresql        = "42.0.0"
  val apache_commons    = "3.5"

  val scalaMeta         = "1.6.0"
  val scalaMetaParadise = "3.0.0.140"
}

object Resolvers {
  val sonatype  = Resolver.sonatypeRepo("public")
  val scalaz    = Resolver.bintrayRepo("scalaz", "releases")
  val irevive   = Resolver.bintrayRepo("irevive", "maven")
  val scalaMeta = Resolver.url("scalameta", url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns)
}

object Library {
  val circe_core            = "io.circe"                    %% "circe-core"           % Version.circe
  val circe_generic         = "io.circe"                    %% "circe-generic"        % Version.circe
  val circe_parser          = "io.circe"                    %% "circe-parser"         % Version.circe
  val circe_jawn            = "io.circe"                    %% "circe-jawn"           % Version.circe

  val slick                 = "com.typesafe.slick"          %% "slick"                % Version.slick
  val slick_codegen         = "com.typesafe.slick"          %% "slick-codegen"        % Version.slick
  val slick_hikaricp        = "com.typesafe.slick"          %% "slick-hikaricp"       % Version.slick

  val slick_pg              = "com.github.tminglei"         %% "slick-pg"             % Version.slick_pg
  val slick_pg_circe_json   = "com.github.tminglei"         %% "slick-pg_circe-json"  % Version.slick_pg

  val akka_http             = "com.typesafe.akka"           %% "akka-http"            % Version.akka_http

  val scalaz_core           = "org.scalaz"                  %% "scalaz-core"          % Version.scalaz
  val scalaz_effect         = "org.scalaz"                  %% "scalaz-effect"        % Version.scalaz

  val logless               = "io.github.irevive"           %% "logless"              % Version.logless
  val config                = "com.typesafe"                % "config"                % Version.config

  val postgresql            = "org.postgresql"              % "postgresql"            % Version.postgresql

  val apache_commons        = "commons-net"                 % "commons-net"           % Version.apache_commons

  val scalaMeta             = "org.scalameta"               %% "scalameta"            % Version.scalaMeta
  val scalaMetaParadise     = "org.scalameta"               % "paradise_2.11.8"       % Version.scalaMetaParadise

}

object Dependencies {
  import Library._

  val macros = List(
    scalaMeta, scalaMetaParadise
  )

  val ftp = List(
    apache_commons, scalaz_core, scalaz_effect
  )

  val postgres = List(
    postgresql,

    circe_core, circe_generic, circe_parser, circe_jawn,

    slick_pg, slick_pg_circe_json
  )

  val common = List(
    akka_http, scalaz_core
  )

  val slick = List(
    Library.slick, slick_codegen, slick_hikaricp,
    scalaz_core
  )

  val errors = List(
    logless
  )
}