package ru.yandex.market.logistics.lrm.tasks.returns;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.ReturnCancelOrderProcessor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Отмена клиентского заказа по возврату")
class ReturnCancelOrderProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_ID = 1;
    private static final long CHECKOUTER_ORDER_ID = 654987;

    @Autowired
    private ReturnCancelOrderProcessor processor;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-12-23T11:12:13.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Возврат из ПВЗ")
    @DatabaseSetup("/database/tasks/returns/cancel-order/before/pickup_point.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/cancel-order/after/success_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void pickupPoint() {
        processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build());
        verifyCancelOrder(ClientRole.PICKUP_SERVICE);
    }

    @Test
    @DisplayName("Возврат от курьера")
    @DatabaseSetup("/database/tasks/returns/cancel-order/before/courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/cancel-order/after/success_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void courier() {
        processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build());
        verifyCancelOrder(ClientRole.DELIVERY_SERVICE);
    }

    @Test
    @DisplayName("Ошибка при отмене")
    @DatabaseSetup("/database/tasks/returns/cancel-order/before/pickup_point.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/cancel-order/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void error() {
        when(checkouterAPI.updateOrderStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.PICKUP_SERVICE),
            isNull(),
            isNull(),
            eq(OrderStatus.CANCELLED),
            eq(OrderSubstatus.FULL_NOT_RANSOM),
            eq(List.of(Color.BLUE)),
            isNull()
        )).thenThrow(new RuntimeException("Cancellation error"));

        assertThatThrownBy(() -> processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Cancellation error");

        verifyCancelOrder(ClientRole.PICKUP_SERVICE);
    }

    private void verifyCancelOrder(ClientRole clientRole) {
        verify(checkouterAPI).updateOrderStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(clientRole),
            isNull(),
            isNull(),
            eq(OrderStatus.CANCELLED),
            eq(OrderSubstatus.FULL_NOT_RANSOM),
            eq(List.of(Color.BLUE)),
            isNull()
        );
    }

}
