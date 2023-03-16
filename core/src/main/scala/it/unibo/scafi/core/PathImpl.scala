package it.unibo.scafi.core

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
