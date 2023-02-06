package ru.yandex.crypta.graph2.matching.human.workflow.init.ops;

import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.helper.EdgeMessageTestHelper;
import ru.yandex.crypta.graph2.matching.human.proto.InitVertexCryptaIdsAndMergeNeighboursInRec;
import ru.yandex.crypta.graph2.matching.human.proto.InitVertexCryptaIdsAndMergeNeighboursOutRec;
import ru.yandex.crypta.graph2.model.id.proto.IdInfo;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.proto.CryptaIdEdgeMessage;
import ru.yandex.crypta.graph2.model.matching.proto.VertexInComponent;
import ru.yandex.crypta.graph2.model.matching.proto.VertexOverlimit;
import ru.yandex.crypta.graph2.model.soup.props.CommonShared;
import ru.yandex.crypta.graph2.model.soup.props.VertexExactSocdem;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.crypta.graph2.matching.human.helper.EdgeMessageTestHelper.edgeMessageToSecondId;
import static ru.yandex.crypta.graph2.matching.human.workflow.init.ops.InitVertexCryptaIdsAndMergeNeighbours.FilterSoupEdges.PROD;

public class InitVertexCryptaIdsAndMergeKeysTest {

    @Test
    public void currentVertexIsCcAtBootstrap() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(200);

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                edgeMessageToSecondId("e1", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("e2", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> result = YtTestHelper.testOneOfProtoReducer(reducer,
                emRecs,
                null);
        ListF<CryptaIdEdgeMessage> outEM = result.getRecsByIndex(2)
                .map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);

        assertEquals(3, outEM.size());

        ComponentCenter expectedCc = new ComponentCenter("y1", EIdType.YANDEXUID);
        String expectedCryptaId = expectedCc.getCryptaId();

        Assert.forAll(outEM, em -> em.getCryptaId() != null);
        Assert.forAll(outEM, em -> em.getCryptaId().equals(expectedCryptaId));

        ListF<VertexInComponent> outVertices =
                result.getRecsByIndex(0).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdVertexRec1);
        assertEquals(1, outVertices.size());
        VertexInComponent vertex = outVertices.first();

