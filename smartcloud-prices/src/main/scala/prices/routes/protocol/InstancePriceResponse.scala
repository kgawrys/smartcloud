package prices.routes.protocol

import prices.data.{ InstanceKind, InstancePrice }
import prices.services.domain.dto.SmartcloudInstancePriceResponse

final case class InstancePriceResponse(kind: InstanceKind, amount: InstancePrice)

object InstancePriceResponse {
  def from(resp: SmartcloudInstancePriceResponse): InstancePriceResponse =
    InstancePriceResponse(
      kind = InstanceKind(resp.kind),
      amount = InstancePrice(resp.price)
    )
}
