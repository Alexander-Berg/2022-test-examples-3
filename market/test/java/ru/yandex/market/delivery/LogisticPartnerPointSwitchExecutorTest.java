package ru.yandex.market.delivery;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.LogisticPartnerStateDao;
import ru.yandex.market.core.delivery.LogisticPointSwitchService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(
        before = "LogisticPartnerPointSwitchExecutorTest.before.csv",
        after = "LogisticPartnerPointSwitchExecutorTest.after.csv"
)
public class LogisticPartnerPointSwitchExecutorTest extends FunctionalTest {

    @Autowired
    private LogisticPartnerStateDao logisticPartnerStateDao;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private EnvironmentService environmentService;

    private CheckouterAPI checkouter;

    private LogisticPartnerPointSwitchExecutor executor;

    @BeforeEach
    void setUp() {
        checkouter = Mockito.mock(CheckouterAPI.class);
        prepareCheckouter();
        LogisticPointSwitchService logisticPointSwitchService =
                new LogisticPointSwitchService(
                        logisticPartnerStateDao,
                        protocolService,
                        featureService,
                        partnerTypeAwareService,
                        environmentService,
                        checkouter,
                        1);

        executor =
                new LogisticPartnerPointSwitchExecutor(
                        environmentService,
                        logisticPointSwitchService,
                        logisticPartnerStateDao);
    }

    @Test
    void doJob() {
        executor.doJob(null);
        executor.doJob(null);
        verify(checkouter, times(4)).getOrdersByShop(any(), anyLong());
    }

    private void prepareCheckouter() {
        Order aliveOrder = new Order();
        aliveOrder.setId(111L);
        aliveOrder.setShopId(603285L);
        aliveOrder.setStatus(OrderStatus.PROCESSING);
        aliveOrder.setSubstatus(OrderSubstatus.READY_TO_SHIP);

        PagedOrders pagedOrders1 =
                new PagedOrders(List.of(aliveOrder), Pager.atPage(2, 0));

        PagedOrders pagedOrders2 =
                new PagedOrders(List.of(), Pager.atPage(2, 0));

        Mockito.when(checkouter.getOrdersByShop(Mockito.argThat(r -> r != null && r.pageInfo.getCurrentPage() == 1),
                anyLong())).thenReturn(pagedOrders1);
        Mockito.when(checkouter.getOrdersByShop(Mockito.argThat(r -> r != null && r.pageInfo.getCurrentPage() == 2),
                anyLong())).thenReturn(pagedOrders2);
    }
}
