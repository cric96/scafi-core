package it.unibo.scafi.core

import it.unibo.scafi.core.Slot.{Branch, FoldHood, Nbr, Rep}
import it.unibo.scafi.core.vm.RoundVM

trait ConstructsSemantics extends Language {
  def vm: RoundVM

  override def mid(): Int = vm.self

  override def rep[A](init: => A)(fun: (A) => A): A = {
    vm.nest(Rep(vm.index))(write = vm.unlessFoldingOnOthers) {
      vm.locally {
        fun(vm.previousRoundVal.getOrElse(init))
      }
    }
  }

  override def foldhood[A](init: => A)(aggr: (A, A) => A)(expr: => A): A = {
    vm.nest(FoldHood(vm.index))(write = true) { // write export always for performance reason on nesting
      val nbrField = vm
        .alignedNeighbours()
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

  def sense[A](name: SensorId): A = vm.localSense(name)

  def nbrvar[A](name: SensorId): A = vm.neighbourSense(name)
}
