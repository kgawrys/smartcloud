package prices.utils

import cats.effect.IO
import cats.implicits.catsSyntaxSemigroup
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import munit.Assertions
import org.http4s.circe._
import org.http4s.{ HttpRoutes, Request, Status }
import prices.data.{ InstanceKind, InstancePrice }
import prices.routes.protocol.InstancePriceResponse

trait HttpTestUtils extends Assertions {

  implicit val InstanceKindDecoder: Decoder[InstanceKind]   = deriveUnwrappedDecoder
  implicit val InstancePriceDecoder: Decoder[InstancePrice] = deriveUnwrappedDecoder

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status): IO[Unit] =
    routes.run(req).value.map {
      case Some(resp) => assertEquals(resp.status, expectedStatus)
      case None       => fail("Not found route")
    }

  def expectHttpStatusAndResponse(
      routes: HttpRoutes[IO],
      req: Request[IO]
  )(expectedStatus: Status, expectedResponse: InstancePriceResponse): IO[Unit] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp
          .asJsonDecode[InstancePriceResponse]
          .map { response =>
            assertEquals(resp.status, expectedStatus) |+| assertEquals(response, expectedResponse)
          }
      case None => IO.pure(fail("Not found route"))
    }
}
