package ru.yandex.crypta.graph2.model.matching.component.score.extractors;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class LoginsCountExtractorTest {

    private GraphInfo graphInfo = mock(GraphInfo.class);

    @Test
    public void testSingleLogin() throws Exception {
        LoginsCountExtractor extractor = new LoginsCountExtractor(false);

        Component cp = new Component(new Vertex("l1", EIdType.LOGIN));

        assertEquals(1, (int) extractor.apply(cp, graphInfo));

    }

    @Test
    public void testSinglePuid() throws Exception {
        LoginsCountExtractor extractor = new LoginsCountExtractor(false);

        Component cp = new Component(new Vertex("p1", EIdType.PUID));

        assertEquals(1, (int) extractor.apply(cp, graphInfo));

    }

    @Test
    public void testSinglePuidWithLogin() throws Exception {
        LoginsCountExtractor extractor = new LoginsCountExtractor(false);

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "l1"));

        assertEquals(1, (int) extractor.apply(cp, graphInfo));

    }

    @Test
    public void testSingleSyntheticPuidLogin() throws Exception {
        LoginsCountExtractor extractor = new LoginsCountExtractor(false);

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "phne-login"));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));

    }

    @Test
    public void testComplexCase() throws Exception {
        LoginsCountExtractor extractor = new LoginsCountExtractor(false);

        Edge externalEdge = new Edge("ext-puid", EIdType.PUID, "d1", EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list()
        );

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "phne-login"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid2", "l2"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid3", "l3"));
        cp.addInnerEdge(externalEdge);

        assertEquals(3, (int) extractor.apply(cp, graphInfo));

    }


}
