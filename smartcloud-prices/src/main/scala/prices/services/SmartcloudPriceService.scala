package prices.services

import cats.MonadError
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
import prices.services.InstancePriceService.SmartcloudException
import prices.services.InstancePriceService.SmartcloudException.{ APICallFailure, APITooManyRequestsFailure, APIUnauthorized, KindNotFound }
import prices.services.domain.dto.SmartcloudInstancePriceResponse

object SmartcloudPriceService {

  def make[F[_]: Async: Logger](
      client: Client[F],
      config: SmartcloudConfig
  ): InstancePriceService[F] =
    new SmartcloudInstancePriceService(
      client,
      config
    )

  private final class SmartcloudInstancePriceService[F[_]: Async: Logger](
      client: Client[F],
      config: SmartcloudConfig
  ) extends InstancePriceService[F]
      with Http4sClientDsl[F] {

    implicit val instancePricesEntityDecoder: EntityDecoder[F, InstancePriceResponse] = jsonOf[F, InstancePriceResponse]

    val getInstancePricePath = s"${config.baseUri}/instances"

    override def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse] = {
      val result = for {
        uri      <- buildUri(kind)
        request  <- buildRequest(uri)
        response <- sendRequest(client, request)
      } yield response

      result.handleErrorWith {
        case err: SmartcloudException =>
          Logger[F].error(s"Failure occurred during instance price fetching: $err Message: ${err.message}") *>
            MonadError[F, Throwable].raiseError(err)
        case err =>
          Logger[F].error(s"Unexpected failure occurred during instance price fetching: $err Message: ${Option(err.getMessage).getOrElse("")}") *>
            MonadError[F, Throwable].raiseError(err)
      }
    }

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

    private def sendRequest(client: Client[F], request: Request[F]): F[InstancePriceResponse] =
      Logger[F].debug(s"Sending request to Smartcloud: $request") *>
        client
          .run(request)
          .use(handleResponse)

    private def handleResponse(response: Response[F]): F[InstancePriceResponse] =
      response.status match {
        case Status.Ok                   => response.asJsonDecode[SmartcloudInstancePriceResponse].map(transformResponse)
        case st @ Status.NotFound        => KindNotFound(buildMsg(st)).raiseError[F, InstancePriceResponse]
        case st @ Status.TooManyRequests => APITooManyRequestsFailure(buildMsg(st)).raiseError[F, InstancePriceResponse]
        case st @ Status.Unauthorized    => APIUnauthorized(buildMsg(st)).raiseError[F, InstancePriceResponse]
        case st                          => APICallFailure(buildMsg(st)).raiseError[F, InstancePriceResponse]
      }

    private def transformResponse(resp: SmartcloudInstancePriceResponse): InstancePriceResponse =
      InstancePriceResponse(
        kind = InstanceKind(resp.kind),
        amount = InstancePrice(resp.price)
      )

    private def buildMsg(st: Status) = s"Failed with code: ${st.code} and message: ${Option(st.reason).getOrElse("unknown")}"
  }

}
