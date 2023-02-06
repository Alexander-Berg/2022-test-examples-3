package ru.yandex.market.crm.platform.reader.http.controllers.custom;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.OrderMonitorings;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class MonitoringsCsvParserTest {

    @Test
    public void parseNotLoggedInUserOrderTest() throws IOException {
        List<CSVRecord> recs = readRecords();

        OrderMonitorings expected = OrderMonitorings.newBuilder()
                .setOrderId(1)
                .addMonitoring("DELAYED_DELIVERY_START")
                .addAllUids(asList(
                        Uids.create(UidType.MUID, "1152921504658616041"),
                        Uids.create(UidType.UUID, "f43cc4d784b5b41ea81ae601ed896a02"),
                        Uids.create(UidType.EMAIL, "fidel@mail.ru"),
                        Uids.create(UidType.PHONE, "111")
                )).build();

        OrderMonitorings actual = MonitoringsCsvParser.parseCsv(recs.get(0));
        assertEquals(expected, actual);
    }

    @Test
    public void parserLoggedInUserOrderTest() throws IOException {
        List<CSVRecord> recs = readRecords();

        OrderMonitorings expected = OrderMonitorings.newBuilder()
                .setOrderId(2)
                .addMonitoring("DELAYED_DELIVERY_START")
                .addAllUids(asList(
                        Uids.create(UidType.PUID, "642576905"),
                        Uids.create(UidType.YANDEXUID, "1479381521535130255"),
                        Uids.create(UidType.EMAIL, "vladimir@mail.ru"),
                        Uids.create(UidType.PHONE, "222")
                )).build();

        OrderMonitorings actual = MonitoringsCsvParser.parseCsv(recs.get(1));
        assertEquals(expected, actual);
    }

    private static List<CSVRecord> readRecords() throws IOException {
        CSVParser parser = CSVParser.parse(
                MonitoringsCsvParserTest.class.getResource("monitorings.csv"),
                Charset.defaultCharset(), OrderMonitoringsController.CSV_FORMAT);
        return parser.getRecords();
    }
}