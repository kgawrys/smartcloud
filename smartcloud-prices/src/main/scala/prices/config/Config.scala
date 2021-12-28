package prices.config

import cats.effect.kernel.Sync
import prices.config.Config.{ AppConfig, HttpClientConfig, SmartcloudConfig }
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class Config(
    app: AppConfig,
    httpClient: HttpClientConfig,
    smartcloud: SmartcloudConfig
)

object Config {

  case class AppConfig(
      host: String,
      port: Int
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

  case class SmartcloudConfig(
      baseUri: String,
      token: String
  )

  def load[F[_]: Sync]: F[Config] =
    Sync[F].delay(ConfigSource.default.loadOrThrow[Config])

}
