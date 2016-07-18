package enum

/**
  * @author Maxim Ochenashko
  */
trait EnumLike {
  def code: Int
}

trait EnumHolder[T <: EnumLike] {
  def values: Seq[T]

  def byCode(code: Int): Option[T] = values.find(_.code == code)

}
