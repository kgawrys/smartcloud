package prices.services

import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse

import scala.util.control.NoStackTrace

trait InstancePriceService[F[_]] {
  def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse]
}

object InstancePriceService {
  sealed trait SmartcloudException extends NoStackTrace {
    val message: String
  }
  object SmartcloudException {
    case class APIUnauthorized(message: String) extends SmartcloudException
    case class APICallFailure(message: String) extends SmartcloudException
    case class APITooManyRequestsFailure(message: String) extends SmartcloudException
  }
}
