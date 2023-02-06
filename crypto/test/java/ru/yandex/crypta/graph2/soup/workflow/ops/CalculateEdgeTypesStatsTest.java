package ru.yandex.crypta.graph2.soup.workflow.ops;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeType;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeTypeActivityStats;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class CalculateEdgeTypesStatsTest {

    private ListF<String> dates(int datesCount) {
        return Cf.range(1, datesCount + 1).map(d -> "2017-10-0" + String.valueOf(d));
    }

    @Test
    public void testDatesActivityStats() throws Exception {
        CalculateEdgeTypesStats.Mapper mapper = new CalculateEdgeTypesStats.Mapper();
        CalculateEdgeTypesStats.Reducer reducer = new CalculateEdgeTypesStats.Reducer();

        EdgeType et1 = new EdgeType(EIdType.YANDEXUID, EIdType.EMAIL, ESourceType.WEBVISOR, ELogSourceType.WEBVISOR_LOG);
        EdgeType et2 = new EdgeType(EIdType.YANDEXUID, EIdType.PHONE, ESourceType.WEBVISOR, ELogSourceType.WEBVISOR_LOG);

        Edge e1 = new Edge(et1, "y1", "e1", dates(1));
        Edge e2 = new Edge(et1, "y1", "e2", dates(2));
        Edge e3 = new Edge(et1, "y2", "e1", dates(3));

        Edge e4 = new Edge(et2, "y1", "e1", dates(1));
        Edge e5 = new Edge(et2, "y1", "e2", dates(1));
        Edge e6 = new Edge(et2, "y2", "e1", dates(8));
        Edge e7 = new Edge(et2, "y3", "e3", dates(8));

        ListF<Edge> input = Cf.list(e1, e2, e3, e4, e5, e6, e7);

        List<EdgeTypeActivityStats> mapperResult = YtTestHelper.testMapper(mapper, input).getAllRecs();

        LocalYield<EdgeTypeActivityStats> yield = new LocalYield<>();
        reducer.reduce(mapperResult.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        assertEquals(2, yield.getAllRecs().size());

        Map<EdgeType, EdgeTypeActivityStats> result = yield.getAllRecs()
                .stream().collect(Collectors.toMap(EdgeTypeActivityStats::getEdgeType, s -> s));

        assertEquals(Cf.map(
                1, 1L,
                2, 1L,
                3, 1L
        ), result.get(et1).getDatesCountHist());

        assertEquals(Cf.map(
                1, 2L,
                8, 2L
        ), result.get(et2).getDatesCountHist());


    }

    @Test
    public void testLifetimeStats() throws Exception {
        CalculateEdgeTypesStats.Mapper mapper = new CalculateEdgeTypesStats.Mapper();
        CalculateEdgeTypesStats.Reducer reducer = new CalculateEdgeTypesStats.Reducer();

        EdgeType et1 = new EdgeType(EIdType.YANDEXUID, EIdType.EMAIL, ESourceType.WEBVISOR, ELogSourceType.WEBVISOR_LOG);
        EdgeType et2 = new EdgeType(EIdType.YANDEXUID, EIdType.PHONE, ESourceType.WEBVISOR, ELogSourceType.WEBVISOR_LOG);

        Edge e14days = new Edge(et1, "y1", "e1", Cf.list("2017-10-01", "2017-10-14"));
        Edge e21days = new Edge(et1, "y1", "e2", Cf.list("2017-10-01", "2017-10-21"));
        Edge e28days = new Edge(et1, "y2", "e1", Cf.list("2017-10-01", "2017-10-10", "2017-10-28"));

        Edge e1day1 = new Edge(et2, "y1", "e1", Cf.list("2017-10-01"));
        Edge e1day2 = new Edge(et2, "y1", "e2", Cf.list("2017-10-04"));
        Edge e8days1 = new Edge(et2, "y2", "e1", dates(8));
        Edge e8days2 = new Edge(et2, "y3", "e3", Cf.list("2017-10-05", "2017-10-12"));

        LocalYield<EdgeTypeActivityStats> yield = new LocalYield<>();

        ListF<Edge> input = Cf.list(
                e14days, e21days, e28days,
                e1day1, e1day2, e8days1, e8days2);

        List<EdgeTypeActivityStats> mapperResult = YtTestHelper.testMapper(mapper, input).getAllRecs();

        reducer.reduce(mapperResult.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        assertEquals(2, yield.getAllRecs().size());

        Map<EdgeType, EdgeTypeActivityStats> result = yield.getAllRecs()
                .stream().collect(Collectors.toMap(EdgeTypeActivityStats::getEdgeType, s -> s));

        assertEquals(Cf.map(
                14, 1L,
                21, 1L,
                28, 1L
        ), result.get(et1).getLifetimeDaysHist());

        assertEquals(Cf.map(
                1, 2L,
                8, 2L
        ), result.get(et2).getLifetimeDaysHist());


    }


}
