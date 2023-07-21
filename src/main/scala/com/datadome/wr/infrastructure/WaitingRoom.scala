package com.datadome.wr.infrastructure

import com.datadome.wr.interfaces.{Verbose, app}
import kamon.Kamon
import zhttp.http.*
import zhttp.service.Server
import zio.*
import zio.concurrent.ConcurrentMap
import zio.json.*
import zio.logging.*
import zio.logging.backend.SLF4J
import zio.redis.*
import zio.schema.*
import zio.schema.codec.* 

object WaitingRoom extends ZIOAppDefault {

  object ProtobufCodecSupplier extends CodecSupplier {
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
  }

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    println("WaitingRoom is starting ...")
    Kamon.init()
    Server
      .start(
        port = 8080,
        http = app() @@ (Verbose.log),
      )
      .provide(
        Redis.layer,
        RedisExecutor.layer,
        ZLayer.succeed(RedisConfig.Default),
        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
      )
  }
}
