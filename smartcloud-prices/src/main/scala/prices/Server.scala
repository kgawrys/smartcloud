package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudPriceService }
import cats.syntax.semigroupk._

// TODO add macwire
object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val instanceKindService = SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
    )

    // todo rename from price to kind service
    // todo share config
    val instancePriceService = SmartcloudPriceService.make[IO](
      SmartcloudPriceService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
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
