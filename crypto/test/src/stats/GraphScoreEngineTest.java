package ru.yandex.crypta.graph.engine.score.stats;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import org.junit.Test;

import ru.yandex.crypta.graph.engine.proto.TGraph;
import ru.yandex.crypta.graph.engine.proto.TGraphStatsArguments;
import ru.yandex.crypta.graph.engine.proto.TStats;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.crypta.lib.proto.identifiers.TGenericID;

import static org.junit.Assert.assertEquals;

public class GraphScoreEngineTest {
    @Test
    public void test() {
        long id = 123;
        TGraph graph = TGraph.newBuilder()
                .setId(id)
                .addVertices(
                        TGenericID
                                .newBuilder()
                                .setTypeValue(1)
                                .setType(EIdType.YANDEXUID)
                                .build())
                .build();
        TGraphStatsArguments args = TGraphStatsArguments
                .newBuilder()
                .setGraph(graph)
                .build();
        TGraphScoreEngine graphScoreEngine = new TGraphScoreEngine();
        List<TStats> statsList = new ArrayList<>();
        StreamObserver<TStats> observer = new StreamObserver<TStats>() {
            @Override
            public void onNext(TStats value) {
                statsList.add(value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        };
        graphScoreEngine.computeStats(args, observer);
        assertEquals(1, statsList.size());
        TStats stats  = statsList.get(0);
        assertEquals(id, stats.getId());

    }
}
