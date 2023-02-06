package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.AS_PART_OF_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.BROKEN_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CANCELLED_COURIER_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_COME_FOR_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_DELIVER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNED_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNS_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_SEARCH_NOT_STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DAMAGED_BOX;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_NOT_MANAGED_REGION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_PROBLEMS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.FULL_NOT_RANSOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INAPPROPRIATE_WEIGHT_SIZE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LATE_CONTACT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.MISSING_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PENDING_CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SERVICE_FAULT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_PENDING_CANCELLED;
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
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WRONG_ITEM;

public class RobotCancellationRequestNegativeSubstatusTest
        extends AbstractCancellationRequestNegativeSubstatusTestBase {

    private static final ClientInfo CRM_ROBOT_CLIENT_INFO = new ClientInfo(ClientRole.CRM_ROBOT, 0L);

    @Test
    void createCancellationRequestWithoutSubstatusesMap() throws Exception {
        EnumSet<OrderSubstatus> negativeRobotUnpaid = EnumSet.allOf(OrderSubstatus.class);
        negativeRobotUnpaid.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeRobotUnpaid.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, REPLACING_ORDER));
        negativeRobotUnpaid.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeRobotPending = EnumSet.allOf(OrderSubstatus.class);
        negativeRobotPending.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeRobotPending.removeAll(EnumSet.of(USER_UNREACHABLE, USER_CHANGED_MIND,
                USER_REFUSED_DELIVERY, USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER,
                REPLACING_ORDER, USER_FRAUD, SERVICE_FAULT, SHOP_FAILED, USER_REFUSED_PRODUCT,
                USER_REFUSED_QUALITY, DELIVERY_SERVICE_NOT_RECEIVED, DELIVERY_SERVICE_LOST,
                SHIPPED_TO_WRONG_DELIVERY_SERVICE, DELIVERY_SERVICE_FAILED));
        negativeRobotPending.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeRobotProcessing = EnumSet.allOf(OrderSubstatus.class);
        EnumSet<OrderSubstatus> negativeRobotProcessingAndDelivery = EnumSet.of(
                USER_CHANGED_MIND,
                USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER,
                USER_BOUGHT_CHEAPER,
                AS_PART_OF_MULTI_ORDER,
                BROKEN_ITEM,
                CANCELLED_COURIER_NOT_FOUND,
                COURIER_NOT_COME_FOR_ORDER,
                COURIER_NOT_DELIVER_ORDER,
                COURIER_RETURNED_ORDER,
                COURIER_RETURNS_ORDER,
                COURIER_SEARCH_NOT_STARTED,
                CUSTOM,
                DELIVERY_NOT_MANAGED_REGION,
                DELIVERY_PROBLEMS,
                DELIVERY_SERVICE_FAILED,
                DELIVERY_SERVICE_LOST,
                DELIVERY_SERVICE_NOT_RECEIVED,
                DELIVERY_SERVICE_UNDELIVERED,
                FULL_NOT_RANSOM,
                INAPPROPRIATE_WEIGHT_SIZE,
                INCOMPLETE_CONTACT_INFORMATION,
                INCOMPLETE_MULTI_ORDER,
                LATE_CONTACT,
                LOST,
                MISSING_ITEM,
                WAREHOUSE_FAILED_TO_SHIP,
                PENDING_CANCELLED,
                PICKUP_EXPIRED,
                REPLACING_ORDER,
                USER_FRAUD,
                SHOP_PENDING_CANCELLED,
                SORTING_CENTER_LOST,
                TECHNICAL_ERROR,
                USER_RECEIVED_TECHNICAL_ERROR,
                USER_REFUSED_PRODUCT,
                USER_REFUSED_QUALITY,
                USER_UNREACHABLE,
                USER_WANTS_TO_CHANGE_ADDRESS,
                USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                WRONG_ITEM,
                SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                SERVICE_FAULT,
                DAMAGED_BOX
        );
        negativeRobotProcessing.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeRobotProcessing.removeAll(negativeRobotProcessingAndDelivery);
        negativeRobotProcessing.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeRobotDelivery = EnumSet.allOf(OrderSubstatus.class);
        negativeRobotDelivery.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeRobotDelivery.removeAll(negativeRobotProcessingAndDelivery);
        negativeRobotDelivery.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeRobotPickup = EnumSet.allOf(OrderSubstatus.class);
        negativeRobotPickup.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeRobotPickup.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, PICKUP_EXPIRED, WRONG_ITEM, BROKEN_ITEM,
                DELIVERY_SERVICE_FAILED, SERVICE_FAULT, DAMAGED_BOX));
        negativeRobotPickup.remove(UNKNOWN);

        List<Object[]> result = new ArrayList<>();
        for (Boolean isCreateByOrderEditApi : new Boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            result.addAll(
                    Stream.of(

                            negativeRobotUnpaid.stream().map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO,
                                    OrderStatus.UNPAID, ss, isCreateByOrderEditApi}),
                            negativeRobotPending.stream().map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO,
                                    OrderStatus.PENDING, ss, isCreateByOrderEditApi}),
                            negativeRobotProcessing.stream().map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO,
                                    OrderStatus.PROCESSING, ss, isCreateByOrderEditApi}),
                            negativeRobotDelivery.stream().map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO,
                                    OrderStatus.DELIVERY, ss, isCreateByOrderEditApi}),
                            negativeRobotPickup.stream().map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO,
                                    OrderStatus.PICKUP, ss, isCreateByOrderEditApi})
                    ).flatMap(Function.identity()).collect(Collectors.toList()));
        }

        for (Object[] r : result) {
            super.createCancellationRequest((ClientInfo) r[0], (OrderStatus) r[1], (OrderSubstatus) r[2],
                    (Boolean) r[3]);
        }
    }
}
