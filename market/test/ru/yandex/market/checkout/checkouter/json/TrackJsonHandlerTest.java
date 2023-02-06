package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class TrackJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws IOException {
        String json = "{" +
                "\"id\":123," +
                "\"trackCode\":\"code\"," +
                "\"deliveryServiceId\":123," +
                "\"trackerId\":456," +
                "\"status\":\"STARTED\"," +
                "\"creationDate\":\"02-06-72389 17:37:02\"" +
                "}";

        Track track = read(Track.class, json);

        Assertions.assertEquals(123L, track.getId().longValue());
        Assertions.assertEquals("code", track.getTrackCode());
        Assertions.assertEquals(123L, track.getDeliveryServiceId().longValue());
        Assertions.assertEquals(456L, track.getTrackerId().longValue());
        Assertions.assertEquals(TrackStatus.STARTED, track.getStatus());
        Assertions.assertEquals(EntityHelper.DATE, track.getCreationDate());
    }

    @Test
    public void serialize() throws IOException, ParseException {
        Track track = EntityHelper.getTrack();

        String json = write(track);

        checkJson(json, "$." + Names.Track.ID, 123);
        checkJson(json, "$." + Names.Track.TRACK_CODE, "code");
        checkJson(json, "$." + Names.Track.DELIVERY_SERVICE_ID, 123);
        checkJson(json, "$." + Names.Track.TRACKER_ID, 456);
        checkJson(json, "$." + Names.Track.STATUS, TrackStatus.STARTED.name());
        checkJson(json, "$." + Names.Track.CHECKPOINTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Track.CHECKPOINTS, hasSize(1));
        checkJson(json, "$." + Names.Track.CREATION_DATE, "02-06-72389 17:37:02");
    }

}
