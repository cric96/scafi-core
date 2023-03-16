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
  override type FACTORY = Factory

  implicit override val factory: EngineFactory = new EngineFactory

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
    ): Context =
      new ContextImpl(selfId, exports, lsens, nbsens)
  }
}
