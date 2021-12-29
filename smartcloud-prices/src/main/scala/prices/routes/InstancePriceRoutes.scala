package prices.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import prices.data.InstanceKind
import prices.services.InstancePriceService
import prices.services.InstancePriceService.SmartcloudException.{ APICallFailure, APITooManyRequestsFailure, APIUnauthorized }

final case class InstancePriceRoutes[F[_]: Sync](instancePriceService: InstancePriceService[F]) extends Http4sDsl[F] {

  val prefix = "/prices"

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instancePriceService
        .getInstancePrice(InstanceKind("sc2-micro")) // todo get kind from query param
        .flatMap(Ok(_))
        .recoverWith {
          case APIUnauthorized(_)           => InternalServerError()
          case APICallFailure(_)            => InternalServerError()
          case APITooManyRequestsFailure(_) => TooManyRequests("Quota of Smartcloud API exceeded, please try again later.")
        }
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
