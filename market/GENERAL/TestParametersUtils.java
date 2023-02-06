package ru.yandex.autotests.market.checkouter.api.data.providers;

import org.apache.commons.lang3.ArrayUtils;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersStatusSteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersSteps;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.balance.BalanceStatusCode;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.body.RequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrdersResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.market.checkouter.api.data.providers.StatusProvider.getCancelSubStatusesForShopFromProcessing;
import static ru.yandex.autotests.market.checkouter.beans.Status.CANCELLED;
import static ru.yandex.autotests.market.checkouter.beans.Status.PENDING;
import static ru.yandex.autotests.market.checkouter.beans.Status.PLACING;
import static ru.yandex.autotests.market.checkouter.beans.Status.PROCESSING;
import static ru.yandex.autotests.market.checkouter.beans.Status.RESERVED;
import static ru.yandex.autotests.market.checkouter.beans.Status.values;

/**
 * User: jkt
 * Date: 01.08.13
 * Time: 10:41
 */
public class TestParametersUtils {

    public static final String ORDERS_TEST_TITLE = "Case: {3}, Order: {4}";

    private static OrdersStatusSteps ordersStatusSteps = new OrdersStatusSteps();

    public static List<Object[]> getForAllStatusesForOrders() {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : ArrayUtils.removeElements(values(), PLACING, RESERVED)) {
            parameters.add(asArray(status));
        }
        return parameters;
    }

    @Deprecated
    public static List<Object[]> getOrdersForAllSubStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (SubStatus subStatus : SubStatus.values()) {
            TestDataOrder order = ordersStatusSteps.getOrderWithSubStatus(subStatus);
            parameters.add(asArray(order.getId(), order.getStatus(), order.getSubstatus()));
        }
        return parameters;
    }

    public static List<Object[]> getAllStatusesWithSubstatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : values()) {
            parameters.addAll(getParametersForStatus(status));
        }
        return parameters;
    }

    public static List<Object[]> getAllCorrectTransitionsFrom(Status fromStatus) {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : values()) {
            if (StatusTransitionsTable.isCorrectTransition(fromStatus, status)) {
                parameters.addAll(getParametersForStatus(status));
            }
        }
        return parameters;
    }

    public static List<Object[]> getAllIncorrectTransitionsFrom(Status fromStatus) {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : values()) {
            if (!StatusTransitionsTable.isCorrectTransition(fromStatus, status)) {
                parameters.addAll(getParametersForStatus(status));
            }
        }
        return parameters;
    }

    public static List<Object[]> getAllStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : ArrayUtils.removeElements(values(), PLACING)) {
            parameters.add(new Object[]{status});
        }
        return parameters;
    }

    public static List<Object[]> getValidStatusesForUser() {
        List<Object[]> parameters = new ArrayList<>();
        for (SubStatus subStatus : SubStatus.forUser()) {
            parameters.add(new Object[]{CANCELLED, subStatus});
        }
        return parameters;
    }

    public static List<Object[]> getInvalidStatusesForUser() {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : values()) {
            parameters.add(new Object[]{status, null});
        }
        SubStatus[] invalidSubStatusesForUser = ArrayUtils.removeElements(SubStatus.values(), SubStatus.forUser());
        for (SubStatus subStatus : invalidSubStatusesForUser) {
            parameters.add(new Object[]{CANCELLED, subStatus});
        }
        return parameters;
    }

    public static List<Object[]> getCancelStatusesForShop() {
        List<Object[]> parameters = new ArrayList<>();
        SubStatus[] validSubStatusesForShop = getCancelSubStatusesForShopFromProcessing();
        for (SubStatus subStatus : validSubStatusesForShop) {
            parameters.add(new Object[]{CANCELLED, subStatus});
        }
        return parameters;
    }


    public static List<Object[]> getCancelStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (SubStatus subStatus : SubStatus.values()) {
            parameters.add(new Object[]{CANCELLED, subStatus});
        }
        return parameters;
    }

    public static List<Object[]> balanceStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (BalanceStatusCode statusCode : BalanceStatusCode.values()) {
            parameters.add(new Object[]{statusCode});
        }
        return parameters;
    }


    public static List<Object[]> getNoCancelStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        Status[] validStatuses = ArrayUtils.removeElements(values(), PROCESSING.previousStatuses());
        validStatuses = ArrayUtils.removeElements(validStatuses, PROCESSING, CANCELLED);
        for (Status status : validStatuses) {
            parameters.add(new Object[]{status, null});
        }
        return parameters;
    }

    public static List<Object[]> getInvalidSubStatusesForShop() {
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(new Object[]{CANCELLED, SubStatus.RESERVATION_EXPIRED});
        return parameters;
    }

    public static List<Object[]> withPreviousStatuses() {
        List<Object[]> parameters = new ArrayList<>();
        for (Status status : values()) {
            if (!status.equals(PENDING)) {// PENDING другая логика перехода
                parameters.addAll(withPreviousStatusesForStatus(status));
            }

        }
        return parameters;
    }

    public static List<Object[]> withPreviousStatusesForStatus(Status status) {
        List<Object[]> parameters = new ArrayList<>();
        for (Status prevoiusStatus : status.previousStatuses()) {
            if (!prevoiusStatus.equals(PENDING)) {// PENDING другая логика перехода
                parameters.add(new Object[]{status, prevoiusStatus});
            }
        }
        return parameters;
    }

    public static List<Object[]> getParametersForStatus(Status status) {
        List<Object[]> parameters = new ArrayList<>();
        for (SubStatus subStatus : status.getSubStatuses()) {
            parameters.add(new Object[]{status, subStatus});
        }
        if (parameters.isEmpty()) {
            parameters.add(new Object[]{status, null});
        }
        return parameters;
    }

    public static List<Object[]> asListOfOrdersWithCase(CheckoutApiRequest... requests) {
        List<Object[]> parameters = new ArrayList<>();
        for (CheckoutApiRequest request : requests) {
            parameters.addAll(asListOfOrdersWithCase(request));
        }
        return parameters;
    }

    public static List<Object[]> asListOfOrdersWithCase(CheckoutApiRequest<? extends RequestBody, ? extends OrdersResponseBody> request) {
        List<Object[]> parameters = new ArrayList<>();
        OrdersSteps resource = new OrdersSteps();
        OrdersResponseBody multiOrder = resource.getMultiOrderBy(request);
        for (OrderResponseBody order : multiOrder.getOrders()) {
            parameters.add(createParametersArray(request, multiOrder, order));
        }
        return parameters;
    }

    private static Object[] createParametersArray(CheckoutApiRequest<? extends RequestBody, ? extends OrdersResponseBody> request,
                                                  OrdersResponseBody multiOrder,
                                                  OrderResponseBody order) {
        return ArrayUtils.addAll(new Object[0], request, multiOrder, order, request.getCaseName(), order.getId());
    }

    public static Object[] asArray(Object... args) {
        return args;
    }
}
