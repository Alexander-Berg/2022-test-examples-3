package ru.yandex.market.volva.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.service.ResolveUidServiceImpl;
import ru.yandex.market.volva.entity.EdgeEvent;
import ru.yandex.market.volva.entity.EventCollection;
import ru.yandex.market.volva.entity.GraphEvent;
import ru.yandex.market.volva.entity.GraphEventType;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.NodeEvent;

public class FireProdUldFilterTest {


    public static final String FIRE_PROD_PUID = "2190550858753437195";

    @Test
    public void filterOutPuidEvents() {
        EdgeEvent usualEdge = EdgeEvent.addTrusted(new Node("4", IdType.PUID), new Node("5", IdType.PUID));
        List<GraphEvent> edgeEvents = Arrays.asList(
                EdgeEvent.addTrusted(new Node(FIRE_PROD_PUID, IdType.PUID), new Node("1", IdType.PUID)),
                EdgeEvent.addTrusted(new Node("2", IdType.PUID), new Node(FIRE_PROD_PUID, IdType.PUID)),
                new NodeEvent(new Node(FIRE_PROD_PUID, IdType.PUID), GraphEventType.ADD_TRUSTED),
                usualEdge
                );
        EventCollection eventCollection = EventCollection.builder()
                .events(edgeEvents)
                .build();
        FireProdUidFilter filter = new FireProdUidFilter(new ResolveUidServiceImpl());
        EventCollection result = filter.process(eventCollection);
        Assert.assertEquals(
                EventCollection.builder().events(Collections.singletonList(usualEdge)).build(),
                result);

    }
}
