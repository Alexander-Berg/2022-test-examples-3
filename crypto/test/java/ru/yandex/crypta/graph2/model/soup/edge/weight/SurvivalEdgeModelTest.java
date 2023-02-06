package ru.yandex.crypta.graph2.model.soup.edge.weight;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeProtoHelper;
import ru.yandex.crypta.graph2.model.soup.edge.weight.estimator.SurvivalEdgeModel;
import ru.yandex.crypta.graph2.model.soup.proto.Edge;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class SurvivalEdgeModelTest {
    private static final long HOUR = 60 * 60;
    private static final long DAY = 24 * HOUR;
    private static final double EPS = 1e-9;

    @Test
    public void getDefaultEdgeWeight() {

        Edge.Builder edgeBuilder = Edge.newBuilder()
                .setId1("123")
                .setId1Type(Soup.CONFIG.name(EIdType.YANDEXUID))
                .setId2("234")
                .setId2Type(Soup.CONFIG.name(EIdType.IDFA))
                .setSourceType(Soup.CONFIG.name(ESourceType.APP_METRICA))
                .setLogSource(Soup.CONFIG.name(ELogSourceType.METRIKA_MOBILE_LOG));

        EdgeProtoHelper.setDates(edgeBuilder, Cf.list("2017-04-04", "2017-04-05"));
        Edge edge = edgeBuilder.build();

        long timestamp = 1491382800;
        SurvivalEdgeModel model = new SurvivalEdgeModel();
        assertEquals(model.getEdgeWeight(timestamp + 1 * DAY, model.getStatsQuery(edge)), 1., EPS);
        assertEquals(model.getEdgeWeight(timestamp + 2 * DAY, model.getStatsQuery(edge)), 1, EPS);
        assertEquals(model.getEdgeWeight(timestamp + 3 * DAY, model.getStatsQuery(edge)), 0.75, EPS);
        assertEquals(model.getEdgeWeight(timestamp + 4 * DAY, model.getStatsQuery(edge)), 0.5625, EPS);
    }
}
