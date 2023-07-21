import com.datadome.wr.interfaces.*
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.equalTo
import zhttp.http.*
import zhttp.service.Handler
import zhttp.service.Client

import sttp.client3._

object Spec extends ZIOSpecDefault {
  case class WRRequest(request_id: String, queue_num: Int)
  object WRRequest {
    implicit val decoder: JsonDecoder[WRRequest] = DeriveJsonDecoder.gen[WRRequest]
    implicit val encoder: JsonEncoder[WRRequest] = DeriveJsonEncoder.gen[WRRequest]
  }

  val backend      = HttpClientSyncBackend()
  val initResponse = basicRequest

  def spec = suiteAll("http") {

    test("Init should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id001&capacity=0")
        .send(backend)

      assertTrue(initResponse.code.code == 200)
    }

    test("First assign should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id002&capacity=0")
        .send(backend)

      val assignResponse = basicRequest
        .get(uri"http://localhost:8080/assign-queue-num?event_id=id002")
        .send(backend)
      val body: String   = assignResponse.body.right.get
      val maybeWRRequest = body.fromJson[WRRequest]

      assertTrue(initResponse.code.code == 200)
      && assertTrue(assignResponse.code.code == 200)
      && assert(assignResponse.body)(Assertion.isRight)
      && assert(maybeWRRequest)(Assertion.isRight)
      && assertTrue(maybeWRRequest.right.get.queue_num == 1)
    }

    test("Second assign should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id003&capacity=0")
        .send(backend)

      val assignResponse = basicRequest
        .get(uri"http://localhost:8080/assign-queue-num?event_id=id003")
        .send(backend)

      val body: String   = assignResponse.body.right.get
      val maybeWRRequest = body.fromJson[WRRequest]

      val secondAssignResponse = basicRequest
        .get(uri"http://localhost:8080/assign-queue-num?event_id=id003")
        .send(backend)

      val secondBody: String   = secondAssignResponse.body.right.get
      val secondMaybeWRRequest = secondBody.fromJson[WRRequest]

      assertTrue(initResponse.code.code == 200)
      && assertTrue(assignResponse.code.code == 200)
      && assert(assignResponse.body)(Assertion.isRight)
      && assert(maybeWRRequest)(Assertion.isRight)
      && assertTrue(maybeWRRequest.right.get.queue_num == 1)
      && assertTrue(secondAssignResponse.code.code == 200)
      && assert(secondAssignResponse.body)(Assertion.isRight)
      && assert(secondMaybeWRRequest)(Assertion.isRight)
      && assertTrue(secondMaybeWRRequest.right.get.queue_num == 2)
    }

    test("Get serving-num should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id004&capacity=0")
        .send(backend)

      val servingNumResponse = basicRequest
        .get(uri"http://localhost:8080/serving-num?event_id=id004")
        .send(backend)

      val body: String = servingNumResponse.body.right.get

      assertTrue(initResponse.code.code == 200)
      && assertTrue(servingNumResponse.code.code == 200)
      && assert(servingNumResponse.body)(Assertion.isRight)
      && assertTrue(body == "0")
    }

    test("Increment serving-num should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id005&capacity=0")
        .send(backend)

      val incrResponse = basicRequest
        .get(uri"http://localhost:8080/increment-serving-num?event_id=id005&increment=10")
        .send(backend)

      val servingNumResponse = basicRequest
        .get(uri"http://localhost:8080/serving-num?event_id=id005")
        .send(backend)

      val body: String = servingNumResponse.body.right.get

      assertTrue(initResponse.code.code == 200)
      && assertTrue(incrResponse.code.code == 200)
      && assert(servingNumResponse.body)(Assertion.isRight)
      && assertTrue(servingNumResponse.code.code == 200 && body == "10")
    }

    test("Early token generation should be ko") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id006&capacity=0")
        .send(backend)

      val assignResponse = basicRequest
        .get(uri"http://localhost:8080/assign-queue-num?event_id=id006")
        .send(backend)

      val body: String   = assignResponse.body.right.get
      val maybeWRRequest = body.fromJson[WRRequest]

      val requestId        = maybeWRRequest.right.get.request_id
      val genTokenResponse = basicRequest
        .get(uri"http://localhost:8080/generate-token?event_id=id006&request_id=$requestId")
        .send(backend)

      assertTrue(initResponse.code.code == 200)
      && assertTrue(assignResponse.code.code == 200)
      && assert(assignResponse.body)(Assertion.isRight)
      && assert(maybeWRRequest)(Assertion.isRight)
      && assertTrue(maybeWRRequest.right.get.queue_num == 1)
      && assertTrue(genTokenResponse.code.code == 412)
    }

    test("Token generation should be ok") {
      val initResponse = basicRequest
        .get(uri"http://localhost:8080/init-event?event_id=id007&capacity=0")
        .send(backend)

      val assignResponse = basicRequest
        .get(uri"http://localhost:8080/assign-queue-num?event_id=id007")
        .send(backend)

      val body: String   = assignResponse.body.right.get
      val maybeWRRequest = body.fromJson[WRRequest]

      val incrResponse = basicRequest
        .get(uri"http://localhost:8080/increment-serving-num?event_id=id007&increment=10")
        .send(backend)

      val requestId        = maybeWRRequest.right.get.request_id
      val genTokenResponse = basicRequest
        .get(uri"http://localhost:8080/generate-token?event_id=id007&request_id=$requestId")
        .send(backend)

      assertTrue(initResponse.code.code == 200)
      && assertTrue(assignResponse.code.code == 200)
      && assert(assignResponse.body)(Assertion.isRight)
      && assert(maybeWRRequest)(Assertion.isRight)
      && assertTrue(maybeWRRequest.right.get.queue_num == 1)
      && assertTrue(incrResponse.code.code == 200)
      && assertTrue(genTokenResponse.code.code == 200)
    }
  }
}
