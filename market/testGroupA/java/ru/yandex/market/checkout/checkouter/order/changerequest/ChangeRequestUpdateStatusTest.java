package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public class ChangeRequestUpdateStatusTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderHistoryDao orderHistoryDao;

    private Order order;
    private RecipientChangeRequestPayload expectedCRPayload;
    private ChangeRequest expectedChangeRequest;

    @BeforeEach
    @SuppressWarnings("checkstyle:HiddenField")
    public void setUp() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        expectedCRPayload = new RecipientChangeRequestPayload(
                new RecipientPerson("Leo", null, "Tolstoy"),
                "+79999999999", null, "leo@ya.ru");

        expectedChangeRequest = transactionTemplate.execute(tc -> {
            Order order = orderReadingDao.getOrder(this.order.getId(), ClientInfo.SYSTEM)
                    .orElseThrow(() -> new OrderNotFoundException(this.order.getId()));

            return changeRequestDao.save(order,
                    expectedCRPayload,
                    ChangeRequestStatus.NEW,
                    ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
        });
        assertNotNull(expectedChangeRequest);
    }

    @Test
    public void shouldSuccessUpdateChangeRequestToStatusProcessing() {
        processChangeRequestToStatus(expectedChangeRequest, ChangeRequestStatus.PROCESSING);

        Order orderAfter = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        // проверяем, что после изменения статуса заявки - статус действительно обновился.
        List<ChangeRequest> changeRequests = orderAfter.getChangeRequests();
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(expectedChangeRequest.getId(), changeRequest.getId());
        assertEquals(ChangeRequestStatus.PROCESSING, changeRequest.getStatus());

        CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi = client.orderHistoryEvents();

        PagedEvents events = checkouterOrderHistoryEventsApi.getOrderHistoryEvents(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                null,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        assertEquals(HistoryEventType.ORDER_CHANGE_REQUEST_STATUS_UPDATED,
                events.getItems().stream().findFirst().get().getType());

        // проверяем, что в ивентах все тоже проросло
        Order orderAfterFromEvent = events.getItems().iterator().next().getOrderAfter();

        ChangeRequest changeRequestFromEvent = orderAfterFromEvent.getChangeRequests().iterator().next();

        assertEquals(expectedChangeRequest.getId(), changeRequestFromEvent.getId());
        assertEquals(ChangeRequestStatus.PROCESSING, changeRequest.getStatus());
    }

    @Test
    void shouldSuccessUpdateChangeRequestFromRejectToSuccesFromParcelCancellation() {
        client.createCancellationRequest(
                order.getId(),
                new CompatibleCancellationRequest(USER_CHANGED_MIND.name(), ""),
                ClientRole.USER,
                BuyerProvider.UID
        );
        Order orderWithCancellationRequest = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        ChangeRequest cancellationRequest = CollectionUtils.emptyIfNull(
                orderWithCancellationRequest.getChangeRequests()
        )
                .stream()
                .filter(cr -> cr.getType().equals(ChangeRequestType.PARCEL_CANCELLATION))
                .findFirst()
                .orElseThrow();

        processChangeRequestToStatus(cancellationRequest, ChangeRequestStatus.PROCESSING);
        processChangeRequestToStatus(cancellationRequest, ChangeRequestStatus.REJECTED);
        processChangeRequestToStatus(cancellationRequest, ChangeRequestStatus.APPLIED);

        Order orderAfter = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        List<ChangeRequest> changeRequests = orderAfter.getChangeRequests();
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(3));
        ChangeRequest changeRequest = changeRequests.stream()
                .filter(cr -> cr.getType().equals(ChangeRequestType.PARCEL_CANCELLATION))
                .findFirst()
                .orElseThrow();

        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());
        assertEquals(CANCELLED, orderAfter.getStatus());
    }

    private void processChangeRequestToStatus(ChangeRequest changeRequest, ChangeRequestStatus status) {
        boolean isSuccess = client.updateChangeRequestStatus(
                order.getId(),
                changeRequest.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                new ChangeRequestPatchRequest(status, null, null)
        );
        assertTrue(isSuccess);
    }

    @Test
    public void updateChangeRequestStatusToRejectedWithMessage() {
        // сначала переводим в процессинг
        client.updateChangeRequestStatus(
                order.getId(),
                expectedChangeRequest.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                new ChangeRequestPatchRequest(ChangeRequestStatus.PROCESSING, null, null));

        // а теперь отклоняем
        ChangeRequestPatchRequest rejectRequest = new ChangeRequestPatchRequest(
                ChangeRequestStatus.REJECTED,
                "Не удалось обновить данные в СД",
                null
        );
        boolean isSuccess = client.updateChangeRequestStatus(
                order.getId(),
                expectedChangeRequest.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                rejectRequest);
        assertTrue(isSuccess);

        Order orderAfter = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        // проверяем, что после изменения статуса заявки - статус действительно обновился.
        List<ChangeRequest> changeRequests = orderAfter.getChangeRequests();
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(expectedChangeRequest.getId(), changeRequest.getId());
        assertEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus());
        assertEquals(rejectRequest.getMessage(), changeRequest.getMessage());
    }

    @Test
    public void failUpdateChangeRequestStatusFromFinalStatusToProcessing() {
        // сначала переводим в обработку
        client.updateChangeRequestStatus(
                order.getId(),
                expectedChangeRequest.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                new ChangeRequestPatchRequest(ChangeRequestStatus.PROCESSING, null, null));

        // а теперь подтверждаем
        processChangeRequestToStatus(expectedChangeRequest, ChangeRequestStatus.APPLIED);


        // попытаемся снова перевести в обработку
        ChangeRequestPatchRequest processingRequest = new ChangeRequestPatchRequest(
                ChangeRequestStatus.PROCESSING,
                null,
                null
        );

        try {
            client.updateChangeRequestStatus(
                    order.getId(),
                    expectedChangeRequest.getId(),
                    ClientInfo.SYSTEM.getRole(),
                    ClientInfo.SYSTEM.getUid(),
                    processingRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignore) {
        }

        Order orderAfter = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        // проверяем, что статус заявки остался APPLIED
        List<ChangeRequest> changeRequests = orderAfter.getChangeRequests();
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(expectedChangeRequest.getId(), changeRequest.getId());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());
    }
}
