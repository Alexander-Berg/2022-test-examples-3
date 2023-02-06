package ru.yandex.market.checkout.checkouter.itemservice;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.itemservice.ItemServiceStatusUpdateValidator;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.EXCEPTION_ON_SERVICE_TERMINAL_STATUS_CHANGE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED;
import static ru.yandex.market.checkout.checkouter.order.ItemServiceStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.ItemServiceStatus.COMPLETED;
import static ru.yandex.market.checkout.checkouter.order.ItemServiceStatus.NEW;
import static ru.yandex.market.checkout.checkouter.order.ItemServiceStatus.values;

/**
 * @author zagidullinri
 * @date 23.05.2022
 */
public class ItemServiceStatusUpdateValidatorTest extends AbstractServicesTestBase {

    private static final Set<OrderStatus> ORDER_STATUSES_TO_PROCEED = Set.of(
            OrderStatus.UNPAID, OrderStatus.PROCESSING, OrderStatus.DELIVERY,
            OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final Set<ItemServiceStatus> TERMINAL_STATUSES = Set.of(CANCELLED, COMPLETED);

    @Autowired
    private ItemServiceStatusUpdateValidator itemServiceStatusUpdateValidator;

    @BeforeEach
    public void setUp() {
        super.setUpBase();
        checkouterFeatureWriter.writeValue(EXCEPTION_ON_SERVICE_TERMINAL_STATUS_CHANGE, true);
        checkouterFeatureWriter.writeValue(VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED, true);
    }

    /**
     * @return Перевод из терминальных статусов
     */
    private static Stream<Arguments> fromTerminalStatuses() {
        return TERMINAL_STATUSES.stream()
                .flatMap(prevStatus -> Arrays.stream(values())
                        .filter(newStatus -> prevStatus != newStatus)
                        .map(newStatus -> Arguments.of(OrderStatus.DELIVERED, prevStatus, newStatus)));
    }

    /**
     * @return Перевод в COMPLETED недоставленного заказа
     */
    private static Stream<Arguments> notDeliveredToCompleted() {
        return Arrays.stream(values())
                .filter(prevStatus -> !TERMINAL_STATUSES.contains(prevStatus))
                .flatMap(prevStatus -> ORDER_STATUSES_TO_PROCEED.stream()
                        .filter(orderStatus -> orderStatus != OrderStatus.DELIVERED)
                        .map(orderStatus -> Arguments.of(orderStatus, prevStatus, COMPLETED)));
    }

    private static Stream<Arguments> notAllowedMoves() {
        return Stream.concat(fromTerminalStatuses(), notDeliveredToCompleted());
    }

    /**
     * @return Перевод в COMPLETED доставленного заказа
     */
    private static Stream<Arguments> deliveredToCompleted() {
        Stream<Arguments> deliveredArguments = Arrays.stream(values())
                .filter(prevStatus -> !TERMINAL_STATUSES.contains(prevStatus))
                .map(prevStatus -> Arguments.of(OrderStatus.DELIVERED, OrderSubstatus.DELIVERY_SERVICE_DELIVERED,
                        prevStatus, COMPLETED));

        return Stream.concat(deliveredArguments,
                Stream.of(Arguments.of(OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED, NEW, COMPLETED)));

    }

    private static Stream<Arguments> allowedMoves() {
        return deliveredToCompleted();
    }


    @ParameterizedTest(name = "Обновление статуса услуги должно быть запрещено из: {1}, в: {2}, статус заказа: {0}")
    @MethodSource("notAllowedMoves")
    public void updateItemServiceStatusShouldFailIfNotAllowed(
            OrderStatus orderStatus, ItemServiceStatus from, ItemServiceStatus to) {
        Order order = OrderProvider.getOrderWithServices();
        order.setStatus(orderStatus);
        ItemService itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        itemService.setStatus(from);

        assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                itemServiceStatusUpdateValidator.validateStatusUpdate(order, itemService, to, ClientInfo.SYSTEM));
    }

    @ParameterizedTest(name =
            "Обновление статуса услуги должно быть разрешено из: {2}, в: {3}, статус заказа: {0}, {1}")
    @MethodSource("allowedMoves")
    public void updateItemServiceStatusShouldNotFailIfAllowed(
            OrderStatus orderStatus, OrderSubstatus orderSubstatus, ItemServiceStatus from, ItemServiceStatus to) {
        Order order = OrderProvider.getOrderWithServices();
        order.setStatus(orderStatus);
        order.setSubstatus(orderSubstatus);
        ItemService itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        itemService.setStatus(from);

        assertDoesNotThrow(() ->
                itemServiceStatusUpdateValidator.validateStatusUpdate(order, itemService, to, ClientInfo.SYSTEM));
    }

    @Test
    void updateItemServiceStatusToCompleteForNotDeliveredOrderShouldNotFailIfTurnedOff() {
        checkouterFeatureWriter.writeValue(VALIDATE_ORDER_STATUS_WHEN_SERVICE_COMPLETED, false);
        Order order = OrderProvider.getOrderWithServices();
        order.setStatus(OrderStatus.PROCESSING);
        order.setSubstatus(OrderSubstatus.SHIPPED);
        ItemService itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        itemService.setStatus(ItemServiceStatus.CONFIRMED);

        assertDoesNotThrow(() -> itemServiceStatusUpdateValidator
                .validateStatusUpdate(order, itemService, COMPLETED, ClientInfo.SYSTEM));
    }
}
