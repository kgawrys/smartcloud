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
import prices.data.{ InstanceKind, InstancePrice }
import prices.routes.protocol.InstancePriceResponse
import prices.services.InstancePriceService.Exception.{ APICallFailure, APITooManyRequestsFailure }
import prices.services.domain.SmartcloudInstancePriceResponse

// todo check and remove printlns if any
object SmartcloudPriceService {

  // todo this config could be shared across services
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Async: Logger](client: Resource[F, Client[F]], config: Config, smartcloudAuthService: SmartcloudAuthService[F]): InstancePriceService[F] =
    new SmartcloudInstancePriceService(
      client,
      config,
      smartcloudAuthService
    )

  private final class SmartcloudInstancePriceService[F[_]: Async: Logger](
      client: Resource[F, Client[F]],
      config: Config,
      smartcloudAuthService: SmartcloudAuthService[F]
  ) extends InstancePriceService[F]
      with Http4sClientDsl[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, InstancePriceResponse] = jsonOf[F, InstancePriceResponse]

    val getInstancePricePath = s"${config.baseUri}/instances"

    private def buildRequest(uri: Uri): Request[F] =
      GET(
        uri,
        Authorization(Credentials.Token(AuthScheme.Bearer, "lxwmuKofnxMxz6O2QE1Ogh")), // todo mock some service that returns auth
        Accept(MediaType.application.json)
      )

    // todo rewrite to for compr
    override def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse] =
      Uri
        .fromString(getInstancePricePath + s"/${kind.getString}") // todo perhaps adding kind to url should be somewhere else
        .liftTo[F]
        .flatMap { uri =>
          val request = buildRequest(uri) // todo add to error handling flow
          client.use { client =>
            client
              .run(request)
              .use(handleResponse)
          }
        }

    private def handleResponse(response: Response[F]): F[InstancePriceResponse] =
      response.status match {
        case Status.Ok =>
          response.asJsonDecode[SmartcloudInstancePriceResponse].map { res =>
            InstancePriceResponse(
              kind = InstanceKind(res.kind),
              amount = InstancePrice(res.price)
            )
          }
        case st @ Status.TooManyRequests => // todo add info for user about exceeded quota and try again later
          val msg = buildMsg(st)
          Logger[F].warn(msg) *>
            APITooManyRequestsFailure(msg).raiseError[F, InstancePriceResponse]
        case st =>
          val msg = buildMsg(st)
          Logger[F].error(msg) *>
            APICallFailure(msg).raiseError[F, InstancePriceResponse]
      }

    private def buildMsg(st: Status) = s"Failed with code: ${st.code} and message: ${Option(st.reason).getOrElse("unknown")}"
  }

}
