name := "orders"

organization := "reactivebbq"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.1"

val akkaHttpVersion = "10.1.11"
val akkaVersion    = "2.6.0"
val akkaManagementVersion =  "1.0.5"
val akkaEnhancementsVersion = "1.1.12"
val slf4jVersion = "1.7.28"
val logbackVersion = "1.2.3"
val scalaTestVersion = "3.0.8"
val typesafeConfigVersion = "1.4.0"

fork := true
parallelExecution in ThisBuild := false

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster"         % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"% akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  "com.lightbend.akka" %% "akka-diagnostics" % akkaEnhancementsVersion,
  "com.lightbend.akka" %% "akka-split-brain-resolver" % akkaEnhancementsVersion,

  "com.h2database" % "h2" % "1.4.199",
  "org.hibernate" % "hibernate-entitymanager" % "5.4.6.Final",
  "org.hibernate" % "hibernate-c3p0" % "5.4.6.Final",

  //Logback
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  //Test dependencies
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion% Test,

  // Lightbend Telemetry dependencies
  Cinnamon.library.cinnamonAkka,
  Cinnamon.library.cinnamonAkkaHttp,
  Cinnamon.library.cinnamonJvmMetricsProducer,
  Cinnamon.library.cinnamonPrometheus,
  Cinnamon.library.cinnamonPrometheusHttpServer,
)

dependencyOverrides ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.typesafe.akka" %% "akka-actor"% akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"         % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"% akkaVersion,
  "com.typesafe.akka" %% "akka-coordination"% akkaVersion,
  "com.typesafe.akka" %% "akka-stream"% akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
)

enablePlugins(Cinnamon)

cinnamon in run := true
cinnamon in test := false
