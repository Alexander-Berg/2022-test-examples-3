package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrescriptionDeliveryRuleTest extends AbstractPrescriptionDeliveryTestBase {

    @Test
    @DisplayName("Игнор правила для оффера без рецептурки")
    public void offerWithoutPrescription() {
        // Assign
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = createOrderWithPrescription(parameters, true, false);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        checkouterClient.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, order.getShopId(),
                OrderStatus.PROCESSING, OrderSubstatus.PACKAGING);
        order = orderService.getOrder(order.getId());
        // Act
        // Проверка не должна срабатывать для офферов без рецептурки
        checkouterClient.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, order.getShopId(),
                OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);

        // Assert
        order = orderService.getOrder(order.getId());
        assertTrue(order.getStatus().equals(OrderStatus.PROCESSING)
                && order.getSubstatus().equals(OrderSubstatus.READY_TO_SHIP));
    }

    // TODO: доделать тесты с использованием сервиса и клиента
}
