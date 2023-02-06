package ru.yandex.crypta.graph.api.service.transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Test;

import ru.yandex.crypta.graph.api.model.graph.Edge;
import ru.yandex.crypta.graph.api.model.graph.Vertex;

import static org.junit.Assert.assertEquals;

public class GraphShrinkingTransformerTest {
    @Test
    public void testTransformer1() {
        GraphShrinkingTransformer transformer = new GraphShrinkingTransformer(x -> true);

        Vertex v1 = new Vertex("1", "some");
        Vertex v2 = new Vertex("2", "some");
        Vertex v3 = new Vertex("3", "some");

        String sourceType = "some";
        String logSource = "some";
        List<String> dates = new ArrayList<>();

        Set<Edge> edges = new HashSet<>();
        edges.add(new Edge(v1, v3, sourceType, logSource, 0.0, dates));
        edges.add(new Edge(v1, v2, sourceType, logSource, 0.0, dates));

        var graph = JGraphTHelper.toSimpleGraph(edges);
        var result = transformer.transform(graph);

        assertEquals(result.edgeSet().size(), 0);
    }

    @Test
    public void testTransformer2() {
        Predicate<Edge> condition = (Edge x) -> {
            return x.getId1().contains("shrunk") && x.getId2().contains("shrunk");
        };
        GraphShrinkingTransformer transformer = new GraphShrinkingTransformer(condition);

        Vertex v1 = new Vertex("1", "some");
        Vertex v2 = new Vertex("2_shrunk", "some");
        Vertex v3 = new Vertex("3_shrunk", "some");

        List<String> dates = new ArrayList<>();

        Set<Edge> edges = new HashSet<>();
        edges.add(new Edge(v1, v3, "some", "some", 0.0, dates));
        edges.add(new Edge(v1, v2, "some", "some", 0.0, dates));
        edges.add(new Edge(v2, v3, "some", "some", 0.0, dates));


        var graph = JGraphTHelper.toSimpleGraph(edges);
        var result = transformer.transform(graph);

        assertEquals(result.edgeSet().size(), 1);
    }

    @Test
    public void testTransformer3() {
        Predicate<Edge> condition = (Edge x) -> {
            return x.getId1().contains("shrunk") && x.getId2().contains("shrunk");
        };
        GraphShrinkingTransformer transformer = new GraphShrinkingTransformer(condition);

        Vertex v1 = new Vertex("1", "some");
        Vertex v2 = new Vertex("2_shrunk", "some");
        Vertex v3 = new Vertex("3_shrunk", "some");
        Vertex v4 = new Vertex("4", "some");

        List<String> dates = new ArrayList<>();

        Set<Edge> edges = new HashSet<>();
        edges.add(new Edge(v1, v3, "some", "some", 0.0, dates));
        edges.add(new Edge(v1, v2, "some", "some", 0.0, dates));
        edges.add(new Edge(v2, v3, "some", "some", 0.0, dates));
        edges.add(new Edge(v3, v4, "some", "some", 0.0, dates));


        var graph = JGraphTHelper.toSimpleGraph(edges);
        var result = transformer.transform(graph);

        assertEquals(result.edgeSet().size(), 2);
    }

    @Test
    public void testTransformer4() {
        Predicate<Edge> condition = (Edge x) -> {
            return x.getId1().contains("shrunk") && x.getId2().contains("shrunk");
        };
        GraphShrinkingTransformer transformer = new GraphShrinkingTransformer(condition);

        Vertex v1 = new Vertex("1", "some");
        Vertex v2 = new Vertex("2_shrunk", "some");
        Vertex v3 = new Vertex("3_shrunk", "some");
        Vertex v4 = new Vertex("4", "some");
        Vertex v5 = new Vertex("5_shrunk", "some");
        Vertex v6 = new Vertex("6_shrunk", "some");
        Vertex v7 = new Vertex("7_shrunk", "some");
        Vertex v8 = new Vertex("8_shrunk", "some");

        List<String> dates = new ArrayList<>();

        Set<Edge> edges = new HashSet<>();
        edges.add(new Edge(v1, v3, "some", "some", 0.0, dates));
        edges.add(new Edge(v1, v2, "some", "some", 0.0, dates));
        edges.add(new Edge(v2, v3, "some", "some", 0.0, dates));
        edges.add(new Edge(v3, v4, "some", "some", 0.0, dates));
        edges.add(new Edge(v5, v4, "some", "some", 0.0, dates));
        edges.add(new Edge(v7, v4, "some", "some", 0.0, dates));
        edges.add(new Edge(v5, v6, "some", "some", 0.0, dates));
        edges.add(new Edge(v7, v6, "some", "some", 0.0, dates));
        edges.add(new Edge(v7, v8, "some", "some", 0.0, dates));

        var graph = JGraphTHelper.toSimpleGraph(edges);
        var result = transformer.transform(graph);

        assertEquals(result.edgeSet().size(), 3);
    }
}
