package it.unibo.scafi.core

trait Path {
  def push(slot: Slot): Path
  def pull(): Path
  def matches(path: Path): Boolean
  def isRoot: Boolean
  def head: Slot
  def path: List[Slot]

  def /(slot: Slot): Path = push(slot)
}

object Path {
  def apply(slots: Slot*): Path = new PathImpl(slots.toList.reverse)
}
