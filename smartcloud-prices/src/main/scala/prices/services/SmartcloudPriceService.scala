package prices.services

import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers._
import org.http4s.{ MediaType, _ }
import org.typelevel.log4cats.Logger
import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse
import prices.services.InstancePriceService.Exception.APICallFailure

object SmartcloudPriceService {

  // todo this config could be shared across services
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Async](config: Config): InstancePriceService[F] = new SmartcloudInstancePriceService(config)

  private final class SmartcloudInstancePriceService[F[_]: Async](
      config: Config
  ) extends InstancePriceService[F]
      with Http4sClientDsl[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, InstancePriceResponse] = jsonOf[F, InstancePriceResponse]

    val getInstancePricePath = s"${config.baseUri}/prices"

    // todo add configurable timeout and idleTimeInPool
    lazy val client: Resource[F, Client[F]] = EmberClientBuilder
      .default[F]
//      .withTimeout(c.timeout)
//      .withIdleTimeInPool(c.idleTimeInPool)
      .build

    override def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse] =
      Uri
        .fromString(getInstancePricePath)
        .liftTo[F]
        .flatMap { uri =>
          val request: Request[F] = GET(
            uri,
            Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame")),
            Accept(MediaType.application.json)
          )
          client.use { client =>
            client
              .run(request)
              .use { resp =>
                resp.status match {
                  case Status.Ok | Status.Conflict =>
                    resp.asJsonDecode[InstancePriceResponse]
                  case st => // todo handle quota exceeded
                    APICallFailure(
                      Option(st.reason).getOrElse("unknown")
                    ).raiseError[F, InstancePriceResponse]
                }
              }
          }
        }
  }

}
