/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.core

import it.unibo.scafi.PlatformDependentConstants

import scala.util.control.Exception._

/**
  * This trait defines a component that extends Core and Language
  * It starts concretising the framework by implementing the key element of field-calculus semantics, namely:
  * - An export is a map from paths to values, and a value is a list of slots
  * - An Execution template implementing the whole operational semantics
  * - A basic Factory
  * - Additional ops to Context and Export, realised by family polymorphism
  *
  * This is still abstract in that we do not dictate how Context and Export are implemented and optimised internally
  */

trait Semantics extends Core with Language {

  override type CONTEXT <: Context with ContextOps
  override type EXPORT <: Export with ExportOps
  override type EXECUTION <: ExecutionTemplate
  type FACTORY <: Factory

  implicit val factory: Factory

  sealed trait Slot {
    def ->(v: Any): (Path, Any) = (factory.path(this), v)
    def /(s: Slot): Path = factory.path(this, s)
  }
  final case class Nbr(index: Int) extends Slot
  final case class Rep(index: Int) extends Slot
  final case class FunCall(index: Int, funId: Any) extends Slot
  final case class FoldHood(index: Int) extends Slot
  final case class Branch(index: Int, tag: Boolean) extends Slot

  trait Path {
    def push(slot: Slot): Path
    def pull(): Path
    def matches(path: Path): Boolean
    def isRoot: Boolean
    def head: Slot
    def path: List[Slot]

    def /(slot: Slot): Path = push(slot)
  }

  trait ExportOps { self: EXPORT =>
    def put[A](path: Path, value: A): A
    def get[A](path: Path): Option[A]
    def paths: Map[Path, Any]
    def getMap[A]: Map[Path, A] = paths.view.mapValues(_.asInstanceOf[A]).toMap
  }

  trait ContextOps { self: CONTEXT =>
    def readSlot[A](i: ID, p: Path): Option[A]
  }

  trait Factory {
    def emptyPath(): Path
    def emptyExport(): EXPORT
    def path(slots: Slot*): Path
    def export(exps: (Path, Any)*): EXPORT
    def context(
        selfId: ID,
        exports: Map[ID, EXPORT],
        lsens: Map[CNAME, Any] = Map.empty,
        nbsens: Map[CNAME, Map[ID, Any]] = Map.empty
    ): CONTEXT
    def /(): Path = emptyPath()
    def /(s: Slot): Path = path(s)
  }

  trait ProgramSchema {
    type MainResult
    def main(): MainResult
  }

  trait AggregateProgramSchema extends ProgramSchema {
    self: Constructs =>
  }

  /**
    * It implements the whole operational semantics.
    */
  trait ExecutionTemplate extends (CONTEXT => EXPORT) with ConstructsSemantics with ProgramSchema {

    var vm: RoundVM = _

    def apply(c: CONTEXT): EXPORT =
      round(c, main())

    def round(c: CONTEXT, e: => Any = main()): EXPORT = {
      vm = new RoundVMImpl(c)
      val result = e
      vm.registerRoot(result)
      vm.export
    }
  }

  trait ConstructsSemantics extends Constructs {
    def vm: RoundVM

    override def mid(): ID = vm.self

    override def rep[A](init: => A)(fun: (A) => A): A = {
      vm.nest(Rep(vm.index))(write = vm.unlessFoldingOnOthers) {
        vm.locally {
          fun(vm.previousRoundVal.getOrElse(init))
        }
      }
    }

    override def foldhood[A](init: => A)(aggr: (A, A) => A)(expr: => A): A = {
      vm.nest(FoldHood(vm.index))(write = true) { // write export always for performance reason on nesting
        val nbrField = vm.alignedNeighbours
          .map(id => vm.foldedEval(expr)(id).getOrElse(vm.locally(init)))
        vm.isolate(nbrField.fold(vm.locally(init))((x, y) => aggr(x, y)))
      }
    }

    override def branch[A](cond: => Boolean)(thn: => A)(els: => A): A = {
      val tag = vm.locally(cond)
      vm.nest(Branch(vm.index, tag))(write = vm.unlessFoldingOnOthers) {
        vm.neighbour match {
          case Some(nbr) if nbr != vm.self => vm.neighbourVal
          case _ => if (tag) vm.locally(thn) else vm.locally(els)
        }
      }
    }

    override def nbr[A](expr: => A): A =
      vm.nest(Nbr(vm.index))(write = vm.onlyWhenFoldingOnSelf) {
        vm.neighbour match {
          case Some(nbr) if nbr != vm.self => vm.neighbourVal
          case _ => expr
        }
      }

