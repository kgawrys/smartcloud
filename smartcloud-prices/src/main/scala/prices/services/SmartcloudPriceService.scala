package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import prices.data.{InstanceKind, InstancePrice}
import prices.routes.protocol.InstancePriceResponse

object SmartcloudPriceService {

  // todo this config could be shared across services
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Concurrent](config: Config): InstancePriceService[F] = new SmartcloudInstancePriceService(config)

  private final class SmartcloudInstancePriceService[F[_]: Concurrent](
      config: Config
  ) extends InstancePriceService[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    val getInstancePrice = s"${config.baseUri}/prices"

    override def getInstancePrice(kinds: List[InstanceKind]): F[List[InstancePriceResponse]] =
      List("sc2-micro", "sc2-small", "sc2-medium")
        .map(kind => InstancePriceResponse(InstanceKind(kind), InstancePrice(0.0)))
        .pure[F]

  }

}
