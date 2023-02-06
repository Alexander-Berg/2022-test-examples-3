package ru.yandex.market.replenishment.autoorder.service.client;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmService;

public class CalendarApiClientTest extends FunctionalTest {

    private CalendarApiClient calendarApiClient;

    @Value("${calendar-api.tvm-service-id}")
    private int tvmServiceId;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TvmService tvmService;

    private MockWebServer mockWebServer;

    @Before
    public void setUp() {
        this.mockWebServer = new MockWebServer();
        calendarApiClient = new CalendarApiClient(
            tvmServiceId,
            mockWebServer.url("/").toString(),
            objectMapper,
            tvmService);
    }

    @Test
    public void testOK() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setBody("{\"holidays\":[{\"date\":\"2015-01-01\",\"type\":\"holiday\",\"name\":\"Новогодние каникулы\"}," +
                "{\"date\":\"2015-01-08\",\"type\":\"weekend\",\"transferDate\":\"2015-01-31\",\"name\":\"Перенос выходного с 31 января\"}," +
                "{\"date\":\"2015-01-13\",\"type\":\"weekday\",\"isTransfer\":false,\"name\":\"День российской печати\"}]}")
            .setResponseCode(200));
        final CalendarApiClient.Holidays holidays = calendarApiClient.getHolidays(LocalDate.now(), LocalDate.now());
        Assertions.assertNotNull(holidays);
        Assertions.assertEquals(3, holidays.getHolidays().size());
    }


}
