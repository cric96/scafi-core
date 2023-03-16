package it.unibo.scafi.core

trait Export extends Equals {
  def root[A](): A
  def put[A](path: Path, value: A): A
  def get[A](path: Path): Option[A]
  def paths: Map[Path, Any]
  def getMap[A]: Map[Path, A] = paths.view.mapValues(_.asInstanceOf[A]).toMap
}

class ExportImpl(private var map: Map[Path, Any] = Map.empty) extends Export with Equals {
  override def put[A](path: Path, value: A): A = { map += (path -> value); value }
  override def get[A](path: Path): Option[A] = map.get(path).map(_.asInstanceOf[A])
  override def root[A](): A = get[A](Path()).get
  override def paths: Map[Path, Any] = map

  override def equals(o: Any): Boolean = o match {
    case x: Export => x.paths == map
    case _ => false
  }

  override def canEqual(that: Any): Boolean =
    that match { case _: Export => true; case _ => false }

  override def hashCode(): Int = map.hashCode()

  override def toString: String = map.toString
}
