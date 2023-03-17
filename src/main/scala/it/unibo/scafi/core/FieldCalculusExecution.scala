package it.unibo.scafi.core

trait FieldCalculusExecution extends (Context => Export) with ConstructsSemantics {
  type MainResult
  def factory: ExportFactory = ExportFactory()

  def main(): MainResult // where the code should be written

  var vm: RoundVM = _

  final def apply(c: Context): Export =
    round(c, main())

  final def round(c: Context, e: => Any = main()): Export = {
    vm = RoundVM(c, factory)
    val result = e
    vm.registerRoot(result)
    vm.export
  }
}

trait FieldCalculusInterpreter extends FieldCalculusExecution with FieldCalculusSyntax {
  type MainResult = Any
  final def main(): Any = ()
}

trait AggregateProgram extends FieldCalculusExecution with FieldCalculusSyntax {}
