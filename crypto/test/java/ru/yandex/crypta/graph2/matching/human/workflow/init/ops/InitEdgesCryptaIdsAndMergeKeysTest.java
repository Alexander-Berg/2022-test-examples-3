package ru.yandex.crypta.graph2.matching.human.workflow.init.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.proto.InitEdgesCryptaIdsAndMergeNeighboursOutRec;
import ru.yandex.crypta.graph2.matching.human.workflow.RandomIdGenerator;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyType;
import ru.yandex.crypta.graph2.model.matching.proto.CryptaIdEdgeMessage;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeProtoHelper;
import ru.yandex.crypta.graph2.model.soup.edge.weight.DefaultEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.proto.Edge;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.crypta.graph2.matching.human.helper.EdgeMessageTestHelper.edgeMessageToSecondId;

public class InitEdgesCryptaIdsAndMergeKeysTest {


    @Test
    public void edgeReceivesSingleCryptaId() throws Exception {
        RandomIdGenerator generator = new RandomIdGenerator();

        String singleCryptaId = generator.randomCryptaId();

        CryptaIdEdgeMessage emDirect = edgeMessageToSecondId(
                generator.randomStringEmail(), EIdType.EMAIL,
                generator.randomStringYuid(), EIdType.YANDEXUID,
                ESourceType.APP_METRICA, ELogSourceType.ACCESS_LOG
        ).toBuilder().setDatesWeight(3.0).setCryptaId(singleCryptaId).build();

        CryptaIdEdgeMessage emReversed = EdgeProtoHelper
                .reverse(emDirect)
                .setCryptaId(singleCryptaId)
                .build();

        for (MergeKeyType mergeType : Cf.list(
                MergeKeyType.BY_EDGE,
                MergeKeyType.BY_COMPONENT,
                MergeKeyType.BY_EDGES_CUT)) {

            LocalYield<InitEdgesCryptaIdsAndMergeNeighboursOutRec> result = doReduce(emDirect, emReversed, mergeType);

            ListF<EdgeInComponent> outEdges =
                    result.getRecsByIndex(0).map(InitEdgesCryptaIdsAndMergeNeighboursOutRec::getEdgeInComponentRec1);

            assertEquals(1, outEdges.size());
            assertEquals(singleCryptaId, outEdges.first().getCryptaId());
            assertEquals(3.0, outEdges.first().getDatesWeight(), Double.MIN_VALUE);
        }

    }

    @Test
    public void edgeReceivesMultiCryptaId() throws Exception {
        RandomIdGenerator generator = new RandomIdGenerator(1234);

        MergeKey mergeKey = MergeKey.betweenComponents(generator.randomCryptaId(), generator.randomCryptaId());
        MergeKey.CryptaIds cryptaIds = MergeKey.CryptaIds.fromMergeKey(mergeKey);
        String cryptaId1 = cryptaIds.leftCryptaId;
        String cryptaId2 = cryptaIds.rightCryptaId;

        CryptaIdEdgeMessage emDirect = edgeMessageToSecondId(
                generator.randomStringEmail(), EIdType.EMAIL,
                generator.randomStringYuid(), EIdType.YANDEXUID,
                ESourceType.APP_METRICA, ELogSourceType.ACCESS_LOG
        ).toBuilder().setCryptaId(cryptaId1).setDatesWeight(3.0).build();

        CryptaIdEdgeMessage emReversed = EdgeProtoHelper
                .reverse(emDirect)
                .setCryptaId(cryptaId2)
                .setDatesWeight(3.0)
                .build();

        for (MergeKeyType mergeType : Cf.list(
                MergeKeyType.BY_EDGE,
                MergeKeyType.BY_COMPONENT,
                MergeKeyType.BY_EDGES_CUT)) {

            LocalYield<InitEdgesCryptaIdsAndMergeNeighboursOutRec> result = doReduce(emDirect, emReversed, mergeType);

            ListF<EdgeBetweenComponents> crossComponentEdge =
                    result.getRecsByIndex(1).map(InitEdgesCryptaIdsAndMergeNeighboursOutRec::getEdgeBetweenComponentsRec2);
            if (mergeType.equals(MergeKeyType.BY_COMPONENT)) {
                // both components, acting as merge key, should know about edges between
                assertEquals(2, crossComponentEdge.size());
            } else {
                assertEquals(1, crossComponentEdge.size());
            }

            EdgeBetweenComponents edgeBetweenComponents = crossComponentEdge.first();
            assertEquals(cryptaId1, edgeBetweenComponents.getLeftCryptaId());
            assertEquals(cryptaId2, edgeBetweenComponents.getRightCryptaId());
            assertEquals(3.0, edgeBetweenComponents.getDatesWeight(), Double.MIN_VALUE);
        }


    }

    @Test
    public void overlimitEdgeIsThrown() throws Exception {
        RandomIdGenerator generator = new RandomIdGenerator();

        String cryptaId1 = generator.randomCryptaId();

        CryptaIdEdgeMessage emDirect = edgeMessageToSecondId(
                generator.randomStringEmail(), EIdType.EMAIL,
                generator.randomStringYuid(), EIdType.YANDEXUID,
                ESourceType.APP_METRICA, ELogSourceType.ACCESS_LOG
        ).toBuilder().setCryptaId(cryptaId1).build();

        for (MergeKeyType mergeType : Cf.list(
                MergeKeyType.BY_EDGE,
                MergeKeyType.BY_COMPONENT,
                MergeKeyType.BY_EDGES_CUT)) {
            // second edge message is missing because of overlimit

            LocalYield<InitEdgesCryptaIdsAndMergeNeighboursOutRec> result = doReduce(emDirect, null, mergeType);

            ListF<EdgeBetweenComponents> crossComponentEdge =
                    result.getRecsByIndex(1).map(InitEdgesCryptaIdsAndMergeNeighboursOutRec::getEdgeBetweenComponentsRec2);
            ListF<Edge> overlimitEdges =
                    result.getRecsByIndex(3).map(InitEdgesCryptaIdsAndMergeNeighboursOutRec::getOomRec4);

            assertEquals(0, crossComponentEdge.size());
            assertEquals(1, overlimitEdges.size());
        }

    }

    private LocalYield<InitEdgesCryptaIdsAndMergeNeighboursOutRec> doReduce(CryptaIdEdgeMessage emDirect,
                                                                            CryptaIdEdgeMessage emReversed,
                                                                            MergeKeyType mergeType) {

        InitEdgesCryptaIdsAndMergeNeighbours reducer = new InitEdgesCryptaIdsAndMergeNeighbours(
                new DefaultEdgeInfoProvider(), mergeType
        );

        ListF<CryptaIdEdgeMessage> inEdgeMessages = Cf.list(emDirect).plus(Option.ofNullable(emReversed));


        return YtTestHelper.testOneOfProtoReducer(reducer, inEdgeMessages, null);
    }


}
