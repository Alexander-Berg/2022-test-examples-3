package ru.yandex.crypta.graph2.matching.human.workflow.merge.ops;

import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.proto.UseMergeStrategyReducerInRec;
import ru.yandex.crypta.graph2.model.id.proto.IdInfo;
import ru.yandex.crypta.graph2.model.matching.component.score.SimpleStupidScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.component.similarity.FakeComponentsSimilarityStrategy;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyType;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.algo.merge.MergeByScoreAndSimilarityAlgorithm;
import ru.yandex.crypta.graph2.model.matching.proto.ComponentToMerge;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.proto.ProtoComponent;
import ru.yandex.crypta.graph2.model.soup.edge.weight.DefaultEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.proto.EnumEdge;
import ru.yandex.crypta.graph2.model.soup.proto.EnumVertex;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;

public class ApplyUseMergeStrategyReducerWithProtoInputTest {

    @Test
    public void edgeMergesTwoComponents() throws Exception {

        DefaultEdgeInfoProvider edgeInfoProvider = new DefaultEdgeInfoProvider();
        UseMergeStrategyReducerWithProtoInput reducer = new UseMergeStrategyReducerWithProtoInput(
                edgeInfoProvider,
                new MergeByScoreAndSimilarityAlgorithm(
                        new SimpleStupidScoringStrategy(),
                        new FakeComponentsSimilarityStrategy(),
                        edgeInfoProvider
                )
        );

        ComponentToMerge component1 = ComponentToMerge.newBuilder()
                .setCryptaId("12345")
                .setGraph(ProtoComponent.newBuilder()
                        .setSingleVertex(
                                EnumVertex.newBuilder()
                                        .setId("y1")
                                        .setIdType(EIdType.IDFA)
                                        .build())
                        .addIdsInfo(
                                IdInfo.newBuilder()
                                        .setId("y1")
                                        .setIdType(Soup.CONFIG.name(EIdType.YANDEXUID))
                                        .setIsActive(true))
                )
                .setMergeKey("12345_54321")
                .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                .setNeighboursWeight(2)
                .setNeighboursCount(2)
                .build();

        ComponentToMerge component2 = ComponentToMerge.newBuilder()
                .setCryptaId("54321")
                .setGraph(ProtoComponent.newBuilder().addEdges(
                        EnumEdge.newBuilder()
                                .setId1("d1")
                                .setId1Type(EIdType.IDFA)
                                .setId2("d2")
                                .setId2Type(EIdType.IDFA)
                                .setSourceType(ESourceType.APP_METRICA)
                                .setLogSource(ELogSourceType.ACCESS_LOG)
                                .build()))
                .setMergeKey("12345_54321")
                .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                .setNeighboursWeight(1)
                .setNeighboursCount(1)
                .build();

        List<EdgeBetweenComponents> edges = Cf.list(
                EdgeBetweenComponents.newBuilder()
                        .setId1("y1")
                        .setId1Type(Soup.CONFIG.name(EIdType.YANDEXUID))
                        .setId2("d1")
                        .setId2Type(Soup.CONFIG.name(EIdType.IDFA))
                        .setSourceType(Soup.CONFIG.name(ESourceType.APP_METRICA))
                        .setLogSource(Soup.CONFIG.name(ELogSourceType.ACCESS_LOG))
                        .setMergeKey("merge_key1")
                        .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                        .build(),
                EdgeBetweenComponents.newBuilder()
                        .setId1("y1")
                        .setId1Type(Soup.CONFIG.name(EIdType.YANDEXUID))
                        .setId2("d2")
                        .setId2Type(Soup.CONFIG.name(EIdType.IDFA))
                        .setSourceType(Soup.CONFIG.name(ESourceType.APP_METRICA))
                        .setLogSource(Soup.CONFIG.name(ELogSourceType.ACCESS_LOG))
                        .setMergeKey("merge_key1")
                        .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                        .build()
        );

        ListF<UseMergeStrategyReducerInRec> inRecs = Cf.list(
                UseMergeStrategyReducerInRec.newBuilder().setComponentToMergeRec1(component1).build(),
                UseMergeStrategyReducerInRec.newBuilder().setComponentToMergeRec1(component2).build(),
                UseMergeStrategyReducerInRec.newBuilder().setEdgeBetweenComponentsRec2(edges.get(0)).build(),
                UseMergeStrategyReducerInRec.newBuilder().setEdgeBetweenComponentsRec2(edges.get(1)).build()
        );

        LocalYield<YTreeMapNode> result = YtTestHelper.testOneOfProtoReducerWithYsonOutput(reducer, inRecs, null);

        ListF<MergeOffer> mergeOffers = result.getRecsByIndex(0).map(r -> reducer.parse(r, MergeOffer.class));

        assertEquals(2, mergeOffers.size());
        MergeOffer direct = mergeOffers.get(0);
        MergeOffer reversed = mergeOffers.get(1);

        assertEquals(direct.getFromCryptaId(), reversed.getToCryptaId());
        assertEquals(direct.getToCryptaId(), reversed.getFromCryptaId());
        assertEquals(direct.getToCryptaIdComponentSize(), reversed.getFromCryptaIdComponentSize());
        assertEquals(direct.getFromCryptaIdComponentSize(), reversed.getToCryptaIdComponentSize());
        assertEquals(direct.getToCryptaIdNeighboursCount(), reversed.getFromCryptaIdNeighboursCount());
        assertEquals(direct.getFromCryptaIdNeighboursCount(), reversed.getToCryptaIdNeighboursCount());

        assertEquals(direct.getScoreGain(), direct.getScoreGain());

        assertEquals(2, direct.getFromCryptaIdComponentSize());
        assertEquals(1, direct.getToCryptaIdComponentSize());
        assertEquals(1, reversed.getFromCryptaIdComponentSize());
        assertEquals(2, reversed.getToCryptaIdComponentSize());

        assertEquals(1, direct.getFromCryptaIdNeighboursCount());
        assertEquals(2, direct.getToCryptaIdNeighboursCount());
        assertEquals(2, reversed.getFromCryptaIdNeighboursCount());
        assertEquals(1, reversed.getToCryptaIdNeighboursCount());


    }

}
