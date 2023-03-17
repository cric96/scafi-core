package it.unibo.scafi.core
import RoundVM._

import scala.util.control.Exception.handling

trait RoundVM {
  def factory: ExportFactory

  def context: Context

  def exportStack: List[Export]

  def status: VMStatus

  def export: Export =
    exportStack.head

  def self: Int =
    context.selfId

  def registerRoot(v: Any): Unit =
    export.put(factory.emptyPath(), v)

  def neighbour: Option[Int] =
    status.neighbour

  def index: Int =
    status.index

  def previousRoundVal[A]: Option[A] =
    context.readSlot[A](self, status.path)

  def neighbourVal[A]: A = context
    .readSlot[A](neighbour.get, status.path)
    .getOrElse(throw OutOfDomainException(context.selfId, neighbour.get, status.path))

  def localSense[A](name: SensorId): A = context
    .sense[A](name)
    .getOrElse(throw new SensorUnknownException(self, name))

  def neighbourSense[A](name: SensorId): A = {
    RoundVM.ensure(neighbour.isDefined, "Neighbouring sensor must be queried in a nbr-dependent context.")
    context
      .neighborhoodSense(name)(neighbour.get)
      .getOrElse(throw NbrSensorUnknownException(self, name, neighbour.get))
  }

  def foldedEval[A](expr: => A)(id: Int): Option[A]

  def nest[A](slot: Slot)(write: Boolean, inc: Boolean = true)(expr: => A): A

  def locally[A](a: => A): A

  def alignedNeighbours(): List[Int]

  def isolate[A](expr: => A): A

  def newExportStack: Any
  def discardExport: Any
  def mergeExport: Any

  def unlessFoldingOnOthers: Boolean = neighbour.forall(_ == self)
  def onlyWhenFoldingOnSelf: Boolean = neighbour.contains(self)
}

object RoundVM {
  def apply(context: Context, factory: ExportFactory): RoundVM =
    new RoundVMImpl(context, factory)
  def ensure(b: => Boolean, s: String): Unit = if (b) throw new Exception(s)

  final case class OutOfDomainException(selfId: Int, nbr: Int, path: Path) extends Exception() {
    override def toString: String = s"OutOfDomainException: $selfId , $nbr, $path"
  }

  final case class SensorUnknownException(selfId: Int, name: SensorId) extends Exception() {
    override def toString: String = s"SensorUnknownException: $selfId , $name"
  }

  final case class NbrSensorUnknownException(selfId: Int, name: SensorId, nbr: Int) extends Exception() {
    override def toString: String = s"NbrSensorUnknownException: $selfId , $name, $nbr"
  }

  class RoundVMImpl(val context: Context, val factory: ExportFactory) extends RoundVM {
    var exportStack: List[Export] = List(factory.emptyExport())
    var status: VMStatus = VMStatus()
    var isolated = false // When true, neighbours are scoped out

    override def foldedEval[A](expr: => A)(id: Int): Option[A] =
      handling(classOf[OutOfDomainException]) by (_ => None) apply {
        try {
          status = status.push()
          status = status.foldInto(Some(id))
          Some(expr)
        } finally status = status.pop()
      }

    override def nest[A](slot: Slot)(write: Boolean, inc: Boolean = true)(expr: => A): A = {
      try {
        status = status.push().nest(slot) // prepare nested call
        if (write) export.get(status.path).getOrElse(export.put(status.path, expr))
        else expr // function return value is result of expr
      } finally status = if (inc) status.pop().incIndex() else status.pop() // do not forget to restore the status
    }

    override def locally[A](a: => A): A = {
      val currentNeighbour = neighbour
      try {
        status = status.foldOut()
        a
      } finally status = status.foldInto(currentNeighbour)
    }

    override def alignedNeighbours(): List[Int] =
      if (isolated) {
        List()
      } else {
        self ::
          context.exports
            .filter(_._1 != self)
            .filter(p => status.path.isRoot || p._2.get(status.path).isDefined)
            .map(_._1)
            .toList
      }

    override def isolate[A](expr: => A): A = {
      val wasIsolated = this.isolated
      try {
        this.isolated = true
        expr
      } finally this.isolated = wasIsolated
    }

    override def newExportStack: Any = exportStack = factory.emptyExport() :: exportStack
    override def discardExport: Any = exportStack = exportStack.tail
    override def mergeExport: Any = {
      val toMerge = export
      exportStack = exportStack.tail
      toMerge.paths.foreach(tp => export.put(tp._1, tp._2))
    }
  }
}