    def sense[A](name: CNAME): A = vm.localSense(name)

    def nbrvar[A](name: CNAME): A = vm.neighbourSense(name)
  }

  trait RoundVM {
    def context: CONTEXT

    def exportStack: List[EXPORT]

    def status: VMStatus

    def export: EXPORT =
      exportStack.head

    def self: ID =
      context.selfId

    def registerRoot(v: Any): Unit =
      export.put(factory.emptyPath, v)

    def neighbour: Option[ID] =
      status.neighbour

    def index: Int =
      status.index

    def previousRoundVal[A]: Option[A] =
      context.readSlot[A](self, status.path)

    def neighbourVal[A]: A = context
      .readSlot[A](neighbour.get, status.path)
      .getOrElse(throw OutOfDomainException(context.selfId, neighbour.get, status.path))

    def localSense[A](name: CNAME): A = context
      .sense[A](name)
      .getOrElse(throw new SensorUnknownException(self, name))

    def neighbourSense[A](name: CNAME): A = {
      RoundVM.ensure(neighbour.isDefined, "Neighbouring sensor must be queried in a nbr-dependent context.")
      context.nbrSense(name)(neighbour.get).getOrElse(throw new NbrSensorUnknownException(self, name, neighbour.get))
    }

    def foldedEval[A](expr: => A)(id: ID): Option[A]

    def nest[A](slot: Slot)(write: Boolean, inc: Boolean = true)(expr: => A): A

    def locally[A](a: => A): A

    def alignedNeighbours(): List[ID]

    def isolate[A](expr: => A): A

    def newExportStack: Any
    def discardExport: Any
    def mergeExport: Any

    def unlessFoldingOnOthers: Boolean = neighbour.forall(_ == self)
    def onlyWhenFoldingOnSelf: Boolean = neighbour.contains(self)
  }

  object RoundVM {
    def ensure(b: => Boolean, s: String): Unit = {
      b match {
        case false => throw new Exception(s)
        case _ =>
      }
    }
  }

  class RoundVMImpl(val context: CONTEXT) extends RoundVM {
    var exportStack: List[EXPORT] = List(factory.emptyExport)
    var status: VMStatus = VMStatus()
    var isolated = false // When true, neighbours are scoped out

    override def foldedEval[A](expr: => A)(id: ID): Option[A] =
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

    override def alignedNeighbours(): List[ID] =
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

  trait VMStatus {
    val path: Path
    val index: Int
    val neighbour: Option[ID]

    def isFolding: Boolean
    def foldInto(id: Option[ID]): VMStatus
    def foldOut(): VMStatus
    def nest(s: Slot): VMStatus
    def incIndex(): VMStatus
    def push(): VMStatus
    def pop(): VMStatus
  }

  object VMStatus {
    def apply(): VMStatus = VMStatusImpl()
  }

  final private case class VMStatusImpl(
      path: Path = factory.emptyPath(),
      index: Int = 0,
      neighbour: Option[ID] = None,
      stack: List[(Path, Int, Option[ID])] = List()
  ) extends VMStatus {

    def isFolding: Boolean = neighbour.isDefined
    def foldInto(id: Option[ID]): VMStatus = VMStatusImpl(path, index, id, stack)
    def foldOut(): VMStatus = VMStatusImpl(path, index, None, stack)
    def push(): VMStatus = VMStatusImpl(path, index, neighbour, (path, index, neighbour) :: stack)
    def pop(): VMStatus = stack match {
      case (p, i, n) :: s => VMStatusImpl(p, i, n, s)
      case _ => throw new Exception()
    }
    def nest(s: Slot): VMStatus = VMStatusImpl(path.push(s), 0, neighbour, stack)
    def incIndex(): VMStatus = VMStatusImpl(path, index + 1, neighbour, stack)
  }

  final case class OutOfDomainException(selfId: ID, nbr: ID, path: Path) extends Exception() {
    override def toString: String = s"OutOfDomainException: $selfId , $nbr, $path"
  }

  final case class SensorUnknownException(selfId: ID, name: CNAME) extends Exception() {
    override def toString: String = s"SensorUnknownException: $selfId , $name"
  }

  final case class NbrSensorUnknownException(selfId: ID, name: CNAME, nbr: ID) extends Exception() {
    override def toString: String = s"NbrSensorUnknownException: $selfId , $name, $nbr"
  }
}
