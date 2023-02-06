package ru.yandex.market.mbi.logprocessor.client;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.logprocessor.client.model.Context;
import ru.yandex.market.mbi.logprocessor.client.model.Error;
import ru.yandex.market.mbi.logprocessor.client.model.Pager;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogRecord;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogRecordsResponse;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse;
import ru.yandex.market.mbi.logprocessor.client.model.ResponseSubError;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientTest {

    private static WireMockServer wm;
    private MbiLogProcessorClient client = new MbiLogProcessorClient.Builder().baseUrl("http://localhost:9000").build();

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(options().port(9000).withRootDirectory(Objects.requireNonNull(Util.getClassPathFile(
                "wiremock")).getAbsolutePath()));
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @DisplayName("Поиск логов по фильтру")
    @Test
    public void findLogsTest() {
        OffsetDateTime fromDate = OffsetDateTime.of(LocalDate.of(2020, 5, 20).atStartOfDay(),
                ZoneOffset.ofHours(3));
        OffsetDateTime toDate = OffsetDateTime.of(LocalDate.of(2020, 5, 20).atTime(LocalTime.MAX),
                ZoneOffset.ofHours(3));
        PushApiLogsFilter filter = PushApiLogsFilter.newBuilder(10268608, fromDate, toDate).build();
        PushApiLogRecordsResponse response = client.findPushApiLogs(filter);
        assertNotNull(response.getApplication());
        assertNotNull(response.getHost());
        assertNotNull(response.getTimestamp());
        Pager pager = response.getPager();
        assertNotNull(pager);
        assertNotNull(pager.getCurrentPage());
        assertNotNull(pager.getTotalCount());
        assertNotNull(pager.getPageSize());
        assertNotNull(pager.getHasLessPages());
        assertNotNull(pager.getHasMorePages());
        assertNotNull(response.getRecords());
        List<PushApiLogRecord> logRecords = response.getRecords();
        assertTrue(logRecords.size() > 0);
        logRecords.forEach(lr -> {
            assertNull(lr.getRequestBody());
            assertNull(lr.getRequestHeaders());
            assertNull(lr.getResponseBody());
            assertNull(lr.getResponseHeaders());
            assertNotNull(lr.getArgs());
            assertNotNull(lr.getContext());
            assertNotNull(lr.getErrorDescription());
            assertNotNull(lr.getEventId());
            assertNotNull(lr.getMethod());
            assertNotNull(lr.getResource());
            assertNotNull(lr.getRequestDate());
            assertNotNull(lr.getResponseTime());
            assertNotNull(lr.getShopId());
            assertNotNull(lr.getSuccess());
            assertNotNull(lr.getUrl());
            assertNotNull(lr.getResponseError());
            assertNotNull(lr.getResponseSubError());
        });
    }

    @DisplayName("Поиск логов по всем фильтрам")
    @Test
    public void findLogsTestAllFiltersTest()  {
        OffsetDateTime fromDate = OffsetDateTime.of(LocalDate.of(2020, 5, 20).atStartOfDay(),
                ZoneOffset.ofHours(3));
        OffsetDateTime toDate = OffsetDateTime.of(LocalDate.of(2020, 5, 20).atTime(LocalTime.MAX),
                ZoneOffset.ofHours(3));
        PushApiLogsFilter filter = PushApiLogsFilter.newBuilder(10268608, fromDate, toDate)
                .withSuccess(false)
                .withBody("test")
                .withResponseSubError(ResponseSubError.HTTP)
                .withContexts(List.of(Context.MARKET, Context.SANDBOX))
                .withResource("/resource")
                .withPage(2)
                .withPageSize(10)
                .build();
        PushApiLogRecordsResponse response = client.findPushApiLogs(filter);
        assertNotNull(response.getRecords());
        List<PushApiLogRecord> logRecords = response.getRecords();
        assertTrue(logRecords.size() > 0);
    }

    @DisplayName("Поиск по уникальному идентифкатору")
    @Test
    public void findLogByIdTest() {
        Instant instant = LocalDate.of(2020, 5, 20).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toInstant();
        PushApiLogId id = new PushApiLogId(10, instant, "/request", "event-id-1");
        Optional<PushApiLogRecord> logRecordOpt = client.findById(id);
        assertTrue(logRecordOpt.isPresent());
        PushApiLogRecord lr = logRecordOpt.get();
        assertNotNull(lr.getRequestBody());
        assertNotNull(lr.getRequestHeaders());
        assertNotNull(lr.getResponseBody());
        assertNotNull(lr.getResponseHeaders());
        assertNotNull(lr.getArgs());
        assertNotNull(lr.getContext());
        assertNotNull(lr.getErrorDescription());
        assertNotNull(lr.getEventId());
        assertNotNull(lr.getMethod());
        assertNotNull(lr.getResource());
        assertNotNull(lr.getRequestDate());
        assertNotNull(lr.getResponseTime());
        assertNotNull(lr.getShopId());
        assertNotNull(lr.getSuccess());
        assertNotNull(lr.getUrl());
        assertNotNull(lr.getResponseError());
        assertNotNull(lr.getResponseSubError());
    }

    @DisplayName("Неудачный поиск по уникальному идентифкатору")
    @Test
    public void findLogByIdNotFoundTest() {
        Instant instant = LocalDate.of(2020, 5, 20).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toInstant();
        PushApiLogId id = new PushApiLogId(20, instant, "/request", "event-id-1");
        Optional<PushApiLogRecord> logRecordOpt = client.findById(id);
        assertTrue(logRecordOpt.isEmpty());
    }

    @DisplayName("Получение статистики по логам")
    @Test
    public void getLogStatsTest() {
        OffsetDateTime fromDate = OffsetDateTime.of(LocalDate.of(2020, 5, 20).atStartOfDay(),
                ZoneOffset.ofHours(3));
        PushApiLogStatsResponse logStatsResponse = client.getLogStats(10, fromDate, "/resource");
        assertNotNull(logStatsResponse.getApplication());
        assertNotNull(logStatsResponse.getTimestamp());
        assertNotNull(logStatsResponse.getHost());
        assertNotNull(logStatsResponse.getCount());
        assertNotNull(logStatsResponse.getSuccessCount());
        assertNotNull(logStatsResponse.getErrorCount());
        assertNotNull(logStatsResponse.getMaxEventTime());
        assertNotNull(logStatsResponse.getMinEventTime());
    }

    @DisplayName("Получение статистики по логам")
    @Test
    public void getLogStatsFromLastEventTest() {
        PushApiLogStatsResponse logStatsResponse = client.getLogStatsFromLastEvent(20, Duration.ofMinutes(30));
        assertNotNull(logStatsResponse.getApplication());
        assertNotNull(logStatsResponse.getTimestamp());
        assertNotNull(logStatsResponse.getHost());
        assertNotNull(logStatsResponse.getCount());
        assertNotNull(logStatsResponse.getSuccessCount());
        assertNotNull(logStatsResponse.getErrorCount());
        assertNotNull(logStatsResponse.getMaxEventTime());
        assertNotNull(logStatsResponse.getMinEventTime());
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 500")
    @Test
    public void  getErrorTest() {
        MbiLogProcessorResponseException thrown = assertThrows(MbiLogProcessorResponseException.class, () -> {
            Instant instant = LocalDate.of(2020, 5, 20).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant();
            PushApiLogId id = new PushApiLogId(30, instant, "/request", "event-id-1");
            client.findById(id);
        });
        assertEquals(500, thrown.getHttpErrorCode());
        assertEquals(1, thrown.getErrors().size());
        Error error = thrown.getErrors().get(0);
        assertEquals("errorCode", error.getCode());
        assertEquals("errorMessage", error.getMessage());
    }

    @DisplayName("Сервер не ответил во время.")
    @Test
    public void timeoutTest() {
        // given
        MbiLogProcessorClient newClient = new MbiLogProcessorClient.Builder()
                .baseUrl("http://localhost:9000")
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        // then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            Instant instant = LocalDate.of(2020, 5, 20).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant();
            PushApiLogId id = new PushApiLogId(40, instant, "/request", "event-id-1");
            newClient.findById(id);
        });
        assertEquals(SocketTimeoutException.class, thrown.getCause().getClass());
        assertEquals("timeout", thrown.getCause().getMessage());
    }

    @DisplayName("Трассировка включена и пишет в лог.")
    @Test
    public void traceTest() {
        // given
        MbiLogProcessorClient newClient = new MbiLogProcessorClient.Builder()
                .baseUrl("http://localhost:9000")
                .withTracingEnabled(Module.MBI_LOG_PROCESSOR)
                .build();

        RequestContextHolder.createContext("123456789");

        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        LoggerConfig loggerConfig = configuration.getLoggerConfig("requestTrace");
        TestAppender appender = new TestAppender();
        appender.start();
        loggerConfig.addAppender(appender, Level.TRACE, null);

        // then
        Instant instant = LocalDate.of(2020, 5, 20).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toInstant();
        PushApiLogId id = new PushApiLogId(20, instant, "/request", "event-id-1");
        newClient.findById(id);
        appender.stop();
        assertEquals(1, appender.getMessages().size());
        String message = appender.getMessages().get(0);
        String expectedPattern = "tskv\tdate=(.*?)\ttype=OUT\trequest_id=123456789/1\t" +
                "target_module=mbi_log_processor\ttarget_host=localhost:9000\tprotocol=http\thttp_method=GET" +
                "\trequest_method=/pushapi/logs\tquery_params=shopId=20" +
                "&requestDate=1589922000000&resource=/request&eventId=event-id-1\ttime_millis=(\\d*?)\thttp_code=200";
        assertTrue(message.matches(expectedPattern));
    }
}
