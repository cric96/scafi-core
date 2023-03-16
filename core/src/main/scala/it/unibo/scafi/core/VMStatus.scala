package it.unibo.scafi.core

trait VMStatus {
  val path: Path
  val index: Int
  val neighbour: Option[Int]

  def isFolding: Boolean
  def foldInto(id: Option[Int]): VMStatus
  def foldOut(): VMStatus
  def nest(s: Slot): VMStatus
  def incIndex(): VMStatus
  def push(): VMStatus
  def pop(): VMStatus
}

object VMStatus {
  def apply(path: Path = Path()): VMStatus = VMStatusImpl(path)

  final private case class VMStatusImpl(
      path: Path,
      index: Int = 0,
      neighbour: Option[Int] = None,
      stack: List[(Path, Int, Option[Int])] = List()
  ) extends VMStatus {

    def isFolding: Boolean = neighbour.isDefined
    def foldInto(id: Option[Int]): VMStatus = VMStatusImpl(path, index, id, stack)
    def foldOut(): VMStatus = VMStatusImpl(path, index, None, stack)
    def push(): VMStatus = VMStatusImpl(path, index, neighbour, (path, index, neighbour) :: stack)
    def pop(): VMStatus = stack match {
      case (p, i, n) :: s => VMStatusImpl(p, i, n, s)
      case _ => throw new Exception()
    }
    def nest(s: Slot): VMStatus = VMStatusImpl(path.push(s), 0, neighbour, stack)
    def incIndex(): VMStatus = VMStatusImpl(path, index + 1, neighbour, stack)
  }
}
