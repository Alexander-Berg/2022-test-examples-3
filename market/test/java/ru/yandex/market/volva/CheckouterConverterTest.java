package ru.yandex.market.volva;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.volva.converter.CheckouterConverter;
import ru.yandex.market.volva.domain.CheckouterEvent;
import ru.yandex.market.volva.domain.HistoryEvent;
import ru.yandex.market.volva.domain.OrderHistoryModel;
import ru.yandex.market.volva.domain.OrderProperties;
import ru.yandex.market.volva.entity.EdgeEvent;
import ru.yandex.market.volva.entity.EventCollection;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.Source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * @author dimkarp93
 */
public class CheckouterConverterTest {
    @Test
    public void convertNull() {
        Assert.assertNull(CheckouterConverter.convert(null));
    }

    @Test
    public void convertUndefined() {
        Assert.assertNull(CheckouterConverter.convert(new CheckouterEvent(123L, HistoryEvent.UNDEFINED, null, null)));
    }

    @Test
    public void convertDeviceId() {
        OrderHistoryModel.Buyer buyer = new OrderHistoryModel.Buyer(123L, null, null, 456L, "03");
        OrderHistoryModel model = new OrderHistoryModel(buyer, new OrderProperties("{\"ios_device_id\":\"0DEB310E-329F-4EC4-AF5C-B87A7F9F6864\"}"));
        assertThat(CheckouterConverter.convert(new CheckouterEvent(123L, HistoryEvent.NEW_ORDER, null, model)))
            .extracting(EventCollection::getEvents, list(EdgeEvent.class))
            .extracting(EdgeEvent::getNode1, EdgeEvent::getNode2)
            .containsExactly(tuple(
                new Node("123", IdType.PUID),
                new Node("0DEB310E-329F-4EC4-AF5C-B87A7F9F6864", IdType.DEVICE_ID)
            ));
    }

    @Test
    public void convertTwoIds() {
        OrderHistoryModel.Buyer buyer = new OrderHistoryModel.Buyer(123L, "abcdef", null, 456L, "03");
        OrderHistoryModel model = new OrderHistoryModel(buyer, new OrderProperties(null));

        EventCollection result = EventCollection.create(
                Collections.singletonList(
                        EdgeEvent.addTrusted(
                                new Node("123",
                                        IdType.PUID),
                                new Node("abcdef",
                                        IdType.UUID)
                        )
                ),
                Source.CHECKOUTER
        );

        Assert.assertEquals(result, CheckouterConverter.convert(new CheckouterEvent(123L, HistoryEvent.NEW_ORDER, null, model)));
    }

    @Test
    public void convertAllIds() {
        OrderHistoryModel.Buyer buyer = new OrderHistoryModel.Buyer(123L, "abcdef", "012345678", 456L, "03");
        OrderHistoryModel model = new OrderHistoryModel(buyer, new OrderProperties(null));
        EventCollection result = EventCollection.create(
            Arrays.asList(
                EdgeEvent.addTrusted(
                    new Node("123",
                        IdType.PUID),
                    new Node("abcdef",
                        IdType.UUID)
                ),
                EdgeEvent.addTrusted(
                    new Node("123",
                                        IdType.PUID),
                                new Node("012345678",
                                        IdType.YANDEXUID)
                        )
                ),
                Source.CHECKOUTER
        );

        Assert.assertEquals(result, CheckouterConverter.convert(new CheckouterEvent(123L, HistoryEvent.NEW_ORDER, null, model)));

    }
}
