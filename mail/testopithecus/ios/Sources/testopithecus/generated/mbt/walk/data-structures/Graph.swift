// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/data-structures/graph.ts >>>

import Foundation

open class CompressedGraph<EdgeType> {
  public var adjList: YSArray<YSArray<Int32>> = YSArray()
  public var edges: YSArray<Edge<Int32, EdgeType>> = YSArray()
  open func addEdge(_ from: Int32, _ to: Int32, _ action: EdgeType) {
    while adjList.length <= from || adjList.length <= to {
      adjList.push(YSArray())
    }
    adjList[from].push(edges.length)
    edges.push(Edge(from, to, action))
  }

  @discardableResult
  open func getDegree(_ vertex: Int32) -> Int32 {
    return adjList.length > vertex ? adjList[vertex].length : 0
  }

  @discardableResult
  open func size() -> Int32 {
    return adjList.length
  }

  @discardableResult
  open func countOfEdges() -> Int32 {
    return edges.length
  }

  @discardableResult
  open func getEdgesId(_ vertex: Int32) -> YSArray<Int32> {
    while adjList.length <= vertex {
      adjList.push(YSArray())
    }
    return adjList[vertex]
  }
}

open class Graph<EdgeType>: CompressedGraph<EdgeType> {
  private var vertexToId: YSMap<Int64, Int32> = YSMap<Int64, Int32>()
  open func addVertex(_ vertex: Int64) {
    if !vertexToId.has(vertex) {
      vertexToId.set(vertex, vertexToId.size)
    }
  }

  open func addEdgeVV(_ from: Int64, _ to: Int64, _ action: EdgeType) {
    addVertex(from)
    addVertex(to)
    super.addEdge(vertexToId.get(from)!, vertexToId.get(to)!, action)
  }

  @discardableResult
  open func getDegreeV(_ vertex: Int64) -> Int32 {
    return super.getDegree(vertexToId.get(vertex)!)
  }

  open func print(_ logger: Logger) {
    logger.log("digraph g {")
    logger.log("}")
  }
}
