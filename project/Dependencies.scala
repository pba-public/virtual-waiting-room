import sbt._

object Dependencies {
  val ZioVersion   = "2.0.15"
  val ZHTTPVersion = "2.0.0-RC11"

  val `zio-http`      = "io.d11" %% "zhttp" % ZHTTPVersion
  val `zio-http-test` = "io.d11" %% "zhttp" % ZHTTPVersion % Test

  val `zio-test`     = "dev.zio" %% "zio-test"     % ZioVersion % Test
  val `zio-test-sbt` = "dev.zio" %% "zio-test-sbt" % ZioVersion % Test

  val `zio-json` = "dev.zio" %% "zio-json" % "0.3.0-RC11"

  val `zio-prelude` = "dev.zio" %% "zio-prelude" % "1.0.0-RC15"

  val `zio-logging` = "dev.zio" %% "zio-logging-slf4j" % "2.1.0"

  val `logback` = "ch.qos.logback" % "logback-classic" % "1.4.4"

  val `logstash-enc` = "net.logstash.logback" % "logstash-logback-encoder" % "7.1"

  val `concurrent` = "dev.zio" %% "zio-concurrent" % "2.0.1"

  val `kamon` = "io.kamon" %% "kamon-core" % "2.6.1"

  val `kprom` = "io.kamon" %% "kamon-prometheus" % "2.6.1"
  
  val `ksys` = "io.kamon" %% "kamon-system-metrics" % "2.6.1"

  val `zquill` = "io.getquill" %% "quill-zio" % "4.6.0"

  val `jdbcquill` = "io.getquill" %% "quill-jdbc-zio" % "4.6.0"

  val `postgres` = "org.postgresql" % "postgresql" % "42.3.1"

  val `conf` = "dev.zio" %% "zio-config" % "3.0.2"

  val `conf-hocon` = "dev.zio" %% "zio-config-typesafe" % "3.0.2"

  val `conf-magnolia` = "dev.zio" %% "zio-config-magnolia" % "3.0.2"
  
  val `redis` = "dev.zio" %% "zio-redis" % "0.2.0"

  val `protobuf` = "dev.zio" %% "zio-schema-protobuf" % "0.4.9"

  val `sttp` = "com.softwaremill.sttp.client3" %% "core" % "3.8.16"
}
