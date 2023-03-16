package it.unibo.scafi.core

import scala.language.implicitConversions

trait SensorId

case class SimpleSensorId(name: String) extends SensorId

object SimpleSensorId {
  implicit def conversion(id: String): SensorId = SimpleSensorId(id)
}
