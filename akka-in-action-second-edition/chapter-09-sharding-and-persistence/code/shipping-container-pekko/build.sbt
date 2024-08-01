ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val essemblyx = "com.essemblyx"
// lazy val scala3Version = "3.3.3"
lazy val scala3Version = "2.13.13"

lazy val pekkoVersion           = "1.0.3"
lazy val pekkoManagementVersion = "1.0.0"

lazy val root = (project in file("."))
  .settings(
    name         := "word-counting-pekko",
    scalaVersion := scala3Version,
    organization := essemblyx
    // Compile / mainClass := Some("com.essemblyx.HelloPekko")
  )

fork := true

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed"             % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-typed"           % pekkoVersion,
  "org.apache.pekko" %% "pekko-management"              % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-http" % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery"               % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed"  % pekkoVersion,
  "ch.qos.logback"    % "logback-classic"               % "1.2.13",
  "org.apache.pekko" %% "pekko-actor-testkit-typed"     % pekkoVersion % Test,
  "org.scalatest"    %% "scalatest"                     % "3.2.18"     % Test,
  "org.apache.pekko" %% "pekko-serialization-jackson"   % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-typed"       % pekkoVersion, // need to persistence
  "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % Test, // need to persistence
  "org.apache.pekko" %% "pekko-persistence-jdbc"    % "1.0.0",
  "org.postgresql"    % "postgresql"                % "42.2.18",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0"
)
