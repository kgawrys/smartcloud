package prices.routes.protocol

import io.circe._
import io.circe.syntax._
import prices.data.{InstanceKind, InstancePrice}

// TODO use money type instead of BigDecimal
// TODO add refined type
final case class InstancePriceResponse(kind: InstanceKind, amount: InstancePrice)

object InstancePriceResponse {

  implicit val encoder: Encoder[InstancePriceResponse] =
    Encoder.instance[InstancePriceResponse] {
      case InstancePriceResponse(k, a) =>
        Json.obj(
          "kind" -> k.getString.asJson,
          "amount" -> a.value.asJson
        )
    }
}
