package prices.services

import prices.data.InstanceKind
import prices.routes.protocol.InstancePriceResponse

import scala.util.control.NoStackTrace

trait InstancePriceService[F[_]] {
  // todo allow for searching for multiple kind with &
  def getInstancePrice(kinds: List[InstanceKind]) : F[List[InstancePriceResponse]]
}

// todo below can be shared between services
object InstancePriceService {
  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
  }
}
