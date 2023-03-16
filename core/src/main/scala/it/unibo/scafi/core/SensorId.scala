package it.unibo.scafi.core

trait SensorId

case class SimpleSensorId(name: String) extends SensorId

object SimpleSensorId {
  implicit def conversion(id: String): SensorId = SimpleSensorId(id)
  def apply(name: String): SimpleSensorId = new SimpleSensorId(name)
}
