package ru.yandex.crypta.graph.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.crypta.graph.api.model.graph.Edge;
import ru.yandex.crypta.graph.api.model.graph.Graph;
import ru.yandex.crypta.graph.api.model.graph.GraphComponent;
import ru.yandex.crypta.graph.api.model.graph.Vertex;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class GraphDiffHelperTest {

    @Test
    public void getGraphsDiff() {
        // computed data section
        List<Vertex> verteces = new ArrayList<>();
        verteces.add(new Vertex("1", "smth"));
        verteces.add(new Vertex("2", "smth"));
        verteces.add(new Vertex("3", "smth"));
        verteces.add(new Vertex("4", "smth"));

        Edge edge12 = new Edge("1", "smth", "2", "smth", "", "", 0.0, emptyList());
        Edge edge23 = new Edge("2", "smth", "3", "smth", "", "", 0.0, emptyList());
        Edge edge34 = new Edge("3", "smth", "4", "smth", "", "", 0.0, emptyList());
        Edge edge13 = new Edge("1", "smth", "3", "smth", "", "", 0.0, emptyList());
        Edge edge14 = new Edge("1", "smth", "4", "smth", "", "", 0.0, emptyList());

        List<Edge> edges1 = Arrays.asList(
                edge12,
                edge23,
                edge13,
                edge34
        );
        GraphComponent comp1 = new GraphComponent("", verteces, edges1);
        Graph graph1 = new Graph(Collections.singletonList(comp1));

        List<Edge> edges2 = Arrays.asList(
                edge12,
                edge23,
                edge34,
                edge14
        );
        GraphComponent comp2 = new GraphComponent("", verteces, edges2);
        Graph graph2 = new Graph(Collections.singletonList(comp2));

        // TEST
        Graph computedDiff = GraphDiffHelper.getGraphsDiff(graph1, graph2);

        Set<Edge> computedDiffEdges = new HashSet<>(computedDiff.getGraphComponents().get(0).getEdges());

        // correct data section
        edge13.setRemovedStatus();
        edge14.setAddedStatus();

        Set<Edge> correctDiffEdges = new HashSet<>();
        correctDiffEdges.add(edge12);
        correctDiffEdges.add(edge13);
        correctDiffEdges.add(edge14);
        correctDiffEdges.add(edge23);
        correctDiffEdges.add(edge34);

        // assertion section
        assertEquals(computedDiffEdges, correctDiffEdges);
    }


}
