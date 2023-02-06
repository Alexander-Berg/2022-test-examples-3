package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.VertexProperties;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class TestComponent {

    public static final String FAKE_CRYPTA_ID = "123456789";

    private final String cryptaId;
    private final ListF<Vertex> vertices;
    private final ListF<VertexProperties> verticesProperties;
    private final ListF<Edge> edges;

    private final YsonMultiEntitySupport serializer = new YsonMultiEntitySupport();

    public TestComponent(ListF<Edge> edges) {
        this(edgesToVertices(edges), Cf.list(), edges);
    }

    public TestComponent(ListF<Vertex> vertices, ListF<Edge> edges) {
        this(vertices, Cf.list(), edges);
    }

    public TestComponent(
            ListF<Vertex> vertices,
            ListF<VertexProperties> verticesProperties,
            ListF<Edge> edges) {
        this.cryptaId = FAKE_CRYPTA_ID;
        this.vertices = vertices;
        this.verticesProperties = verticesProperties;
        this.edges = edges;
    }

    private static ListF<Vertex> edgesToVertices(CollectionF<Edge> edges) {
        return edges.flatMap(Edge::getVertices).unique().sorted();
    }

    public ListF<YTreeMapNode> toRecs() {
        ListF<VertexInComponent> verticesInComponent = vertices.map(v -> VertexInComponent.fromVertex(v, cryptaId));
        ListF<VertexProperties> vpsInComponent = verticesProperties.map(vp -> vp.inComponent(cryptaId));
        ListF<EdgeInComponent> edgesInComponent = edges.map(e -> EdgeInComponent.fromEdge(e, cryptaId));

        return Cf.list(
                YtTestHelper.toYsonRecs(serializer, verticesInComponent, 0),
                YtTestHelper.toYsonRecs(serializer, vpsInComponent, 1),
                YtTestHelper.toYsonRecs(serializer, edgesInComponent, 2)
        ).flatten();
    }

    public Component asComponent() {
        Component component = new Component(cryptaId);
        component.setVertices(Cf.toHashSet(vertices));
        component.setInnerEdges(Cf.toHashSet(edges));
        return component;
    }

    public ListF<Vertex> getVertices() {
        return vertices;
    }

    public ListF<VertexProperties> getVerticesProperties() {
        return verticesProperties;
    }

    public ListF<Edge> getEdges() {
        return edges;
    }
}
