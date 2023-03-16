/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.test
import it.unibo.scafi.core.{ContextImpl, Export, FieldCalculusInterpreter, SensorId, SimpleSensorId}
import org.scalactic.Equality

import scala.collection.mutable

trait CoreTestUtils {
  def ctx(
      selfId: Int,
      exports: Map[Int, Export] = Map(),
      lsens: Map[String, Any] = Map(),
      nbsens: Map[String, Map[Int, Any]] = Map()
  )(implicit node: FieldCalculusInterpreter): ContextImpl = {
    val localSensorsWithId = lsens.map { case (k, v) => (SimpleSensorId(k): SensorId) -> v }
    val neighborhoodSensorWithId = nbsens.map { case (k, v) =>
      (SimpleSensorId(k): SensorId) -> v
    }
    new ContextImpl(selfId, exports, localSensorsWithId, neighborhoodSensorWithId)
  }

  def assertEquivalence[T](
      nbrs: Map[Int, List[Int]],
      execOrder: Iterable[Int],
      comparer: (T, T) => Boolean = (_: Any) == (_: Any)
  )(program1: => Any)(program2: => Any)(implicit interpreter: FieldCalculusInterpreter): Boolean = {
    val states = mutable.Map[Int, (Export, Export)]()
    execOrder.foreach { curr =>
      val nbrExports = states.filterKeys(nbrs(curr).contains(_))
      val currCtx1 = ctx(curr, exports = nbrExports.mapValues(_._1).toMap)
      val currCtx2 = ctx(curr, exports = nbrExports.mapValues(_._2).toMap)

      val exp1 = interpreter.round(currCtx1, program1)
      val exp2 = interpreter.round(currCtx2, program2)
      if (!comparer(exp1.root(), exp2.root())) {
        throw new Exception(s"Not equivalent: \n$exp1\n$currCtx1\n--------\n$exp2\n$currCtx2")
      }
      states.put(curr, (exp1, exp2))
    }
    true
  }

  def fullyConnectedTopologyMap(elems: Iterable[Int]): Map[Int, List[Int]] =
    elems.map(elem => elem -> elems.toList).toMap
}

object CoreTestUtils extends CoreTestUtils
