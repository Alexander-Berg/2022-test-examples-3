package ru.yandex.crypta.graph2.model.matching.graph.cryptaid;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.DefaultEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CryptaIdDispenserWithPuidPriorityTest {


    private static final String EMPTY_CC = "empty_cc";

    private static final Vertex PUID_VERTEX = new Vertex("a_puid", EIdType.PUID);
    private static final Vertex GOOD_LOGIN_VERTEX = new Vertex("login", EIdType.PUID);
    private static final Vertex BAD_LOGIN_VERTEX = new Vertex("phne-login", EIdType.PUID);

    private static final Yandexuid NOT_ACTIVE_YUID = new Yandexuid(new Vertex("b_not_active_yuid", EIdType.YANDEXUID),
            EMPTY_CC, "", Option.empty(), "", false
    );
    private static final Yandexuid ACTIVE_YUID = new Yandexuid(new Vertex("b_active_yuid", EIdType.YANDEXUID),
            EMPTY_CC, "", Option.empty(), "", true
    );

    private static final VertexPropertiesCollector VERTEX_PROPERTIES = new VertexPropertiesCollector(
            Cf.map(
                    NOT_ACTIVE_YUID.getVertex(), NOT_ACTIVE_YUID,
                    ACTIVE_YUID.getVertex(), ACTIVE_YUID
            ),
            Cf.map(), Cf.map(), Cf.map(), Cf.map()
    );

    private static final GraphInfo GRAPH_INFO = new GraphInfo(Cf.map(), Cf.map(), VERTEX_PROPERTIES, Cf.list());

    @Test
    public void testChooseSinglePuidBasedCryptaId() {
        Vertex activeYuidVertex = ACTIVE_YUID.getVertex();
        testChooseSinglePuidBasedCryptaId(activeYuidVertex, GOOD_LOGIN_VERTEX, PUID_VERTEX); // the only positive case
        testChooseSinglePuidBasedCryptaId(activeYuidVertex, BAD_LOGIN_VERTEX, activeYuidVertex);

        Vertex notActiveYuidVertex = NOT_ACTIVE_YUID.getVertex();
        testChooseSinglePuidBasedCryptaId(notActiveYuidVertex, GOOD_LOGIN_VERTEX, notActiveYuidVertex);
        testChooseSinglePuidBasedCryptaId(notActiveYuidVertex, BAD_LOGIN_VERTEX, notActiveYuidVertex);
    }


    private void testChooseSinglePuidBasedCryptaId(Vertex yuid, Vertex login, Vertex expectedCCVertex) {

        Component component = getPuidYuidComponent(yuid, login, PUID_VERTEX);

        CryptaIdDispenserWithPuidPriority dispenser = new CryptaIdDispenserWithPuidPriority(
                new DefaultEdgeInfoProvider(),
                (CryptaIdDispenser) (component1, graphInfo) -> Option.of(ComponentCenter.fromVertex(yuid))
        );

        Option<ComponentCenter> cryptaId = dispenser.getCryptaId(component, GRAPH_INFO);
        assertTrue(cryptaId.isPresent());

        assertEquals(expectedCCVertex, cryptaId.get().convertToVertex());

    }

    @Test
    public void testChooseBestPuidCryptaId() {
        Vertex puid1 = new Vertex("puid1", EIdType.PUID);
        Vertex login1 = new Vertex("login1", EIdType.LOGIN);

        Vertex bestPuid = new Vertex("puid2", EIdType.PUID);
        Vertex login2 = new Vertex("login2", EIdType.LOGIN);
        // extra edge makes puid best
        Vertex x = new Vertex("x", EIdType.EMAIL);
        Edge extraEdge = new Edge(bestPuid.getId(), bestPuid.getIdType(), x.getId(), x.getIdType(),
                ESourceType.LOGIN_TO_EMAIL, ELogSourceType.SOUP_PREPROCESSING, Cf.list(), Option.of(1.0),
                Option.of(1.0));

        Vertex puid3 = new Vertex("puid3", EIdType.PUID);
        Vertex login3 = new Vertex("login3", EIdType.LOGIN);

        Component c1 = getPuidYuidComponent(ACTIVE_YUID.getVertex(), login1, puid1);
        Component c2 = getPuidYuidComponent(ACTIVE_YUID.getVertex(), login2, bestPuid);
        Component c3 = getPuidYuidComponent(ACTIVE_YUID.getVertex(), login3, puid3);

        Component component = c1.merge(c2, Cf.list()).merge(c3, Cf.list());
        component.addInnerEdge(extraEdge);

        CryptaIdDispenserWithPuidPriority dispenser = new CryptaIdDispenserWithPuidPriority(
                new DefaultEdgeInfoProvider(),
                (CryptaIdDispenser) (component1, graphInfo) -> Option.of(ComponentCenter.fromVertex(bestPuid))
        );

        Option<ComponentCenter> cryptaId = dispenser.getCryptaId(component, GRAPH_INFO);
        assertTrue(cryptaId.isPresent());

        assertEquals(bestPuid, cryptaId.get().convertToVertex());
    }

    private Component getPuidYuidComponent(Vertex yuid, Vertex login, Vertex puid) {
        Edge puidYuidEdge = new Edge(yuid.getId(), yuid.getIdType(),
                puid.getId(), puid.getIdType(),
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, Cf.list(), Option.of(1.0), Option.of(1.0));

        Edge puidLoginEdge = new Edge(CryptaIdDispenserWithPuidPriority.PUID_LOGIN_EDGE_TYPE, puid.getId(),
                login.getId(), Cf.list(),
                Option.of(1.0), Option.of(1.0));

        Component component = new Component();
        component.addInnerEdges(Cf.list(
                puidYuidEdge,
                puidLoginEdge
        ));
        return component;
    }


}
