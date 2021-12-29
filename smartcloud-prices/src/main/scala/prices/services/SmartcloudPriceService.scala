package prices.services

import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.{ MediaType, _ }
import org.typelevel.log4cats.Logger
import prices.config.Config.SmartcloudConfig
import prices.data.{ InstanceKind, InstancePrice }
import prices.routes.protocol.InstancePriceResponse
import prices.services.InstancePriceService.Exception.{ APICallFailure, APITooManyRequestsFailure, APIUnauthorized }
import prices.services.domain.dto.SmartcloudInstancePriceResponse

// todo check and remove printlns if any
object SmartcloudPriceService {

  def make[F[_]: Async: Logger](
      client: Resource[F, Client[F]],
      config: SmartcloudConfig
  ): InstancePriceService[F] =
    new SmartcloudInstancePriceService(
      client,
      config
    )

  private final class SmartcloudInstancePriceService[F[_]: Async: Logger](
      client: Resource[F, Client[F]],
      config: SmartcloudConfig
  ) extends InstancePriceService[F]
      with Http4sClientDsl[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, InstancePriceResponse] = jsonOf[F, InstancePriceResponse]

    val getInstancePricePath = s"${config.baseUri}/instances"

    override def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse] =
      for {
        uri      <- buildUri(kind)
        request  <- buildRequest(uri)
        response <- sendRequest(client, request)
      } yield response

    private def buildUri(kind: InstanceKind): F[Uri] =
      Uri.fromString(getInstancePricePath + s"/${kind.getString}").liftTo[F]

    private def buildRequest(uri: Uri): F[Request[F]] =
      Async[F].pure(
        GET(
          uri,
          Authorization(Credentials.Token(AuthScheme.Bearer, config.token)),
          Accept(MediaType.application.json)
        )
      )

    private def sendRequest(client: Resource[F, Client[F]], request: Request[F]): F[InstancePriceResponse] =
      Logger[F].debug(s"Sending request to Smartcloud: $request") *>
        client.use { client =>
          client
            .run(request)
            .use(handleResponse)
        }

    private def handleResponse(response: Response[F]): F[InstancePriceResponse] =
      response.status match {
        case Status.Ok =>
          response.asJsonDecode[SmartcloudInstancePriceResponse].map(transformResponse)
        case st @ Status.TooManyRequests =>
          val msg = buildMsg(st)
          Logger[F].warn(msg) *>
            APITooManyRequestsFailure(msg).raiseError[F, InstancePriceResponse]
        case st @ Status.Unauthorized =>
          val msg = buildMsg(st)
          Logger[F].error(msg) *>
            APIUnauthorized(msg).raiseError[F, InstancePriceResponse]
        case st =>
          val msg = buildMsg(st)
          Logger[F].error(msg) *>
            APICallFailure(msg).raiseError[F, InstancePriceResponse]
      }

    private def transformResponse(resp: SmartcloudInstancePriceResponse): InstancePriceResponse =
      InstancePriceResponse(
        kind = InstanceKind(resp.kind),
        amount = InstancePrice(resp.price)
      )

    private def buildMsg(st: Status) = s"Failed with code: ${st.code} and message: ${Option(st.reason).getOrElse("unknown")}"
  }

}
