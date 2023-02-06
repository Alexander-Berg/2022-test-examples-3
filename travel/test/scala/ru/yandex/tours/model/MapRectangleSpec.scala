package ru.yandex.tours.model

import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.immutable.IndexedSeq

class MapRectangleSpec extends WordSpecLike with Matchers {
  "Map rectangle should" must {
    "be created by boundaries" in {
      val x = MapRectangle.byBoundaries(-1, -2, 3, 4)
      x.minLat shouldBe -2
      x.minLon shouldBe -1
      x.maxLon shouldBe 3
      x.maxLat shouldBe 4
      x.latCenter shouldBe 1
      x.lonCenter shouldBe 1
      x.latSpan shouldBe 6
      x.lonSpan shouldBe 4
    }

    "be created by center and span" in {
      val x = MapRectangle.byCenterAndSpan(1, 1, 4, 6)
      x.minLat shouldBe -2
      x.minLon shouldBe -1
      x.maxLon shouldBe 3
      x.maxLat shouldBe 4
      x.latCenter shouldBe 1
      x.lonCenter shouldBe 1
      x.latSpan shouldBe 6
      x.lonSpan shouldBe 4
    }

    "be created by points" in {
      val x = MapRectangle.byPoints(Iterable((0d, 0d), (1d, 1d), (-2d, 2d), (5d, -5d)))
      x.minLat shouldBe -5
      x.minLon shouldBe -2
      x.maxLon shouldBe 5
      x.maxLat shouldBe 2
      x.latCenter shouldBe -1.5
      x.lonCenter shouldBe 1.5
      x.latSpan shouldBe 7
      x.lonSpan shouldBe 7
    }

    "response to contain queries. Overflow case" in {
      val x = MapRectangle.byBoundaries(179, -1, -179, 1)
      x.contains(180, 0) shouldBe true
      x.contains(179, -1) shouldBe true
      x.contains(-180, 0) shouldBe true
      x.contains(-178, 0) shouldBe false
      x.contains(178, 0) shouldBe false
    }

    "response to contain queries" in {
      val y = MapRectangle.byBoundaries(0, 0, 1, 1)
      y.contains(0.5, 0.5) shouldBe true
      y.contains(2, 2) shouldBe false
    }

    "create right rectangle in case of big span" in {
      val x = MapRectangle.byCenterAndSpan(0, 0, 1000, 1000)
      x.minLat shouldBe -90
      x.minLon shouldBe -180
      x.maxLon shouldBe 180
      x.maxLat shouldBe 90
      x.latCenter shouldBe 0
      x.lonCenter shouldBe 0
      x.latSpan shouldBe 180
      x.lonSpan shouldBe 360
    }

    "return coordinate indexes. Overflow case" in {
      val x = MapRectangle.byBoundaries(179, -1, -179, 1)
      val set = x.coordinateIndexes.toSet
      set.size shouldBe 12
      set.contains((178, -2)) shouldBe false
      set.contains((-179, -2)) shouldBe false
      set.contains((-180, 0)) shouldBe true
      set.contains((180, 0)) shouldBe true
      set.contains((179, 1)) shouldBe true
      set.contains((-180, -1)) shouldBe true
      set.contains((180, 1)) shouldBe true
    }

    "return coordinate indexes" in {
      val x = MapRectangle.byBoundaries(-1, -1, 1, 1)
      val set = x.coordinateIndexes.toSet
      set.size shouldBe 9
      set.contains((0, 0)) shouldBe true
      set.contains((-2, -2)) shouldBe false
      set.contains((2, -2)) shouldBe false
      set.contains((3, 0)) shouldBe false
      set.contains((1, 1)) shouldBe true
    }
  }
}
