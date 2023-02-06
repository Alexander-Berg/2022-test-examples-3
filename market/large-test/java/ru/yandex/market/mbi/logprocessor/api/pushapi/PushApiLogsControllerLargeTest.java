package ru.yandex.market.mbi.logprocessor.api.pushapi;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.yandex.market.mbi.logprocessor.LargeTest;
import ru.yandex.market.mbi.logprocessor.TestUtil;
import ru.yandex.market.mbi.logprocessor.YtContainer;
import ru.yandex.market.mbi.logprocessor.YtInitializer;
import ru.yandex.market.mbi.logprocessor.storage.yt.model.PushApiLogEntity;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "Тестовый тест" для проверки large тестов в Arcanum.
 */
@Testcontainers
public class PushApiLogsControllerLargeTest extends LargeTest {
    @Container
    public static YtContainer ytContainer = YtContainer.create();

    @Autowired
    private BindingTable<PushApiLogEntity> pushApiLogsTable;

    @Autowired
    @Qualifier("pushApiLogTargetYt")
    private YtClientProxy targetYt;

    private YtInitializer ytInitializer;

    @BeforeEach
    public void setup() {
        ytInitializer = new YtInitializer(targetYt);
        ytInitializer.initializeFromFile("data/pushapi/push-api-inserts.csv", pushApiLogsTable);
    }

    @AfterEach
    public void clean() {
        ytInitializer.cleanTable(pushApiLogsTable);
    }

    @DisplayName("Поиск по фильтру.")
    @Test
    void testPushApiLogsGet_findShortData() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }
}
