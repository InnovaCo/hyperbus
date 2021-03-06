organization := "eu.inn"

name := "hyperbus"

libraryDependencies ++= Seq(
  "eu.inn" %% "hyperbus-model" % version.value,
  "eu.inn" %% "hyperbus-t-inproc" % version.value % "test",
  "eu.inn" %% "binders-core" % "0.12.93",
  "io.reactivex" %% "rxscala" % "0.26.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
