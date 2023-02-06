package ru.yandex.crypta.graph2.model.matching.component.score.extractors;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.DeviceId;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class DevicesCountExtractorTest {

    public static GraphInfo graphInfoWithDevices(DeviceId... deviceIds) {
        MapF<Vertex, DeviceId> devicesVps = Cf.wrap(deviceIds).toMap(DeviceId::getVertex, d -> d);
        VertexPropertiesCollector vps = new VertexPropertiesCollector(Cf.map(), devicesVps, Cf.map(), Cf.map(),
                Cf.map());
        return new GraphInfo(Cf.map(), Cf.map(), vps, Cf.list());

    }

    private DeviceId device(Vertex d1, boolean isActive) {
        return new DeviceId(
                d1,
                "fake",
                "",
                "",
                "os", Option.empty(),
                "",
                isActive);
    }

    @Test
    public void apply() throws Exception {

        Vertex d1 = new Vertex("d1", EIdType.IDFA);
        Vertex d2 = new Vertex("d2", EIdType.IDFA);
        Vertex d3 = new Vertex("d3", EIdType.IDFA);

        DeviceId activeDevice = device(d1, true);
        DeviceId notActiveDevice = device(d2, false);
        GraphInfo graphInfo = graphInfoWithDevices(activeDevice, notActiveDevice);

        // device with activity
        Component cp = new Component(d1);
        // device with no activity
        cp.addInnerEdge(new Edge(d2.getId(), d2.getIdType(), "y1", EIdType.YANDEXUID,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, Cf.list()));
        // unknown device
        cp.addInnerEdge(new Edge(d3.getId(), d3.getIdType(), "y1", EIdType.YANDEXUID,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, Cf.list()));

        DevicesCountExtractor extractor = new DevicesCountExtractor(true);
        int devicesCount = extractor.apply(cp, graphInfo);

        assertEquals(1, devicesCount);

    }


}
