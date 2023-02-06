package ru.yandex.tours.util.graph

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.tours.testkit.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BfsWalkerSpec extends BaseSpec {
  "Bfs walker" should {
    "find connected components" in {
      val graph = Map(
        1 -> Set(2, 3),
        2 -> Set(3, 4),
        5 -> Set(5, 8)
      )
      val walker = new BfsWalker(wrapMap(graph))
      walker.getReachableNeighbours(1).futureValue shouldBe Set(1, 2, 3, 4)
    }
  }

  private def wrapMap(map: Map[Int, Set[Int]])(x: Int) = Future.successful {
    map.getOrElse(x, Set.empty[Int])
  }
}
