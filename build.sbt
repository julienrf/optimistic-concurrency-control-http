scalaVersion := "3.0.0-RC1"

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.3").withDottyCompat(scalaVersion.value),
  ("org.endpoints4s" %% "akka-http-server" % "2.0.0").withDottyCompat(scalaVersion.value),
  ("org.slf4j" % "slf4j-simple" % "1.7.30").withDottyCompat(scalaVersion.value),
  ("org.endpoints4s" %% "akka-http-client" % "1.1.0" % Test).withDottyCompat(scalaVersion.value),
  "org.scalameta" %% "munit" % "0.7.22" % Test
)

testFrameworks += new TestFramework("munit.Framework")

run / fork := true

Global / onChangedBuildSource := ReloadOnSourceChanges
