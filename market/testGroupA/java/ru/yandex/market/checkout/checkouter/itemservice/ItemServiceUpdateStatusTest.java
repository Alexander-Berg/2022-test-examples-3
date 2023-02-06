package ru.yandex.market.checkout.checkouter.itemservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED;

/**
 * @author zagidullinri
 * @date 23.05.2022
 */
public class ItemServiceUpdateStatusTest extends AbstractWebTestBase {

    @Autowired
    private ItemServiceUpdateService itemServiceUpdateService;

    @BeforeEach
    public void setUp() {
        super.setUpBase();
        checkouterFeatureWriter.writeValue(VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED, true);
    }

    @Test
    public void updateStatusCompletedWhenOrderNotDeliveredShouldFail() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        assertNotEquals(OrderStatus.DELIVERED, order.getStatus());
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        String expectedMessage =
                "ItemService cannot be completed while order is not delivered. ItemServiceId=" + itemServiceId;

        Executable itemServiceStatusUpdater = () -> itemServiceUpdateService.updateItemServiceStatus(
                        order.getId(), itemServiceId, ItemServiceStatus.COMPLETED, ClientInfo.SYSTEM);

        assertThrows(ItemServiceUpdateNotAllowedException.class, itemServiceStatusUpdater, expectedMessage);
    }

    @Test
    public void updateStatusCompletedWhenOrderDeliveredShouldSuccess() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        ItemService itemService = itemServiceUpdateService.updateItemServiceStatus(
                order.getId(), itemServiceId, ItemServiceStatus.COMPLETED, ClientInfo.SYSTEM);

        assertEquals(ItemServiceStatus.COMPLETED, itemService.getStatus());
    }

    @Test
    public void updateStatusCompletedWhenOrderNotDeliveredAndValidationTurnedOffShouldSucceed() {
        checkouterFeatureWriter.writeValue(VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED, false);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        assertNotEquals(OrderStatus.DELIVERED, order.getStatus());
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        ItemService itemService = itemServiceUpdateService.updateItemServiceStatus(
                order.getId(), itemServiceId, ItemServiceStatus.COMPLETED, ClientInfo.SYSTEM);

        assertEquals(ItemServiceStatus.COMPLETED, itemService.getStatus());
    }
}
