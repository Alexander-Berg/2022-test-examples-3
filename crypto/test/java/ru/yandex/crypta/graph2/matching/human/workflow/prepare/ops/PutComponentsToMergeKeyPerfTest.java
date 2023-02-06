package ru.yandex.crypta.graph2.matching.human.workflow.prepare.ops;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyType;
import ru.yandex.crypta.graph2.model.matching.merge.MergeNeighbour;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.graph2.testlib.PerformanceTestHelper;
import ru.yandex.crypta.graph2.testlib.PerformanceTestHelper.ToBytesYield;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class PutComponentsToMergeKeyPerfTest {

    private static final int DATA_SIZE = 1;
    private static final int ITERS = 1;

    private final PerformanceTestHelper performanceTestHelper = new PerformanceTestHelper();

    @Ignore
    @Test
    public void test() throws Exception {

        String cryptaId = "ccId";

        ListF<MergeNeighbour> mergeOffers = Cf.list(
                new MergeNeighbour(cryptaId, new MergeKey("m1", MergeKeyType.BY_EDGE)),
                new MergeNeighbour(cryptaId, new MergeKey("m2", MergeKeyType.BY_EDGE))
        );

        VertexInComponent vertex = VertexInComponent.fromVertex(new Vertex("234", EIdType.YANDEXUID), cryptaId);
        EdgeInComponent edge = EdgeInComponent.fromEdge(new Edge(vertex.getVertex().getId(),
                vertex.getVertex().getIdType(),
                "234", EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list("d1", "d2"), Option.of(3.0), Option.empty()), cryptaId);
        Yandexuid prop = new Yandexuid(vertex.getVertex(), cryptaId, "dsf", Option.empty(), "", true);

        PutComponentsToMergeKey reducer = new PutComponentsToMergeKey();

        ToBytesYield out = new ToBytesYield();

        for (int i = 0; i < ITERS; i++) {
            performanceTestHelper.prepareTableDataFromEntity(mergeOffers, 0, 2, out);
            performanceTestHelper.prepareTableDataFromEntity(vertex, 1, DATA_SIZE, out);
            performanceTestHelper.prepareTableDataFromEntity(edge, 2, DATA_SIZE, out);
            performanceTestHelper.prepareTableDataFromEntity(prop, 3, DATA_SIZE, out);
        }

        out.close();

        performanceTestHelper.runPerfTest(reducer, out.toByteArray(), 100);

    }
}
