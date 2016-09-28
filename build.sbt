import sbt.Keys._

scalaVersion := "2.11.8"

organization := "eu.inn"

lazy val paradiseVersionRoot = "2.1.0"

val projectMajorVersion = settingKey[String]("Defines the major version number")

val projectBuildNumber = settingKey[String]("Defines the build number")

val defaultSettings = Seq(
  scalaVersion := "2.11.8",
  projectMajorVersion := "0.1",
  projectBuildNumber := "SNAPSHOT",
  version := projectMajorVersion.value + "." + projectBuildNumber.value,
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-optimise",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"
  ),
  javacOptions ++= Seq(
    "-source", "1.8",
    "-target", "1.8",
    "-encoding", "UTF-8",
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  publishTo := Some("Innova libs repo" at "http://repproxy.srv.inn.ru/artifactory/libs-release-local"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".innova_credentials"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersionRoot cross CrossVersion.full)
)

lazy val `hyperbus-root` = project.in(file(".")) aggregate(
  `hyperbus-transport`,
  `hyperbus-model`,
  hyperbus,
  `hyperbus-akka`,
  `hyperbus-t-inproc`,
  `hyperbus-t-distributed-akka`,
  `hyperbus-t-kafka`,
  `hyperbus-cli`,
  `hyperbus-sbt-plugin`
  )

lazy val `hyperbus-transport` = project.in(file("hyperbus-transport")).settings(defaultSettings)

lazy val `hyperbus-model` = project.in(file("hyperbus-model")).settings(defaultSettings) dependsOn `hyperbus-transport`

lazy val `hyperbus` = project.in(file("hyperbus")).settings(defaultSettings) dependsOn(`hyperbus-model`, `hyperbus-t-inproc`)

lazy val `hyperbus-akka` = project.in(file("hyperbus-akka")).settings(defaultSettings) dependsOn `hyperbus`

lazy val `hyperbus-t-inproc` = project.in(file("hyperbus-t-inproc")).settings(defaultSettings) dependsOn `hyperbus-transport`

lazy val `hyperbus-t-distributed-akka` = project.in(file("hyperbus-t-distributed-akka")).settings(defaultSettings) dependsOn(`hyperbus-transport`, `hyperbus-model`)

lazy val `hyperbus-t-kafka` = project.in(file("hyperbus-t-kafka")).settings(defaultSettings) dependsOn(`hyperbus-transport`, `hyperbus-model`)

lazy val `hyperbus-cli` = project.in(file("hyperbus-cli")).settings(defaultSettings) dependsOn(`hyperbus`, `hyperbus-t-distributed-akka`)

lazy val `hyperbus-sbt-plugin` = project.in(file("hyperbus-sbt-plugin")).settings(defaultSettings) // dependsOn `hyperbus-transport`
