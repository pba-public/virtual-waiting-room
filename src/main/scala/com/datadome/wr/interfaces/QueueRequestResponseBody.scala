package com.datadome.wr.interfaces

import zio.json.*

final case class QueueRequestResponseBody(@jsonField("request_id") requestId: String, @jsonField("queue_num") queueNum: Long)

object QueueRequestResponseBody:
  given JsonEncoder[QueueRequestResponseBody] =
    DeriveJsonEncoder.gen[QueueRequestResponseBody]
  given JsonDecoder[QueueRequestResponseBody] =
    DeriveJsonDecoder.gen[QueueRequestResponseBody]
