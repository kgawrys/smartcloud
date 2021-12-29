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

    def instanceKindService(httpClient: Client[IO])  = SmartcloudInstanceKindService.make[IO](config.smartcloud)
    def instancePriceService(httpClient: Client[IO]) = SmartcloudPriceService.make[IO](httpClient, config.smartcloud)

    def instanceKindRoutes(httpClient: Client[IO])  = InstanceKindRoutes[IO](instanceKindService(httpClient)).routes
    def instancePriceRoutes(httpClient: Client[IO]) = InstancePriceRoutes[IO](instancePriceService(httpClient)).routes

    val loggers: HttpApp[IO] => HttpApp[IO] = {
      { http: HttpApp[IO] =>
        RequestLogger.httpApp(true, true)(http)
      } andThen { http: HttpApp[IO] =>
        ResponseLogger.httpApp(true, true)(http)
      }
    }

    def routes(httpClient: Client[IO]) = (instanceKindRoutes(httpClient) <+> instancePriceRoutes(httpClient)).orNotFound

    Stream
      .resource(httpClient)
      .evalMap { httpClient =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.app.host)
          .withPort(config.app.port)
          .withHttpApp(loggers(routes(httpClient)))
          .build
          .useForever
      }
  }

}
