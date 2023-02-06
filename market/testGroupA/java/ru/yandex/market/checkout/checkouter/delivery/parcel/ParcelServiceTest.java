package ru.yandex.market.checkout.checkouter.delivery.parcel;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUtils;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ParcelServiceTest extends AbstractWebTestBase {

    @Autowired
    OrderPayHelper orderPayHelper;
    private Order order;
    @Autowired
    private ParcelService parcelService;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    public void setUp() {
        assertNotNull(parcelService);

        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        order = orderService.getOrder(order.getId());

        assertTrue(CollectionUtils.isNonEmpty(order.getDelivery().getParcels()));
        assertSame(1, order.getDelivery().getParcels().size());
    }

    @Test
    public void createCancellationRequest() {
        Parcel cancellingParcel = Optional.ofNullable(order.getDelivery().getParcels())
                .orElseThrow(() -> new ParcelNotFoundException(0L))
                .iterator()
                .next();

        CancellationRequest cancellationRequest = new CancellationRequest(
                OrderSubstatus.USER_PLACED_OTHER_ORDER,
                "some sad writings..."
        );

        Parcel updatedParcel = parcelService.createCancellationRequest(
                order.getId(),
                cancellingParcel.getId(),
                cancellationRequest,
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 1121L),
                true
        );

        assertNotNull(updatedParcel.getCancellationRequest());

        Order updatedOrder = orderService.getOrder(order.getId());
        updatedParcel = DeliveryUtils.requireParcel(cancellingParcel.getId(), updatedOrder);
        assertNotNull(updatedParcel.getCancellationRequest());
        assertSame(OrderSubstatus.USER_PLACED_OTHER_ORDER, updatedParcel.getCancellationRequest().getSubstatus());
    }

    @Test
    public void checkDeliveryDeadlineStatusTrue() {
        boolean deliveryDeadlineStatus = client.checkDeliveryDeadlineStatus(order.getId(),
                DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW);
        assertTrue(deliveryDeadlineStatus);
    }

    @Test
    public void checkDeliveryDeadlineStatusFalse() {
        orderStatusHelper.updateOrderStatus(order.getId(), PROCESSING, SHIPPED);

        boolean deliveryDeadlineStatus = client.checkDeliveryDeadlineStatus(order.getId(),
                DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW);
        assertFalse(deliveryDeadlineStatus);
    }
}
