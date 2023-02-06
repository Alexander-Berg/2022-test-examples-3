package ru.yandex.market.logshatter.parser.delivery.blue.market;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.checkout.events.Event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static ru.yandex.market.logshatter.parser.delivery.blue.market.DeliveryOrderStatusEventType.PARCEL_ERROR;
import static ru.yandex.market.logshatter.parser.delivery.blue.market.DeliveryOrderStatusEventType.TRACKCODE_ABSENCE;

public class DeliveryOrderStatusMonitoringLogParserTest {

    private final DeliveryOrderStatusMonitoringLogParser logParser = new DeliveryOrderStatusMonitoringLogParser();
    private final LogParserChecker checker = new LogParserChecker(logParser);
    private final DeliveryMonitoringLogParserTestHelper testHelper = new DeliveryMonitoringLogParserTestHelper();

    @Test
    public void parsePendingTransition() throws Exception {
        String line = testHelper.readInput("pendingTransition.json");
        checker.check(line, new Date(1518002485000L),
            "gravicapa02ht", // host
            2114855L, // orderId
            -1L, // shipmentId
            99, // deliveryServiceId
            "PENDING_TRANSITION", // eventType
            "GREEN" //color
        );
    }

    @Test
    public void checkEmpty() throws Exception {
        String line = testHelper.readInput("empty.json");
        checker.checkEmpty(line);
    }

    @Test
    public void parcelError() throws Exception {
        Event event = testHelper.readEvent("parcelError.json");
        List<OrderStatusMonitoringRecord> monitoringRecords = logParser.createMonitoringRecords(event);
        testHelper.assertRecords(monitoringRecords,
            record(new Date(1518000067000L), "gravicapa01ht", 2105479L, 193324L, 9, PARCEL_ERROR, "GREEN"),
            record(new Date(1518000067000L), "gravicapa01ht", 2105479L, 193325L, 9, PARCEL_ERROR, "GREEN"));
    }

    @Test
    public void lackOfTrackCode() throws Exception {
        Event event = testHelper.readEvent("dimensionsAppearLackOfTrackCode.json");
        List<OrderStatusMonitoringRecord> monitoringRecords = logParser.createMonitoringRecords(event);
        testHelper.assertRecords(monitoringRecords,
            record(new Date(1518000067000L), "gravicapa01ht", 2105479L, 193326L, 9, TRACKCODE_ABSENCE, "GREEN"));
    }

    @Test
    public void trackCodeAppearance() throws Exception {
        Event event = testHelper.readEvent("appearanceOfTrackCodes.json");
        List<OrderStatusMonitoringRecord> monitoringRecords = logParser.createMonitoringRecords(event);
        testHelper.assertRecords(monitoringRecords,
            record(new Date(1518000067000L), "gravicapa01ht", 2105479L, 193326L, 129, TRACKCODE_ABSENCE, "GREEN"));
    }

    @Test
    public void testOnRealDataWithoutExceptions() throws Exception {
        Path path = Paths.get(getClass().getClassLoader()
            .getResource("blueOrderMonitoring/realData.log").toURI());
        Files.lines(path).map(line -> testHelper.getGson().fromJson(line, Event.class))
            .forEach(logParser::createMonitoringRecords);
        Assert.assertTrue(true);
    }

    private OrderStatusMonitoringRecord record(Date date, String host, Long orderId, Long shipmentId, Integer dsId,
                                               DeliveryOrderStatusEventType eventType, String color) {
        OrderStatusMonitoringRecord record = new OrderStatusMonitoringRecord();
        record.setTranDate(date);
        record.setHost(host);
        record.setOrderId(orderId);
        record.setShipmentId(shipmentId);
        record.setDeliveryServiceId(dsId);
        record.setEventType(eventType);
        record.setColor(color);
        return record;
    }

}