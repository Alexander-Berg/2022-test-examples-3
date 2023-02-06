package ru.yandex.crypta.graph2.model.matching.component.score;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.props.DeviceId;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class HumanMultiHistogramScoringStrategyTest {

    public static Tuple2<Component, GraphInfo> generateGraph() {
        String cryptaId = "123";
        Component component = new Component(cryptaId);
        component.addAllVertices(Cf.list(
                new Vertex("masha", EIdType.LOGIN),
                new Vertex("mariya", EIdType.LOGIN),
                new Vertex("ma", EIdType.LOGIN),

                new Vertex("aba1095@gmail.com", EIdType.EMAIL),
                new Vertex("abacaba@gmail.com", EIdType.EMAIL),
                new Vertex("abacaba@yandex.ru", EIdType.EMAIL),
                new Vertex("daba1095@mail.ru", EIdType.EMAIL),

                new Vertex("+79188281272", EIdType.PHONE),
                new Vertex("+79291543413", EIdType.PHONE),

                new Vertex("qdslgcx", EIdType.VK_ID),
                new Vertex("123", EIdType.PUID)
        ));
        for (String yuid : Cf.list(
                "276465041541005189", "138404121574335861", "557696311494880781",
                "931979201358323513", "593496201307144478", "627973341407690483",
                "370909691559040470", "548860001186844509")) {
            component.addVertex(new Vertex(yuid, EIdType.YANDEXUID));
        }
        Vertex activeYuid = new Vertex("276465041541005189", EIdType.YANDEXUID);
        Vertex activeDeviceId = new Vertex("00000000-0000-0000-0000-111111111100", EIdType.IDFA);
        component.addAllVertices(Cf.list(activeYuid, activeDeviceId));

        GraphInfo graphInfo = new GraphInfo();

        graphInfo.verticesProperties.addYandexuid(new Yandexuid(activeYuid, cryptaId, "d|desk|windows|6.1", Option.of(123), "", true));
        graphInfo.verticesProperties.addDeviceId(new DeviceId(activeDeviceId, cryptaId, "", "d", "", Option.of(123), "", true));
        return Tuple2.tuple(component, graphInfo);
    }

    @Test
    public void checkInitialization() {
        MetricsTree
                metricsTree = new HumanMultiHistogramScoringStrategy(false, Option.empty()).scoreTree(new Component(), new GraphInfo());
//        assertEquals(1.0, metricsTree.getScore(), Double.MIN_NORMAL); // anomaly prob for empty graph = 1, the rest = 0
        assertEquals(0.0, metricsTree.getScore(), Double.MIN_NORMAL);
    }

    @Test
    public void strategyTest() {
        Tuple2<Component, GraphInfo> graph = generateGraph();
        Component component = graph.get1();

        MetricsTree metricsTree = new HumanMultiHistogramScoringStrategy(false, Option.empty())
                .scoreTree(component, new GraphInfo());

        var scores = metricsTree.getChildren();
        assertEquals(0.3, scores.getOrElse("emails_count", 0.), Double.MIN_NORMAL);
        assertEquals(0.011764705882352944, scores.getOrElse("logins_count", 0.), Double.MIN_NORMAL);
        assertEquals(0.12, scores.getOrElse("logins_lcs", 0.), Double.MIN_NORMAL);
        assertEquals(0.11, scores.getOrElse("emails_lcs", 0.), Double.MIN_NORMAL);
        assertEquals(0.6, scores.getOrElse("phones_count", 0.), Double.MIN_NORMAL);
        assertEquals(0.6, scores.getOrElse("vk_count", 0.), Double.MIN_NORMAL);
        assertEquals(0.002797202797202798, scores.getOrElse("vertices_count", 0.), Double.MIN_NORMAL);
    }
}
