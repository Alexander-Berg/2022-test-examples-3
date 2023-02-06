package ru.yandex.market.checkout.checkouter.client;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.time.DateUtils;
import org.apache.curator.shaded.com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceNotFoundException;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.ItemServiceReplaceResponse;
import ru.yandex.market.checkout.checkouter.storage.itemservice.ItemServiceDao;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.FoundOffer;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CheckoutClientItemServiceTest extends AbstractWebTestBase {

    @Autowired
    private ItemServiceDao itemServiceDao;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;

    public static Stream<Arguments> allowedClientInfosForUpdateItemService() {
        return Stream.of(
                new RequestClientInfo(ClientRole.SHOP, 774L),
                new RequestClientInfo(ClientRole.SYSTEM, null)
        ).map(Arguments::of);
    }

    public static Stream<Arguments> notAllowedClientInfosForUpdateItemService() {
        return Stream.of(
                new RequestClientInfo(ClientRole.USER, BuyerProvider.UID),
                new RequestClientInfo(ClientRole.REFEREE, BuyerProvider.UID),
                new RequestClientInfo(ClientRole.SHOP_USER, BuyerProvider.UID, WhiteParametersProvider.WHITE_SHOP_ID),
                new RequestClientInfo(ClientRole.CALL_CENTER_OPERATOR, BuyerProvider.UID),
                new RequestClientInfo(ClientRole.CRM_ROBOT, BuyerProvider.UID),
                new RequestClientInfo(ClientRole.ANTIFRAUD_ROBOT, BuyerProvider.UID),
                new RequestClientInfo(ClientRole.BUSINESS, OrderProvider.BUSINESS_ID),
                new RequestClientInfo(ClientRole.BUSINESS_USER, BuyerProvider.UID, null,
                        WhiteParametersProvider.WHITE_BUSINESS_ID),
                new RequestClientInfo(ClientRole.ANTIFRAUD_BNPL, BuyerProvider.UID)
        ).map(Arguments::of);
    }

    public static Stream<Arguments> notAllowedClientInfosForUpdateItemServiceInConfirm() {
        return Stream.concat(notAllowedClientInfosForUpdateItemService(),
                Stream.of(
                        new RequestClientInfo(ClientRole.SHOP, 774L)
                ).map(Arguments::of));
    }

    @Test
    public void checkoutShouldSaveItemServiceInterval() {
        final Order order = createOrderWithItemService();
        ItemService itemService = Iterables.getOnlyElement(Iterables.getOnlyElement(order.getItems()).getServices());

        assertNotNull(itemService.getFromTime());
        assertNotNull(itemService.getToTime());
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void canUpdateItemServiceStatus(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        ItemServiceStatus expectedStatus = ItemServiceStatus.CANCELLED;
        client.updateItemServiceStatus(
                order.getId(),
                itemServiceId,
                clientInfo,
                expectedStatus
        );

        ItemService updatedItemService = getFirstItemService(order.getId());
        assertEquals(itemServiceId, updatedItemService.getId());
        assertEquals(expectedStatus, updatedItemService.getStatus());
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void canUpdateItemServiceDate(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        Date expectedDate = truncate(new Date(), Calendar.SECOND);
        client.updateItemServiceDate(
                order.getId(),
                itemServiceId,
                clientInfo,
                expectedDate
        );

        ItemService updatedItemService = getFirstItemService(order.getId());
        assertEquals(itemServiceId, updatedItemService.getId());
        assertEquals(expectedDate, updatedItemService.getDate());
    }

    private ItemService getFirstItemService(long orderId) {
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        OrderRequest orderRequest = OrderRequest.builder(orderId)
                .withPartials(Set.of(OptionalOrderPart.ITEM_SERVICES))
                .build();
        Order foundOrder = client.getOrder(requestClientInfo, orderRequest);
        return getOnlyElement(getOnlyElement(foundOrder.getItems()).getServices());
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void updateItemServiceStatusShouldReturnBadRequestWhenOrderIdIsBad(RequestClientInfo clientInfo) {
        long badId = 123456789L;
        ItemServiceStatus targetStatus = ItemServiceStatus.CANCELLED;

        Assertions.assertThrows(OrderNotFoundException.class, () ->
                client.updateItemServiceStatus(
                        badId,
                        badId,
                        clientInfo,
                        targetStatus
                ));
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void updateItemServiceDateShouldReturnBadRequestWhenOrderIdIsBad(RequestClientInfo clientInfo) {
        long badId = 123456789L;
        Date expectedDate = truncate(new Date(), Calendar.SECOND);

        Assertions.assertThrows(OrderNotFoundException.class, () ->
                client.updateItemServiceDate(
                        badId,
                        badId,
                        clientInfo,
                        expectedDate
                ));
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void updateItemServiceStatusShouldReturnBadRequestWhenItemServiceIdIsBad(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        long badId = 123456789L;
        ItemServiceStatus targetStatus = ItemServiceStatus.CANCELLED;

        Assertions.assertThrows(ItemServiceNotFoundException.class, () ->
                client.updateItemServiceStatus(
                        order.getId(),
                        badId,
                        clientInfo,
                        targetStatus
                ));
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void updateItemServiceDateShouldReturnBadRequestWhenItemServiceIdIsBad(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        long badId = 123456789L;
        Date expectedDate = truncate(new Date(), Calendar.SECOND);

        Assertions.assertThrows(ItemServiceNotFoundException.class, () ->
                client.updateItemServiceDate(
                        order.getId(),
                        badId,
                        clientInfo,
                        expectedDate
                ));
    }

    @ParameterizedTest
    @MethodSource("allowedClientInfosForUpdateItemService")
    public void updateItemServiceDateShouldReturnBadRequestWhenDateIsInThePast(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        Date expectedDate = DateUtils.addDays(truncate(new Date(), Calendar.SECOND), -2);

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                client.updateItemServiceDate(
                        order.getId(),
                        itemServiceId,
                        clientInfo,
                        expectedDate
                ));
    }

    @ParameterizedTest
    @MethodSource("notAllowedClientInfosForUpdateItemService")
    public void updateItemServiceStatusShouldReturnBadRequestWhenRoleIsNotAllowed(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        ItemServiceStatus expectedStatus = ItemServiceStatus.CANCELLED;

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                client.updateItemServiceStatus(
                        order.getId(),
                        itemServiceId,
                        clientInfo,
                        expectedStatus
                ));
    }

    @ParameterizedTest
    @MethodSource("notAllowedClientInfosForUpdateItemService")
    public void updateItemServiceDateShouldReturnBadRequestWhenRoleIsNotAllowed(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        Date expectedDate = truncate(new Date(), Calendar.SECOND);

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                client.updateItemServiceDate(
                        order.getId(),
                        itemServiceId,
                        clientInfo,
                        expectedDate
                ));
    }

    private Date addTwoDays(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 2);
        return c.getTime();
    }

    @ParameterizedTest
    @MethodSource("notAllowedClientInfosForUpdateItemServiceInConfirm")
    public void noUpdateRightInConfirmation(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                client.confirmService(
                        order.getId(),
                        itemServiceId,
                        clientInfo,
                        new Date(),
                        "123"
                ));
    }

    @Test
    public void replaceItemServiceTest() throws IOException {
        final Order order = createOrderWithItemService();
        OrderItem orderItem = getOnlyElement(order.getItems());
        ItemService itemService = getLast(orderItem.getServices());

        Date dateBefore = itemService.getDate();
        long nowWithMils = new Date().getTime();
        nowWithMils -= nowWithMils % 1000;
        Date expectedDate = addTwoDays(new Date(nowWithMils));

        mockSearchReport(order, orderItem);

        String inn = "123";
        ItemServiceReplaceResponse replaceResponse = client.replaceService(
                order.getId(),
                itemService.getId(),
                new RequestClientInfo(ClientRole.SYSTEM, BuyerProvider.UID),
                expectedDate,
                inn,
                "333222111",
                ItemServiceStatus.NEW
        );

        assertNotNull(replaceResponse);
        assertNotNull(replaceResponse.getId());

        Order orderServiceOrder = orderService.getOrder(order.getId());
        itemServiceDao.loadToOrders(Collections.singletonList(orderServiceOrder));

        assertEquals(1, orderServiceOrder.getItems().size());
        Set<ItemService> updatedItemServices = orderServiceOrder.getItems().iterator().next().getServices();
        Set<ItemService> itemServices = order.getItems().iterator().next().getServices();

        assertEquals(2, updatedItemServices.size());
        assertEquals(1, itemServices.size());

        Map<Boolean, List<ItemService>> updatedServices =
                updatedItemServices.stream().collect(Collectors.partitioningBy(it -> inn.equals(it.getInn())));

        assertEquals(1, updatedServices.get(true).size());
        assertEquals(1, updatedServices.get(false).size());

        assertEquals(replaceResponse.getId(), updatedServices.get(true).get(0).getId());

        assertEquals(ItemServiceStatus.WAITING_SLOT, updatedServices.get(true).get(0).getStatus());
        assertEquals(ItemServiceStatus.CANCELLED, updatedServices.get(false).get(0).getStatus());

        assertEquals(expectedDate, updatedServices.get(true).get(0).getDate());
        assertEquals(dateBefore, updatedServices.get(false).get(0).getDate());
    }

    @Test
    public void replaceItemServiceSwapDateAndStatusTest() throws IOException {
        final Order order = createOrderWithItemService();
        OrderItem orderItem = getOnlyElement(order.getItems());
        ItemService itemService = getLast(orderItem.getServices());

        Date dateBefore = itemService.getDate();

        mockSearchReport(order, orderItem);

        String inn = "123";
        ItemServiceReplaceResponse replaceResponse = client.replaceService(
                order.getId(),
                itemService.getId(),
                new RequestClientInfo(ClientRole.SYSTEM, BuyerProvider.UID),
                null,
                inn,
                "333222111",
                null
        );

        assertNotNull(replaceResponse);
        assertNotNull(replaceResponse.getId());

        Order orderServiceOrder = orderService.getOrder(order.getId());
        itemServiceDao.loadToOrders(Collections.singletonList(orderServiceOrder));

        Set<ItemService> updatedItemServices = orderServiceOrder.getItems().iterator().next().getServices();

        Map<Boolean, List<ItemService>> updatedServices =
                updatedItemServices.stream().collect(Collectors.partitioningBy(it -> inn.equals(it.getInn())));

        assertEquals(1, updatedServices.get(true).size());
        assertEquals(1, updatedServices.get(false).size());

        assertNotEquals(itemService.getStatus(), updatedServices.get(true).get(0).getStatus()); // отменный в NEW
        assertEquals(ItemServiceStatus.WAITING_SLOT, updatedServices.get(true).get(0).getStatus());
        assertEquals(ItemServiceStatus.CANCELLED, updatedServices.get(false).get(0).getStatus());

        assertEquals(dateBefore, updatedServices.get(true).get(0).getDate());
        assertEquals(dateBefore, updatedServices.get(false).get(0).getDate());
    }

    @Test
    public void replaceItemServiceCheckHistoryTest() throws IOException {
        final Order order = createOrderWithItemService();
        OrderItem orderItem = getOnlyElement(order.getItems());
        ItemService itemService = getLast(orderItem.getServices());

        mockSearchReport(order, orderItem);

        String inn = "123";
        ItemServiceReplaceResponse replaceResponse = client.replaceService(
                order.getId(),
                itemService.getId(),
                new RequestClientInfo(ClientRole.SYSTEM, BuyerProvider.UID),
                null,
                inn,
                "333222111",
                null
        );

        List<OrderHistoryEvent> eventStatuses = orderHistoryEventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEM_SERVICE_STATUS_UPDATED)
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ITEM_SERVICE_STATUS_UPDATED)
                .collect(Collectors.toList());

        assertEquals(1, eventStatuses.size());
        assertEquals(2, eventStatuses.get(0).getOrderBefore().getItems().iterator().next().getServices().size());
    }

    private void mockSearchReport(Order order, OrderItem orderItem) throws IOException {
        Parameters parameters = new Parameters(order);
        FoundOffer foundOffer = FoundOfferBuilder.createFrom(orderItem).build();
        parameters.getReportParameters().setOffers(List.of(foundOffer));

        orderCreateHelper.initializeMock(parameters);
    }

    @ParameterizedTest
    @MethodSource("notAllowedClientInfosForUpdateItemServiceInConfirm")
    public void noUpdateRightInReplace(RequestClientInfo clientInfo) {
        final Order order = createOrderWithItemService();
        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class, () ->
                client.replaceService(
                        order.getId(),
                        itemServiceId,
                        clientInfo,
                        new Date(),
                        "123",
                        "1",
                        ItemServiceStatus.CANCELLED
                ));
    }

    private Order createOrderWithItemService() {
        return orderServiceHelper.createPostOrder(orderBeforeSave ->
                Iterables.getOnlyElement(orderBeforeSave.getItems())
                        .addService(ItemServiceProvider.defaultItemService())
        );
    }

    @Test
    public void confirmItemServiceTest() {
        final Order order = createOrderWithItemService();
        OrderItem orderItem = getOnlyElement(order.getItems());
        ItemService itemService = getLast(orderItem.getServices());

        Date dateBefore = itemService.getDate();
        long nowWithMils = new Date().getTime();
        nowWithMils -= nowWithMils % 1000;
        Date expectedDate = addTwoDays(new Date(nowWithMils));

        client.confirmService(
                order.getId(),
                itemService.getId(),
                new RequestClientInfo(ClientRole.SYSTEM, BuyerProvider.UID),
                expectedDate,
                "123"
        );

        Order orderServiceOrder = orderService.getOrder(order.getId());
        itemServiceDao.loadToOrders(Collections.singletonList(orderServiceOrder));
        ItemService updatedService = orderServiceOrder.getItems().iterator().next().getServices().iterator().next();
        ItemService prevService = order.getItems().iterator().next().getServices().iterator().next();

        assertEquals("123", updatedService.getInn());
        assertEquals(2, updatedService.getCount()); // потому что указали при создании (хотя item ов 1 шт.)
        assertEquals(ItemServiceStatus.CONFIRMED, updatedService.getStatus());
        assertNotEquals(prevService.getDate(), updatedService.getDate());

        assertEquals(expectedDate, updatedService.getDate());

        OrderHistoryEvent eventDate = orderHistoryEventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED)
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED)
                .findFirst()
                .orElse(null);

        assertNotNull(eventDate);

        assertEquals(updatedService.getId(), eventDate.getItemServiceId());

        Set<ItemService> servicesBeforeDate = eventDate.getOrderBefore().getItem(orderItem.getId()).getServices();
        assertThat(servicesBeforeDate, hasSize(1));
        Set<ItemService> servicesAfterDate = eventDate.getOrderAfter().getItem(orderItem.getId()).getServices();
        assertThat(servicesAfterDate, hasSize(1));
        ItemService serviceBeforeDate = Iterables.getOnlyElement(servicesBeforeDate);
        ItemService serviceAfterDate = Iterables.getOnlyElement(servicesAfterDate);

        assertEquals(dateBefore, serviceBeforeDate.getDate());
        assertEquals(expectedDate, serviceAfterDate.getDate());

        OrderHistoryEvent eventStatus = orderHistoryEventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEM_SERVICE_STATUS_UPDATED)
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ITEM_SERVICE_STATUS_UPDATED)
                .findFirst()
                .orElse(null);

        assertNotNull(eventStatus);

        assertEquals(updatedService.getId(), eventStatus.getItemServiceId());

        Set<ItemService> servicesBeforeStatus = eventStatus.getOrderBefore().getItem(orderItem.getId()).getServices();
        assertThat(servicesBeforeStatus, hasSize(1));
        Set<ItemService> servicesAfterStatus = eventStatus.getOrderAfter().getItem(orderItem.getId()).getServices();
        assertThat(servicesAfterStatus, hasSize(1));
        ItemService serviceBeforeStatus = Iterables.getOnlyElement(servicesBeforeStatus);
        ItemService serviceAfterStatus = Iterables.getOnlyElement(servicesAfterStatus);

        assertNotEquals(ItemServiceStatus.CONFIRMED, serviceBeforeStatus.getStatus());
        assertEquals(ItemServiceStatus.CONFIRMED, serviceAfterStatus.getStatus());

    }
}
