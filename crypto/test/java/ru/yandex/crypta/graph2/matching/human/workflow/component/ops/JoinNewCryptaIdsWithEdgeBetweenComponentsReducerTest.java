package ru.yandex.crypta.graph2.matching.human.workflow.component.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonCachedSerializerSupport;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.workflow.RandomIdGenerator;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;


public class JoinNewCryptaIdsWithEdgeBetweenComponentsReducerTest {
    @Test
    public void reduceTest() {
        RandomIdGenerator generator = new RandomIdGenerator();

        JoinNewCryptaIdsWithEdgeBetweenComponentsReducer reducer =
                new JoinNewCryptaIdsWithEdgeBetweenComponentsReducer();

        MergeKey mk = MergeKey.betweenComponents(generator.randomCryptaId(), generator.randomCryptaId());
        MergeKey.CryptaIds cryptaIds = MergeKey.CryptaIds.fromMergeKey(mk);

        String cLeft = cryptaIds.leftCryptaId;
        String cRight = cryptaIds.rightCryptaId;
        String cNewLeft = generator.randomCryptaId();
        String cNewRight = generator.randomCryptaId();


        MergeKeyWithNewCryptaIds mkWithNewLeftCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cLeft)
                        .updateFromTo(cLeft, cNewLeft);
        MergeKeyWithNewCryptaIds mkWithNewRightCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cRight)
                        .updateFromTo(cRight, cNewRight);

        Edge edge = new Edge(
                generator.randomStringYuid(), EIdType.YANDEXUID,
                generator.randomStringIdfa(), EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, Cf.list()
        );

        EdgeBetweenWithNewCryptaIds edgeLeft = new EdgeBetweenWithNewCryptaIds(edge, mkWithNewLeftCryptaId);
        EdgeBetweenWithNewCryptaIds edgeRight = new EdgeBetweenWithNewCryptaIds(edge, mkWithNewRightCryptaId);

        ListF<EdgeBetweenWithNewCryptaIds> edges = Cf.list(edgeLeft, edgeRight);
        YsonCachedSerializerSupport serializer = new YsonCachedSerializerSupport();
        ListF<YTreeMapNode> inRecs = edges.map(serializer::serialize);

        LocalYield<YTreeMapNode> result = YtTestHelper.testReducer(reducer, inRecs);

        ListF<EdgeBetweenComponents> edgesOut = serializer.parse(result.getRecsByIndex(0),
                EdgeBetweenComponents.class).toList();

        assertEquals(1, edgesOut.size());
        EdgeBetweenComponents edgeOut = edgesOut.first();
        assertEquals(cNewLeft, edgeOut.getLeftCryptaId());
        assertEquals(cNewRight, edgeOut.getRightCryptaId());
        assertEquals(MergeKey.betweenComponents(cNewLeft, cNewRight), edgeOut.getMergeKey());
        assertEquals(edge, edgeOut.getEdge());
    }
}
