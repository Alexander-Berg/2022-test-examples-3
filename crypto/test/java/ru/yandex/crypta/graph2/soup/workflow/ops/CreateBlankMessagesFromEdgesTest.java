package ru.yandex.crypta.graph2.soup.workflow.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.matching.proto.CryptaIdEdgeMessage;
import ru.yandex.crypta.graph2.model.soup.proto.Edge;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CreateBlankMessagesFromEdgesTest {

    private static final String MM_DEVICE_ID_TYPE = Soup.CONFIG.name(EIdType.MM_DEVICE_ID);
    private static final String IDFA_ID_TYPE = Soup.CONFIG.name(EIdType.IDFA);
    private static final String APP_METRICA_SOURCE_TYPE = Soup.CONFIG.name(ESourceType.APP_METRICA);
    private static final String APP_METRICA_LOG_SOURCE = Soup.CONFIG.name(ELogSourceType.METRIKA_MOBILE_LOG);

    private Edge toEdge(CryptaIdEdgeMessage message) {
        return Edge.newBuilder()
                .setId1(message.getId1())
                .setId1Type(message.getId1Type())
                .setId2(message.getId2())
                .setId2Type(message.getId2Type())
                .setSourceType(message.getSourceType())
                .setLogSource(message.getLogSource())
                .build();
    }

    @Test
    public void messagesTest() {
        // must be real edge type
        Edge edge = Edge.newBuilder()
                .setId1("id1")
                .setId1Type(IDFA_ID_TYPE)
                .setId2("id2")
                .setId2Type(MM_DEVICE_ID_TYPE)
                .setSourceType(APP_METRICA_SOURCE_TYPE)
                .setLogSource(APP_METRICA_LOG_SOURCE)
                .build();

        LocalYield<CryptaIdEdgeMessage> yield = new LocalYield<>();
        new CreateBlankMessagesFromEdges().map(edge, yield, new StatisticsSlf4jLoggingImpl());

        ListF<CryptaIdEdgeMessage> allRecs = yield.getAllRecs();

        assertEquals(2, allRecs.size());

        CryptaIdEdgeMessage outDirectMessage = allRecs.get(0);
        CryptaIdEdgeMessage outReverseMessage = allRecs.get(1);

        assertFalse(outDirectMessage.getReversed());
        assertEquals(edge.getId2(), outDirectMessage.getId());
        assertEquals(edge.getId2Type(), outDirectMessage.getIdType());

        assertEquals(toEdge(outDirectMessage), edge);

        assertTrue(outReverseMessage.getReversed());
        assertEquals(edge.getId1(), outReverseMessage.getId());
        assertEquals(edge.getId1Type(), outReverseMessage.getIdType());
        assertEquals(toEdge(outReverseMessage), edge);

        assertNotEquals(outDirectMessage, outReverseMessage);

        assertEquals(outDirectMessage.getCryptaId(), outReverseMessage.getCryptaId());
    }
}
