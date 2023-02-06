package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.mock.http.MockHttpInputMessage;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class TrackJsonDeserializerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON_STR =
            "{\"checkpoints\": [" +
                    "{\"id\":1705," +
                    "\"trackerCheckpointId\":99022," +
                    "\"status\":\"INFO_RECEIVED\"," +
                    "\"date\":\"25-10-2017 13:30:18\"," +
                    "\"deliveryStatus\":1" +
                    "}," +
                    "{\"id\":1706," +
                    "\"trackerCheckpointId\":99023," +
                    "\"status\":\"DELIVERED\"," +
                    "\"date\":\"25-10-2017 13:37:52\"," +
                    "\"deliveryStatus\":50}]" +
                    "}";

    @Test
    public void shouldDeserializeCheckpoints() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(JSON_STR.getBytes());
        Track track = (Track) converter.read(Track.class, inputMessage);
        assertThat(track.getCheckpoints(), hasSize(2));

        assertCheckPoint(
                1705L,
                CheckpointStatus.INFO_RECEIVED,
                1,
                "25-10-2017 13:30:18",
                track.getCheckpoints().get(0));

        assertCheckPoint(
                1706L,
                CheckpointStatus.DELIVERED,
                50,
                "25-10-2017 13:37:52",
                track.getCheckpoints().get(1));
    }

    private void assertCheckPoint(long expectedId,
                                  CheckpointStatus expectedStatus,
                                  Integer expectedDeliveryStatus,
                                  String expectedDate,
                                  TrackCheckpoint actual) {
        AssertHelper.from(actual)
                .assertThat(x -> x.getId(), equalTo(expectedId))
                .assertThat(x -> x.getTrackerCheckpointId(), equalTo(0L))
                .assertThat(x -> x.getCheckpointDate(), notNullValue())
                .assertThat(x -> x.getCheckpointDate(), equalTo(parseDate(expectedDate)))
                .assertThat(x -> x.getCheckpointStatus(), equalTo(expectedStatus))
                .assertThat(x -> x.getDeliveryCheckpointStatus(), equalTo(expectedDeliveryStatus));
    }

    private Date parseDate(String v) {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        try {
            return df.parse(v);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static class AssertHelper<T> {

        private final T actual;

        AssertHelper(T actual) {
            this.actual = actual;
        }

        public static <U> AssertHelper<U> from(U actual) {
            return new AssertHelper<U>(actual);
        }

        public <R> AssertHelper<T> assertThat(Function<T, R> getter, Matcher<? super R> matcher) {
            MatcherAssert.assertThat(getter.apply(actual), matcher);
            return this;
        }
    }
}
