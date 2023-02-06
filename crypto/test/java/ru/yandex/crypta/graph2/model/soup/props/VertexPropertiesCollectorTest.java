package ru.yandex.crypta.graph2.model.soup.props;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.model.id.proto.IdInfo;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class VertexPropertiesCollectorTest {
    @Test
    public void accept() throws Exception {
        YsonMultiEntitySupport serializer = new YsonMultiEntitySupport();

        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex d1 = new Vertex("d1", EIdType.OLD_DEVICE_ID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);
        String someCc = "c1";
        ListF<String> sharedTypes = Cf.list("YANDEX DRIVE", "HEURISTIC DESKTOP");

        Yandexuid yandexuid = new Yandexuid(
                y1,
                someCc,
                "ua1",
                Option.of(1),
                "yuid_with_all",
                false
        );
        DeviceId deviceId = new DeviceId(
                d1,
                someCc,
                "ua2",
                "device",
                "",
                Option.empty(), "mm",
                false
        );
        CommonShared identifier = new CommonShared(
                y2,
                someCc,
                CommonShared.COMMON_SHARED_SOURCE,
                sharedTypes
        );

        VertexPropertiesCollector collector = new VertexPropertiesCollector();
        for (VertexProperties vp : Cf.list(yandexuid, deviceId, identifier)) {
            collector.accept(serializer.serialize(vp));
        }

        Yandexuid collectedYandexuid = collector.getYuids().getTs(y1);
        DeviceId collectedDeviceId = collector.getDeviceIds().getTs(d1);
        CommonShared collectedIdentifier = collector.getSharedIds().getTs(y2);

        assertEquals(y1, collectedYandexuid.getVertex());
        assertEquals(d1, collectedDeviceId.getVertex());
        assertEquals(y2, collectedIdentifier.getVertex());

        assertEquals("ua1", collectedYandexuid.getUaProfile());
        assertEquals("ua2", collectedDeviceId.getUaProfile());
//        assertEquals(sharedTypes, collectedIdentifier.getSharedTypes());


    }

    @Test
    public void acceptProto() throws Exception {
        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex d1 = new Vertex("d1", EIdType.OLD_DEVICE_ID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);
        String someCc = "c1";
        IdInfo yandexuid = IdInfo.newBuilder()
                .setId(y1.getId())
                .setIdType(Soup.CONFIG.name(y1.getIdType()))
                .setCryptaId(someCc)
                .setUaProfile("ua1")
                .setMainRegion(1)
                .setSource("yuid_with_all")
                .setIsActive(false)
                .build();

        IdInfo deviceId = IdInfo.newBuilder()
                .setId(d1.getId())
                .setIdType(Soup.CONFIG.name(d1.getIdType()))
                .setCryptaId(someCc)
                .setUaProfile("ua2")
                .setDeviceType("device")
                .setOs("os")
                .setSource("mm")
                .setIsActive(false)
                .build();

        IdInfo identifier = IdInfo.newBuilder()
                .setId(y2.getId())
                .setIdType(Soup.CONFIG.name(y2.getIdType()))
                .setCryptaId(someCc)
                .setSource(CommonShared.COMMON_SHARED_SOURCE)
                .setIsActive(false)
                .build();


        VertexPropertiesCollector collector = new VertexPropertiesCollector();
        for (IdInfo vp : Cf.list(yandexuid, deviceId, identifier)) {
            collector.accept(vp);
        }

        Yandexuid collectedYandexuid = collector.getYuids().getTs(y1);
        DeviceId collectedDeviceId = collector.getDeviceIds().getTs(d1);
        CommonShared collectedIdentifier = collector.getSharedIds().getTs(y2);

        assertEquals(y1, collectedYandexuid.getVertex());
        assertEquals(d1, collectedDeviceId.getVertex());
        assertEquals(y2, collectedIdentifier.getVertex());

        assertEquals("ua1", collectedYandexuid.getUaProfile());
        assertEquals("ua2", collectedDeviceId.getUaProfile());
    }

}
