/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.incarnations

import it.unibo.scafi.core.{Core, ExportFactory, RichLanguage, Semantics}
import it.unibo.scafi.space.BasicSpatialAbstraction
import it.unibo.scafi.time.TimeAbstraction

abstract class Incarnation(factory: ExportFactory)
    extends Semantics(factory)
    with Core
    with RichLanguage
    with BasicSpatialAbstraction
    with TimeAbstraction {

  trait FieldCalculusSyntax extends Constructs with Builtins

  trait AggregateComputation[T] extends ExecutionTemplate with FieldCalculusSyntax with Serializable {
    type MainResult = T
  }

  trait AggregateInterpreter extends ExecutionTemplate with FieldCalculusSyntax with Serializable {
    type MainResult = Any
  }

  trait AggregateProgram extends AggregateInterpreter

  class BasicAggregateInterpreter extends AggregateInterpreter {
    override def main(): MainResult = ???
  }
}
