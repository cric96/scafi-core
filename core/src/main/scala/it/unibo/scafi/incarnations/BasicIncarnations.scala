/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.incarnations

import it.unibo.scafi.space.BasicSpatialAbstraction
import it.unibo.scafi.time.BasicTimeAbstraction
import it.unibo.utils.{Interop, Linearizable}

/**
 * An aggregate-programming system is ultimately created
 * as an object taking code from
 *  - Engine (aggregate programming machine),
 *  - Features (all linguistic elements), and
 *  - Simulation (to simulate).
 * It gives final concrete types for sensor IDs and device IDs
 */

trait BasicAbstractIncarnation extends Incarnation {
  override type CNAME = String
  override type ID = Int
  override type EXECUTION = AggregateInterpreter

  override def cnameFromString(s: String): CNAME = s

  @transient implicit override val linearID: Linearizable[ID] = new Linearizable[ID] {
    override def toNum(v: ID): Int = v
    override def fromNum(n: Int): ID = n
  }
  @transient implicit override val interopID: Interop[ID] = new Interop[ID] {
    def toString(id: ID): String = id.toString
    def fromString(str: String) = str.toInt
  }
  @transient implicit override val interopCNAME: Interop[CNAME] = new Interop[CNAME] {
    def toString(lsns: CNAME): String = lsns
    def fromString(str: String): CNAME = str
  }
}

class AbstractTestIncarnation extends BasicAbstractIncarnation with BasicSpatialAbstraction with BasicTimeAbstraction {}
