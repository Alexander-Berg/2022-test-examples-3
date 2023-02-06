package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.BROKEN_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CANCELLED_COURIER_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_COME_FOR_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_DELIVER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_SEARCH_NOT_STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DAMAGED_BOX;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_NOT_MANAGED_REGION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_PROBLEMS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DROPOFF_CLOSED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DROPOFF_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INAPPROPRIATE_WEIGHT_SIZE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.MISSING_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SERVICE_FAULT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SORTING_CENTER_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.UNKNOWN;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FRAUD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED_TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_PRODUCT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_QUALITY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_UNREACHABLE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WRONG_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WRONG_ITEM_DELIVERED;

public class CallCenterCancellationRequestNegativeSubstatusTest
        extends AbstractCancellationRequestNegativeSubstatusTestBase {

    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(
            ClientRole.CALL_CENTER_OPERATOR,
            123L
    );

    @Test
    void createCancellationRequestWithoutSubstatusMap() throws Exception {
        EnumSet<OrderSubstatus> negativeCallCenterUnpaid = EnumSet.allOf(OrderSubstatus.class);
        negativeCallCenterUnpaid.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeCallCenterUnpaid.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, REPLACING_ORDER,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_RECEIVED_TECHNICAL_ERROR,
                USER_WANTS_TO_CHANGE_ADDRESS, USER_WANTS_TO_CHANGE_DELIVERY_DATE
        ));
        negativeCallCenterUnpaid.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeCallCenterPending = EnumSet.allOf(OrderSubstatus.class);
        negativeCallCenterPending.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeCallCenterPending.removeAll(EnumSet.of(USER_UNREACHABLE, USER_CHANGED_MIND,
                USER_REFUSED_DELIVERY, USER_PLACED_OTHER_ORDER, USER_REFUSED_PRODUCT, SHOP_FAILED,
                USER_BOUGHT_CHEAPER, REPLACING_ORDER, USER_FRAUD, SERVICE_FAULT, USER_REFUSED_QUALITY,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_RECEIVED_TECHNICAL_ERROR,
                USER_WANTS_TO_CHANGE_ADDRESS, USER_WANTS_TO_CHANGE_DELIVERY_DATE
        ));
        negativeCallCenterPending.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeCallCenterProcessing = EnumSet.allOf(OrderSubstatus.class);
        negativeCallCenterProcessing.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeCallCenterProcessing.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, MISSING_ITEM, WAREHOUSE_FAILED_TO_SHIP,
                REPLACING_ORDER, USER_FRAUD, SERVICE_FAULT,
                SORTING_CENTER_LOST, LOST, DELIVERY_NOT_MANAGED_REGION,
                INCOMPLETE_CONTACT_INFORMATION, INCOMPLETE_MULTI_ORDER, TECHNICAL_ERROR,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_RECEIVED_TECHNICAL_ERROR,
                DELIVERY_SERVICE_NOT_RECEIVED, DELIVERY_SERVICE_LOST, SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                COURIER_SEARCH_NOT_STARTED, CANCELLED_COURIER_NOT_FOUND, COURIER_NOT_COME_FOR_ORDER, SHOP_FAILED,
                USER_WANTS_TO_CHANGE_ADDRESS, USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                DROPOFF_LOST, DROPOFF_CLOSED, DAMAGED_BOX
        ));
        negativeCallCenterProcessing.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeCallCenterDelivery = EnumSet.allOf(OrderSubstatus.class);
        negativeCallCenterDelivery.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeCallCenterDelivery.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, WRONG_ITEM, BROKEN_ITEM, DELIVERY_PROBLEMS,
                DELIVERY_SERVICE_FAILED, SERVICE_FAULT,
                SORTING_CENTER_LOST, LOST,
                COURIER_SEARCH_NOT_STARTED, CANCELLED_COURIER_NOT_FOUND, COURIER_NOT_COME_FOR_ORDER,
                DELIVERY_NOT_MANAGED_REGION, INAPPROPRIATE_WEIGHT_SIZE, INCOMPLETE_CONTACT_INFORMATION,
                INCOMPLETE_MULTI_ORDER, TECHNICAL_ERROR,
                USER_FRAUD, USER_UNREACHABLE,
                DELIVERY_SERVICE_NOT_RECEIVED, DELIVERY_SERVICE_LOST, SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                MISSING_ITEM, WAREHOUSE_FAILED_TO_SHIP, SHOP_FAILED, USER_REFUSED_PRODUCT, USER_REFUSED_QUALITY,
                USER_WANTS_TO_CHANGE_ADDRESS, USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                COURIER_NOT_DELIVER_ORDER, DAMAGED_BOX
        ));
        negativeCallCenterDelivery.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeCallCenterPickup = EnumSet.allOf(OrderSubstatus.class);
        negativeCallCenterPickup.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeCallCenterPickup.removeAll(EnumSet.of(
                USER_CHANGED_MIND, USER_REFUSED_DELIVERY, USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER,
                PICKUP_EXPIRED, WRONG_ITEM, BROKEN_ITEM, DELIVERY_SERVICE_FAILED, SERVICE_FAULT, LOST,
                DELIVERY_SERVICE_NOT_RECEIVED, DELIVERY_SERVICE_LOST, SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                USER_WANTS_TO_CHANGE_ADDRESS, USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                SHOP_FAILED, USER_FRAUD, USER_REFUSED_PRODUCT, WRONG_ITEM_DELIVERED,
                USER_UNREACHABLE, USER_REFUSED_QUALITY, DAMAGED_BOX
        ));
        negativeCallCenterPickup.remove(UNKNOWN);

        List<Object[]> result = new ArrayList<>();
        for (Boolean isCreateByOrderEditApi : new Boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            Stream.of(
                            negativeCallCenterUnpaid.stream().map(ss ->
                                    new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.UNPAID, ss,
                                            isCreateByOrderEditApi}),
                            negativeCallCenterPending.stream().map(ss ->
                                    new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.PENDING, ss,
                                            isCreateByOrderEditApi}),
                            negativeCallCenterProcessing.stream().map(ss ->
                                    new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.PROCESSING, ss,
                                            isCreateByOrderEditApi}),
                            negativeCallCenterDelivery.stream().map(ss ->
                                    new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.DELIVERY, ss,
                                            isCreateByOrderEditApi}),
                            negativeCallCenterPickup.stream().map(ss ->
                                    new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.PICKUP, ss,
                                            isCreateByOrderEditApi})
                    ).flatMap(Function.identity())
                    .forEach(result::add);
        }
        for (Object[] r : result) {
            super.createCancellationRequest((ClientInfo) r[0], (OrderStatus) r[1], (OrderSubstatus) r[2],
                    (Boolean) r[3]);
        }
    }
}
