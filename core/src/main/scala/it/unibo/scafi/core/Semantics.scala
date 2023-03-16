/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.core

import it.unibo.scafi.core.RoundVM.RoundVMImpl
import it.unibo.scafi.core.Slot._

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

abstract class Semantics(val factory: ExportFactory) {

  trait ProgramSchema {
    type MainResult
    def main(): MainResult
  }
  /**
    * It implements the whole operational semantics.
    */
  trait ExecutionTemplate extends (Context => Export) with ConstructsSemantics with ProgramSchema {

    var vm: RoundVM = _

    def apply(c: Context): Export =
      round(c, main())

    def round(c: Context, e: => Any = main()): Export = {
      vm = RoundVM(c, factory)
      val result = e
      vm.registerRoot(result)
      vm.export
    }
  }

}
