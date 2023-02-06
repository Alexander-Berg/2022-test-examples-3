package ru.yandex.market.olap2.step;

import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Test;

import ru.yandex.market.olap2.step.model.CreateStepEventParams;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.olap2.step.StepSender.EVENT_TIMESTAMP_FORMAT;
import static ru.yandex.market.olap2.step.StepSender.eventTs;

public class StepSenderTest {
    @Test
    public void testParseGroup() {
        assertEquals("cubes-etl/tbl",
                StepSender.parseGroup(createEvent("//some/yt/path/tbl/p1", true)));
        assertEquals("cubes-etl/tbl",
                StepSender.parseGroup(createEvent("//some/yt/path/tbl", false)));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testEventTs() throws JsonProcessingException {
        assertEquals("Year partition, last day",
                new Date(120, Calendar.DECEMBER, 31),
                eventTs(createTimedEvent("2020", "2020-12-31 00:00:00")));

        assertEquals("Month partition, last day",
                new Date(120, Calendar.SEPTEMBER, 30),
                eventTs(createTimedEvent("2020-09", "2020-09-30 00:00:00")));

        assertEquals("Year partition, prev year, first day",
                new Date(119, Calendar.DECEMBER, 31),
                eventTs(createTimedEvent("2019", "2020-01-01 00:00:00")));

        assertEquals("Month partition, prev month, first day",
                new Date(120, Calendar.SEPTEMBER, 30),
                eventTs(createTimedEvent("2020-09", "2020-10-01 00:00:00")));

        assertEquals("Day partition, same day",
                new Date(120, Calendar.OCTOBER, 21, 10, 11, 13),
                eventTs(createTimedEvent("2020-10-21", "2020-10-21 10:11:13.508000")));

        assertEquals("Day partition, calculated yesterday",
                new Date(120, Calendar.OCTOBER, 19),
                eventTs(createTimedEvent("2020-10-19", "2020-10-20 00:00:00")));

        assertEquals("Day partition, calculated today - 2",
                new Date(120, Calendar.OCTOBER, 18),
                eventTs(createTimedEvent("2020-10-18", "2020-10-20 00:00:00")));

        assertEquals("Month partition, same month",
                new Date(120, Calendar.OCTOBER, 20),
                eventTs(createTimedEvent("2020-10", "2020-10-20 00:00:00")));

        assertEquals("Month partition, calculated previous month",
                new Date(120, Calendar.OCTOBER, 1),
                eventTs(createTimedEvent("2020-10", "2020-11-20 00:00:00")));

        assertEquals("Not partitioned",
                new Date(120, Calendar.OCTOBER, 20),
                eventTs(createTimedEvent(null, "2020-10-20 00:00:00")));

        assertEquals("white e2e case",
                new Date(120, Calendar.APRIL, 26, 0, 0, 0),
                eventTs(createTimedEvent("2020-04-26", "2020-04-27 09:38:33.924000")));
    }

    @SuppressWarnings("deprecation")
    @Test
    @SneakyThrows
    public void testCreateStepEventParamsSerializer() {
        CreateStepEventParams params = CreateStepEventParams.builder()
                .cluster("olga")
                .destination("dest")
                .group("grgr")
                .parent_id("paranetisid")
                .timestamp(EVENT_TIMESTAMP_FORMAT.get().format(new Date(120, Calendar.APRIL, 26, 0, 0, 0)))
                .type("market-etl")
                .path("//zhaba")
                .partition("part")
                .priority("Low")
                .build();
        ObjectMapper MAPPER = new ObjectMapper();
        assertEquals("{\"group\":\"grgr\",\"timestamp\":\"2020-04-26T00:00:00\",\"destination\":\"dest\"," +
                "\"type\":\"market-etl\",\"parent_id\":\"paranetisid\",\"cluster\":\"olga\",\"path\":\"//zhaba\"," +
                "\"partition\":\"part\",\"priority\":\"Low\"}", MAPPER.writeValueAsString(params));
    }

    private StepEvent createTimedEvent(String partition, String timeCreated) {
        StepEventParams params = new StepEventParams();
        params.setPartition(partition);
        StepEvent e = new StepEvent();
        e.setStepEventParams(params);
        e.setTimeCreated(timeCreated);
        return e;
    }

    private StepEvent createEvent(String path, boolean hasPartitions) {
        StepEventParams params = new StepEventParams();
        params.setPath(path);
        if(hasPartitions) {
            params.setPartition("p1");
        }

        StepEvent e = new StepEvent();
        e.setStepEventParams(params);

        return e;
    }
}
