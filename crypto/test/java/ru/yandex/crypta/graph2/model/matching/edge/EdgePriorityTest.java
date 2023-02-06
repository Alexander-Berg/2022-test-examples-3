package ru.yandex.crypta.graph2.model.matching.edge;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.model.matching.helper.DatesActivityEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.MOBILE_REDIRECT_BIND_ID_LOG;
import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.SOUP_PREPROCESSING;
import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.WEBVISOR_LOG;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.APP_METRICA_SDK;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.MD5_HASH;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.WEBVISOR;

public class EdgePriorityTest {
    @Test
    public void artificialAndTrustedEdgesComesFirst() throws Exception {
        Edge usualEdge3 = new Edge("v1", EIdType.YANDEXUID, "v2", EIdType.EMAIL,
                WEBVISOR, WEBVISOR_LOG,
                Cf.list("d1", "d2", "d3"));

        Edge usualEdge4 = new Edge("v3", EIdType.YANDEXUID, "v4", EIdType.EMAIL,
                WEBVISOR, WEBVISOR_LOG,
                Cf.list("d1", "d2", "d3", "d4"));

        Edge artificialEdge0 = new Edge("v1", EIdType.PHONE, "v2", EIdType.PHONE_MD5,
                MD5_HASH, SOUP_PREPROCESSING,
                Cf.list("d1"));

        Edge trustedEdge1 = new Edge("v1", EIdType.YANDEXUID, "v2", EIdType.UUID,
                APP_METRICA_SDK, MOBILE_REDIRECT_BIND_ID_LOG,
                Cf.list("d1"));


        EdgePriority ep = new EdgePriority(new DatesActivityEdgeInfoProvider());
        ListF<Edge> sorted = ep.sortEdgesByActivityDesc(Cf.set(
                usualEdge3, usualEdge4, artificialEdge0, trustedEdge1
        ));

        assertEquals(
                Cf.list(artificialEdge0, trustedEdge1, usualEdge4, usualEdge3),
                sorted);
    }

}
