package prices.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.{ HttpRoutes, QueryParamDecoder }
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import prices.data.InstanceKind
import prices.services.InstancePriceService
import prices.services.InstancePriceService.SmartcloudException.{ APICallFailure, APITooManyRequestsFailure, APIUnauthorized, KindNotFound }

final case class InstancePriceRoutes[F[_]: Sync](instancePriceService: InstancePriceService[F]) extends Http4sDsl[F] {

  val prefix = "/prices"

  implicit val instanceKindQueryParamDecoder: QueryParamDecoder[InstanceKind] = QueryParamDecoder[String].map(InstanceKind)
  object InstanceKindQueryParam extends QueryParamDecoderMatcher[InstanceKind]("kind")

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? InstanceKindQueryParam(instanceKind) =>
      instancePriceService
        .getInstancePrice(instanceKind)
        .flatMap(Ok(_))
        .recoverWith {
          case APIUnauthorized(_)           => InternalServerError()
          case APICallFailure(_)            => InternalServerError()
          case KindNotFound(_)              => NotFound(s"Price not found for given kind.")
          case APITooManyRequestsFailure(_) => TooManyRequests("Quota of Smartcloud API exceeded, please try again later.")
        }
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )
}
