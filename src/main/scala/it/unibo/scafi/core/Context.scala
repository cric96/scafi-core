package it.unibo.scafi.core

trait Context {
  def selfId: Int
  def exports(): Iterable[(Int, Export)]
  def sense[T](localSensorName: SensorId): Option[T]
  def neighborhoodSense[T](nbrSensorName: SensorId)(nbr: Int): Option[T] = sense[Int => T](nbrSensorName).map(_(nbr))
  def readSlot[A](i: Int, p: Path): Option[A]
}

class ContextImpl(
    val selfId: Int,
    currentExports: Iterable[(Int, Export)],
    val localSensor: Map[SensorId, Any],
    val nbrSensor: Map[SensorId, Map[Int, Any]]
) extends Context {
  private var exportsMap: Map[Int, Export] = currentExports.toMap
  def updateExport(id: Int, exportData: Export): Unit = exportsMap += id -> exportData

  override def exports(): Iterable[(Int, Export)] = exportsMap

  def readSlot[A](i: Int, p: Path): Option[A] =
    exportsMap get i flatMap (_.get[A](p))

  override def toString: String =
    s"C[\n\tI:$selfId,\n\tE:$currentExports,\n\tS1:$localSensor,\n\tS2:$nbrSensor\n]"

  override def sense[T](localSensorName: SensorId): Option[T] =
    localSensor.get(localSensorName).map(_.asInstanceOf[T])

  override def neighborhoodSense[T](nbrSensorName: SensorId)(nbr: Int): Option[T] =
    nbrSensor.get(nbrSensorName).flatMap(_.get(nbr)).map(_.asInstanceOf[T])
}
