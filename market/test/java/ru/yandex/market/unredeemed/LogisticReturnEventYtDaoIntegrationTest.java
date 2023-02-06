package ru.yandex.market.unredeemed;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.unredeemed.LogisticReturnsEventDao;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtil;

@Disabled("Проверка интеграции с YT - чтения. dev отладка")
class LogisticReturnEventYtDaoIntegrationTest {

    private LogisticReturnEventYtDao logisticReturnEventYtDao;

    private LogisticReturnsEventService logisticReturnsEventService;

    @BeforeEach
    void setUp() {
        logisticReturnEventYtDao = new LogisticReturnEventYtDao(
                new YtTemplate(YtUtil.buildYtClusters(
                        List.of("hahn.yt.yandex.net"),
                        getToken())),
                "//home/cdc/prod/market/logistics_lrm/return_event",
                "//home/cdc/prod/market/logistics_lrm/return");

        logisticReturnsEventService = new LogisticReturnsEventService(
                Mockito.mock(LogisticReturnsEventDao.class)
        );

    }

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    void testImport() {
        System.out.println(logisticReturnEventYtDao.fillLrmEvents(
                List.of(1L, 2L, 123L, 200L),
                logisticReturnsEventService::processEvents));
    }

    @DisplayName("Проверяем, что запрос выполняется корректно с пустым списком")
    @Test
    void testImportEmptyList() {
        logisticReturnEventYtDao.fillLrmEvents(
                Collections.emptyList(),
                logisticReturnsEventService::processEvents);
    }

    /**
     * Для запуска теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     */
    private String getToken() {
        return "${mbi.robot.yt.token}";
    }
}
