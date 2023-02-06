package ru.yandex.market.mstat.graphs;


import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.mstat.graphs.impl.SimpleEdgeConstructor;
import ru.yandex.market.mstat.graphs.impl.SimpleGraphEdge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class GraphToolTest {

    @Test
    public void removeNode() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4),
                new SimpleGraphEdge<>(4, 7),
                new SimpleGraphEdge<>(5, 6),
                new SimpleGraphEdge<>(5, 7)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        GraphSplitResult<Integer> splitResult = graphTool.removeNode(graph, 7);
        assertThat(splitResult.getSingleNodes()).containsExactly(7);
        Set<GraphEdge<Integer>> island1 = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4)
        );
        Set<GraphEdge<Integer>> island2 = Set.of(
                new SimpleGraphEdge<>(5, 6)
        );
        assertThat(splitResult.getGraphs()).containsExactlyInAnyOrder(
                new Graph<>(island1),
                new Graph<>(island2)
        );
        assertThat(splitResult.getRemovedEdges()).containsExactlyInAnyOrder(
                new SimpleGraphEdge<>(5, 7),
                new SimpleGraphEdge<>(4, 7)
        );
    }

    @Test
    public void removeNode2() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(2, 3),
                new SimpleGraphEdge<>(2, 4),
                new SimpleGraphEdge<>(4, 5),
                new SimpleGraphEdge<>(4, 6),
                new SimpleGraphEdge<>(4, 8),
                new SimpleGraphEdge<>(4, 9),
                new SimpleGraphEdge<>(4, 10),
                new SimpleGraphEdge<>(5, 7),
                new SimpleGraphEdge<>(6, 7),
                new SimpleGraphEdge<>(10, 11)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        GraphSplitResult<Integer> splitResult = graphTool.removeNode(graph, 4);
        assertThat(splitResult.getSingleNodes()).containsExactlyInAnyOrder(4, 8, 9);
        Set<GraphEdge<Integer>> island1 = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(2, 3)
        );
        Set<GraphEdge<Integer>> island2 = Set.of(
                new SimpleGraphEdge<>(5, 7),
                new SimpleGraphEdge<>(6, 7)
        );
        Set<GraphEdge<Integer>> island3 = Set.of(
                new SimpleGraphEdge<>(10, 11)
        );
        assertThat(splitResult.getGraphs()).containsExactlyInAnyOrder(
                new Graph<>(island1),
                new Graph<>(island2),
                new Graph<>(island3)
        );
        assertThat(splitResult.getRemovedEdges()).containsExactlyInAnyOrder(
                new SimpleGraphEdge<>(2, 4),
                new SimpleGraphEdge<>(4, 5),
                new SimpleGraphEdge<>(4, 6),
                new SimpleGraphEdge<>(4, 8),
                new SimpleGraphEdge<>(4, 9),
                new SimpleGraphEdge<>(4, 10)
        );
    }

    @Test
    public void removeEdge() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4),
                new SimpleGraphEdge<>(4, 7),
                new SimpleGraphEdge<>(5, 6),
                new SimpleGraphEdge<>(5, 7)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        GraphSplitResult<Integer> splitResult = graphTool.removeEdge(graph, new SimpleGraphEdge<>(4, 7));
        assertThat(splitResult.getSingleNodes()).isEmpty();
        Set<GraphEdge<Integer>> island1 = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4)
        );
        Set<GraphEdge<Integer>> island2 = Set.of(
                new SimpleGraphEdge<>(5, 6),
                new SimpleGraphEdge<>(5, 7)
        );
        assertThat(splitResult.getGraphs()).containsExactlyInAnyOrder(
                new Graph<>(island1),
                new Graph<>(island2)
        );
        assertThat(splitResult.getRemovedEdges()).containsExactlyInAnyOrder(
                new SimpleGraphEdge<>(4, 7)
        );
    }

    @Test
    public void addEdge() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        graph = graphTool.addEdge(graph, new SimpleGraphEdge<>(2, 5));
        graph = graphTool.addEdge(graph, new SimpleGraphEdge<>(3, 4));
        assertThat(graph.getEdges()).contains(
                new SimpleGraphEdge<>(2, 5),
                new SimpleGraphEdge<>(3, 4)
        );
        assertThat(graph.getEdges()).hasSize(5);
    }


    @Test
    public void addEdges() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        Set<Graph<Integer>> graphs = graphTool.addEdges(graph, List.of(
                new SimpleGraphEdge<>(2, 5),
                new SimpleGraphEdge<>(3, 4),
                new SimpleGraphEdge<>(9, 10)
        ));
        assertThat(graphs).containsExactlyInAnyOrder(
                new Graph<>(Set.of(
                        new SimpleGraphEdge<>(1, 2),
                        new SimpleGraphEdge<>(1, 3),
                        new SimpleGraphEdge<>(2, 4),
                        new SimpleGraphEdge<>(2, 5),
                        new SimpleGraphEdge<>(3, 4)
                )),
                new Graph<>(Set.of(
                        new SimpleGraphEdge<>(9, 10)
                ))
        );
    }

    @Test
    public void getGraphIslands() {
        Set<GraphEdge<Integer>> edges = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4),
                new SimpleGraphEdge<>(5, 6)
        );
        Graph<Integer> graph = new Graph<>(edges);
        GraphTool<Integer> graphTool = new GraphTool<>(new SimpleEdgeConstructor<>(Comparator.comparingInt(i -> i)));
        Set<Graph<Integer>> graphs = graphTool.getGraphIslands(graph);
        Set<GraphEdge<Integer>> island1 = Set.of(
                new SimpleGraphEdge<>(1, 2),
                new SimpleGraphEdge<>(1, 3),
                new SimpleGraphEdge<>(2, 4)
        );
        Set<GraphEdge<Integer>> island2 = Set.of(
                new SimpleGraphEdge<>(5, 6)
        );
        assertThat(graphs).containsExactlyInAnyOrder(
                new Graph<>(island1),
                new Graph<>(island2)
        );
    }
}
