package json.codec

import io.circe.Decoder._
import io.circe._
import io.circe.syntax._

import scalaz._

/**
  * @author Maxim Ochenashko
  */
object TaggedTypeCodec {

  implicit def decoder[A, X](implicit e: Decoder[A]): Decoder[A @@ X] = new Decoder[A @@ X] {
    override def apply(c: HCursor): Result[A @@ X] = c.as[A].map(a => Tag.apply[A, X](a))
  }

  implicit def encoder[A, X](implicit e: Encoder[A]): Encoder[A @@ X] = new Encoder[A @@ X] {
    override def apply(a: A @@ X): Json = Tag.unwrap(a).asJson
  }
}
