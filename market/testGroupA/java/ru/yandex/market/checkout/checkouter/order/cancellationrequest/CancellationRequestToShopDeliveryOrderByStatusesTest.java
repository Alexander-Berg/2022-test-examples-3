package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.util.ClientHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_UNREACHABLE;


public class CancellationRequestToShopDeliveryOrderByStatusesTest extends BaseStatusModelTest {

    private static Map<ClientRole, Function<Order, ClientInfo>> clientInfoProviders() {
        return Map.of(ClientRole.SHOP, ClientHelper::shopClientFor,
                ClientRole.SHOP_USER, ClientHelper::shopUserClientFor,
                ClientRole.BUSINESS, ClientHelper::businessClientFor,
                ClientRole.BUSINESS_USER, ClientHelper::businessUserClientFor
        );
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> shopCancellationSubstatuses() {
        return Arrays.stream(new Object[][]{
                        {PROCESSING, SHOP_FAILED},
                        {PROCESSING, USER_CHANGED_MIND},
                        {PROCESSING, USER_UNREACHABLE},
                        {DELIVERY, SHOP_FAILED},
                        {DELIVERY, USER_CHANGED_MIND},
                        {DELIVERY, USER_UNREACHABLE},
                        {PICKUP, PICKUP_EXPIRED},
                        {PICKUP, SHOP_FAILED},
                        {PICKUP, USER_CHANGED_MIND},
                        {PICKUP, USER_UNREACHABLE},
                        {PICKUP, PICKUP_EXPIRED}
                }).flatMap(arr -> clientInfoProviders().keySet()
                        .stream()
                        .map(clientRole -> ArrayUtils.addAll(arr, clientRole)))
                .map(Arguments::of);
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        Mockito.when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_USER_UNREACHABLE_VALIDATION))
                .thenReturn(false);
    }

    @ParameterizedTest(name = "orderFromStatus={0},cancellationToSubstatus={1},role={2}")
    @MethodSource("shopCancellationSubstatuses")
    public void checkForShopCanCancel(
            OrderStatus orderFromStatus,
            OrderSubstatus cancellationToSubstatus,
            ClientRole clientRole
    ) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);
        Order order = createShopDeliveryOrderAndProceedTo(orderFromStatus);
        ClientInfo clientInfo = clientInfoProviders().get(clientRole).apply(order);

        boolean canBeAppliedNow = cancellationRequestProcessor.canBeAppliedNow(order, null, clientInfo);
        assertTrue(canBeAppliedNow, "cancellationRequestProcessor.canBeAppliedNow should return true");
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, cancellationToSubstatus, clientInfo);
    }

    private Order createShopDeliveryOrderAndProceedTo(OrderStatus status) {
        Order order = new Order();
        order.setId(923753285L);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        order.setDelivery(delivery);
        order.setStatus(status);
        if (status == PROCESSING) {
            order.setSubstatus(STARTED);
        }
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return order;
    }
}
