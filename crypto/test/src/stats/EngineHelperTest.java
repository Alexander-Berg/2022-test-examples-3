package ru.yandex.crypta.graph.engine.score.stats;

import org.junit.Test;

import ru.yandex.crypta.graph.engine.proto.TGraph;
import ru.yandex.crypta.graph.engine.proto.TStats;
import ru.yandex.crypta.graph.engine.proto.TStatsOptions;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.crypta.lib.proto.identifiers.TGenericID;

import static org.junit.Assert.assertEquals;

public class EngineHelperTest {
    @Test
    public void test() {
        long id = 123;
        TGenericID genericID = TGenericID.newBuilder()
                .setTypeValue(1)
                .setType(EIdType.AVITO_ID).build();
        TGraph graph = TGraph.newBuilder()
                .setId(id)
                .addVertices(genericID)
                .addVertices(genericID)
                .addVertices(genericID)
                .addVertices(genericID)
                .build();

        TStatsOptions options = TStatsOptions.newBuilder().setCrossDeviceWeight(8.).build();
        TStats stats = EngineHelper.collectProdStats(graph, options);

        assertEquals(id, stats.getId());
    }
}
