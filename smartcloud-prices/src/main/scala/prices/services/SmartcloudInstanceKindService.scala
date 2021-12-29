package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import prices.config.Config.SmartcloudConfig
import prices.data._

object SmartcloudInstanceKindService {

  def make[F[_]: Concurrent](config: SmartcloudConfig): InstanceKindService[F] = new SmartcloudInstanceKindService(config)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](config: SmartcloudConfig) extends InstanceKindService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    val getAllUri = s"${config.baseUri}/instances"

    override def getAll(): F[List[InstanceKind]] =
      List("sc2-micro", "sc2-small", "sc2-medium") // Dummy data. Your implementation should call the smartcloud API.
        .map(InstanceKind(_))
        .pure[F]

  }

}
