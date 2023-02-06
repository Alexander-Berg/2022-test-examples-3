package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen;

import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.graph.JGraphTUtils;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.DeviceId;
import ru.yandex.crypta.graph2.model.soup.props.VertexProperties;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.random.Random2;

public class RandomComponentGenerator extends TestComponentGenerator {

    private final int numberOfEdges;

    public RandomComponentGenerator(int numberOfEdges) {
        super();
        this.numberOfEdges = numberOfEdges;
    }

    @Override
    public TestComponent generateComponent() {

        ListF<Vertex> vertices = Cf.arrayList();
        ListF<VertexProperties> yandexuidProps = Cf.arrayList();
        ListF<VertexProperties> deviceIdProps = Cf.arrayList();
        ListF<Edge> edges = Cf.arrayList();

        ListF<Edge> edgeCandidates = generateEdges(numberOfEdges);

        UndirectedGraph<Vertex, Edge> vertexEdgeWeightedGraph = JGraphTUtils.toGraph(edgeCandidates);
        ConnectivityInspector<Vertex, Edge> ci = new ConnectivityInspector<>(vertexEdgeWeightedGraph);
        Set<Vertex> largestComponent = Cf.wrap(ci.connectedSets()).maxBy(Set::size);

        for (Edge edge : edgeCandidates) {
            Vertex v1 = edge.getVertex1();
            Vertex v2 = edge.getVertex2();

            if (largestComponent.contains(v1) && largestComponent.contains(v2)) {

                vertices.add(v1);
                vertices.add(v2);

                yandexuidProps.addAll(maybeYandexuidProp(v1));
                yandexuidProps.addAll(maybeYandexuidProp(v2));
                deviceIdProps.addAll(maybeDeviceIdProp(v1));
                deviceIdProps.addAll(maybeDeviceIdProp(v2));

                edges.add(edge);

            }

        }

        System.out.println(String.format("Created connected component:\nVertices:%d\nEdges:%d\nVPs:%d",
                vertices.size(), edges.size(), yandexuidProps.size() + deviceIdProps.size())
        );

        return new TestComponent(
                vertices,
                yandexuidProps.plus(deviceIdProps),
                edges
        );

    }

    @SuppressWarnings("UnusedVariable")
    private ListF<Edge> generateEdges(int numberOfEdges) {
        ListF<Edge> edges = Cf.arrayList();
        ListF<Vertex> vertices = Cf.repeat(()->generator.randomVertex(), numberOfEdges);
        for (int idx : Cf.range(0, numberOfEdges)) {

            Vertex v1 = Random2.R.randomElement(vertices);
            Vertex v2 = Random2.R.randomElement(vertices);

            if (v1.equals(v2)) {
                continue;
            }
            Edge edge = randomEdge(v1, v2, numberOfEdges);
            edges.add(edge);
        }
        return edges;
    }

    private Edge randomEdge(Vertex v1, Vertex v2, int upper) {
        int rnd = Random2.R.nextInt(upper);
        int sourceTypeN = rnd % ESourceType.values().length;
        int logSourceN = rnd % ELogSourceType.values().length;
        ListF<String> dates = Cf.range(0, rnd % 10).map(String::valueOf);

        return new Edge(
                v1.getId(),
                v1.getIdType(),
                v2.getId(),
                v2.getIdType(),
                ESourceType.values()[sourceTypeN],
                ELogSourceType.values()[logSourceN],
                dates,
                Option.of(1.0),
                Option.of(1.0)
        );

    }

    private Option<Yandexuid> maybeYandexuidProp(Vertex v) {
        return Option.when(
                v.getIdType() == EIdType.YANDEXUID,
                new Yandexuid(v, null, "123", Option.of(123), "", false)
        );
    }

    private Option<DeviceId> maybeDeviceIdProp(Vertex v) {
        return Option.when(
                Soup.CONFIG.isDeviceIdMainId(v.getIdType()),
                new DeviceId(new Vertex(v.getId(), EIdType.OLD_DEVICE_ID),
                        null, "", "mobile",
                        "ios", Option.of(124), "", false
                )
        );
    }
}
