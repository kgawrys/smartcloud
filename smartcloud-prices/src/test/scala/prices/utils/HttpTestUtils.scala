package prices.utils

import cats.effect.IO
import cats.implicits.catsSyntaxSemigroup
import io.circe.generic.auto._
import munit.Assertions
import org.http4s.circe._
import org.http4s.{ HttpRoutes, Request, Status }

trait HttpTestUtils extends Assertions {

  import prices.routes.protocol._

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
