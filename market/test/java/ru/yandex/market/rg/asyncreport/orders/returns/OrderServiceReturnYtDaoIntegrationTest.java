package ru.yandex.market.rg.asyncreport.orders.returns;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.order.returns.os.OrderServiceReturnDao;
import ru.yandex.market.core.order.returns.os.model.ReturnLine;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtil;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtDynamicTableClientFactory;
import ru.yandex.market.yt.client.config.ConnectionConfig;
import ru.yandex.market.yt.utils.LimitedExecutor;
import ru.yandex.market.yt.utils.Medium;
import ru.yandex.market.yt.utils.ResourceUtils;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;

import static ru.yandex.market.request.trace.Module.MBI_PARTNER;

/**
 * Тесты для {@link OrderServiceReturnDao}
 */
@Disabled("Проверка интеграции с YT - чтения. dev отладка")
class OrderServiceReturnYtDaoIntegrationTest {
    private OrderServiceReturnDao orderServiceReturnDao;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        var rpcCredentials = new RpcCredentials(
                getUser(),
                ConnectionConfig.ytToken(getToken())
        );
        var ytBusConnector = YtClientProxy.busConnector();
        var factory = new YtDynamicTableClientFactory(
                MBI_PARTNER,
                ytBusConnector,
                rpcCredentials,
                ResourceUtils.manifestArcadiaRevision(),
                new LimitedExecutor(1, "yt-initializer-")
        );
        var binding = factory.makeTableBinding(
                "//home/market/testing/mbi/order-service/returns/return_line",
                ReturnLine.class,
                "market-mbi-testing",
                true, // replicated
                false, // enableReplicatedTracker
                0,
                Medium.SSD_BLOBS
        );
        var proxy = factory.makeClient(
                "seneca-vla"
        );
        var proxySource = factory.makeClientSourceCanary(
                binding,
                proxy,
                "seneca-vla,seneca-sas",
                false, // don't wait for active cluster
                true
        );
        orderServiceReturnDao = new OrderServiceReturnDao(
                binding,
                proxySource,
                new YtTemplate(YtUtil.buildYtClusters(
                        List.of("seneca-vla.yt.yandex.net"),
                        getToken())),
                environmentService,
                "//home/market/testing/mbi/order-service/returns/return_line",
                "//home/market/testing/mbi/order-service/returns/logistic_return_line"
        );
    }

    @Test
    void testReturnLines() {
        var result = orderServiceReturnDao.getReturnLines(
                10361574L,
                Instant.ofEpochMilli(1649003609913L),
                Instant.ofEpochMilli(1749003609913L)
        );
        System.out.println(result);
    }

    @Test
    void testReadyForPickupReturnLines() {
        var result = orderServiceReturnDao.getReturnLinesReadyForPickup(10361574L);
        System.out.println(result);
    }

    @Test
    void testLogisticReturnLines() {
        var result = orderServiceReturnDao.getLogisticReturnLinesByIds(
                List.of(
                        20137L,
                        20136L,
                        20135L
                ));
        System.out.println(result);
    }

    @Test
    void testReturnLinesByKeys() {
        var result = orderServiceReturnDao.getReturnLineByKeys(
                Set.of(new ReturnLine.ReturnLineKey(10361574L, 32984701L, 9922835L)));
        System.out.println(result);
    }

    private String getUser() {
        return "robot-market-mbi-ts";
    }

    /**
     * Для запуска теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     */
    private String getToken() {
        return "token";
    }
}
