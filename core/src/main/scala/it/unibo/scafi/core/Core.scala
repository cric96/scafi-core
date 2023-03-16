/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.core

/**
 * This trait is the root of the family polymorphism (i.e., component-based) hierarchy.
 * It provides the basic interfaces and types
 */

trait Core {

  /**
   *  A computation round, as an I/O function
   */
  type EXECUTION <: (Context => Export)
}
