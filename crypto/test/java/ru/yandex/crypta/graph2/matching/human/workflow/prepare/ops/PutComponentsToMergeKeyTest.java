package ru.yandex.crypta.graph2.matching.human.workflow.prepare.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.proto.PutComponentsToMergeKeyInRec;
import ru.yandex.crypta.graph2.matching.human.proto.PutComponentsToMergeKeyOutRec;
import ru.yandex.crypta.graph2.matching.human.workflow.RandomIdGenerator;
import ru.yandex.crypta.graph2.model.id.proto.IdInfo;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyType;
import ru.yandex.crypta.graph2.model.matching.proto.ComponentToMerge;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.proto.MergeNeighbour;
import ru.yandex.crypta.graph2.model.matching.proto.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class PutComponentsToMergeKeyTest {

    private PutComponentsToMergeKey reducer = new PutComponentsToMergeKey();

    public static String name(EIdType t) {
        return Soup.CONFIG.name(t);
    }

    public static String name(ESourceType t) {
        return Soup.CONFIG.name(t);
    }

    public static String name(ELogSourceType t) {
        return Soup.CONFIG.name(t);
    }

    @Test
    public void severalMergeKeysAreAssigned() throws Exception {
        RandomIdGenerator generator = new RandomIdGenerator(123);
        String cryptaId = "123";

        VertexInComponent v1 = generator.randomVertexInComponent(cryptaId, EIdType.YANDEXUID);
        VertexInComponent v2 = generator.randomVertexInComponent(cryptaId, EIdType.IDFA);
        VertexInComponent v3 = generator.randomVertexInComponent(cryptaId, EIdType.EMAIL);

        EdgeInComponent edgeInComponent = EdgeInComponent.newBuilder()
                .setId1(v1.getId())
                .setId1Type(v1.getIdType())
                .setId2(v2.getId())
                .setId2Type(v2.getIdType())
                .setSourceType(name(ESourceType.APP_METRICA))
                .setLogSource(name(ELogSourceType.METRIKA_MOBILE_LOG))
                .setCryptaId(cryptaId)
                .build();

        IdInfo yandexuidProps = IdInfo.newBuilder()
                .setId(v1.getId())
                .setIdType(v1.getIdType())
                .setUaProfile("sdf")
                .setSource(Yandexuid.YUID_WITH_ALL_SOURCE)
                .setIsActive(true)
                .setMainRegion(255)
                .setCryptaId(cryptaId)
                .build();

        MergeNeighbour mergeKey1 = MergeNeighbour.newBuilder()
                .setMergeKey("merge_key1")
                .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                .setMergeStrength(TEdgeProps.EEdgeStrength.USUAL.name())
                .setCryptaId(cryptaId)
                .build();

        MergeNeighbour mergeKey2 = MergeNeighbour.newBuilder()
                .setMergeKey("merge_key2")
                .setMergeKeyType(MergeKeyType.BY_EDGES_CUT.name())
                .setMergeStrength(TEdgeProps.EEdgeStrength.ARTIFICIAL.name())
                .setCryptaId(cryptaId)
                .build();

        ListF<PutComponentsToMergeKeyInRec> inRecs = Cf.list(
                PutComponentsToMergeKeyInRec.newBuilder().setMergeNeighbourRec1(mergeKey1).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setMergeNeighbourRec1(mergeKey2).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v1).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v2).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v3).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexPropertiesInComponentRec3(yandexuidProps).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setEdgeInComponentsRec4(edgeInComponent).build()
        );


        LocalYield<PutComponentsToMergeKeyOutRec> result = YtTestHelper.testOneOfProtoReducer(
                reducer, inRecs, null
        );

        ListF<ComponentToMerge> componentToMerge = result
                .getRecsByIndex(0)
                .map(PutComponentsToMergeKeyOutRec::getComponentToMergeRec1);

        assertEquals(2, componentToMerge.size());
        assertEquals(1, componentToMerge.get(0).getGraphEngine().getEdgesCount());
        assertEquals(1, componentToMerge.get(0).getGraphEngine().getIdsInfo().getBrowsersInfoCount());
        assertEquals(2, componentToMerge.get(0).getNeighboursCount());

        // all elements are also put to table with neighbours
        ListF<VertexInComponent> outVertices = result
                .getRecsByIndex(1)
                .map(PutComponentsToMergeKeyOutRec::getVertexWithMergeKey2);
        ListF<IdInfo> outVps = result
                .getRecsByIndex(2)
                .map(PutComponentsToMergeKeyOutRec::getIdInfoWithMergeKey3);
        ListF<EdgeInComponent> outEdges = result
                .getRecsByIndex(3)
                .map(PutComponentsToMergeKeyOutRec::getEdgeWithMergeKey4);

        assertEquals(0, outVertices.size()); // vertices present only for single vertices
        assertEquals(1, outVps.size());  // 5 by 2 merge keys and mergeInfo
        assertEquals(1, outEdges.size());  // 5 by 2 merge keys and mergeInfo

    }

    @Test
    public void noMergeKeysAreAssigned() throws Exception {
        RandomIdGenerator generator = new RandomIdGenerator(456);
        String cryptaId = "123";

        VertexInComponent v1 = generator.randomVertexInComponent(cryptaId, EIdType.YANDEXUID);
        VertexInComponent v2 = generator.randomVertexInComponent(cryptaId, EIdType.IDFA);
        VertexInComponent v3 = generator.randomVertexInComponent(cryptaId, EIdType.EMAIL);

        EdgeInComponent edgeInComponent = EdgeInComponent.newBuilder()
                .setId1(v1.getId())
                .setId1Type(v1.getIdType())
                .setId2(v2.getId())
                .setId2Type(v2.getIdType())
                .setSourceType(name(ESourceType.APP_METRICA))
                .setLogSource(name(ELogSourceType.METRIKA_MOBILE_LOG))
                .setCryptaId(cryptaId)
                .build();

        IdInfo yandexuidProps = IdInfo.newBuilder()
                .setId(v1.getId())
                .setIdType(v1.getIdType())
                .setUaProfile("sdf")
                .setSource(Yandexuid.YUID_WITH_ALL_SOURCE)
                .setIsActive(true)
                .setCryptaId(cryptaId)
                .build();


        ListF<PutComponentsToMergeKeyInRec> inRecs = Cf.list(
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v1).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v2).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexInComponentRec2(v3).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setVertexPropertiesInComponentRec3(yandexuidProps).build(),
                PutComponentsToMergeKeyInRec.newBuilder().setEdgeInComponentsRec4(edgeInComponent).build()
        );

        LocalYield<PutComponentsToMergeKeyOutRec> result = YtTestHelper.testOneOfProtoReducer(
                reducer, inRecs, null
        );

        ListF<ComponentToMerge> componentToMerge = result
                .getRecsByIndex(0)
                .map(PutComponentsToMergeKeyOutRec::getComponentToMergeRec1);

        assertEquals(0, componentToMerge.size());  // 5 by 2 merge keys and mergeInfo

        // all elements are also put to table with no neighbours
        ListF<VertexInComponent> outVertices = result
                .getRecsByIndex(4)
                .map(PutComponentsToMergeKeyOutRec::getVertexNoMergeKey5);
        ListF<IdInfo> outVps = result
                .getRecsByIndex(5)
                .map(PutComponentsToMergeKeyOutRec::getIdInfoNoMergeKey6);
        ListF<EdgeInComponent> outEdges = result
                .getRecsByIndex(6)
                .map(PutComponentsToMergeKeyOutRec::getEdgeNoMergeKey7);

        assertEquals(0, outVertices.size()); // vertices present only for single vertices
        assertEquals(1, outVps.size());  //
        assertEquals(1, outEdges.size());  //

    }

}
