package prices.services

import cats.effect.Async

class SmartcloudAuthService[F[_]: Async] {
  def getAuth: F[String] = Async[F].pure("lxwmuKofnxMxz6O2QE1Og")
}
