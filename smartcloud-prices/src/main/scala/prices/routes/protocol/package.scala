package prices.routes

import io.circe.generic.extras.semiauto.{ deriveUnwrappedDecoder, deriveUnwrappedEncoder }
import io.circe.{ Decoder, Encoder }
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import prices.data._

package object protocol {

  implicit val InstanceKindEncoder: Encoder[InstanceKind]   = deriveUnwrappedEncoder
  implicit val InstancePriceEncoder: Encoder[InstancePrice] = deriveUnwrappedEncoder
  implicit val InstanceKindDecoder: Decoder[InstanceKind]   = deriveUnwrappedDecoder
  implicit val InstancePriceDecoder: Decoder[InstancePrice] = deriveUnwrappedDecoder

  implicit val instanceKindQueryParam: QueryParamDecoder[InstanceKind] =
    QueryParamDecoder[String].map(InstanceKind)

  object InstanceKindQueryParam extends QueryParamDecoderMatcher[InstanceKind]("kind")

}
