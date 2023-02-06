package ru.yandex.market.logshatter.parser.delivery.blue.market;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.checkout.events.Event;

import java.util.Date;
import java.util.List;

public class CheckpointStatusMonitoringLogParserTest {

    private final CheckpointStatusMonitoringLogParser logParser = new CheckpointStatusMonitoringLogParser();
    private final LogParserChecker checker = new LogParserChecker(logParser);
    private final DeliveryMonitoringLogParserTestHelper testHelper = new DeliveryMonitoringLogParserTestHelper();

    @Test
    public void checkpointStatuses() throws Exception {
        String line = testHelper.readInput("checkpointStatuses.json");
        checker.check(line, new Date(1518000067000L),
            "gravicapa01ht", 48, 20, 26357000L, 3565577L, 150885L, 50, "GREEN"
        );
    }

    @Test
    public void checkpointStatusesDifferentTracks() throws Exception {
        Event event = testHelper.readEvent("checkpointStatusesDifferentTracks.json");
        List<CheckpointStatusMonitoringRecord> monitoringRecords = logParser.processCheckpointStatuses(event);
        testHelper.assertRecords(monitoringRecords,
            record(new Date(1518000067000L), "gravicapa01ht", 48, 20, 26357000L, 3565577L, 150885L, 50, "BLUE"),
            record(new Date(1518000067000L), "gravicapa01ht", 110, 100, 3808000L, 3565577L, 150886L, 50, "BLUE"),
            record(new Date(1518000067000L), "gravicapa01ht", 120, 110, 7442000L, 3565577L, 150886L, 50, "BLUE")
        );
    }

    private CheckpointStatusMonitoringRecord record(Date date, String host, Integer status, Integer previousStatus,
                                                    Long duration, Long orderId, Long trackId,
                                                    Integer deliveryServiceId, String color) {
        CheckpointStatusMonitoringRecord record = new CheckpointStatusMonitoringRecord();
        record.setTranDate(date);
        record.setHost(host);
        record.setStatus(status);
        record.setPreviousStatus(previousStatus);
        record.setDurationMillis(duration);
        record.setOrderId(orderId);
        record.setTrackId(trackId);
        record.setDeliveryServiceId(deliveryServiceId);
        record.setColor(color);
        return record;
    }

}