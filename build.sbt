scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.3",
  "org.endpoints4s" %% "akka-http-server" % "2.0.0",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "org.endpoints4s" %% "akka-http-client" % "1.1.0" % Test,
  "org.scalameta" %% "munit" % "0.7.9" % Test
)

testFrameworks += new TestFramework("munit.Framework")

run / fork := true

Global / onChangedBuildSource := ReloadOnSourceChanges
