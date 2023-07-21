import Dependencies._

ThisBuild / organization := "com.datadome"
ThisBuild / version      := "1.1"

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(BuildHelper.stdSettings)
  .settings(
    name := "WaitinRoom",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio-test`,
      `zio-test-sbt`,
      `zio-http`,
      `zio-http-test`,
      `zio-json`,
      `zio-prelude`,
      `zio-logging`,
      `logback`,
      `logstash-enc`,
      `concurrent`,
      `kamon`,
      `kprom`,
      `ksys`,
      `zquill`,
      `jdbcquill`,
      `postgres`,
      `conf`,
      `conf-hocon`,
      `conf-magnolia`,
      `redis`,
      `protobuf`,
      `sttp`
    ),
  )
  .settings(
    Docker / version          := version.value,
    Compile / run / mainClass := Option("com.datadome.wr.infrastructure.WaitingRoom"),
  )

addCommandAlias("fmt", "scalafmt; Test / scalafmt; sFix;")
addCommandAlias("fmtCheck", "scalafmtCheck; Test / scalafmtCheck; sFixCheck")
addCommandAlias("sFix", "scalafix OrganizeImports; Test / scalafix OrganizeImports")
addCommandAlias(
  "sFixCheck",
  "scalafix --check OrganizeImports; Test / scalafix --check OrganizeImports",
)
