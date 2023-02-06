package ru.yandex.market.core.delivery;

import java.time.Instant;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.model.LogisticPointSwitchState;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тест кейсы для сервиса {@link LogisticPointSwitchService}
 */
@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "LogisticPointSwitchServiceTest.before.csv")
public class LogisticPointSwitchServiceTest extends FunctionalTest {

    private static final Instant POINT_SWITCH_TIME = DateTimes.toInstantAtDefaultTz(2020, 8, 30, 11, 30, 15);

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

    @Mock
    private CheckouterAPI checkouterAPI;

    private LogisticPointSwitchService pointSwitchService;

    @BeforeEach
    void setUp() {
        pointSwitchService =
                new LogisticPointSwitchService(
                        logisticPartnerStateDao,
                        protocolService,
                        featureService,
                        partnerTypeAwareService,
                        environmentService,
                        checkouterAPI,
                        1
                );

        mockCheckouterAPI();
    }

    @Test
    @DbUnitDataSet(after = "LogisticPointSwitchServiceTest.after.csv")
    @DisplayName("Переключение поставщиком активной логистической точки")
    void onPartnerLogisticPointSwitch() {
        LogisticPointSwitchState pointSwitchState =
                LogisticPointSwitchState.builder()
                        .setPartnerId(582306)
                        .setDeliveryServiceId(47728)
                        .setPointId(56374)
                        .setPointName("Я Точка")
                        .setPointAddress("Льва Толстого д. 18б")
                        .setShipmentType(LogisticPointShipmentType.IMPORT)
                        .setSwitchTime(POINT_SWITCH_TIME)
                        .build();

        pointSwitchService.onPartnerLogisticPointSwitch(pointSwitchState);
    }

    @Test
    @DbUnitDataSet(after = "LogisticPointSwitchServiceTest.crossdock.after.csv")
    @DisplayName("Переключение кроссдоком активной логистической точки")
    void onCrossdockPointSwitch() {
        LogisticPointSwitchState pointSwitchState =
                LogisticPointSwitchState.builder()
                        .setPartnerId(10392205)
                        .setDeliveryServiceId(47728)
                        .setPointId(56374)
                        .setPointName("Я Точка Кроссдока")
                        .setPointAddress("Льва Толстого д. 18б")
                        .setShipmentType(LogisticPointShipmentType.IMPORT)
                        .setSwitchTime(POINT_SWITCH_TIME)
                        .build();

        pointSwitchService.onPartnerLogisticPointSwitch(pointSwitchState);
    }

    @Test
    @DbUnitDataSet(
            before = "LogisticPointSwitchServiceTest.before.update.csv",
            after = "LogisticPointSwitchServiceTest.after.update.csv")
    @DisplayName("При переключении активной точки уже отключенным поставщиком просто продлеваем ему время отключения")
    void updateSwitchPointTime() {
        LogisticPointSwitchState pointSwitchState =
                LogisticPointSwitchState.builder()
                        .setPartnerId(582306)
                        .setDeliveryServiceId(47728)
                        .setPointId(56374124)
                        .setPointName("Я Точка 2")
                        .setPointAddress("Льва Толстого д. 18б")
                        .setShipmentType(LogisticPointShipmentType.IMPORT)
                        .setSwitchTime(POINT_SWITCH_TIME.plusSeconds(20 * 60))
                        .build();

        pointSwitchService.onPartnerLogisticPointSwitch(pointSwitchState);
    }

    @Test
    @DbUnitDataSet(before = "LogisticPointSwitchServiceTest.check.before.csv")
    @DisplayName("Проверка статуса заказа и удаление доставленных заказов из базы")
    void checkOrderState() {
        Order deliveryOrder = new Order();
        deliveryOrder.setId(8523697L);
        deliveryOrder.setShopId(582306L);
        deliveryOrder.setStatus(OrderStatus.DELIVERY);

        Order shippedOrder = new Order();
        shippedOrder.setId(126578L);
        shippedOrder.setShopId(582306L);
        shippedOrder.setStatus(OrderStatus.PROCESSING);
        shippedOrder.setSubstatus(OrderSubstatus.SHIPPED);

        Order undeliveredOrder = new Order();
        undeliveredOrder.setId(4511223L);
        undeliveredOrder.setShopId(582306L);
        undeliveredOrder.setStatus(OrderStatus.UNPAID);

        Order notexistedOrder = new Order();
        notexistedOrder.setId(583759L);
        notexistedOrder.setShopId(582306L);
        notexistedOrder.setStatus(OrderStatus.DELIVERED);

        pointSwitchService.checkOrderState(deliveryOrder);
        pointSwitchService.checkOrderState(shippedOrder);
        pointSwitchService.checkOrderState(undeliveredOrder);
        pointSwitchService.checkOrderState(notexistedOrder);
    }

    private void mockCheckouterAPI() {
        Order suitableOrder = new Order();
        suitableOrder.setId(8523697L);
        suitableOrder.setStatus(OrderStatus.UNPAID);

        Order processingOrder = new Order();
        processingOrder.setId(523984L);
        processingOrder.setStatus(OrderStatus.PROCESSING);
        processingOrder.setSubstatus(OrderSubstatus.READY_TO_SHIP);

        Order shippedOrder = new Order();
        shippedOrder.setId(126578L);
        shippedOrder.setStatus(OrderStatus.PROCESSING);
        shippedOrder.setSubstatus(OrderSubstatus.SHIPPED);

        Order cancelledInProcessing = new Order();
        cancelledInProcessing.setId(654123L);
        ;
        cancelledInProcessing.setStatus(OrderStatus.PROCESSING);
        cancelledInProcessing.setSubstatus(OrderSubstatus.PACKAGING);
        cancelledInProcessing.setCancellationRequest(new CancellationRequest(OrderSubstatus.PACKAGING, null));

        PagedOrders pagedOrders1 =
                new PagedOrders(
                        List.of(suitableOrder, processingOrder),
                        Pager.atPage(1, 2));

        PagedOrders pagedOrders2 =
                new PagedOrders(
                        List.of(shippedOrder, cancelledInProcessing),
                        Pager.atPage(2, 2));

        PagedOrders pagedOrders3 =
                new PagedOrders(
                        List.of(),
                        Pager.atPage(3, 0));
        when(checkouterAPI.getOrdersByShop(Mockito.argThat(r -> r != null && r.pageInfo.getCurrentPage() == 1),
                anyLong())).thenReturn(pagedOrders1);
        when(checkouterAPI.getOrdersByShop(Mockito.argThat(r -> r != null && r.pageInfo.getCurrentPage() == 2),
                anyLong())).thenReturn(pagedOrders2);
        when(checkouterAPI.getOrdersByShop(Mockito.argThat(r -> r != null && r.pageInfo.getCurrentPage() == 3),
                anyLong())).thenReturn(pagedOrders3);
    }
}
