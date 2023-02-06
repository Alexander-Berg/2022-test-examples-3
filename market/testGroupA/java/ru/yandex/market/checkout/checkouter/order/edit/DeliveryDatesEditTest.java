package ru.yandex.market.checkout.checkouter.order.edit;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDatesEditOptions;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReasonWithDetails;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryDatesEditException;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.MethodOfChange;
import ru.yandex.market.checkout.checkouter.order.changerequest.deliverydatesoptions.DeliveryDatesEditOptionsRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.itemservice.DateChangeParam;
import ru.yandex.market.checkout.checkouter.request.PagedOrderEventsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_DATE_CHANGE_NOTIFICATION;

public class DeliveryDatesEditTest extends AbstractWebTestBase {

    @Value("${market.checkouter.deliveryDatesEditOptions.countTotal:3}")
    private Integer countTotal;
    @Value("${market.checkouter.deliveryDatesEditOptions.daysMax:3}")
    private Integer daysMax;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    @Qualifier("yaUslugiObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private WireMockServer yaUslugiMock;
    @Autowired
    private DeliveryDatesEditOptionsRequestProcessor deliveryDatesEditOptionsRequestProcessor;

    @Mock
    private DeliveryServiceInfoService deliveryServiceInfoService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(deliveryServiceInfoService.getPossibleOrderChanges(Mockito.anyLong()))
                .thenReturn(List.of(new PossibleOrderChange(
                        ChangeRequestType.DELIVERY_DATES,
                        MethodOfChange.PARTNER_API,
                        null,
                        48)));
    }

    @Test
    public void editDeliveryDatesTest() {
        editDeliveryDatesTest((orderId, orderEditRequest) -> client.editOrder(
                orderId, ClientRole.SHOP, OrderProvider.SHOP_ID, singletonList(WHITE), orderEditRequest));
    }

