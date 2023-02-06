package ru.yandex.market.checkout.checkouter.tasks.v2.changerequest;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.RecipientChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.backbone.QueryPartition;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.CancelInvalidChangeRequestPartitionTaskV2Factory;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestServiceImpl.ACTIVE_CHANGE_REQUEST_STATUSES;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CancelInvalidChangeRequestTaskV2Test extends AbstractWebTestBase {

    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderHistoryDao orderHistoryDao;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancelInvalidChangeRequestPartitionTaskV2Factory cancelInvalidChangeRequestPartitionTaskV2Factory;

    @BeforeEach
    public void beforeEach() {
        cleanOrders();
    }

    @AfterEach
    public void afterEach() {
        cleanOrders();
    }

    @Test
    public void expiredChangeRequestIsInvalid() {
        var pair = createLocalOrder(createPayload());

        changeRequestDao.findByOrders(Collections.singletonList(pair.getFirst()))
                .stream()
                .map(ChangeRequest::getStatus)
                .forEach(s -> assertThat(s, is(ChangeRequestStatus.PROCESSING)));

        jumpToFuture(2, DAYS);

        cancelInvalidChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var changeRequestId = Collections.singletonList(pair.getSecond().getId());
        var changeRequest = changeRequestDao.getChangeRequestsById(changeRequestId).get(0);
        assertEquals(ChangeRequestStatus.INVALID, changeRequest.getStatus());
    }

    @Test
    public void changeRequestForOrderInTerminalStatusIsInvalid() {
        var pair = createLocalOrder(null);

        changeRequestDao.findByOrders(Collections.singletonList(pair.getFirst()))
                .stream()
                .map(ChangeRequest::getStatus)
                .forEach(s -> assertThat(s, is(ChangeRequestStatus.PROCESSING)));

        orderStatusHelper.updateOrderStatus(pair.getFirst().getId(), ClientInfo.SYSTEM, OrderStatus.CANCELLED,
                OrderSubstatus.USER_PLACED_OTHER_ORDER);

        jumpToFuture(2, DAYS);

        cancelInvalidChangeRequestPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        var changeRequestId = Collections.singletonList(pair.getSecond().getId());
        var changeRequest = changeRequestDao.getChangeRequestsById(changeRequestId).get(0);
        assertEquals(ChangeRequestStatus.INVALID, changeRequest.getStatus());
    }

    @Test
    public void correctMapping() {
        var pair = createLocalOrder(createPayload());
        var changeRequests = changeRequestDao.findForValidation(
                ACTIVE_CHANGE_REQUEST_STATUSES, 100, 0L,
                QueryPartition.asNoPartition());
        Assertions.assertFalse(changeRequests.isEmpty());
        var changeRequest = changeRequests
                .stream()
                .filter(cr -> cr.getOrderId().equals(pair.getFirst().getId()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(changeRequest);
        Assertions.assertEquals(changeRequest.getOrderId(), pair.getFirst().getId());
        Assertions.assertEquals(changeRequest.getId(), pair.getSecond().getId());
    }

    private DeliveryDatesChangeRequestPayload createPayload() {
        DeliveryDatesChangeRequestPayload deliveryDatesPayload = new DeliveryDatesChangeRequestPayload();
        deliveryDatesPayload.setFromDate(LocalDate.now().plusDays(1));
        deliveryDatesPayload.setToDate(LocalDate.now().plusDays(1));
        deliveryDatesPayload.setShipmentDate(LocalDate.now());
        deliveryDatesPayload.setReason(HistoryEventReason.USER_MOVED_DELIVERY_DATES);
        return deliveryDatesPayload;
    }

    private Pair<Order, ChangeRequest> createLocalOrder(AbstractChangeRequestPayload payload) {
        var parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();

        // Будто где-то параллельно создали заказ, но мы это переживем
        orderCreateHelper.createOrder(parameters);

        Order localOrder = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(localOrder, PROCESSING);
        var changeRequest = bindChangeRequest(localOrder, payload);
        return new Pair<>(localOrder, changeRequest);
    }

    private ChangeRequest bindChangeRequest(Order order, AbstractChangeRequestPayload payload) {
        AbstractChangeRequestPayload localPayload = avoidNull(payload, new RecipientChangeRequestPayload(
                new RecipientPerson("Leo", null, "Tolstoy"),
                "+79999999999", null, "leo@ya.ru"));

        return transactionTemplate.execute(tc -> {
            Order sameOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                    .orElseThrow(() -> new OrderNotFoundException(order.getId()));

            return changeRequestDao.save(sameOrder, localPayload, ChangeRequestStatus.PROCESSING, ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
        });
    }
}
