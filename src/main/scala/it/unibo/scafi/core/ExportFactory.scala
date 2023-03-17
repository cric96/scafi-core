package it.unibo.scafi.core

/**
  * A factory used for creating export objects, composed of paths and values.
  */
trait ExportFactory {
  def emptyPath(): Path
  def emptyExport(): Export
  def path(slots: Slot*): Path
  def export(exps: (Path, Any)*): Export
  def /(): Path = emptyPath()
  def /(s: Slot): Path = path(s)
}

object ExportFactory {
  def apply(): ExportFactory = new ExportFactoryImpl

  private class ExportFactoryImpl extends ExportFactory {
    override def emptyPath(): Path = new PathImpl(List())
    override def emptyExport(): Export = new ExportImpl
    override def path(slots: Slot*): Path = new PathImpl(List(slots: _*).reverse)
    override def export(exps: (Path, Any)*): Export = {
      val exp = new ExportImpl()
      exps.foreach { case (p, v) => exp.put(p, v) }
      exp
    }
  }
}
