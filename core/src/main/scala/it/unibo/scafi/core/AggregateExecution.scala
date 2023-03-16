package it.unibo.scafi.core

trait AggregateExecution extends (Context => Export) {
  type MainResult
  def main(): MainResult // where the code should be written
}
