package ru.yandex.market.fulfillment.stockstorage.service.audit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.EventType;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.PayloadType;
import ru.yandex.market.fulfillment.stockstorage.events.stock.StockEvent;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class LogBrokerStockEventsHandlerTest {
    private static final String TIMEZONE = "Asia/Vladivostok";

    private final LogBrokerStockEventsHandler stockEventsHandler =
            new LogBrokerStockEventsHandler(new EventHandlesConfiguration().singleLineObjectMapper());

    private final String lineSeparator = System.lineSeparator();

    private TimeZone beforeTimezone;

    @BeforeEach
    public void setUp() throws Exception {
        beforeTimezone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
    }

    @AfterEach
    public void tearDown() throws Exception {
        TimeZone.setDefault(beforeTimezone);
    }

    @Test
    public void handle() throws JSONException {
        String string = stockEventsHandler.toLogRecord(createStockEvent());
        Assert.assertFalse("Log record must be single lined json", string.contains(lineSeparator));
        String expectedStr = extractFileContent("stockEvents.json");
        JSONAssert.assertEquals(expectedStr, string, false);
    }

    private StockEvent createStockEvent() {
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime dateTime = LocalDateTime.of(2018, Month.JANUARY, 14, 18, 42, 42, 42);

        return StockEvent.builder(dateTime)
                .withEventType(EventType.FREEZE_SUCCESSFUL)
                .withId("someId")
                .withPayload(genereatePayload())
                .withPayloadType(PayloadType.SKU)
                .withRequestId("someRequestId").build();
    }

    protected final String extractFileContent(String relativePath) {
        try {
            return IOUtils.toString(getSystemResourceAsStream(relativePath), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<EventAuditFieldConstants, Object> genereatePayload() {
        return ImmutableMap.of(EventAuditFieldConstants.STOCK, "SomeVal", EventAuditFieldConstants.STOCKS, "SomeVal2");
    }
}
