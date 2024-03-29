name := """Triviador"""
organization := "student"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.7"


// @see https://mvnrepository.com/artifact/mysql/mysql-connector-java
libraryDependencies ++= Seq(
    guice,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    jdbc,
    "mysql" % "mysql-connector-java" % "5.1.46",
    "com.typesafe.play" %% "anorm" % "2.5.3"
)

libraryDependencies += ws
libraryDependencies ++= Seq(
  "org.reactivemongo" % "play2-reactivemongo_2.12" % "0.17.1-play27"
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "student.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "student.binders._"
