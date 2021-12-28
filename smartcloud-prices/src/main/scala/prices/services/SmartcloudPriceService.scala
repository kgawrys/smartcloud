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
import prices.services.InstancePriceService.Exception.{ APICallFailure, APITooManyRequestsFailure }

object SmartcloudPriceService {

  // todo this config could be shared across services
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Async: Logger](config: Config, smartcloudAuthService: SmartcloudAuthService[F]): InstancePriceService[F] = new SmartcloudInstancePriceService(
    config,
    smartcloudAuthService
  )

  private final class SmartcloudInstancePriceService[F[_]: Async: Logger](
      config: Config,
      smartcloudAuthService: SmartcloudAuthService[F]
  ) extends InstancePriceService[F]
      with Http4sClientDsl[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, InstancePriceResponse] = jsonOf[F, InstancePriceResponse]

    val getInstancePricePath = s"${config.baseUri}/instances"

    // todo add configurable timeout and idleTimeInPool
    lazy val client: Resource[F, Client[F]] = EmberClientBuilder
      .default[F]
//      .withTimeout(c.timeout)
//      .withIdleTimeInPool(c.idleTimeInPool)
      .build

    private def buildRequest(uri: Uri, kind: InstanceKind): Request[F] = {
      val uriWithQueryParams = uri.withQueryParam("kind", kind.getString) // todo consider when no query params are passed
      GET(
        uriWithQueryParams,
        Authorization(Credentials.Token(AuthScheme.Bearer, "lxwmuKofnxMxz6O2QE1Ogh")), // todo mock some service that returns auth
        Accept(MediaType.application.json)
      )
    }

    // todo rewrite to for compr
    override def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse] =
      Uri
        .fromString(getInstancePricePath)
        .liftTo[F]
        .flatMap { uri =>
          val request = buildRequest(uri, kind) // todo add to error handling flow
          client.use { client =>
            client
              .run(request)
              .use { resp =>
                resp.status match { // todo handle unauth
                  case Status.Ok =>
                    resp.asJsonDecode[InstancePriceResponse]
                  case st @ Status.TooManyRequests =>
                    val msg = buildMsg(st)
                    Logger[F].warn(msg) *>
                      APITooManyRequestsFailure(msg).raiseError[F, InstancePriceResponse]
                  case st =>
                    val msg = buildMsg(st)
                    Logger[F].error(msg) *>
                      APICallFailure(msg).raiseError[F, InstancePriceResponse]
                }
              }
          }
        }

    private def buildMsg(st: Status) = s"Failed with code: ${st.code} and message: ${Option(st.reason).getOrElse("unknown")}"
  }

}
