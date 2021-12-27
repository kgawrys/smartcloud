package prices.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ EntityEncoder, HttpRoutes }
import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse
import prices.services.InstancePriceService

final case class InstancePriceRoutes[F[_]: Sync](instancePriceService: InstancePriceService[F]) extends Http4sDsl[F] {

  val prefix = "/prices"

  // TODO move this to some separate trait
  implicit val instancePriceResponseEncoder: EntityEncoder[F, List[InstancePriceResponse]] = jsonEncoderOf[F, List[InstancePriceResponse]]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instancePriceService.getInstancePrice(InstanceKind("sc2-micro")).flatMap(Ok(_))
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
