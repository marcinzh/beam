ThisBuild / organization := "io.github.marcinzh"
ThisBuild / version := "0.8.0"
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / crossScalaVersions := Seq(scalaVersion.value)

ThisBuild / watchBeforeCommand := Watch.clearScreen
ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger
ThisBuild / watchForceTriggerOnAnyChange := true

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  // "-Wnonunit-statement",
  "-Xfatal-warnings",
  "-Ykind-projector:underscores",
  // Seq(
  //   "java.lang",
  //   "scala",
  //   "scala.Predef",
  //   "scala.util.chaining",
  // ).mkString("-Yimports:", ",", "")
)

val Deps = {
  val tur_v = "0.98.0"
  object deps {
    val specs2_core = "org.specs2" %% "specs2-core" % "5.4.0" % "test"
    val turbolift_core = "io.github.marcinzh" %% "turbolift-core" % tur_v
    val turbolift_bindless = "io.github.marcinzh" %% "turbolift-bindless" % tur_v
    val betterFiles = ("com.github.pathikrit" %% "better-files" % "3.9.1").cross(CrossVersion.for3Use2_13)
  }
  deps
}

lazy val root = project
  .in(file("."))
  .settings(sourcesInBase := false)
  .settings(publish / skip := true)
  .aggregate(core, examples)

lazy val core = project
  .in(file("modules/core"))
  .settings(name := "beam-core")
  .settings(testSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Deps.turbolift_core,
  ))

lazy val examples = project
  .in(file("modules/examples"))
  .settings(name := "beam-examples")
  .settings(publish / skip := true)
  .settings(libraryDependencies ++= Seq(
    Deps.turbolift_bindless,
  ))
  .dependsOn(core)

//=================================================

lazy val testSettings = Seq(
  libraryDependencies += Deps.specs2_core,
  Test / parallelExecution := false,
)

ThisBuild / description := "Stream processing with algebraic effects and handlers"
ThisBuild / organizationName := "marcinzh"
ThisBuild / homepage := Some(url("https://github.com/marcinzh/beam"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/marcinzh/beam"), "scm:git@github.com:marcinzh/beam.git"))
ThisBuild / licenses := List("MIT" -> new URL("http://www.opensource.org/licenses/MIT"))
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  isSnapshot.value match {
    case true => Some("snapshots" at nexus + "content/repositories/snapshots")
    case false => Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}
ThisBuild / pomExtra := (
  <developers>
    <developer>
      <id>marcinzh</id>
      <name>Marcin Żebrowski</name>
      <url>https://github.com/marcinzh</url>
    </developer>
  </developers>
)
