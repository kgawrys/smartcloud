package prices

import cats.effect._
import cats.syntax.semigroupk._
import fs2.Stream
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{ RequestLogger, ResponseLogger }
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudPriceService }

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    lazy val httpClient: Resource[IO, Client[IO]] = EmberClientBuilder
      .default[IO]
      .withTimeout(config.httpClient.timeout)
      .withIdleTimeInPool(config.httpClient.idleTimeInPool)
      .build

    val instanceKindService  = httpClient.map(client => SmartcloudInstanceKindService.make[IO](config.smartcloud))
    val instancePriceService = httpClient.map(client => SmartcloudPriceService.make[IO](client, config.smartcloud))

    val routes: Resource[IO, HttpApp[IO]] = for {
      instanceKindService  <- instanceKindService
      instancePriceService <- instancePriceService
    } yield (InstanceKindRoutes[IO](instanceKindService).routes <+> InstancePriceRoutes[IO](instancePriceService).routes).orNotFound

    val loggers: HttpApp[IO] => HttpApp[IO] = {
      { http: HttpApp[IO] =>
        RequestLogger.httpApp(true, true)(http)
      } andThen { http: HttpApp[IO] =>
        ResponseLogger.httpApp(true, true)(http)
      }
    }

    Stream
      .resource(routes)
      .evalMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.app.host)
          .withPort(config.app.port)
          .withHttpApp(loggers(routes))
          .build
          .useForever
      }
  }

}
