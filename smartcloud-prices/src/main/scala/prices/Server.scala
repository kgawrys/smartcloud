package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudAuthService, SmartcloudInstanceKindService, SmartcloudPriceService }
import cats.syntax.semigroupk._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

// TODO add macwire
object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    val instanceKindService = SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
    )

    val smartcloudAuthService: SmartcloudAuthService[IO] = new SmartcloudAuthService[IO]

    // todo rename from price to kind service
    // todo share config
    val instancePriceService = SmartcloudPriceService.make[IO](
      SmartcloudPriceService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      ),
      smartcloudAuthService
    )

    val httpApp = (
      InstanceKindRoutes[IO](instanceKindService).routes <+> InstancePriceRoutes[IO](instancePriceService).routes
    ).orNotFound

    // todo add request/response logging

    Stream
      .eval(
        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.app.host).get)
          .withPort(Port.fromInt(config.app.port).get)
          .withHttpApp(Logger.httpApp(true, true)(httpApp))
          .build
          .useForever
      )
  }

}
