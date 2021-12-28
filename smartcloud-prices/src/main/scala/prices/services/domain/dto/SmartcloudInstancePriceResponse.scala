package prices.services.domain.dto

import java.time.ZonedDateTime

final case class SmartcloudInstancePriceResponse(kind: String, price: BigDecimal, timestamp: ZonedDateTime)