        assertNotNull(vertex.getCryptaId());
        assertEquals(expectedCryptaId, vertex.getCryptaId());

    }

    @Test
    public void previousIterationComponentAsCc() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(200);

        // previous CC out from nowhere was really strong!
        ComponentCenter prevIterCc = new ComponentCenter("cc1", EIdType.YANDEXUID);
        String prevIterCryptaId = prevIterCc.getCryptaId();

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> verticesRecs = Cf.list(
                toVertex("y1", EIdType.YANDEXUID, prevIterCc)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setPrevCryptaIdVertexRec1(r).build());

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                edgeMessageToSecondId("e1", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("e2", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> result = YtTestHelper.testOneOfProtoReducer(reducer,
                verticesRecs.plus(emRecs), null);
        ListF<CryptaIdEdgeMessage> outEM =
                result.getRecsByIndex(2).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);

        assertEquals(3, outEM.size());

        Assert.forAll(outEM, em -> em.getCryptaId() != null);
        Assert.forAll(outEM, em -> em.getCryptaId().equals(prevIterCryptaId));

        ListF<VertexInComponent> outVertices =
                result.getRecsByIndex(0).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdVertexRec1);
        assertEquals(1, outVertices.size());
        VertexInComponent vertex = outVertices.first();

        assertNotNull(vertex.getCryptaId());
        assertEquals(vertex.getCryptaId(), prevIterCryptaId);

    }

    private VertexInComponent toVertex(String id, EIdType idType, ComponentCenter prevIterCc) {
        return VertexInComponent.newBuilder()
                .setId(id)
                .setIdType(Soup.CONFIG.name(idType))
                .setCryptaId(prevIterCc.getCryptaId())
                .build();
    }

    @Test
    public void removeSharedVertices() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(200);

        // previous CC out from nowhere was really strong!
        ComponentCenter prevIterCc = new ComponentCenter("cc1", EIdType.YANDEXUID);
        ComponentCenter prevIterCc2 = new ComponentCenter("cc2", EIdType.YANDEXUID);
        String prevIterCryptaId2 = prevIterCc2.getCryptaId();

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> verticesRecs = Cf.list(
                toVertex("y1", EIdType.YANDEXUID, prevIterCc),
                toVertex("y2", EIdType.YANDEXUID, prevIterCc2)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setPrevCryptaIdVertexRec1(r).build());


        IdInfo sharedId = IdInfo.newBuilder()
                .setId("y1")
                .setIdType(Soup.CONFIG.name(EIdType.YANDEXUID))
                .setSource(CommonShared.COMMON_SHARED_SOURCE)
                .build();
        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> sharedRecs = Cf.list(
                sharedId
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setIdInfoRec2(r).build());

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                edgeMessageToSecondId("e1", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("e2", EIdType.EMAIL, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID),
                edgeMessageToSecondId("e3", EIdType.EMAIL, "y2", EIdType.YANDEXUID),
                edgeMessageToSecondId("e4", EIdType.EMAIL, "y2", EIdType.YANDEXUID),
                edgeMessageToSecondId("p2", EIdType.PHONE, "y2", EIdType.YANDEXUID)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> result = YtTestHelper.testOneOfProtoReducer(reducer,
                verticesRecs.plus(sharedRecs).plus(emRecs), null);
        ListF<InitVertexCryptaIdsAndMergeNeighboursOutRec> sharedEdges = result.getRecsByIndex(6);
        assertEquals(3, sharedEdges.size());

        assertEquals("y1", sharedEdges.first().getSharedEdgeRec7().getId());
        assertEquals(Soup.CONFIG.name(EIdType.YANDEXUID),
                sharedEdges.first().getSharedEdgeRec7().getIdType());

        ListF<CryptaIdEdgeMessage> outEM =
                result.getRecsByIndex(2).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);
        assertEquals(3, outEM.size());

        Assert.forAll(outEM, em -> em.getCryptaId() != null);
        Assert.forAll(outEM, em -> em.getCryptaId().equals(prevIterCryptaId2));

        ListF<VertexInComponent> outVertices =
                result.getRecsByIndex(0).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdVertexRec1);
        assertEquals(1, outVertices.size());
        VertexInComponent vertex = outVertices.first();

        assertNotNull(vertex.getCryptaId());
        assertEquals(vertex.getCryptaId(), prevIterCryptaId2);
    }

    @Test
    public void throwOverlimitEdges() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(1);


        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                // 1
                edgeMessageToSecondId("e1", EIdType.EMAIL, "y1", EIdType.YANDEXUID, ESourceType.APP_METRICA,
                        ELogSourceType.ACCESS_LOG),
                edgeMessageToSecondId("e2", EIdType.EMAIL, "y1", EIdType.YANDEXUID, ESourceType.APP_METRICA,
                        ELogSourceType.ACCESS_LOG),
                // 2
                edgeMessageToSecondId("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, ESourceType.ADV_BLOCK,
                        ELogSourceType.ACCESS_LOG),
                edgeMessageToSecondId("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, ESourceType.ADV_BLOCK,
                        ELogSourceType.ACCESS_LOG),
                // 3
                edgeMessageToSecondId("e3", EIdType.EMAIL, "y2", EIdType.YANDEXUID, ESourceType.ACCOUNT_MANAGER,
                        ELogSourceType.ACCESS_LOG),
                edgeMessageToSecondId("e4", EIdType.EMAIL, "y2", EIdType.YANDEXUID, ESourceType.ACCOUNT_MANAGER,
                        ELogSourceType.ACCESS_LOG),
                edgeMessageToSecondId("e5", EIdType.EMAIL, "y2", EIdType.YANDEXUID, ESourceType.ACCOUNT_MANAGER,
                        ELogSourceType.ACCESS_LOG),
                // 4: same source, another id type
                edgeMessageToSecondId("p2", EIdType.PHONE, "y2", EIdType.YANDEXUID, ESourceType.ACCOUNT_MANAGER,
                        ELogSourceType.ACCESS_LOG),
                // 5
                edgeMessageToSecondId("p2", EIdType.PHONE, "y3", EIdType.YANDEXUID, ESourceType.ACCOUNT_MANAGER,
                        ELogSourceType.ACCESS_LOG)
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        var result = YtTestHelper.testOneOfProtoReducer(reducer, emRecs, null);

        // takes single rec from each group
        ListF<CryptaIdEdgeMessage> outEM =
                result.getRecsByIndex(2).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);
        assertEquals(5, outEM.size());

        // number of groups with overlimit
        ListF<VertexOverlimit> overlimitVertices =
                result.getRecsByIndex(4).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getOverlimitVertexRec5);
        assertEquals(3, overlimitVertices.size());

    }

    @Test
    public void throwOverlimitByWeight() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(3);


        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                edgeMessageToSecondId("e1", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 10, 1),
                edgeMessageToSecondId("e2", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 10, 2),
                edgeMessageToSecondId("e3", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 11, 1),
                edgeMessageToSecondId("e4", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 11, 1),
                edgeMessageToSecondId("e5", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 12, 0),
                edgeMessageToSecondId("e6", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 11, 2),
                edgeMessageToSecondId("e7", EIdType.EMAIL, "y1", EIdType.YANDEXUID, 9, 99)

        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> result = YtTestHelper.testOneOfProtoReducer(reducer,
                emRecs,
                null);

        // takes single rec from each group
        ListF<CryptaIdEdgeMessage> outEM =
                result.getRecsByIndex(2).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);
        assertEquals(3, outEM.size());

        // large survival weights go first
        assertEquals(outEM.get(0).getSurvivalWeight(), 12.0, Double.MIN_VALUE);
        assertEquals(outEM.get(0).getDatesWeight(), 0, Double.MIN_VALUE);

        assertEquals(outEM.get(1).getSurvivalWeight(), 11.0, Double.MIN_VALUE);
        assertEquals(outEM.get(1).getDatesWeight(), 2, Double.MIN_VALUE);

        assertEquals(outEM.get(2).getSurvivalWeight(), 11.0, Double.MIN_VALUE);
        assertEquals(outEM.get(2).getDatesWeight(), 1, Double.MIN_VALUE);


    }

    @Test
    public void edgeReceivesSingleCryptaId() throws Exception {
        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(200);

        String id = "y1";
        String idType = Soup.CONFIG.name(EIdType.YANDEXUID);

        String cryptaId = "c1";
        VertexInComponent vertexCryptaId = VertexInComponent.newBuilder()
                .setId(id)
                .setIdType(idType)
                .setCryptaId(cryptaId)
                .build();

        IdInfo idInfo1 = IdInfo.newBuilder()
                .setId(id)
                .setIdType(idType)
                .setUaProfile("ua123")
                .setSource(Yandexuid.YUID_WITH_ALL_SOURCE)
                .setIsActive(false)
                .setMainRegion(1)
                .build();

        IdInfo idInfo2 = IdInfo.newBuilder()
                .setId(id)
                .setIdType(idType)
                .setSource(VertexExactSocdem.EXACT_SOCDEM_SOURCE)
                .setGender("male")
                .build();

        CryptaIdEdgeMessage edgeMessage = edgeMessageToSecondId(
                "e1", EIdType.EMAIL,
                "y1", EIdType.YANDEXUID,
                10, 1);


        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> recs1 = Cf.list(vertexCryptaId).map(v ->
                InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setPrevCryptaIdVertexRec1(v).build()
        );

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> recs2 = Cf.list(idInfo1, idInfo2).map(v ->
                InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setIdInfoRec2(v).build()
        );

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> recs3 = Cf.list(edgeMessage).map(em ->
                InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(em).build()
        );


        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> outRecs = YtTestHelper.testOneOfProtoReducer(
                reducer, recs1.plus(recs2).plus(recs3), null
        );

        ListF<IdInfo> idInfoWithCryptaId =
                outRecs.getRecsByIndex(1).map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getIdInfoRec2);

        assertEquals(2, idInfoWithCryptaId.size());
        assertEquals(cryptaId, idInfoWithCryptaId.get(0).getCryptaId());
        assertEquals(cryptaId, idInfoWithCryptaId.get(1).getCryptaId());


    }

    @Test
    public void filterEdgeTypes() throws Exception {

        List<TEdgeType> filter = List.of(TEdgeType.newBuilder()
                .setId1Type(EIdType.GAID)
                .setId2Type(EIdType.MM_DEVICE_ID)
                .setSourceType(ESourceType.APP_METRICA)
                .setLogSource(ELogSourceType.METRIKA_MOBILE_LOG)
                .build()
        );

        InitVertexCryptaIdsAndMergeNeighbours reducer = new InitVertexCryptaIdsAndMergeNeighbours(200, filter, PROD);

        CryptaIdEdgeMessage goodEdge = EdgeMessageTestHelper.build(
                "id1", EIdType.IDFA,
                "id2", EIdType.MM_DEVICE_ID,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG
        );

        CryptaIdEdgeMessage edgeToFilter = EdgeMessageTestHelper.build(
                "id1", EIdType.GAID,
                "id2", EIdType.MM_DEVICE_ID,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG
        );

        CryptaIdEdgeMessage edgeNotInConfig = EdgeMessageTestHelper.build(
                "id1", EIdType.MM_DEVICE_ID,
                "id2", EIdType.MM_DEVICE_ID,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG
        );

        ListF<InitVertexCryptaIdsAndMergeNeighboursInRec> emRecs = Cf.list(
                goodEdge, edgeToFilter, edgeNotInConfig
        ).map(r -> InitVertexCryptaIdsAndMergeNeighboursInRec.newBuilder().setEdgeMessageRec3(r).build());

        LocalYield<InitVertexCryptaIdsAndMergeNeighboursOutRec> result = YtTestHelper.testOneOfProtoReducer(reducer,
                emRecs,
                null);
        ListF<CryptaIdEdgeMessage> outEM = result.getRecsByIndex(2)
                .map(InitVertexCryptaIdsAndMergeNeighboursOutRec::getCryptaIdMessageRec3);

        assertEquals(1, outEM.size());
    }
}
