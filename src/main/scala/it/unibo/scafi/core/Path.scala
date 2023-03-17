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

class PathImpl(val path: List[Slot]) extends Path with Equals {
  def push(s: Slot): Path = new PathImpl(s :: path)
  def pull(): Path = path match {
    case _ :: p => new PathImpl(p)
    case _ => throw new Exception()
  }

  override def isRoot: Boolean = path.isEmpty

  override def toString(): String = "P:/" + path.reverse.mkString("/")

  def matches(p: Path): Boolean = this == p

  def canEqual(other: Any): Boolean = other match { case _: Path => true; case _ => false }

  override def equals(other: Any): Boolean = {
    other match {
      case that: Path => path == that.path
      case _ => false
    }
  }

  override def hashCode(): Int = path.hashCode

  override def head: Slot = path.head
}
