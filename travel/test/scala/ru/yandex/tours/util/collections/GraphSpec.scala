package ru.yandex.tours.util.collections

import ru.yandex.tours.testkit.BaseSpec

class GraphSpec extends BaseSpec {
  "Graph" should {
    "Find connected components" in {
      val edges = Seq(
        (1, 2),
        (2, 3),
        (3, 5),
        (4, 6),
        (100, 1001),
        (5, 1),
        (1, 5)
      )
      val graph = new Graph(edges)
      val components = graph.getConnectedComponents.toSet
      components should have size 3
      components should contain (Set(1, 2, 3, 5))
      components should contain (Set(4, 6))
      components should contain (Set(100, 1001))

    }
  }
}
