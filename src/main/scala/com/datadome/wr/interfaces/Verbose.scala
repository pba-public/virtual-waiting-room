package com.datadome.wr.interfaces

import zhttp.http.*
import zio.*
import zio.logging.backend.SLF4J
import zio.logging.{LogAnnotation, LogFormat}

object Verbose {
  private val statusLogAnnotation = LogAnnotation[String]("status", (_, i) => i, _.toString)

  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def log[R, E >: Throwable]: Middleware[R, E, Request, Response, Request, Response] =
    new Middleware[R, E, Request, Response, Request, Response] {
      override def apply[R1 <: R, E1 >: E](
          http: Http[R1, E1, Request, Response],
        ): Http[R1, E1, Request, Response] =
        http
          .contramapZIO[R1, E1, Request] { r =>
            for {
              _ <- ZIO.logInfo(s"> ${r.method} ${r.path} ").provide(logger)
            } yield r.addHeader(("start", "10"))
          }
          .mapZIO[R1, E1, Response] { r =>
            ZIO.logAnnotate("status", r.status.code.toString)
            for {
              _ <- ZIO
                .logInfo(s"< ${r.status.code} ${r.status} ${r.headerValue("start")}")
                .provide(logger) @@ statusLogAnnotation(r.status.code.toString)
            } yield r
          }
    }
}
