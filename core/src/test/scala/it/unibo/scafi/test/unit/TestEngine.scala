/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
 */

package it.unibo.scafi.test.unit

import it.unibo.scafi.core.Slot._
import it.unibo.scafi.core.{Export, Path, PathImpl, SensorId}
import it.unibo.scafi.test.{CoreTestIncarnation, CoreTestUtils}
import org.scalatest.funspec.AnyFunSpec
import it.unibo.scafi.core.SimpleSensorId._
import org.scalatest.matchers.should.Matchers

class TestEngine extends AnyFunSpec with Matchers with CoreTestUtils {

  describe("ConcreteSemantics") {

    def getEngine = CoreTestIncarnation

    val engine = getEngine
    import engine._
    import factory./

    describe("Export implementation") {
      it("should support the reading and writing of exports") {
        // ARRANGE
        val exp: Export = factory.emptyExport()
        val path1 = factory.path(Nbr(0), Rep(0))

        // ASSERT (initial, empty state)
        exp.get[Nothing](/) shouldBe None
        exp.get[Nothing](path1) shouldBe None

        // ACT + ASSERT (insertion at root path)
        exp.put(/, "foo")
        exp.get[String](/) shouldBe Some("foo")
        exp.get[String](/).get shouldBe exp.root[String]()

        // ACT + ASSERT (insertion at different path)
        exp.put(path1, "bar")
        exp.get[String](path1).get shouldBe "bar"

        // ACT + ASSERT (overwriting with a different data type)
        exp.put(/, 77)
        exp.get[Int](/).get shouldBe 77
      }
    } // END describe("Export implementation")

    describe("Context implementation") {
      it("should support the reading of properties and slots") {
        // ARRANGE
        import factory./
        val exportForId1 = factory.export(/ -> "one")
        val pathNbr = factory.emptyPath().push(Nbr(0))
        val exportForId5 = factory.export(pathNbr -> "five")
        val localSensors: Map[SensorId, Any] = Map(("s1": SensorId) -> 77, ("s1": SensorId) -> false)
        val neighborhoodSensors: Map[SensorId, Map[ID, Any]] = Map(("x": SensorId) -> Map(4 -> false))
        val ctx = new ContextImpl(
          selfId = 1,
          exports = Map(1 -> exportForId1, 5 -> exportForId5),
          localSensor = localSensors,
          nbrSensor = neighborhoodSensors
        )

        // ASSERT on fields
        ctx.selfId shouldBe 1
        ctx.exports.size shouldBe 2
        ctx.localSensor shouldEqual localSensors
        ctx.nbrSensor shouldEqual neighborhoodSensors

        // ASSERT readslot
        ctx.readSlot(2, /) shouldBe None
        ctx.readSlot(1, /) shouldEqual Some("one")
        ctx.readSlot(5, /) shouldBe None
        ctx.readSlot(5, pathNbr) shouldEqual Some("five")
      }
    } // END describe("Context implementation")

    describe("Path implementation") {
      it("should allow the reading and writing of paths") {
        // ARRANGE
        val emptyPath: Path with Equals = new PathImpl(List())

        // ACT
        val path1 = emptyPath.push(Nbr(0))
        val path2a = path1.push(Rep(0))
        val tail1a = path2a.pull()
        val tail2a = tail1a.pull()

        // ASSERT
        intercept[Exception](tail2a.pull())
        path1.matches(new PathImpl(List(Nbr(0)))) shouldBe true
        path2a.matches(new PathImpl(List(Rep(0), Nbr(0)))) shouldBe true
        tail1a.matches(new PathImpl(List(Nbr(0)))) shouldBe true
        tail2a.matches(new PathImpl(List())) shouldBe true
      }
    } // END describe("Path implementation")
  } // END describe("Engine")
}
