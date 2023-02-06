package ru.yandex.crypta.graph2.model.matching.component.similarity;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.props.DeviceId;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComponentsRegionSimilarityTest {

    private static final String FAKE_CC = "";
    private static final int REGION_1 = 123;
    private static final int REGION_2 = 124;

    private GraphInfo regionVps(MapF<Vertex, Yandexuid> yuids,
                                MapF<Vertex, DeviceId> deviceIds) {
        return new GraphInfo(Cf.map(), Cf.map(), new VertexPropertiesCollector(
                yuids, deviceIds, Cf.map(), Cf.map(), Cf.map()
        ), Cf.list());
    }

    @Test
    public void testSameRegion() {
        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex d1 = new Vertex("d1", EIdType.IDFA);

        GraphInfo vps = regionVps(
                Cf.map(y1, new Yandexuid(y1, FAKE_CC, "", Option.of(REGION_1), "", false)),
                Cf.map(d1, new DeviceId(d1, FAKE_CC, "", "m", "", Option.of(REGION_1), "", false))
        );

        ComponentsSimilarityStrategy similarity = new ComponentsRegionSimilarityStrategy();
        boolean sameRegion = similarity.isSimilar(new Component(y1), new Component(d1), vps).first().isSimilar();
        assertTrue(sameRegion);
    }

    @Test
    public void testDifferentRegion() {
        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex d1 = new Vertex("d1", EIdType.IDFA);

        GraphInfo vps = regionVps(
                Cf.map(y1, new Yandexuid(y1, FAKE_CC, "", Option.of(REGION_1), "", false)),
                Cf.map(d1, new DeviceId(d1, FAKE_CC, "", "m", "", Option.of(REGION_2), "", false))
        );

        ComponentsSimilarityStrategy similarity = new ComponentsRegionSimilarityStrategy();
        boolean sameRegion = similarity.isSimilar(new Component(y1), new Component(d1), vps).first().isSimilar();
        assertFalse(sameRegion);
    }

}
