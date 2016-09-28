organization := "eu.inn"

name := "hyperbus-t-http-client"

libraryDependencies ++= Seq(
  "eu.inn" %% "hyperbus-transport" % version.value,
  "org.asynchttpclient" % "async-http-client" % "2.0.15",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"
)
