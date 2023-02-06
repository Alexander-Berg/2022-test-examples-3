import { CompressedGraph } from "../data-structures/graph";
import { StrongComponentAlgo } from "./strong-component-algo";
import { Int32, range } from "../../../../ys/ys";
import { Logger } from "../../../utils/logger";
import { EulerGraphAlgo } from "./euler-graph-algo";
import { DistanceAlgo } from "./distance-algo";
import { Stack } from "../data-structures/stack";
import { TopSortAlgo } from "./topsort-algo";

export class LongestPathAlgo {

  public static getLongestPath<T>(graph: CompressedGraph<T>, logger: Logger): T[] {
    if (graph.size() === 0 || graph.countOfEdges() === 0) {
      return []
    }

    const components = StrongComponentAlgo.getStrongConnectedComponents(graph);
    const condensedGraph = StrongComponentAlgo.getCondensedGraph(graph);
    const longestPath = LongestPathAlgo.getLongestPathInCondensedGraph(condensedGraph, components);

    let current_component: Int32 = 0;
    let current_vertex: Int32 = 0;
    for (const i of range(0, components.size())) {
      if (components.get(i).has(current_vertex)) {
        current_component = i;
      }
    }
    logger.log(`Current component id = ${current_component}, size = ${components.get(current_component).size}`);
    let path: T[] = EulerGraphAlgo.getEulerCircleInComponent(graph, components.get(current_component), current_vertex).map((edgeId) => graph.edges[edgeId].getAction());
    let edgeId: Int32;

    while (longestPath.getEdgesId(current_component).length > 0) {
      edgeId = longestPath.getEdgesId(current_component)[0];
      const condensedEdge = longestPath.edges[edgeId];
      const edge = condensedEdge.getAction();
      const distances = DistanceAlgo.getDistances(graph, current_vertex, components.get(current_component));
      const connected_path = DistanceAlgo.getPathTo(edge.getFrom(), distances, graph).map((edgeId) => graph.edges[edgeId].getAction());
      path = path.concat(connected_path);
      path.push(edge.getAction());

      current_component = condensedEdge.getTo();
      current_vertex = edge.getTo();

      logger.log(`Current component id = ${current_component}, size = ${components.get(current_component).size}`);
      const component_path = EulerGraphAlgo.getEulerCircleInComponent(graph, components.get(current_component), current_vertex).map((edgeId) => graph.edges[edgeId].getAction());
      path = path.concat(component_path);
    }
    return path;
  }

  private static getLongestPathInCondensedGraph<T, VertexType>(condensed: CompressedGraph<T>, components: Stack<Set<VertexType>>): CompressedGraph<T> {
    const path: CompressedGraph<T> = new CompressedGraph();
    let size: Int32[] = [];
    for (const _ of range(0, condensed.size())) {
      size.push(0);
    }

    const topSort = TopSortAlgo.getTopSort(condensed);
    for (const i of range(0, topSort.size())) {
      const vertex = topSort.get(i);
      size[vertex] += components.get(vertex).size;
      let mx: Int32 = -1;
      let edge_index: Int32 = -1;
      for (const edgeId of condensed.getEdgesId(vertex)) {
        const to = condensed.edges[edgeId].getTo();
        if (size[to] > mx) {
          mx = size[to];
          edge_index = edgeId;
        }
      }
      if (edge_index >= 0) {
        size[vertex] += mx;
        path.addEdge(vertex, condensed.edges[edge_index].getTo(), condensed.edges[edge_index].getAction());
      }
    }
    return path;
  }
}
