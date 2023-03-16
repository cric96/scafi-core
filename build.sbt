val scalatest = "org.scalatest" %% "scalatest" % "3.2.15" % "test"
val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
val playJson = "com.typesafe.play" %% "play-json" % "2.9.4"

ThisBuild / scalaVersion := "2.13.6"

lazy val scafiJVM: Seq[ProjectReference] = Seq(
  core,
  commons,
)

lazy val scafi = project
  .in(file("."))
  .aggregate(scafiJVM:_*)

lazy val commons = project
  .in(file("commons"))
  .settings(
    name := "scafi-commons"
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(commons)
  .settings(
    name := "scafi-core",
    libraryDependencies += scalatest
  )