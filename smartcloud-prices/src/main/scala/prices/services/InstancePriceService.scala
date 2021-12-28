package prices.services

import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse

import scala.util.control.NoStackTrace

trait InstancePriceService[F[_]] {
  def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse]
}

object InstancePriceService {
  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APIUnauthorized(message: String) extends Exception
    case class APICallFailure(message: String) extends Exception
    case class APITooManyRequestsFailure(message: String) extends Exception
  }
}
