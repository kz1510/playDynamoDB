import sbt.Keys._

name := "playDynamoDB"

val commonSettings: Seq[Setting[_]] = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(
    "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
  ),
  scalacOptions ++= Seq(
    // https://github.com/scala/scala/blob/2.11.x/src/compiler/scala/tools/nsc/settings
    "-deprecation" // Emit warning and location for usages of deprecated APIs.
    , "-feature" // Emit warning and location for usages of features that should be imported explicitly.
    , "-unchecked" // Enable additional warnings where generated code depends on assumptions.
    , "-Xlint" // Enable recommended additional warnings.
  ),
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
)

addCommandAlias("du", "dependencyUpdates")

lazy val root = {
  Project(id = "root", base = file("."))
    .settings(commonSettings: _*)
    .enablePlugins(PlayScala)
    .settings(
      // Play provides two styles of routers, one expects its actions to be injected, the
      // other, legacy style, accesses its actions statically.
      routesGenerator := InjectedRoutesGenerator
      , routesImport ++= Seq(
      )
      , TwirlKeys.templateImports ++= Seq(
      )
      , libraryDependencies ++= Seq(
        specs2 % Test,
        "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.26"
      )
    )
}