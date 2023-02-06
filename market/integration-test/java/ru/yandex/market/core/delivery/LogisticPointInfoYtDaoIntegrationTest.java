package ru.yandex.market.core.delivery;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link LogisticPointInfoYtDao}
 */
@Disabled("Проверка интеграции с YT - чтения. dev отладка")
class LogisticPointInfoYtDaoIntegrationTest {
    private LogisticPointInfoYtDao logisticPointInfoYtDao;

    @BeforeEach
    void setUp() {
        var orderServiceMock = mock(OrderService.class);
        when(orderServiceMock.getOrdersItems(Mockito.any())).thenReturn(List.of());

        logisticPointInfoYtDao = new LogisticPointInfoYtDao(
                new YtTemplate(YtUtil.buildYtClusters(
                        List.of("hahn.yt.yandex.net", "arnold.yt.yandex.net"),
                        getToken())),
                "//home/cdc/prod/market/logistics_management_service/yt_outlet",
                "//home/cdc/prod/market/logistics_management_service/logistics_point",
                "//home/cdc/prod/market/logistics_management_service/address");
    }

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    void testImport() {
        var result = logisticPointInfoYtDao.lookupLogisticPointsInfo(
                List.of(10000000003L, 10001636327L, 123L, 10001737656L, 10000000004L)
        );
        System.out.println(result);
    }

    @DisplayName("Проверяем, что достаются точки СЦ/Складов")
    @Test
    void testImportSC() {
        var result = logisticPointInfoYtDao.lookupLogisticPointsInfo(
                List.of(10000000003L, 10001700279L)
        );
        System.out.println(result);
    }

    @DisplayName("Проверяем, что запрос выполняется корректно с пустым списком")
    @Test
    void testImportEmptyList() {
        var result = logisticPointInfoYtDao.lookupLogisticPointsInfo(
                Collections.emptyList()
        );
        System.out.println(result);
    }

    /**
     * Для запуска теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     */
    private String getToken() {
        return "${mbi.robot.yt.token}";
    }
}
