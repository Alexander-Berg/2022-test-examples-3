package ru.yandex.market.crm.mapreduce.domain.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

public class IdsGraphTest {
    @Test
    public void removeFewNodesTest() {
        IdsGraph graph = new IdsGraph();

        Uid node0 = Uid.of(UidType.EMAIL, "t0@yandex.ru");
        Uid node1 = Uid.of(UidType.EMAIL, "t1@yandex.ru");
        Uid node2 = Uid.of(UidType.EMAIL, "t2@yandex.ru");
        Uid node3 = Uid.of(UidType.EMAIL, "t3@yandex.ru");
        Uid node4 = Uid.of(UidType.EMAIL, "t4@yandex.ru");
        Uid node5 = Uid.of(UidType.EMAIL, "t5@yandex.ru");
        Uid node6 = Uid.of(UidType.EMAIL, "t6@yandex.ru");

        graph.addNode(node0);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node5);
        graph.addNode(node6);

        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(4, 5, 4);
        graph.addEdge(5, 6, 5);

        graph.removeNodes(ImmutableSet.of(node1, node5));

        List<Uid> actualNodes = graph.getNodes();
        Assert.assertEquals(5, actualNodes.size());
        Assert.assertEquals(node0, actualNodes.get(0));
        Assert.assertEquals(node2, actualNodes.get(1));
        Assert.assertEquals(node3, actualNodes.get(2));
        Assert.assertEquals(node4, actualNodes.get(3));
        Assert.assertEquals(node6, actualNodes.get(4));

        List<IdsGraph.Edge> actualEdges = graph.getEdges();
        List<IdsGraph.Edge> expectedEdges = new ArrayList<>();
        expectedEdges.add(new IdsGraph.Edge(0, 1, 1));
        expectedEdges.add(new IdsGraph.Edge(1, 2, 3));
        expectedEdges.add(new IdsGraph.Edge(3, 4, 4));

