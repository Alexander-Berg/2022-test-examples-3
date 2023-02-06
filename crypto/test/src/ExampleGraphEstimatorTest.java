package ru.yandex.crypta.graph.engine.score;

import org.junit.Test;

import ru.yandex.crypta.graph.engine.proto.TBrowserInfo;
import ru.yandex.crypta.graph.engine.proto.TEdge;
import ru.yandex.crypta.graph.engine.proto.TEdgeBetween;
import ru.yandex.crypta.graph.engine.proto.TGraph;
import ru.yandex.crypta.graph.engine.proto.TGraphs;
import ru.yandex.crypta.graph.engine.proto.TIdsInfo;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.crypta.lib.proto.identifiers.TEmail;
import ru.yandex.crypta.lib.proto.identifiers.TGaid;
import ru.yandex.crypta.lib.proto.identifiers.TGenericID;
import ru.yandex.crypta.lib.proto.identifiers.TLogin;
import ru.yandex.crypta.lib.proto.identifiers.TPuid;
import ru.yandex.crypta.lib.proto.identifiers.TSha256;
import ru.yandex.crypta.lib.proto.identifiers.TYandexuid;
import ru.yandex.crypta.lib.proto.identifiers.UInt128;
import ru.yandex.crypta.lib.proto.identifiers.UInt256;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleGraphEstimatorTest {

    @Test
    public void testExample() {

        TIdsInfo idsInfo = TIdsInfo.newBuilder()
                .addBrowsersInfo(TBrowserInfo.newBuilder()
                        .setBrowserName("yabro")
                        .setBrowserVersion("123super")
                )
                .build();

        TGraph graph1 = TGraph
                .newBuilder()
                .addVertices(TGenericID.newBuilder()
                        .setYandexuid(TYandexuid.newBuilder().setValue(123))
                        .setType(EIdType.YANDEXUID))
                .addVertices(TGenericID.newBuilder()
                        .setGaid(TGaid.newBuilder()
                                .setValue(UInt128.newBuilder()
                                        .setHi(123)
                                        .setLo(123)
                                ))
                        .setType(EIdType.GAID)
                )
                .addEdges(TEdge.newBuilder()
                        .setVertex1(0)
                        .setVertex2(1)
                        .setSourceType(ESourceType.ACCESS_YP_COOKIE)
                        .setLogSource(ELogSourceType.ACCESS_LOG))
                .setId(1)
                .setIdsInfo(idsInfo)
                .build();

        TGraph graph2 = TGraph
                .newBuilder()
                .addVertices(TGenericID
                        .newBuilder()
                        .setLogin(TLogin.newBuilder().setValue("mylogin"))
                        .setType(EIdType.LOGIN))
                .addVertices(TGenericID
                        .newBuilder()
                        .setPuid(TPuid.newBuilder().setValue(999L))
                        .setType(EIdType.PUID))
                .addVertices(TGenericID
                        .newBuilder()
                        .setEmail(TEmail.newBuilder().setLogin("login_part").setDomain("yandex.ru"))
                        .setType(EIdType.EMAIL))
                .addEdges(TEdge
                        .newBuilder()
                        .setVertex1(0)
                        .setVertex2(1)
                        .setSourceType(ESourceType.ACCESS_YP_COOKIE)
                        .setLogSource(ELogSourceType.ACCESS_LOG))
                .addEdges(TEdge
                        .newBuilder()
                        .setVertex1(0)
                        .setVertex2(2)
                        .setSourceType(ESourceType.ACCESS_YP_COOKIE)
                        .setLogSource(ELogSourceType.ACCESS_LOG))
                .setId(1)
                .build();


        TGraphs graphs = TGraphs.newBuilder()
                .addSubgraphs(graph1)
                .addSubgraphs(graph2)
                .addEdgesBetween(TEdgeBetween.newBuilder()
                        .setVertex1(TGenericID
                                .newBuilder()
                                .setSha256(TSha256.newBuilder()
                                        .setValue(UInt256.newBuilder()
                                                .setV0(1)
                                                .setV1(2)
                                                .setV2(3)
                                                .setV3(4)
                                        )
                                )
                                .setType(EIdType.YANDEXUID))
                        .setVertex2(TGenericID
                                .newBuilder()
                                .setLogin(TLogin.newBuilder()
                                        .setValue("login2")
                                )
                                .setType(EIdType.YANDEXUID))
                        .setSourceType(ESourceType.APP_URL_REDIR)
                        .setLogSource(ELogSourceType.EXPORT_ACCESS_LOG)
                )
                .build();

        GraphEstimator estimator = new ExampleGraphEstimator();

        assertEquals(1.0, estimator.estimate(graph1), 0.001);
        assertEquals(2.0, estimator.estimate(graph2), 0.001);

        ExampleGraphMerger exampleGraphMerger = new ExampleGraphMerger();
        boolean doMerge = exampleGraphMerger.mergeGraphs(graphs);

        assertTrue(doMerge);

    }
}
