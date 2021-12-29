package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{ Logger, RequestLogger, ResponseLogger }
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudPriceService }
import cats.syntax.semigroupk._
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

// TODO add macwire
object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    // todo add to separate class resource
    lazy val httpClient: Resource[IO, Client[IO]] = EmberClientBuilder
      .default[IO]
      .withTimeout(config.httpClient.timeout)
      .withIdleTimeInPool(config.httpClient.idleTimeInPool)
      .build

    val instanceKindService = SmartcloudInstanceKindService.make[IO](config.smartcloud)

    val instancePriceService = SmartcloudPriceService.make[IO](
      httpClient,
      config.smartcloud
    )

    val loggers: HttpApp[IO] => HttpApp[IO] = {
      { http: HttpApp[IO] =>
        RequestLogger.httpApp(true, true)(http)
      } andThen { http: HttpApp[IO] =>
        ResponseLogger.httpApp(true, true)(http)
      }
    }

    val routes = (
      InstanceKindRoutes[IO](instanceKindService).routes <+> InstancePriceRoutes[IO](instancePriceService).routes
    ).orNotFound

    // todo extract building server to separate class
    Stream
      .eval(
        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.app.host).get)
          .withPort(Port.fromInt(config.app.port).get)
          .withHttpApp(loggers(routes))
          .build
          .useForever
      )
  }

}
