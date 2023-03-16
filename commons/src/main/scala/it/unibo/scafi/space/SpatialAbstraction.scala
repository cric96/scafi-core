/*
 * Copyright (C) 2016-2019, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENSE file distributed with this work for additional information regarding copyright ownership.
*/

package it.unibo.scafi.space
import scala.collection.concurrent.TrieMap
import scala.language.higherKinds

/**
  * Component which represents a spatial abstraction
  */

trait SpatialAbstraction {
  type P // Type for "position"

  type SPACE[E] <: Space[E] // Type for "spatial container" of elements E

  trait NeighbouringRelation {
    def neighbouring(p1: P, p2: P): Boolean
  }

  def buildNewSpace[E](elems: Iterable[(E,P)]): SPACE[E]

  trait Space[E] extends NeighbouringRelation {
    def contains(e: E): Boolean
    def getLocation(e: E): P
    def getAt(p: P): Option[E]
    def getAll(): Iterable[E]
    def getNeighbors(e: E): Iterable[E] = {
      val p1 = getLocation(e)
      getAll().filter(e2 => neighbouring(p1, getLocation(e2)))
    }
  }

  trait MutableSpace[E] extends Space[E] {
    def add(e: E, p: P): Unit
    def remove(e: E): Unit
    def setLocation(e: E, p: P): Unit
  }
}

object SpatialAbstraction {

}

trait MetricSpatialAbstraction extends SpatialAbstraction {
  override type SPACE[E] <: Space[E] with DistanceStrategy

  type D // Type for "distance"

  implicit val positionOrdering: Ordering[P]

  trait DistanceStrategy {
    def getDistance(p1: P, p2: P): D
  }

  trait MetricSpace[E] extends Space[E] with DistanceStrategy {
    def getNeighborsWithDistance(e: E) : Iterable[(E,D)] =
      getNeighbors(e) map (elem =>
        (elem, getDistance(getLocation(elem), getLocation(e))))
  }

  trait MutableMetricSpace[E] extends MetricSpace[E] with MutableSpace[E]
}

trait BasicSpatialAbstraction extends MetricSpatialAbstraction {
  override type P <: Point3D
  override type D = Double

  override type SPACE[E] = Space3D[E]

  implicit val positionOrdering: Ordering[P] = new Ordering[P] {
    override def compare(a: P, b: P): Int = {
      if (a.z > b.z){ +1 }
      else if (a.z < b.z){ -1 }
      else if (a.y > b.y){ +1 }
      else if (a.y < b.y){ -1 }
      else if (a.x > b.x){ +1 }
      else if (a.x < b.x){ -1 }
      else { 0 }
    }
  }

  override def buildNewSpace[E](elems: Iterable[(E,P)]): SPACE[E] =
    new Basic3DSpace(elems.toMap)

  abstract class Space3D[E](var elemPositions: Map[E,P],
                    override val proximityThreshold: Double) extends MutableMetricSpace[E]
    with EuclideanStrategy
    with Serializable {
    def add(e: E, p: P): Unit = elemPositions += (e -> p)
    def getLocation(e: E): P = elemPositions(e)
    def getAll(): Iterable[E] = elemPositions.keys
    def remove(e: E): Unit = elemPositions -= e

    override def contains(e: E): Boolean = elemPositions.contains(e)
  }
  class Basic3DSpace[E](pos: Map[E,P],
                        proximityThreshold: Double = EuclideanStrategy.DefaultProximityThreshold)
    extends Space3D[E](pos,proximityThreshold){

    var neighbourhoodMap: Map[E, Set[(E,D)]] = initNeighbours()

    def initNeighbours(): Map[E, Set[(E,D)]] = {
      var result = Map[E, Set[(E,D)]]()
      elemPositions.foreach(elem => { result += (elem._1 -> this.calculateNeighbours(elem._1).toSet) })
      result
    }

    def setLocation(e: E, p: P): Unit = {
      add(e, p)
      var newNeighbours: Set[(E,D)] = this.calculateNeighbours(e).toSet
      var oldNeighbours: Set[(E,D)] = neighbourhoodMap(e)
      if(oldNeighbours != newNeighbours){
        var noMoreNeighbours: Set[(E,D)] = oldNeighbours.filter(el => !newNeighbours.exists(newNbr => newNbr._1==el._1))
        var brandNewNeighbours: Set[(E,D)] = newNeighbours.filter(el => !oldNeighbours.exists(old => old._1==el))
        for (elem <- noMoreNeighbours) {
          neighbourhoodMap += (elem._1 -> this.calculateNeighbours(elem._1).toSet)
        }
        for (elem <- brandNewNeighbours) {
          neighbourhoodMap += (elem._1 -> this.calculateNeighbours(elem._1).toSet)
        }
        neighbourhoodMap += (e -> newNeighbours)
      }
    }

    private def calculateNeighbours(e: E): Iterable[(E,D)] = {

      val p1 = getLocation(e)
      getAll()
        .filter(nbr => nbr != e && neighbouring(p1,getLocation(nbr)))
       .map(e2 => (e2, getDistance(getLocation(e), getLocation(e2))))
    }

    override def add(e: E, p: P): Unit = {
      elemPositions += (e -> p)
      if (!neighbourhoodMap.contains(e)) neighbourhoodMap += (e -> Set())
    }
    override def getAt(p: P): Option[E] = elemPositions.find(_._2 == p).map(_._1)

    override def getNeighbors(e: E): Iterable[E] = getNeighborsWithDistance(e) map (_._1)
    override def getNeighborsWithDistance(e: E): Iterable[(E, D)] = neighbourhoodMap(e)
  }

  trait EuclideanStrategy extends DistanceStrategy
    with NeighbouringRelation with Serializable {
    val proximityThreshold: Double
    def neighbouring(p1: P, p2: P): Boolean =
      getDistance(p1,p2) <= proximityThreshold

    override def getDistance(p1: P, p2: P): Double = {
      p1.distance(p2)
    }
  }

  object EuclideanStrategy {
    val DefaultProximityThreshold: Double = 1
  }
}
