package prices.services

import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse

import scala.util.control.NoStackTrace

trait InstancePriceService[F[_]] {
  def getInstancePrice(kind: InstanceKind): F[InstancePriceResponse]
}

// todo below can be shared between services
object InstancePriceService {
  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
  }
}
