package ru.yandex.market.checkout.checkouter.json;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class TrackCheckpointJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        TrackCheckpoint trackCheckpoint = EntityHelper.createTrackCheckpoint();

        String json = write(trackCheckpoint);

        checkJson(json, "$." + Names.TrackCheckpoint.ID, 123);
        checkJson(json, "$." + Names.TrackCheckpoint.TRACKER_ID, 456);
        checkJson(json, "$." + Names.TrackCheckpoint.COUNTRY, "country");
        checkJson(json, "$." + Names.TrackCheckpoint.CITY, "city");
        checkJson(json, "$." + Names.TrackCheckpoint.LOCATION, "location");
        checkJson(json, "$." + Names.TrackCheckpoint.MESSAGE, "message");
        checkJson(json, "$." + Names.TrackCheckpoint.STATUS, CheckpointStatus.DELIVERED.name());
        checkJson(json, "$." + Names.TrackCheckpoint.ZIPCODE, "zipCode");
        checkJson(json, "$." + Names.TrackCheckpoint.DATE,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(EntityHelper.CHECKPOINT_DATE));
        checkJson(json, "$." + Names.TrackCheckpoint.DELIVERY_STATUS, 123);
        checkJson(json, "$." + Names.TrackCheckpoint.TRANSLATED_COUNTRY, "страна");
        checkJson(json, "$." + Names.TrackCheckpoint.TRANSLATED_CITY, "город");
        checkJson(json, "$." + Names.TrackCheckpoint.TRANSLATED_LOCATION, "местоположение");
        checkJson(json, "$." + Names.TrackCheckpoint.TRANSLATED_MESSAGE, "сообщение");
    }

}
