package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.recipient.RecipientEditRequest;
import ru.yandex.market.checkout.helpers.ChangeRequestStatusHelper;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class RecipientEditTest extends AbstractWebTestBase {

    private static final String PHONE = "+ 7-999-999-99-99";

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private ChangeRequestStatusHelper changeRequestStatusHelper;

    private YandexMarketDeliveryHelper.MarDoOrderBuilder marDoOrderBuilder;
    private RecipientPerson expectedRecipientPerson;
    private OrderEditRequest orderEditRequest;

    @BeforeEach
    public void setUp() throws Exception {
        expectedRecipientPerson = new RecipientPerson("Ivan", null, "Ivanov");

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(expectedRecipientPerson);
        recipientEditRequest.setPhone(PHONE);

        orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        marDoOrderBuilder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE);
    }

    @Test
    public void shouldCreateRecipientChangeRequest() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));

        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        assertEquals(ChangeRequestStatus.NEW, recipientChangeRequest.getStatus());
        assertEquals(ChangeRequestType.RECIPIENT, recipientChangeRequest.getType());
        RecipientChangeRequestPayload recipientChangeRequestPayload =
                (RecipientChangeRequestPayload) recipientChangeRequest.getPayload();

        assertEquals(expectedRecipientPerson, recipientChangeRequestPayload.getPerson());
        assertEquals(PHONE, recipientChangeRequestPayload.getPhone());
    }

    @Test
    public void shouldApplyRecipientChangeRequest() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.APPLIED);

        // проверяем, что получатель обновился
        Order orderAfter = client.getOrder(order.getId(), ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getUid());

        assertEquals(PHONE, orderAfter.getDelivery().getRecipient().getPhone());
        assertEquals(expectedRecipientPerson.getFirstName(),
                orderAfter.getDelivery().getRecipient().getPerson().getFirstName());
        assertEquals(expectedRecipientPerson.getMiddleName(),
                orderAfter.getDelivery().getRecipient().getPerson().getMiddleName());
        assertEquals(expectedRecipientPerson.getLastName(),
                orderAfter.getDelivery().getRecipient().getPerson().getLastName());
    }

    @Test
    public void shouldApplyChangeRequestForPostOrder() {
        Parameters parameters = marDoOrderBuilder
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.APPLIED);

        // проверяем, что получатель обновился
        Order orderAfter = client.getOrder(order.getId(), ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getUid());

        assertEquals(PHONE, orderAfter.getDelivery().getRecipient().getPhone());
        assertEquals(expectedRecipientPerson.getFirstName(),
                orderAfter.getDelivery().getRecipient().getPerson().getFirstName());
        assertEquals(expectedRecipientPerson.getMiddleName(),
                orderAfter.getDelivery().getRecipient().getPerson().getMiddleName());
        assertEquals(expectedRecipientPerson.getLastName(),
                orderAfter.getDelivery().getRecipient().getPerson().getLastName());
    }

    @Test
    public void shouldApplyChangeRequestForPickupOrder() {
        Order order = marDoOrderBuilder
                .withDeliveryType(DeliveryType.PICKUP)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.APPLIED);

        // проверяем, что получатель обновился
        Order orderAfter = client.getOrder(order.getId(), ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getUid());

        assertEquals(PHONE, orderAfter.getDelivery().getRecipient().getPhone());
        assertEquals(expectedRecipientPerson.getFirstName(),
                orderAfter.getDelivery().getRecipient().getPerson().getFirstName());
        assertEquals(expectedRecipientPerson.getMiddleName(),
                orderAfter.getDelivery().getRecipient().getPerson().getMiddleName());
        assertEquals(expectedRecipientPerson.getLastName(),
                orderAfter.getDelivery().getRecipient().getPerson().getLastName());
    }

    @Test
    public void shouldSuccessProcessingDuplicatingRequest() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        // перевести в процессинг
        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.PROCESSING);

        // попытаться перевести в процессинг еще раз
        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.PROCESSING);
        // assert, что метод отработает без exception-a
    }

    @Test
    public void shouldSuccessChangeStatusNewToApplied() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        // перевести в APPLIED
        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.APPLIED);
        // assert, что метод отработает без exception-a
    }

    @Test
    public void changeOnlyRecipientName() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, UNPAID);

        String expectedPhone = order.getDelivery().getRecipient().getPhone();

        expectedRecipientPerson = new RecipientPerson("Ivan", null, "Ivanov");

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(expectedRecipientPerson);

        orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        // создать заявку на изменение данных
        // т.к. заказ еще не оплачен - то заявка перейдет в статус APPLIED
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();
        assertNull(((RecipientChangeRequestPayload) recipientChangeRequest.getPayload()).getPhone());
        assertEquals(ChangeRequestStatus.APPLIED, recipientChangeRequest.getStatus());

        Order orderAfter = client.getOrder(order.getId(), ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getUid(),
                false, singletonList(BLUE), EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));
        assertNull(((RecipientChangeRequestPayload) orderAfter.getChangeRequests().iterator().next().getPayload())
                .getPhone());

        // проверяем, что номер телефона не изменился.
        assertEquals(expectedPhone, orderAfter.getDelivery().getRecipient().getPhone());
    }

    @Test
    public void syncApplyChangeRequestForDropshipWithoutDeliveryTrack() {
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        assertEquals(ChangeRequestStatus.APPLIED, recipientChangeRequest.getStatus());
    }


    @Test
    public void fillDeliveryRecipientInEvents() {
        Parameters parameters = marDoOrderBuilder.buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        ChangeRequest recipientChangeRequest = changeRequests.iterator().next();

        changeRequestStatusHelper.proceedToStatus(order, recipientChangeRequest, ChangeRequestStatus.APPLIED);

        // проверяем, что получатель обновился в сгенерированном event-е
        CheckouterOrderHistoryEventsApi orderHistoryEventsApi = client.orderHistoryEvents();

        OrderHistoryEvents events = orderHistoryEventsApi.getOrderHistoryEvents(
                0,
                10,
                EnumSet.of(HistoryEventType.ORDER_DELIVERY_UPDATED),
                false,
                null,
                OrderFilter.builder().setRgb(Color.BLUE).build());

        int size = events.getContent().size();
        OrderHistoryEvent orderHistoryEvent = events.getContent().stream().skip(size - 1).findFirst().get();

        assertNotNull(orderHistoryEvent.getOrderBefore().getDelivery().getRecipient());
        assertNotNull(orderHistoryEvent.getOrderAfter().getDelivery().getRecipient());

        assertEquals(expectedRecipientPerson,
                orderHistoryEvent.getOrderAfter().getDelivery().getRecipient().getPerson());
        assertEquals(order.getDelivery().getRecipient(),
                orderHistoryEvent.getOrderBefore().getDelivery().getRecipient());
    }
}
