package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeType;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class SingleLineComponentGenerator extends TestComponentGenerator {

    public static final EdgeType FAKE_EDGE_TYPE = new EdgeType(
            EIdType.YANDEXUID, EIdType.YANDEXUID, ESourceType.APP_METRICA, ELogSourceType.ACCESS_LOG
    );

    private final int numberOfEdges;

    public SingleLineComponentGenerator(int numberOfEdges) {
        super();
        this.numberOfEdges = numberOfEdges;
    }

    @Override
    public TestComponent generateComponent() {
        return generateSingleLineComponent().getComponent();
    }

    public class SingleLineComponent {
        private final TestComponent component;
        private final ListF<Vertex> line;

        public SingleLineComponent(int numberOfEdges) {
            ListF<Edge> edges = Cf.arrayList();
            line = Cf.repeat(()->generator.randomVertex(EIdType.YANDEXUID), numberOfEdges + 1);
            Vertex prevVertex = null;
            for (Vertex vertex : line) {
                if (prevVertex == null) {
                    prevVertex = vertex;
                    continue;
                }
                ListF<String> dates = Cf.repeat("d", generator.random.nextInt(10));
                edges.add(new Edge(FAKE_EDGE_TYPE, prevVertex.getId(), vertex.getId(), dates, Option.of(1.0), Option.of(1.0)));
                prevVertex = vertex;
            }
            component = new TestComponent(edges);
        }

        public TestComponent getComponent() {
            return component;
        }

        public ListF<Vertex> getVerticesLine() {
            return line;
        }
    }

    public SingleLineComponent generateSingleLineComponent() {
        return new SingleLineComponent(numberOfEdges);
    }
}
