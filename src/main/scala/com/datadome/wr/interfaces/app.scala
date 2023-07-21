package com.datadome.wr.interfaces

import com.datadome.wr.model.*
import kamon.Kamon
import zhttp.http.*
import zhttp.service.Logging
import zio.*
import zio.concurrent.ConcurrentMap
import zio.config.*
import ConfigDescriptor.*
import ConfigSource.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.json.*
import zio.logging.*
import zio.logging.backend.SLF4J
import zio.metrics.*
import zio.prelude.Validation
import zio.redis.*

import java.io.{PrintWriter, StringWriter}
import java.util.UUID
import scala.io.Source

object app {
  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  case class AppConfig(secret: String)
  val INIT_EVENT_PATH       = !! / "init-event"
  val ASSIGN_QUEUE_NUM_PATH = !! / "assign-queue-num"
  val SERVING_NUM_PATH      = !! / "serving-num"
  val INCR_SERVING_NUM_PATH = !! / "increment-serving-num"
  val GENERATE_TOKEN_PATH   = !! / "generate-token"
  val counterMetricName     = "wr.api.counter"

  def apply(
    ): Http[Redis, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      // GET "init-event?event_id=001&capacity=10"
      case req @ (Method.GET -> INIT_EVENT_PATH) =>
        val maybeEventIds = req.url.queryParams.get("event_id")
        val maybeEventId  = maybeEventIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val maybeCapacities = req.url.queryParams.get("capacity")
        val maybeCapacity  = maybeCapacities match {
          case Some(c :: t) => Some(c)
          case _             => None
        }
        val init          = for {
          eventId <- maybeEventId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: event_id"))
          }
          capacity <- maybeCapacity match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: capacity"))
          }
          redis   <- ZIO.service[Redis]
          _       <- redis.set("QUEUE_NUM:" + eventId, "0")
          _       <- redis.set("SERVING_NUM:" + eventId, capacity)
        } yield ()
        init.foldZIO(
          e =>
            e match {
              case IllegalArgumentsError(message) =>
                handleErrorResponse(
                  INIT_EVENT_PATH,
                  Method.GET,
                  Status.BadRequest,
                  Some(message),
                )
              case _                              =>
                handleErrorResponse(
                  INIT_EVENT_PATH,
                  Method.GET,
                  Status.InternalServerError,
                  Some(getPrintableStacktraceFrom(e)),
                )
            },
          _ =>
            handleSuccessResponse(
              Response.status(Status.Ok),
              INIT_EVENT_PATH,
              Method.GET,
              Status.Ok,
              Map.empty,
            ),
        )

      // GET "assign-queue-num?event_id=001"
      case req @ (Method.GET -> ASSIGN_QUEUE_NUM_PATH) =>
        val maybeEventIds = req.url.queryParams.get("event_id")
        val maybeEventId  = maybeEventIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val requestId     = UUID.randomUUID.toString()
        val qr            = for
          eventId    <- maybeEventId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: event_id"))
          }
          requestKey <- ZIO.succeed(eventId + ":" + requestId)
          redis      <- ZIO.service[Redis]
          incr       <- redis.incr("QUEUE_NUM:" + maybeEventId.get)
          //TODO Duration for expiration must be an event parameter
          _ <- redis.set("REQUEST:" + requestKey, incr.toString(), Some(1.hour))
        yield QueueRequestResponseBody(requestId, incr)
        qr.foldZIO(
          e =>
            e match {
              case IllegalArgumentsError(message) =>
                handleErrorResponse(
                  ASSIGN_QUEUE_NUM_PATH,
                  Method.GET,
                  Status.BadRequest,
                  Some(message),
                )
              case _                              =>
                handleErrorResponse(
                  ASSIGN_QUEUE_NUM_PATH,
                  Method.GET,
                  Status.InternalServerError,
                  Some(getPrintableStacktraceFrom(e)),
                )
            },
          q =>
            handleSuccessResponse(
              Response.json(q.toJson),
              ASSIGN_QUEUE_NUM_PATH,
              Method.GET,
              Status.Ok,
              Map.empty,
            ),
        )

      // GET "serving-num?event_id=001"
      case req @ (Method.GET -> SERVING_NUM_PATH) =>
        val maybeEventIds = req.url.queryParams.get("event_id")
        val maybeEventId  = maybeEventIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val qr            = for {
          eventId    <- maybeEventId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: event_id"))
          }
          redis      <- ZIO.service[Redis]
          servingNum <- redis.get("SERVING_NUM:" + eventId).returning[String]
        } yield servingNum
        qr.foldZIO(
          e =>
            e match {
              case IllegalArgumentsError(message) =>
                handleErrorResponse(
                  SERVING_NUM_PATH,
                  Method.GET,
                  Status.BadRequest,
                  Some(message),
                )
              case _                              =>
                handleErrorResponse(
                  SERVING_NUM_PATH,
                  Method.GET,
                  Status.InternalServerError,
                  Some(getPrintableStacktraceFrom(e)),
                )
            },
          q =>
            handleSuccessResponse(
              Response.text(q.get),
              SERVING_NUM_PATH,
              Method.GET,
              Status.Ok,
              Map.empty,
            ),
        )

      // GET "increment-serving-num?event_id=001&increment=10"
      case req @ (Method.GET -> INCR_SERVING_NUM_PATH) =>
        val maybeEventIds   = req.url.queryParams.get("event_id")
        val maybeEventId    = maybeEventIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val maybeIncrements = req.url.queryParams.get("increment")
        val maybeIncrement  = maybeIncrements match {
          case Some(incr :: t) => Some(incr)
          case _               => None
        }
        val qr              = for {
          eventId <- maybeEventId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: event_id"))
          }
          incr    <- maybeIncrement match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: increment"))
          }
          redis   <- ZIO.service[Redis]
          _       <- redis.incrBy("SERVING_NUM:" + eventId, incr.toInt)
        } yield ()
        qr.foldZIO(
          e =>
            e match {
              case IllegalArgumentsError(message) =>
                handleErrorResponse(
                  INCR_SERVING_NUM_PATH,
                  Method.GET,
                  Status.BadRequest,
                  Some(message),
                )
              case _                              =>
                handleErrorResponse(
                  INCR_SERVING_NUM_PATH,
                  Method.GET,
                  Status.InternalServerError,
                  Some(getPrintableStacktraceFrom(e)),
                )
            },
          _ =>
            handleSuccessResponse(
              Response.status(Status.Ok),
              INCR_SERVING_NUM_PATH,
              Method.GET,
              Status.Ok,
              Map.empty,
            ),
        )

      // GET "serving-num?event_id=001&request_id=f4a6c8be-9bb3-471a-bd7d-fcb2399ec55a"
      case req @ (Method.GET -> GENERATE_TOKEN_PATH) =>
        val maybeEventIds   = req.url.queryParams.get("event_id")
        val maybeEventId    = maybeEventIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val maybeRequestIds = req.url.queryParams.get("request_id")
        val maybeRequestId  = maybeRequestIds match {
          case Some(id :: t) => Some(id)
          case _             => None
        }
        val jwt             = for {
          eventId            <- maybeEventId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: event_id"))
          }
          requestId          <- maybeRequestId match {
            case Some(e) => ZIO.succeed(e)
            case None    => ZIO.fail(IllegalArgumentsError("Missing parameter: request_id"))
          }
          requestKey         <- ZIO.succeed(eventId + ":" + requestId)
          redis              <- ZIO.service[Redis]
          maybeExistingToken <- redis.get("TOKEN:" + requestKey).returning[String]
          _                  <- maybeExistingToken match {
            case Some(t) => ZIO.fail(TokenAlreadyExistsError(t))
            case _       => ZIO.succeed("")
          }
          maybeAssignedNum   <- redis.get("REQUEST:" + requestKey).returning[String]
          assignedNum        <- maybeAssignedNum match {
            case None    =>
              ZIO.fail(
                EntityNotFoundError(
                  s"Request not found for event id: $eventId and request id: $requestId",
                ),
              )
            case Some(a) => ZIO.succeed(a)
          }
          maybeServingNum    <- redis.get("SERVING_NUM:" + eventId).returning[String]
          servingNum         <- maybeServingNum match {
            case None    =>
              ZIO.fail(EntityNotFoundError(s"Serving number not found for event id: $eventId"))
            case Some(a) => ZIO.succeed(a)
          }
          token              <- ZIO.succeed(generateToken())
          _                  <-
            if (assignedNum.toInt <= servingNum.toInt)
              //TODO Duration for expiration must be an event parameter
              redis.set("TOKEN:" + requestKey, token.toString, Some(5.minutes))
            else ZIO.fail(StillInWaitingRoomError())
        } yield token
        jwt.foldZIO(
          e =>
            e match {
              case IllegalArgumentsError(message) =>
                handleErrorResponse(
                  GENERATE_TOKEN_PATH,
                  Method.GET,
                  Status.BadRequest,
                  Some(message),
                )
              case TokenAlreadyExistsError(t)     =>
                handleSuccessResponse(
                  Response.status(Status.Ok),
                  GENERATE_TOKEN_PATH,
                  Method.GET,
                  Status.Ok,
                  Map.empty,
                )
                ZIO.succeed(Response.text(t.toString()))
              case StillInWaitingRoomError()      =>
                handleErrorResponse(
                  GENERATE_TOKEN_PATH,
                  Method.GET,
                  Status.PreconditionFailed,
                  Some("Not yet eligible"),
                )
              case EntityNotFoundError(message)   =>
                handleErrorResponse(GENERATE_TOKEN_PATH, Method.GET, Status.NotFound, Some(message))
              case _                              =>
                handleErrorResponse(
                  GENERATE_TOKEN_PATH,
                  Method.GET,
                  Status.InternalServerError,
                  Some(getPrintableStacktraceFrom(e)),
                )
            },
          t =>
            handleSuccessResponse(
              Response.text(t.toString()),
              GENERATE_TOKEN_PATH,
              Method.GET,
              Status.Ok,
              Map.empty,
            ),
        )

      case req @ (Method.GET -> !! / "healthcheck") =>
        for
          redis <- ZIO.service[Redis]
          _     <- redis.set("HEALTH", "CHECK", Some(30.seconds))
        yield Response.status(Status.Ok)
    }

  private def getPrintableStacktraceFrom(e: Throwable): String = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  private def handleSuccessResponse(
      response: Response,
      path: Path,
      method: Method,
      status: Status,
      tagsMap: Map[String, String],
    ) = {
    val counter         = Kamon
      .counter(counterMetricName)
      .withTag("endpoint", path.toString)
      .withTag("method", method.toString)
      .withTag("status", status.code)
    val counterWithTags = tagsMap.foldLeft(counter) { (acc, kv) =>
      acc.withTag(kv._1, kv._2)
    }
    counterWithTags.increment()
    ZIO.succeed(response)
  }

  private def handleErrorResponse(
      endpoint: Path,
      method: Method,
      status: Status,
      errorMessage: Option[String],
    ) = {
    Kamon
      .counter(counterMetricName)
      .withTag("endpoint", endpoint.toString)
      .withTag("method", method.toString())
      .withTag("status", status.code)
      .increment()
    errorMessage match {
      case Some(error) =>
        ZIO
          .logError(error)
          .provide(logger)
          .as(
            Response.text(error).setStatus(status),
          )
      case None        => ZIO.succeed(Response.status(status))
    }
  }

  //TODO Stub for JWT generation
  private def generateToken(): String = UUID.randomUUID.toString()
}
