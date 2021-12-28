package prices.services.domain

import java.time.ZonedDateTime

final case class SmartcloudInstancePriceResponse(kind: String, price: BigDecimal, timestamp: ZonedDateTime)
