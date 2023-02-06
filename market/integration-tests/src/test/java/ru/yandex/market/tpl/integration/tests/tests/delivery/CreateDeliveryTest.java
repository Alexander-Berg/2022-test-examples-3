package ru.yandex.market.tpl.integration.tests.tests.delivery;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.common.util.RandomUtils;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.DeliveryTestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.DeliveryApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Доставка")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CreateDeliveryTest {
    private final DeliveryApiFacade deliveryApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final Clock clock;
    private String senderId;
    private String warehouseId;

    @BeforeEach
    void before() {
        DeliveryTestConfiguration configuration = AutoTestContextHolder.getContext().getDeliveryTestConfiguration();
        configuration.setCreateOrderRequestPath("/order/create_order.xml");
        configuration.setUpdateOrderRequestPath("/order/update_order.xml");
        configuration.setGetOrdersStatusRequestPath("/order/get_orders_status.xml");
        configuration.setGetOrderHistoryRequestPath("/order/get_order_history.xml");
        int i = RandomUtils.nextIntInRange(1_000_000_000, 2_000_000_000);
        this.senderId = i + "";
        this.warehouseId = i + "";
        configuration.setSenderId(senderId);
        configuration.setWarehouseId(warehouseId);

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        OffsetDateTime deliveryDate = tomorrow.atStartOfDay(clock.getZone()).toOffsetDateTime();
        configuration.setDeliveryDate(deliveryDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteOrder();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Feature("Доставка")
    @Stories({@Story("Создать заказ"), @Story("Обновить заказ"),
            @Story("Получить статус заказа"), @Story("Получить историю статусов заказа")})
    @DisplayName(value = "Создание заказа")
    void test() {
        deliveryApiFacade.createOrder();
        deliveryApiFacade.updateOrder();
        var orderStatusResponse = deliveryApiFacade.getOrderStatus();
        checkOrderStatus(orderStatusResponse);
        var orderStatusHistoryResponse = deliveryApiFacade.getOrderStatusHistory();
        checkHistory(orderStatusHistoryResponse);
        DetailedOrderDto detailedOrderDto = deliveryApiFacade.getDetailedOrderInfo();
        checkOrderSenderAndWarehouse(detailedOrderDto);
    }

    private void checkOrderSenderAndWarehouse(DetailedOrderDto detailedOrderDto) {
        assertThat(detailedOrderDto.getSender().getYandexId()).isEqualTo(senderId);
        assertThat(detailedOrderDto.getWarehouse().getYandexId()).isEqualTo(warehouseId);
        assertThat(detailedOrderDto.getWarehouseReturn().getYandexId()).isEqualTo(warehouseId);
    }

    private void checkOrderStatus(GetOrdersStatusResponse orderStatusResponse) {
        assertThat(orderStatusResponse.getOrderStatusHistories().size()).isEqualTo(1);
        assertThat(orderStatusResponse.getOrderStatusHistories().iterator().next().getHistory().iterator().next().getStatusCode())
                .isEqualTo(OrderStatusType.ORDER_CREATED_DS);
    }

    private void checkHistory(GetOrderHistoryResponse orderStatusHistoryResponse) {
        assertThat(orderStatusHistoryResponse.getOrderStatusHistory().getHistory().size()).isEqualTo(1);
        assertThat(orderStatusHistoryResponse.getOrderStatusHistory().getHistory().iterator().next().getStatusCode())
                .isEqualTo(OrderStatusType.ORDER_CREATED_DS);
    }
}
