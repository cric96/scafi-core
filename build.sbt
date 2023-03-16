val scalatest = "org.scalatest" %% "scalatest" % "3.2.15" % "test"
val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
val playJson = "com.typesafe.play" %% "play-json" % "2.9.4"

ThisBuild / scalaVersion := "2.13.10"

lazy val scafiJVM: Seq[ProjectReference] = Seq(
  core
)

lazy val scafi = project
  .in(file("."))
  .aggregate(scafiJVM: _*)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scafi-core",
    libraryDependencies += scalatest
  )
