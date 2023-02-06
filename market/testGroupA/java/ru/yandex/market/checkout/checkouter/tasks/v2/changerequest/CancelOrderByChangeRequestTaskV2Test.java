package ru.yandex.market.checkout.checkouter.tasks.v2.changerequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.CancelOrderByChangeRequestPartitionTaskV2Factory;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CancelOrderByChangeRequestTaskV2Test extends AbstractWebTestBase {

    @Autowired
    private CancelOrderByChangeRequestPartitionTaskV2Factory cancelOrderByChangeRequestPartitionTaskV2Factory;
    @Autowired
    private TestableClock clock;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderHistoryDao orderHistoryDao;

    private final ClientInfo user = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void crForCanceledOrderShallBeRejected(boolean isDbs) {
        var orderId = prepareExpiredOrder(OrderStatus.CANCELLED, user, isDbs);

        cancelOrderByChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var expiredCancelledOrder = orderReadingDao.getOrder(orderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        assertEquals(OrderStatus.CANCELLED, expiredCancelledOrder.getStatus());
        var changeRequests = changeRequestDao.findByOrder(expiredCancelledOrder);
        assertNotNull(changeRequests);
        assertEquals(1, changeRequests.size());
        assertEquals(ChangeRequestStatus.REJECTED, changeRequests.get(0).getStatus());
    }

    @Test
    public void crForExpiredProcessingOrderShallBeApplied() {
        var orderId = prepareExpiredOrder(OrderStatus.PROCESSING, user, true);

        cancelOrderByChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var expiredProcessingOrder = orderReadingDao.getOrder(orderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        assertEquals(OrderStatus.CANCELLED, expiredProcessingOrder.getStatus());
        var changeRequests = changeRequestDao.findByOrder(expiredProcessingOrder);
        assertNotNull(changeRequests);
        assertEquals(1, changeRequests.size());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequests.get(0).getStatus());
    }

    @Test
    public void crForExpiredProcessingNonDbsOrderShallNotBeApplied() {
        var orderId = prepareExpiredOrder(OrderStatus.PROCESSING, user, false);

        cancelOrderByChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var expiredProcessingOrder = orderReadingDao.getOrder(orderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        assertEquals(OrderStatus.PROCESSING, expiredProcessingOrder.getStatus());
        var changeRequests = changeRequestDao.findByOrder(expiredProcessingOrder);
        assertNotNull(changeRequests);
        assertEquals(1, changeRequests.size());
        assertEquals(ChangeRequestStatus.NEW, changeRequests.get(0).getStatus());
    }

    public static Stream<Arguments> clientInfoData() {
        return Stream.of(
                new ClientInfo(ClientRole.USER, BuyerProvider.UID),
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 0L),
                new ClientInfo(ClientRole.ANTIFRAUD_ROBOT, 0L),
                ClientInfo.SYSTEM
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "Применяется cancellation request, созданный с ролью {0}")
    @MethodSource("clientInfoData")
    public void hangingCallCenterOperatorCRForDeliveryOrderShallBeApplied(ClientInfo clientInfo) {
        var orderId = prepareExpiredOrder(OrderStatus.DELIVERY, clientInfo, true);

        cancelOrderByChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var cancelledDeliveryOrder = orderReadingDao.getOrder(orderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        assertEquals(OrderStatus.CANCELLED, cancelledDeliveryOrder.getStatus());
        var changeRequests = changeRequestDao.findByOrder(cancelledDeliveryOrder);
        assertNotNull(changeRequests);
        assertEquals(1, changeRequests.size());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequests.get(0).getStatus());
    }

    private Long prepareExpiredOrder(OrderStatus orderStatus, ClientInfo clientInfo, boolean isDbs) {
        return transactionTemplate.execute(tc -> {
            clock.setFixed(Instant.now().minus(49L, ChronoUnit.HOURS), ZoneId.systemDefault());
            var expiredProcessingOrder = createOrderWithStatus(orderStatus, isDbs);
            var historyId = orderHistoryDao.insertOrderHistory(
                    expiredProcessingOrder.getId(),
                    HistoryEventType.ORDER_CHANGE_REQUEST_CREATED,
                    clientInfo);
            var changeRequestPayload = new CancellationRequestPayload(OrderSubstatus.USER_CHANGED_MIND, null, null,
                    null);

            changeRequestDao.save(expiredProcessingOrder,
                    changeRequestPayload,
                    ChangeRequestStatus.NEW,
                    clientInfo,
                    historyId);
            return expiredProcessingOrder.getId();
        });
    }

    private Order createOrderWithStatus(OrderStatus status, boolean isDbs) {
        var parameters = isDbs
                ? WhiteParametersProvider.simpleWhiteParameters()
                : BlueParametersProvider.defaultBlueOrderParameters();

        var order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, status);
    }
}
