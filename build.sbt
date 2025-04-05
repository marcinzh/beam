val ScalaLTS = "3.3.5"
val ScalaNext = "3.6.4"
ThisBuild / organization := "io.github.marcinzh"
ThisBuild / version := "0.13.0-SNAPSHOT"
ThisBuild / scalaVersion := ScalaLTS
ThisBuild / crossScalaVersions := Seq(ScalaLTS, ScalaNext)
ThisBuild / watchBeforeCommand := Watch.clearScreen
ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger
ThisBuild / watchForceTriggerOnAnyChange := true

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  // "-Wnonunit-statement",
  "-Xfatal-warnings",
  // Seq(
  //   "java.lang",
  //   "scala",
  //   "scala.Predef",
  //   "scala.util.chaining",
  // ).mkString("-Yimports:", ",", "")
)
ThisBuild / scalacOptions += {
  if (VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=3.4.0")))
    "-Xkind-projector:underscores"
  else
    "-Ykind-projector:underscores"
}
ThisBuild / publish / skip := (scalaVersion.value != ScalaLTS)


val Deps = {
  val tur_v = "0.108.0"
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
  .settings(libraryDependencies += Deps.turbolift_core)

lazy val examples = project
  .in(file("modules/examples"))
  .settings(name := "beam-examples")
  .settings(publish / skip := true)
  .settings(Compile / run / mainClass := Some("runner.Main"))
  .settings(libraryDependencies += Deps.turbolift_bindless)
  .dependsOn(core)

//=================================================

val cls = taskKey[Unit]("cls")
cls := { print("\u001b[0m\u001b[2J\u001bc") }

lazy val testSettings = Seq(
  libraryDependencies += Deps.specs2_core,
  Test / parallelExecution := false,
)

ThisBuild / description := "Stream processing with algebraic effects and handlers"
ThisBuild / organizationName := "marcinzh"
ThisBuild / homepage := Some(url("https://github.com/marcinzh/beam"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/marcinzh/beam"), "scm:git@github.com:marcinzh/beam.git"))
ThisBuild / licenses := List("MIT" -> url("http://www.opensource.org/licenses/MIT"))
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