        checkEdges(actualEdges, expectedEdges);
    }

    @Test
    public void removeFewIncidentNodesTest() {
        IdsGraph graph = new IdsGraph();

        Uid node0 = Uid.of(UidType.EMAIL, "t0@yandex.ru");
        Uid node1 = Uid.of(UidType.EMAIL, "t1@yandex.ru");
        Uid node2 = Uid.of(UidType.EMAIL, "t2@yandex.ru");
        Uid node3 = Uid.of(UidType.EMAIL, "t3@yandex.ru");
        Uid node4 = Uid.of(UidType.EMAIL, "t4@yandex.ru");
        Uid node5 = Uid.of(UidType.EMAIL, "t5@yandex.ru");
        Uid node6 = Uid.of(UidType.EMAIL, "t6@yandex.ru");

        graph.addNode(node0);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node5);
        graph.addNode(node6);

        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(1, 3, 4);
        graph.addEdge(2, 4, 5);
        graph.addEdge(3, 4, 6);
        graph.addEdge(4, 5, 7);
        graph.addEdge(3, 6, 8);

        graph.removeNodes(ImmutableSet.of(node1, node3));

        List<Uid> actualNodes = graph.getNodes();
        Assert.assertEquals(5, actualNodes.size());
        Assert.assertEquals(node0, actualNodes.get(0));
        Assert.assertEquals(node2, actualNodes.get(1));
        Assert.assertEquals(node4, actualNodes.get(2));
        Assert.assertEquals(node5, actualNodes.get(3));
        Assert.assertEquals(node6, actualNodes.get(4));

        List<IdsGraph.Edge> actualEdges = graph.getEdges();
        List<IdsGraph.Edge> expectedEdges = new ArrayList<>();
        expectedEdges.add(new IdsGraph.Edge(0, 1, 1));
        expectedEdges.add(new IdsGraph.Edge(0, 2, 1));
        expectedEdges.add(new IdsGraph.Edge(0, 4, 1));
        expectedEdges.add(new IdsGraph.Edge(1, 4, 3));
        expectedEdges.add(new IdsGraph.Edge(1, 2, 3));
        expectedEdges.add(new IdsGraph.Edge(2, 4, 6));
        expectedEdges.add(new IdsGraph.Edge(2, 3, 7));

        checkEdges(actualEdges, expectedEdges);
    }

    @Test
    public void removeNodeFromEmptyGraphTest() {
        IdsGraph graph = new IdsGraph();

        graph.removeNodes(Collections.singleton(Uid.of(UidType.EMAIL, "1@mail.ru")));

        List<Uid> actualNodes = graph.getNodes();
        Assert.assertEquals(0, actualNodes.size());

        List<IdsGraph.Edge> actualEdges = graph.getEdges();
        Assert.assertEquals(0, actualEdges.size());
    }

    @Test
    public void removeNonexistentNodeTest() {
        IdsGraph graph = new IdsGraph();

        Uid node0 = Uid.of(UidType.EMAIL, "t0@yandex.ru");
        Uid node1 = Uid.of(UidType.EMAIL, "t1@yandex.ru");
        Uid node2 = Uid.of(UidType.EMAIL, "t2@yandex.ru");

        graph.addNode(node0);
        graph.addNode(node1);
        graph.addNode(node2);

        graph.addEdge(0, 1, 0);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 2, 2);

        graph.removeNodes(Collections.singleton(Uid.of(UidType.EMAIL, "1@mail.ru")));

        List<Uid> actualNodes = graph.getNodes();
        Assert.assertEquals(3, actualNodes.size());
        Assert.assertEquals(node0, actualNodes.get(0));
        Assert.assertEquals(node1, actualNodes.get(1));
        Assert.assertEquals(node2, actualNodes.get(2));

        List<IdsGraph.Edge> actualEdges = graph.getEdges();
        List<IdsGraph.Edge> expectedEdges = new ArrayList<>();
        expectedEdges.add(new IdsGraph.Edge(0, 1, 0));
        expectedEdges.add(new IdsGraph.Edge(0, 2, 1));
        expectedEdges.add(new IdsGraph.Edge(1, 2, 2));

        checkEdges(actualEdges, expectedEdges);
    }

    @Test
    public void removeOneNodeTest() {
        IdsGraph graph = new IdsGraph();

        Uid node0 = Uid.of(UidType.EMAIL, "t0@yandex.ru");
        Uid node1 = Uid.of(UidType.EMAIL, "t1@yandex.ru");
        Uid node2 = Uid.of(UidType.EMAIL, "t2@yandex.ru");
        Uid node3 = Uid.of(UidType.EMAIL, "t3@yandex.ru");
        Uid node4 = Uid.of(UidType.EMAIL, "t4@yandex.ru");

        graph.addNode(node0);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);

        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(1, 3, 4);
        graph.addEdge(2, 4, 5);
        graph.addEdge(3, 4, 6);

        graph.removeNodes(Collections.singleton(node1));

        List<Uid> actualNodes = graph.getNodes();
        Assert.assertEquals(4, actualNodes.size());
        Assert.assertEquals(node0, actualNodes.get(0));
        Assert.assertEquals(node2, actualNodes.get(1));
        Assert.assertEquals(node3, actualNodes.get(2));
        Assert.assertEquals(node4, actualNodes.get(3));

        List<IdsGraph.Edge> actualEdges = graph.getEdges();

        List<IdsGraph.Edge> expectedEdges = new ArrayList<>();
        expectedEdges.add(new IdsGraph.Edge(0, 1, 1));
        expectedEdges.add(new IdsGraph.Edge(0, 2, 1));
        expectedEdges.add(new IdsGraph.Edge(1, 2, 3));
        expectedEdges.add(new IdsGraph.Edge(2, 3, 6));
        expectedEdges.add(new IdsGraph.Edge(1, 3, 5));

        checkEdges(actualEdges, expectedEdges);
    }

    private void checkEdges(List<IdsGraph.Edge> actualEdges, List<IdsGraph.Edge> expectedEdges) {
        Assert.assertEquals(expectedEdges.size(), actualEdges.size());

        Comparator<IdsGraph.Edge> edgeComparator = (first, second) -> {
            if (first.getNode1() < second.getNode1()) {
                return -1;
            }

            if (first.getNode1() > second.getNode1()) {
                return 1;
            }

            int res = Integer.compare(first.getNode2(), second.getNode2());
            if (res != 0) {
                return res;
            }

            return Long.compare(first.getTimestamp(), second.getTimestamp());
        };

        Set<IdsGraph.Edge> actual = new TreeSet<>(edgeComparator);
        actual.addAll(actualEdges);

        Set<IdsGraph.Edge> expected = new TreeSet<>(edgeComparator);
        expected.addAll(expectedEdges);

        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertTrue(expected.containsAll(actual));
    }
}
