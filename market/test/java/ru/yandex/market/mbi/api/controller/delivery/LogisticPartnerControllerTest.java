package ru.yandex.market.mbi.api.controller.delivery;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.LogisticPointSwitchRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.delivery.LogisticPointShipmentType.IMPORT;

/**
 * Тестовые кейсы для {@link LogisticPartnerController}
 */
@DbUnitDataSet(before = "LogisticPartnerControllerTest.before.csv")
public class LogisticPartnerControllerTest extends FunctionalTest {

    @Autowired
    private CheckouterAPI checkouterAPI;

    @BeforeEach
    void setUp() {
        mockCheckouterAPI();
    }

    @Test
    @DbUnitDataSet(after = "LogisticPartnerControllerTest.after.csv")
    @DisplayName("Ручка информирования MBI о том что поставщик сменил логистическую точку")
    void switchPartnerLogisticPoint() {
        LogisticPointSwitchRequest request = new LogisticPointSwitchRequest(47728, 56374, "Я Точка", "Льва Толстого д. 18б", IMPORT);
        mbiApiClient.switchPartnerLogisticPoint(request);
    }

    @Test
    @DbUnitDataSet(
            before = "LogisticPartnerControllerTest.before.clear.csv",
            after = "LogisticPartnerControllerTest.after.clear.csv")
    @DisplayName("Принудительное снятие катоффа и очистка списка недовезенных заказов поставщика")
    void clearLogisticPointSwitch() {
        mbiApiClient.clearLogisticPointSwitch(582306);
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

        PagedOrders pagedOrders =
                new PagedOrders(List.of(suitableOrder, processingOrder, shippedOrder), Pager.atPage(1, 3));

        when(checkouterAPI.getOrdersByShop(any(), anyLong())).thenReturn(pagedOrders);
    }
}
