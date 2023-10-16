val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ConfigExtractor",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.2",
      "org.scalameta" %% "munit" % "0.7.29" % Test)
  )
