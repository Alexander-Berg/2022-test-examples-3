package ru.yandex.crypta.graph2.soup.workflow.ops;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.testlib.PerformanceTestHelper;
import ru.yandex.crypta.graph2.testlib.PerformanceTestHelper.ToBytesYield;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class CreateBlankMessagesFromEdgesPerfTest {

    private static final int DATA_SIZE = 1;
    private static final int ITERS = 1;

    private final PerformanceTestHelper performanceTestHelper = new PerformanceTestHelper();

    @Ignore
    @Test
    public void perf() throws Exception {
        Edge directEdge = new Edge("234", EIdType.YANDEXUID, "234", EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list("d1", "d2"), Option.of(3.0), Option.empty());

        CreateBlankMessagesFromEdges mapper = new CreateBlankMessagesFromEdges();

        ToBytesYield yield = new ToBytesYield();
        performanceTestHelper.prepareTableDataFromEntity(directEdge, 0, DATA_SIZE, yield);
        performanceTestHelper.runPerfTest(mapper, yield.toByteArray(), ITERS);

    }


}
