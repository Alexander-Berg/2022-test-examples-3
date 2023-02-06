package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmService;
import ru.yandex.market.replenishment.autoorder.repository.postgres.LogisticsParamRepository;
import ru.yandex.market.replenishment.autoorder.service.client.CalendarApiClient;

public class CalendarValuesLoaderTest extends FunctionalTest {

    @Value("${calendar-api.tvm-service-id}")
    private int tvmServiceId;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TvmService tvmService;
    @Autowired
    private LogisticsParamRepository logisticsParamRepository;
    @Autowired
    private SqlSession batchSqlSession;
    @Autowired
    private TimeService timeService;

    private MockWebServer mockWebServer;
    private CalendarValuesLoader calendarValuesLoader;

    @Before
    public void setUp() {
        this.mockWebServer = new MockWebServer();
        CalendarApiClient calendarApiClient = new CalendarApiClient(
            tvmServiceId,
            mockWebServer.url("/").toString(),
            objectMapper,
            tvmService);
        calendarValuesLoader =
            new CalendarValuesLoader(logisticsParamRepository, batchSqlSession, timeService, calendarApiClient);
    }

    @Test
    @DbUnitDataSet(
        after = "CalendarValuesLoaderTest_load.after.csv",
        before = "CalendarValuesLoaderTest_load.before.csv")
    public void test() {
        setTestTime(LocalDate.of(2015, 1, 1));
        mockWebServer.enqueue(new MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setBody("{\"holidays\":[{\"date\":\"2015-01-01\",\"type\":\"holiday\",\"name\":\"Новогодние каникулы\"}," +
                "{\"date\":\"2015-01-08\",\"type\":\"weekend\",\"transferDate\":\"2015-01-31\",\"name\":\"Перенос выходного с 31 января\"}," +
                "{\"date\":\"2015-01-13\",\"type\":\"weekday\",\"isTransfer\":false,\"name\":\"День российской печати\"}]}")
            .setResponseCode(200));
        calendarValuesLoader.load();
    }

}
