package ru.yandex.market.checkout.checkouter.tasks;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class OrderEventDuplicatesStatsTest {

    private static final int EVENT_ID_DUPLICATES_COUNT = 22;
    private static final int HISTORY_IDS_DUPLICATES_COUNT = 33;
    private static final long TIMESTAMP = 1600000000000L;
    private static final Date DATE = new Date(TIMESTAMP);
    private static final String SERIALIZED_VALUE = "1600000000000,22,33,";

    @BeforeEach
    public void setup() throws Exception {
    }

    @Test
    public void serializeTest() {
        OrderEventDuplicatesStats stats = new OrderEventDuplicatesStats(
                DATE,
                EVENT_ID_DUPLICATES_COUNT,
                HISTORY_IDS_DUPLICATES_COUNT
        );

        StringBuilder sb = new StringBuilder();
        stats.serialize(sb);

        assertThat(sb.toString(), equalTo(SERIALIZED_VALUE));
    }


    @Test
    public void deserializeAllFields() {
        OrderEventDuplicatesStats stats = OrderEventDuplicatesStats.deserialize(SERIALIZED_VALUE);

        assertThat(stats.getTimestamp(), equalTo(DATE));
        assertThat(stats.getEventIdsDuplicatesCount(), equalTo(EVENT_ID_DUPLICATES_COUNT));
        assertThat(stats.getHistoryIdsDuplicatesCount(), equalTo(HISTORY_IDS_DUPLICATES_COUNT));
    }

    @Test
    public void deserializeTimestamp() {
        OrderEventDuplicatesStats stats = OrderEventDuplicatesStats.deserialize("1600000000000");

        assertThat(stats.getTimestamp(), equalTo(DATE));
        assertThat(stats.getEventIdsDuplicatesCount(), equalTo(-1));
        assertThat(stats.getHistoryIdsDuplicatesCount(), equalTo(-1));
    }

    @Test
    public void deserializeTimestampAndEventIds() {
        OrderEventDuplicatesStats stats = OrderEventDuplicatesStats.deserialize("1600000000000,55");

        assertThat(stats.getTimestamp(), equalTo(DATE));
        assertThat(stats.getEventIdsDuplicatesCount(), equalTo(55));
        assertThat(stats.getHistoryIdsDuplicatesCount(), equalTo(-1));
    }

    @Test
    public void deserializeNumberFormatException() {
        OrderEventDuplicatesStats stats = OrderEventDuplicatesStats.deserialize("aaa,bbb,ccc");

        assertThat(stats.getTimestamp(), nullValue());
        assertThat(stats.getEventIdsDuplicatesCount(), equalTo(-1));
        assertThat(stats.getHistoryIdsDuplicatesCount(), equalTo(-1));
    }

}
