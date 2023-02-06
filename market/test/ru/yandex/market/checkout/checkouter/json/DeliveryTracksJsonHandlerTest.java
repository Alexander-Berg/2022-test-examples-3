package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTracks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class DeliveryTracksJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "[{}]";
        DeliveryTracks deliveryTracks = read(DeliveryTracks.class, json);

        assertThat(deliveryTracks.getTracks(), hasSize(1));
    }

    @Test
    public void serialize() throws Exception {
        DeliveryTracks deliveryTracks = new DeliveryTracks(Collections.singletonList(new DeliveryTrack()));

        String json = write(deliveryTracks);
        System.out.println(json);

        checkJson(json, "$", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$", hasSize(1));
    }
}
