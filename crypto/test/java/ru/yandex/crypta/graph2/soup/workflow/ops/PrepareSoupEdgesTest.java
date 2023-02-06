package ru.yandex.crypta.graph2.soup.workflow.ops;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeProtoHelper;
import ru.yandex.crypta.graph2.model.soup.edge.weight.estimator.SurvivalEdgeModel;
import ru.yandex.crypta.graph2.model.soup.edge.weight.estimator.SurvivalEdgeWeightEstimator;
import ru.yandex.crypta.graph2.model.soup.proto.Edge;
import ru.yandex.crypta.graph2.model.soup.sources.DefaultEdgeTypeConfigProvider;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.SOUP_PREPROCESSING;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.MD5_HASH;

public class PrepareSoupEdgesTest {

    private PrepareSoupEdges mapper;
    private LocalDate soupPrepareDate = LocalDate.parse("2017-09-26");

    @Before
    public void setUp() throws Exception {
        DefaultEdgeTypeConfigProvider edgeTypeConfigProvider = new DefaultEdgeTypeConfigProvider();
        SurvivalEdgeWeightEstimator survivalEdgeWeightEstimator = new SurvivalEdgeWeightEstimator(
                new SurvivalEdgeModel(soupPrepareDate),
                edgeTypeConfigProvider
        );
        mapper = new PrepareSoupEdges(edgeTypeConfigProvider, edge -> 0, survivalEdgeWeightEstimator, soupPrepareDate,1);
    }

    private Edge edgeWithDates(String... dates) {
        Edge.Builder edgeBuilder = buildEdge(
                "y1",
                EIdType.YANDEXUID,
                "d1",
                EIdType.IDFA,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG
        );

        EdgeProtoHelper.setDates(edgeBuilder, Cf.list(dates));
        return edgeBuilder.build();
    }

    private Edge duidEdgeWithDates(String... dates) {
        Edge.Builder edgeBuilder = buildEdge(
                "y1",
                EIdType.DUID,
                "d1",
                EIdType.UUID,
                ESourceType.APP_METRICA_SOCKETS_ANDROID,
                ELogSourceType.WATCH_LOG
        );

        EdgeProtoHelper.setDates(edgeBuilder, Cf.list(dates));
        return edgeBuilder.build();
    }

    private Edge.Builder buildEdge(String id1, EIdType id1Type, String id2, EIdType id2Type,
                                   ESourceType sourceType, ELogSourceType logSource) {
        return Edge.newBuilder()
                .setId1(id1)
                .setId1Type(Soup.CONFIG.name(id1Type))
                .setId2(id2)
                .setId2Type(Soup.CONFIG.name(id2Type))
                .setSourceType(Soup.CONFIG.name(sourceType))
                .setLogSource(Soup.CONFIG.name(logSource));
    }

    private Optional<Edge> map(Edge edge) {
        LocalYield<Edge> yield = new LocalYield<>();
        mapper.map(edge, yield, new StatisticsSlf4jLoggingImpl());

        List<Edge> result = yield.getRecsByIndex(0);
        Assert.assertTrue(result.size() <= 1);

        return result.stream().findFirst();
    }

    @Test
    public void testAllPasses() throws Exception {
        Optional<Edge> activeTwoDates = map(edgeWithDates("2017-08-01", "2017-08-02"));
        assertTrue(activeTwoDates.isPresent());

        Optional<Edge> recentActiveOneDate = map(edgeWithDates("2017-09-01"));
        assertTrue(recentActiveOneDate.isPresent());

        Optional<Edge> oldActiveOneDate = map(edgeWithDates("2017-04-01"));
        assertTrue(oldActiveOneDate.isPresent());

        // edges with no dates should not be filtered
        Optional<Edge> notActiveEdge = map(edgeWithDates());
        assertTrue(notActiveEdge.isPresent());
    }

    @Test
    public void testDecay() throws Exception {
        Optional<Edge> duidTwoDatesFresh = map(duidEdgeWithDates("2017-01-20", "2017-09-02"));
        assertTrue(duidTwoDatesFresh.isPresent());

        Optional<Edge> duidTwoDatesOld = map(duidEdgeWithDates("2017-01-20", "2017-01-22"));
        assertFalse(duidTwoDatesOld.isPresent());

        Optional<Edge> duidOneDateFresh = map(duidEdgeWithDates("2017-09-21"));
        assertTrue(duidOneDateFresh.isPresent());

        Optional<Edge> duidOneDateOld = map(duidEdgeWithDates("2017-01-21"));
        assertFalse(duidOneDateOld.isPresent());

        Optional<Edge> duidNoDates = map(duidEdgeWithDates());
        assertFalse(duidNoDates.isPresent());

        // edges that are not decayed should go
        Optional<Edge> activeTwoDates = map(edgeWithDates("2017-01-01", "2017-01-02"));
        assertTrue(activeTwoDates.isPresent());

        Optional<Edge> notActiveEdge = map(edgeWithDates());
        assertTrue(notActiveEdge.isPresent());
    }

    @Test
    public void testNoActivityEdgePasses() throws Exception {
        Edge artificialEdge = buildEdge(
                "p1", EIdType.PHONE, "ph1",
                EIdType.PHONE_MD5, MD5_HASH, SOUP_PREPROCESSING
        ).build();

        Optional<Edge> artificialEdgeKept = map(artificialEdge);

        assertTrue(artificialEdgeKept.isPresent());
    }


}
