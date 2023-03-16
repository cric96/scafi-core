package it.unibo.scafi.core

sealed trait Slot {
  def ->(v: Any): (Path, Any) = (Path(this), v)
  def /(slot: Slot): Path = Path(this, slot)
}

object Slot {
  final case class Nbr(index: Int) extends Slot
  final case class Rep(index: Int) extends Slot
  final case class FunCall(index: Int, funId: Any) extends Slot
  final case class FoldHood(index: Int) extends Slot
  final case class Branch(index: Int, tag: Boolean) extends Slot
}
