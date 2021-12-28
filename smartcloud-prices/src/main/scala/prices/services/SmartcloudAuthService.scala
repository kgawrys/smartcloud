package prices.services

import cats.effect.Async
import prices.services.domain.SmartcloudAuthToken

class SmartcloudAuthService[F[_]: Async] {
  def getAuth: F[SmartcloudAuthToken] = Async[F].pure(SmartcloudAuthToken("lxwmuKofnxMxz6O2QE1Ogh"))
}
