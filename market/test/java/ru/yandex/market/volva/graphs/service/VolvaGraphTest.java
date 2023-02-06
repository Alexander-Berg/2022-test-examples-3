package ru.yandex.market.volva.graphs.service;

import java.util.Set;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mstat.graphs.Graph;
import ru.yandex.market.mstat.graphs.GraphSplitResult;
import ru.yandex.market.mstat.graphs.impl.SimpleGraphEdge;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.yt.YtEdge;
import ru.yandex.market.volva.yt.YtUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VolvaGraphTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    Supplier<String> newMark;

    @Test
    public void createNewEdge() {
        var graph = new VolvaGraph("operation", Set.of(), newMark);
        when(newMark.get()).thenReturn("mark");

        var edge = new SimpleGraphEdge<>(new Node("id1", IdType.PUID), new Node("id2", IdType.PUID));
        graph.apply(g -> new GraphSplitResult<>(Set.of(new Graph<>(Set.of(edge))), Set.of(), Set.of()));
        graph.remarkIfChanged();

        assertThat(graph.getDelta().getChangedEdges())
            .extracting(YtEdge::getOperationId, YtEdge::getGlueId)
            .containsOnly(tuple("operation", "mark"));
        assertThat(graph.getDelta().getChangedUsers())
            .hasSize(2)
            .extracting(YtUser::getGlueId)
            .containsOnly("mark");
        verify(newMark, only()).get();
    }

    @Test
    public void joinTwoComponents() {
        var edge1 = YtEdge.fromNodes(new Node("id1", IdType.PUID), new Node("id2", IdType.PUID), "old").withGlueId("m1");
        var edge2 = YtEdge.fromNodes(new Node("id3", IdType.PUID), new Node("id4", IdType.PUID), "old").withGlueId("m2");
        var graph = new VolvaGraph("operation", Set.of(edge1, edge2), newMark);
        when(newMark.get()).thenReturn("mark");

        var edge3 = new SimpleGraphEdge<>(new Node("id1", IdType.PUID), new Node("id3", IdType.PUID));
        graph.apply(g -> new GraphSplitResult<>(Set.of(new Graph<>(Set.of(edge1.toGraphEdge(), edge2.toGraphEdge(), edge3))), Set.of(), Set.of()));
        graph.remarkIfChanged();

        assertThat(graph.getDelta().getRemovedEdges())
            .containsOnly(edge1, edge2);
        assertThat(graph.getDelta().getChangedEdges())
            .hasSize(3)
            .extracting(YtEdge::getGlueId)
            .containsOnly("mark");
        assertThat(graph.getDelta().getChangedUsers())
            .hasSize(4)
            .extracting(YtUser::getGlueId)
            .containsOnly("mark");
        verify(newMark, only()).get();
    }

    @Test
    public void removeNode() {
        var edge1 = YtEdge.fromNodes(new Node("id1", IdType.PUID), new Node("id2", IdType.PUID), "old").withGlueId("m1");
        var edge2 = YtEdge.fromNodes(new Node("id2", IdType.PUID), new Node("id3", IdType.PUID), "old").withGlueId("m1");
        var graph = new VolvaGraph("operation", Set.of(edge1, edge2), newMark);
        when(newMark.get()).thenReturn("mark");

        graph.apply(g -> new GraphSplitResult<>(Set.of(new Graph<>(Set.of(edge1.toGraphEdge()))), Set.of(new Node("id3", IdType.PUID)), Set.of(edge2.toGraphEdge())));
        graph.remarkIfChanged();

        assertThat(graph.getDelta().getRemovedEdges())
            .containsOnly(edge1, edge2);
        assertThat(graph.getDelta().getChangedEdges())
            .hasSize(1)
            .extracting(YtEdge::getGlueId)
            .containsOnly("mark");
        assertThat(graph.getDelta().getRemovedUsers())
            .extracting(YtUser::getNode)
            .containsOnly(new Node("id3", IdType.PUID));
        assertThat(graph.getDelta().getChangedUsers())
            .hasSize(2)
            .extracting(YtUser::getGlueId)
            .containsOnly("mark");
        verify(newMark, only()).get();
    }

    @Test
    public void splitComponent() {
        var edge1 = YtEdge.fromNodes(new Node("id1", IdType.PUID), new Node("id2", IdType.PUID), "old").withGlueId("m1");
        var edge2 = YtEdge.fromNodes(new Node("id2", IdType.PUID), new Node("id3", IdType.PUID), "old").withGlueId("m1");
        var edge3 = YtEdge.fromNodes(new Node("id3", IdType.PUID), new Node("id4", IdType.PUID), "old").withGlueId("m1");
        var graph = new VolvaGraph("operation", Set.of(edge1, edge2, edge3), newMark);
        when(newMark.get()).thenReturn("mark1", "mark2");

        graph.apply(g -> new GraphSplitResult<>(Set.of(new Graph<>(Set.of(edge1.toGraphEdge())), new Graph<>(Set.of(edge3.toGraphEdge()))), Set.of(), Set.of(edge2.toGraphEdge())));
        graph.remarkIfChanged();

        assertThat(graph.getDelta().getRemovedEdges())
            .containsOnly(edge1, edge2, edge3);
        assertThat(graph.getDelta().getChangedEdges())
            .hasSize(2)
            .extracting(YtEdge::getGlueId)
            .containsOnly("mark1", "mark2");
        assertThat(graph.getDelta().getChangedUsers())
            .hasSize(4)
            .extracting(YtUser::getGlueId)
            .containsOnly("mark1", "mark2");
        verify(newMark, times(2)).get();
    }
}