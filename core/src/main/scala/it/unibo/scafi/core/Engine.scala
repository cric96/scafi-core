/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.core

/**
 * This trait defines a component that extends Semantics.
 * It defines an implementation of Context and Export (and Path),
 * with associated factories.
 */

import it.unibo.utils.{Interop, Linearizable}

trait Engine extends Semantics {
  override type CONTEXT = Context with ContextOps
  override type FACTORY = Factory

  implicit override val factory: EngineFactory = new EngineFactory

  abstract class BaseContextImpl(val selfId: Int, _exports: Iterable[(Int, Export)]) extends Context with ContextOps {
    self: CONTEXT =>

    private var exportsMap: Map[Int, Export] = _exports.toMap
    def updateExport(id: Int, export: Export): Unit = exportsMap += id -> export

    override def exports(): Iterable[(Int, Export)] = exportsMap

    def readSlot[A](i: Int, p: Path): Option[A] =
      exportsMap get i flatMap (_.get[A](p))
  }

  class ContextImpl(
      selfId: Int,
      exports: Iterable[(Int, Export)],
      val localSensor: Map[SensorId, Any],
      val nbrSensor: Map[SensorId, Map[Int, Any]]
  ) extends BaseContextImpl(selfId, exports) { self: CONTEXT =>

    override def toString(): String =
      s"C[\n\tI:$selfId,\n\tE:$exports,\n\tS1:$localSensor,\n\tS2:$nbrSensor\n]"

    override def sense[T](localSensorName: SensorId): Option[T] =
      localSensor.get(localSensorName).map { case x: T @unchecked => x }

    override def nbrSense[T](nbrSensorName: SensorId)(nbr: Int): Option[T] =
      nbrSensor.get(nbrSensorName).flatMap(_.get(nbr)).map { case x: T @unchecked => x }
  }

  class EngineFactory extends Factory { self: FACTORY =>
    override def emptyPath(): Path = new PathImpl(List())
    override def emptyExport(): Export = new ExportImpl
    override def path(slots: Slot*): Path = new PathImpl(List(slots: _*).reverse)
    override def export(exps: (Path, Any)*): Export = {
      val exp = new ExportImpl()
      exps.foreach { case (p, v) => exp.put(p, v) }
      exp
    }
    override def context(
        selfId: Int,
        exports: Map[Int, Export],
        lsens: Map[SensorId, Any] = Map.empty,
        nbsens: Map[SensorId, Map[Int, Any]] = Map.empty
    ): CONTEXT =
      new ContextImpl(selfId, exports, lsens, nbsens)
  }
}
