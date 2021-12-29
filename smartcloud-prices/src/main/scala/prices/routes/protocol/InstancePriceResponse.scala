package prices.routes.protocol

import io.circe._
import io.circe.syntax._
import prices.data.{ InstanceKind, InstancePrice }
import prices.services.domain.dto.SmartcloudInstancePriceResponse

final case class InstancePriceResponse(kind: InstanceKind, amount: InstancePrice)

object InstancePriceResponse {

  def from(resp: SmartcloudInstancePriceResponse): InstancePriceResponse =
    InstancePriceResponse(
      kind = InstanceKind(resp.kind),
      amount = InstancePrice(resp.price)
    )

  implicit val encoder: Encoder[InstancePriceResponse] =
    Encoder.instance[InstancePriceResponse] {
      case InstancePriceResponse(k, a) =>
        Json.obj(
          "kind" -> k.getString.asJson,
          "amount" -> a.value.asJson
        )
    }
}