    @Test
    public void editDeliveryDatesWithMultiClientIdTest() {
        editDeliveryDatesTest((orderId, orderEditRequest) -> client.editOrder(
                orderId,
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(OrderProvider.SHOP_ID, OrderProvider.FAKE_SHOP_ID))
                        .build(),
                singletonList(WHITE),
                orderEditRequest));
    }

    private void editDeliveryDatesTest(BiFunction<Long, OrderEditRequest, List<ChangeRequest>> editOrder) {
        var order = createDsbsOrder();

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var changeRequests = editOrder.apply(order.getId(), orderEditRequest);

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime() + TimeUnit.DAYS.toMillis(postponeDays)
        );
    }

    @Test
    public void editDeliveryDatesWithTooLargeDaysTest() {
        var order = createDsbsOrder();

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax + 1;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var orderId = order.getId();
        try {
            client.editOrder(
                    orderId,
                    ClientRole.SHOP,
                    OrderProvider.SHOP_ID,
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown DeliveryDatesEditException");
        } catch (DeliveryDatesEditException e) {
            Assertions.assertEquals(DeliveryDatesEditException.DAYS_COUNT_EXCEEDED, e.getCode());
            Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail("Should be thrown DeliveryDatesEditException");
        }


        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesTooMuchTriesTest() {
        var order = createDsbsOrder();

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;

        //Проводим максимально возможное количество переносов даты доставки
        for (int i = 0; i < countTotal; i++) {
            var fromDate = convertToLocalDate(deliveryBefore
                    .getDeliveryDates()
                    .getFromDate())
                    .plusDays(postponeDays);
            var toDate = convertToLocalDate(deliveryBefore
                    .getDeliveryDates()
                    .getToDate())
                    .plusDays(postponeDays);

            // Изменяем дату доставки заказа
            var orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                    .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                    .build());

            var changeRequests = client.editOrder(
                    order.getId(),
                    ClientRole.SHOP,
                    OrderProvider.SHOP_ID,
                    singletonList(WHITE),
                    orderEditRequest
            );

            order = orderService.getOrder(order.getId());

            var deliveryAfter = order.getDelivery();

            Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
            Assertions.assertEquals(
                    deliveryAfter
                            .getDeliveryDates()
                            .getToDate()
                            .getTime(),
                    deliveryBefore
                            .getDeliveryDates()
                            .getToDate()
                            .getTime() + TimeUnit.DAYS.toMillis(postponeDays)
            );
            deliveryBefore = deliveryAfter;
        }

        //Выполняем еще одну попытку переноса даты доставки
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var orderId = order.getId();
        try {
            client.editOrder(
                    orderId,
                    ClientRole.SHOP,
                    OrderProvider.SHOP_ID,
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown DeliveryDatesEditException");
        } catch (DeliveryDatesEditException e) {
            Assertions.assertEquals(DeliveryDatesEditException.EDIT_COUNT_EXCEEDED, e.getCode());
            Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail("Should be thrown DeliveryDatesEditException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesByUserTest() {
        var order = createDsbsOrder();

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        try {
            client.editOrder(
                    order.getId(),
                    ClientRole.USER,
                    order.getBuyer().getUid(),
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown ErrorCodeException");
        } catch (ErrorCodeException e) {
            Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Should be thrown ErrorCodeException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesWithTheSameDates() {
        var order = createDsbsOrder();

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        var orderEditOptionsResponseBefore = client.getOrderEditOptions(order.getId(), ClientRole.SYSTEM, 0L,
                singletonList(WHITE), orderEditOptionsRequest);


        var deliveryBefore = order.getDelivery();

        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate());
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate());

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(
                        deliveryBefore.getDeliveryDates().getFromTime(),
                        deliveryBefore.getDeliveryDates().getToTime()
                ))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        try {
            client.editOrder(
                    order.getId(),
                    ClientRole.USER,
                    order.getBuyer().getUid(),
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown ErrorCodeException");
        } catch (ErrorCodeException e) {
            Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Should be thrown ErrorCodeException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();


        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );

        var orderEditOptionsResponseAfter = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                0L,
                singletonList(WHITE),
                orderEditOptionsRequest);

        //Проверяем что для заказа не изменилось ограничение по количеству переносов
        Assertions.assertEquals(
                orderEditOptionsResponseBefore.getDeliveryDatesEditOptions().getCountRemain(),
                orderEditOptionsResponseAfter.getDeliveryDatesEditOptions().getCountRemain()
        );
    }

    @Test
    public void editDeliveryDatesForOrderInDeliveryStatus() {
        var order = createDsbsOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SHOP,
                OrderProvider.SHOP_ID,
                singletonList(WHITE),
                orderEditRequest
        );

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime() + TimeUnit.DAYS.toMillis(postponeDays)
        );
    }

    @Test
    public void editDeliveryDatesForOrderInDeliveredStatus() {
        var order = createDsbsOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        try {
            client.editOrder(
                    order.getId(),
                    ClientRole.SHOP,
                    order.getShopId(),
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown ErrorCodeException");
        } catch (ErrorCodeException e) {
            Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Should be thrown ErrorCodeException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesForOrderInPickupStatus() {
        var order = createPickUpDsbsOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);
        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        try {
            client.editOrder(
                    order.getId(),
                    ClientRole.SHOP,
                    order.getShopId(),
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown ErrorCodeException");
        } catch (ErrorCodeException e) {
            Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Should be thrown ErrorCodeException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesForOrderInUnpaidStatus() {
        var order = createDsbsOrder();
        order.setStatus(OrderStatus.UNPAID);
        order.setSubstatus(OrderSubstatus.WAITING_BANK_DECISION);
        final Order o = order;
        o.setInternalDeliveryId(99L);
        transactionTemplate.execute(ts -> {
            orderWritingDao.updateOrderStatus(o, ClientInfo.SYSTEM, HistoryEventType.ORDER_STATUS_UPDATED,
                    HistoryEventReasonWithDetails.withNullableFields());
            return null;
        });

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        try {
            client.editOrder(
                    order.getId(),
                    ClientRole.SHOP,
                    order.getShopId(),
                    singletonList(WHITE),
                    orderEditRequest
            );
            Assertions.fail("Should be thrown ErrorCodeException");
        } catch (ErrorCodeException e) {
            Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Should be thrown ErrorCodeException");
        }

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime()
        );
    }

    @Test
    public void editDeliveryDatesForOrderInPendingStatus() {
        var order = createDsbsOrder();
        order.setStatus(OrderStatus.PENDING);
        order.setSubstatus(OrderSubstatus.ASYNC_PROCESSING);
        final Order o = order;
        o.setInternalDeliveryId(99L);
        transactionTemplate.execute(ts -> {
            orderWritingDao.updateOrderStatus(o, ClientInfo.SYSTEM, HistoryEventType.ORDER_STATUS_UPDATED,
                    HistoryEventReasonWithDetails.withNullableFields());
            return null;
        });

        var deliveryBefore = order.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SHOP,
                OrderProvider.SHOP_ID,
                singletonList(WHITE),
                orderEditRequest
        );

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();

        Assertions.assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime() + TimeUnit.DAYS.toMillis(postponeDays)
        );
    }

    @Test
    public void editDeliveryDatesWhenOrderHasServiceTest() throws Exception {
        yaUslugiMock.stubFor(
                put(urlPathMatching("/ydo/api/market_partner_orders/services/.*")).willReturn(ok("{}")));

        var parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.addItemService(b ->
                b.date(Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(getClock().getZone())
                        .toInstant())));
        parameters.getOrder()
                .getItems().iterator().next()
                .getServices().iterator().next()
                .setStatus(ItemServiceStatus.CONFIRMED);
        var orderBefore = orderCreateHelper.createOrder(parameters);
        var itemServiceBefore = orderBefore.getItems().iterator().next().getServices().iterator().next();
        var deliveryBefore = orderBefore.getDelivery();

        Assertions.assertEquals(deliveryBefore.getDeliveryDates().getToDate(), itemServiceBefore.getDate());

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        orderStatusHelper.proceedOrderToStatus(orderBefore, OrderStatus.PROCESSING);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(orderBefore.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        var changeRequests = client.editOrder(
                orderBefore.getId(),
                ClientRole.SHOP,
                orderBefore.getShopId(),
                singletonList(WHITE),
                orderEditRequest
        );

        var orderAfter = orderService.getOrder(orderBefore.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.ITEM_SERVICES));
        var itemServiceAfter = orderAfter.getItems().iterator().next().getServices().iterator().next();
        var deliveryAfter = orderAfter.getDelivery();

        Assertions.assertEquals(itemServiceBefore.getDate(), itemServiceAfter.getDate());
        Assertions.assertNotEquals(ItemServiceStatus.WAITING_SLOT, itemServiceAfter.getStatus());

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                ITEM_SERVICE_DATE_CHANGE_NOTIFICATION, itemServiceAfter.getId());
        Assertions.assertEquals(1, result.size());
        QueuedCall qc = result.iterator().next();
        Assertions.assertNotNull(qc.getPayload());

        DateChangeParam dateChangeParam = objectMapper.readValue(qc.getPayload(), DateChangeParam.class);
        Date newItemServiceDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                .parse(dateChangeParam.getChangeDate());
        Assertions.assertEquals(deliveryAfter.getDeliveryDates().getToDate(), newItemServiceDate);

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_DATE_CHANGE_NOTIFICATION);
        ItemService executedService = orderService.getOrder(orderBefore.getId(), ClientInfo.SYSTEM,
                        Set.of(OptionalOrderPart.ITEM_SERVICES))
                .getItems().iterator().next().getServices().iterator().next();

        Assertions.assertEquals(ItemServiceStatus.WAITING_SLOT, executedService.getStatus());
    }

    @Test
    public void editEstimatedDeliveryTest() {
        Parameters parameters = WhiteParametersProvider.applyTo(WhiteParametersProvider.defaultWhiteParameters());
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        var orderBefore = orderCreateHelper.createOrder(parameters);

        assertTrue(orderBefore.getDelivery().getEstimated());

        DeliveryDatesEditOptions editOptionsBefore =
                deliveryDatesEditOptionsRequestProcessor.getDeliveryDatesEditOptions(orderBefore);

        assertEquals(4, editOptionsBefore.getCountRemain());

        var deliveryBefore = orderBefore.getDelivery();

        var postponeDays = daysMax;
        var fromDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryBefore
                .getDeliveryDates()
                .getToDate())
                .plusDays(postponeDays);

        orderBefore = orderStatusHelper.proceedOrderToStatus(orderBefore, OrderStatus.PROCESSING);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(orderBefore.getDelivery().getDeliveryServiceId())
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());

        client.editOrder(
                orderBefore.getId(),
                ClientRole.SHOP,
                orderBefore.getShopId(),
                singletonList(WHITE),
                orderEditRequest
        );

        var orderAfter = orderService.getOrder(orderBefore.getId(), ClientInfo.SYSTEM);

        DeliveryDatesEditOptions editOptionsAfter =
                deliveryDatesEditOptionsRequestProcessor.getDeliveryDatesEditOptions(orderBefore);

        assertEquals(3, editOptionsAfter.getCountRemain());

        Delivery deliveryAfter = orderAfter.getDelivery();
        assertNull(deliveryAfter.getEstimated());

        Assertions.assertEquals(
                deliveryAfter
                        .getDeliveryDates()
                        .getToDate()
                        .getTime(),
                deliveryBefore
                        .getDeliveryDates()
                        .getToDate()
                        .getTime() + TimeUnit.DAYS.toMillis(postponeDays));

        // проверим, что в выгрузке будет изменение по estimated (нужно будет для анализа в триггерной платформе)
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, orderBefore.getUid());

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(clientInfo,
                PagedOrderEventsRequest.builder(orderBefore.getId()).build());

        // крайнее событие проверяем, что там произошла смена и что там есть estimated
        Optional<OrderHistoryEvent> historyEvent =
                events.getItems().stream().filter(it -> it.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                        .max(Comparator.comparing(OrderHistoryEvent::getId));

        assertTrue(historyEvent.isPresent());

        assertTrue(historyEvent.get().getOrderBefore().getDelivery().getEstimated());
        assertNull(historyEvent.get().getOrderAfter().getDelivery().getEstimated());
    }

    private LocalDate convertToLocalDate(Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private Order createDsbsOrder() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        return orderCreateHelper.createOrder(parameters);
    }

    private Order createPickUpDsbsOrder() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        return orderCreateHelper.createOrder(parameters);
    }

}
