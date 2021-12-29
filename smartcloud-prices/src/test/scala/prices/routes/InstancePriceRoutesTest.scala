package prices.routes

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.{ ->, /, GET, Ok, Root, _ }
import org.http4s.implicits._
import org.http4s.{ HttpRoutes, Response, Status }
import org.typelevel.log4cats.noop.NoOpLogger
import prices.config.Config.SmartcloudConfig
import prices.data.{ InstanceKind, InstancePrice }
import prices.routes.protocol.InstancePriceResponse
import prices.services.SmartcloudPriceService
import prices.utils.HttpTestUtils

class InstancePriceRoutesTest extends CatsEffectSuite with HttpTestUtils {

  implicit val lg    = NoOpLogger[IO]
  private val config = SmartcloudConfig("", "token")

  private def mockedRoutes(mkResponse: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "instances" / _ => mkResponse
      }
      .orNotFound

  test("return 200 and expected body on valid request") {
    val req              = GET(uri"/prices?kind=sc2-micro")
    val client           = Client.fromHttpApp(mockedRoutes(Ok.apply("""{"kind":"sc2-micro","price":1.081}""")))
    val routes           = InstancePriceRoutes[IO](SmartcloudPriceService.make[IO](client, config)).routes
    val expectedResponse = InstancePriceResponse(InstanceKind("sc2-micro"), InstancePrice(1.081))

    expectHttpStatusAndResponse(routes, req)(Status.Ok, expectedResponse)
  }

  test("return 404 when unknown kind passed") {
    val req    = GET(uri"/prices?kind=sc2-micro")
    val client = Client.fromHttpApp(mockedRoutes(NotFound("")))
    val routes = InstancePriceRoutes[IO](SmartcloudPriceService.make[IO](client, config)).routes

    expectHttpStatus(routes, req)(Status.NotFound)
  }

  test("return 500 when Smartcloud returns InternalServerError") {
    val req    = GET(uri"/prices?kind=sc2-micro")
    val client = Client.fromHttpApp(mockedRoutes(InternalServerError("")))
    val routes = InstancePriceRoutes[IO](SmartcloudPriceService.make[IO](client, config)).routes

    expectHttpStatus(routes, req)(Status.InternalServerError)
  }

  test("return 429 when Smartcloud returns TooManyRequests") {
    val req    = GET(uri"/prices?kind=sc2-micro")
    val client = Client.fromHttpApp(mockedRoutes(TooManyRequests("")))
    val routes = InstancePriceRoutes[IO](SmartcloudPriceService.make[IO](client, config)).routes

    expectHttpStatus(routes, req)(Status.TooManyRequests)
  }

  test("return 500 when Smartcloud returns unexpected HTTP error code") {
    val req    = GET(uri"/prices?kind=sc2-micro")
    val client = Client.fromHttpApp(mockedRoutes(PayloadTooLarge("")))
    val routes = InstancePriceRoutes[IO](SmartcloudPriceService.make[IO](client, config)).routes

    expectHttpStatus(routes, req)(Status.InternalServerError)
  }
}
