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

public class LoginsLcsExtractorTest {
    private GraphInfo graphInfo = mock(GraphInfo.class);

    @Test
    public void testSingleLogin() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component(new Vertex("l1", EIdType.LOGIN));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testSinglePuidWithLogin() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "l1"));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testTwoLoginsSmall() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "l1"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "l2"));

        assertEquals(1, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testTwoLoginsLong() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "ll1"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "ll2"));

        assertEquals(2, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testSingleSyntheticPuidLogin() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "phne-login"));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testComplexCase() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Edge externalEdge = new Edge("ext-puid", EIdType.PUID, "d1", EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list()
        );

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "phne-login"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid2", "kl2"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid3", "lkl3"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid4", "lkl4"));
        cp.addInnerEdge(externalEdge);

        assertEquals(2, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testComplexCase2() throws Exception {
        LoginsLcsExtractor extractor = new LoginsLcsExtractor();

        Component cp = new Component();
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "ya-"));
        cp.addInnerEdge(new Edge(LoginsCountExtractor.PUID_LOGIN_EDGE_TYPE, "puid1", "-ya"));

        assertEquals(2, (int) extractor.apply(cp, graphInfo));
    }
}
