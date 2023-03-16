/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.platform

import it.unibo.scafi.core.{Core, Engine, Export, SensorId}
import it.unibo.scafi.space.MetricSpatialAbstraction
import it.unibo.scafi.time.TimeAbstraction

import scala.concurrent.duration.FiniteDuration

/**
 * This trait defines a component that requires to be "attached" to Core
 * It defines a whole platform view, with a NETWORK type modelling the whole computational system
 */
trait Platform {
  self: Platform.PlatformDependency =>
}

object Platform {
  type PlatformDependency = Core with Engine
}

trait TimeAwarePlatform extends Platform {
  self: Platform.PlatformDependency with TimeAbstraction =>

  trait TimeAwareDevice {
    def currentTime(): Time
    def timestamp(): Long
    //def lastExecutionTime(): Time
    def deltaTime(): FiniteDuration

    //def nbrLastExecutionTime(): Time
    def nbrDelay(): FiniteDuration
    def nbrLag(): FiniteDuration
  }
}

trait SpaceAwarePlatform extends Platform {
  self: Platform.PlatformDependency with MetricSpatialAbstraction =>

  trait SpaceAwareDevice {
    def currentPosition(): P

    def nbrRange(): D
    def nbrVector(): P
  }
}

trait SpaceTimeAwarePlatform extends SpaceAwarePlatform with TimeAwarePlatform {
  self: SpaceTimeAwarePlatform.PlatformDependency =>

  trait SpaceTimeAwareDevice extends SpaceAwareDevice with TimeAwareDevice
}

object SpaceTimeAwarePlatform {
  type PlatformDependency = Platform.PlatformDependency with MetricSpatialAbstraction with TimeAbstraction
}

trait SimulationPlatform extends SpaceTimeAwarePlatform {
  self: SpaceTimeAwarePlatform.PlatformDependency =>

  type NETWORK <: Network

  trait Network {
    def ids: Set[ID]
    def neighbourhood(id: ID): Set[ID]
    def localSensor[A](name: SensorId)(id: ID): A
    def nbrSensor[A](name: SensorId)(id: ID)(idn: ID): A
    def export(id: ID): Option[Export]
    def exports(): Map[ID, Option[Export]]
    def sensorState(
        filter: (SensorId, ID) => Boolean = (s, n) => true
    ): collection.Map[SensorId, collection.Map[ID, Any]]
    def neighbouringSensorState(
        filter: (SensorId, ID, ID) => Boolean = (s, n, nbr) => true
    ): collection.Map[SensorId, collection.Map[ID, collection.Map[ID, Any]]]
  }
}

object SimulationPlatform {
  type PlatformDependency = SpaceTimeAwarePlatform.PlatformDependency
}
