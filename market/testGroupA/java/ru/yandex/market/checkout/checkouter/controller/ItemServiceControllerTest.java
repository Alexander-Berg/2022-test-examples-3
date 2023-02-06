package ru.yandex.market.checkout.checkouter.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.request.ItemServiceReplaceRequest;
import ru.yandex.market.checkout.checkouter.storage.itemservice.ItemServiceDao;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceConfirmViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceViewModel;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.ItemServiceTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_SERVICE;

public class ItemServiceControllerTest extends AbstractWebTestBase {

    @Autowired
    private ItemServiceDao itemServiceDao;
    @Autowired
    private ItemServiceTestHelper itemServiceTestHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private DSLContext dsl;

    @Test
    public void getAllOrderItemServicesSuccess() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        OrderItem orderItem = order.getItems().iterator().next();
        ItemService itemService = orderItem.getServices().iterator().next();
        SimpleDateFormat dateFormat = new SimpleDateFormat(CheckouterDateFormats.DEFAULT);

        mockMvc.perform(
                get("/orders/{orderId}/services", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0].id").value(equalTo(itemService.getId()), Long.class))
                .andExpect(jsonPath("$.[0].serviceId").value(equalTo(itemService.getServiceId()), Long.class))
                .andExpect(jsonPath("$.[0].date").value(dateFormat.format(itemService.getDate())))
                .andExpect(jsonPath("$.[0].price").value(equalTo(itemService.getPrice()), BigDecimal.class))
                .andExpect(jsonPath("$.[0].title").value(itemService.getTitle()))
                .andExpect(jsonPath("$.[0].description").value(itemService.getDescription()))
                .andExpect(jsonPath("$.[0].status").value(itemService.getStatus().name()))
                .andExpect(jsonPath("$.[0].itemName").value(orderItem.getOfferName()));
    }

    @Test
    public void getAllOrderItemServicesNoServices() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        mockMvc.perform(
                get("/orders/{orderId}/services", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    public void shouldUpdateStatus() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        ItemServiceStatus expectedStatus = ItemServiceStatus.CANCELLED;

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/status", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": " + expectedStatus.getId() + "}"))
                .andExpect(status().isOk());
        ItemService itemService = itemServiceDao.findById(itemServiceId);

        //Убеждаемся что статус применился в БД
        assertEquals(expectedStatus.getId(), itemService.getStatus().getId());
    }

    @Test
    public void shouldNotUpdateStatus() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();
        ItemServiceStatus expectedStatus = ItemServiceStatus.CONFIRMED;

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/status", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": " + expectedStatus.getId() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateDateAndChangeStatus() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        var itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        var itemServiceId = itemService.getId();
        var newDate = new Date(LocalDateTime.ofInstant(itemService.getDate().toInstant(), ZoneId.of("Europe/Moscow"))
                .plusDays(1L)
                .atZone(ZoneId.of("Europe/Moscow"))
                .toInstant().toEpochMilli()
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat(CheckouterDateFormats.DEFAULT);

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/date", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"date\": \"" + dateFormat.format(newDate) + "\" }"))
                .andExpect(status().isOk());
        ItemService updatedItemService = itemServiceDao.findById(itemServiceId);

        //Убеждаемся что статус и дата применились в БД
        assertEquals(newDate.getTime(), updatedItemService.getDate().getTime());
        assertEquals(ItemServiceStatus.CONFIRMED, updatedItemService.getStatus());
    }

    @Test
    public void shouldConfirmItemServiceWithoutVat() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        var itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        var itemServiceId = itemService.getId();
        var newDate = new Date(LocalDateTime.ofInstant(itemService.getDate().toInstant(), ZoneId.of("Europe/Moscow"))
                .plusDays(1L)
                .atZone(ZoneId.of("Europe/Moscow"))
                .toInstant().toEpochMilli()
        );
        var newInn = "123123123";

        ItemServiceConfirmViewModel body = new ItemServiceConfirmViewModel();
        body.setDate(newDate);
        body.setInn(newInn);
        body.setVat(null);

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        assertTrue(events.getItems().stream()
                .noneMatch(e -> HistoryEventType.ITEM_SERVICE_STATUS_UPDATED == e.getType()
                        || HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED == e.getType())
        );

        itemServiceTestHelper.confirm(order.getId(), itemServiceId, body)
                .andExpect(status().isOk());
        ItemService updatedItemService = itemServiceDao.findById(itemServiceId);

        assertEquals(newDate.getTime(), updatedItemService.getDate().getTime());

        assertEquals(ItemServiceStatus.CONFIRMED, updatedItemService.getStatus());
        assertEquals(newInn, updatedItemService.getInn());
        assertEquals(VatType.NO_VAT, updatedItemService.getVat());

        //проверяем ивенты
        events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        assertTrue(events.getItems().stream()
                .anyMatch(e -> HistoryEventType.ITEM_SERVICE_STATUS_UPDATED == e.getType()));

        assertTrue(events.getItems().stream()
                .anyMatch(e -> HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED == e.getType()));
    }

    @Test
    public void shouldReplaceItemService() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        var itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        var itemServiceId = itemService.getId();
        var newDate = new Date(LocalDateTime.ofInstant(itemService.getDate().toInstant(), ZoneId.of("Europe/Moscow"))
                .plusDays(1L)
                .atZone(ZoneId.of("Europe/Moscow"))
                .toInstant().toEpochMilli()
        );
        var newInn = "123123123";

        ItemServiceReplaceRequest body = new ItemServiceReplaceRequest();
        body.setYaServiceId("333222111");
        body.setStatus(ItemServiceStatus.WAITING_SLOT);
        body.setDate(newDate);
        body.setInn(newInn);
        body.setVat(null);

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        assertTrue(events.getItems().stream()
                .noneMatch(e -> HistoryEventType.ITEM_SERVICE_STATUS_UPDATED == e.getType()
                        || HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED == e.getType())
        );

        itemServiceTestHelper.replace(order.getId(), itemServiceId, body)
                .andExpect(status().isOk());

        var updatedOrder = orderService.getOrder(order.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));
        var updatedItem = updatedOrder.getItems().iterator().next();
        var oldService = updatedItem.getServices().stream()
                .filter(service -> Objects.equals(service.getId(), itemServiceId))
                .findFirst().orElse(null);
        //старая услуга
        assertNotNull(oldService);
        assertEquals(ItemServiceStatus.CANCELLED, oldService.getStatus());
        //новая услуга
        var newService = updatedItem.getServices().stream()
                .filter(service -> !Objects.equals(service.getId(), itemServiceId))
                .findFirst().orElse(null);
        assertNotNull(newService);
        assertEquals(ItemServiceStatus.WAITING_SLOT, newService.getStatus());
        assertEquals("333222111", newService.getYaServiceId());
        assertEquals(newDate, newService.getDate());
        assertEquals(newInn, newService.getInn());
        assertEquals(VatType.NO_VAT, newService.getVat());

        //todo необходимо добавить проверку на ивенты
    }

    @Test
    public void replaceShouldThrowIfItemServiceIsCancelled() throws Exception {
        //create order with service
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        var itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        var itemServiceId = itemService.getId();

        //cancel service
        ItemServiceViewModel cancelRequest = new ItemServiceViewModel();
        cancelRequest.setStatus(ItemServiceStatus.CANCELLED);
        itemServiceTestHelper.updateOrderItemServiceStatus(order.getId(), itemServiceId, cancelRequest);

        //try to replace cancelled service and assert throws
        ItemServiceReplaceRequest body = new ItemServiceReplaceRequest();
        body.setYaServiceId("333222111");
        body.setStatus(ItemServiceStatus.WAITING_SLOT);
        body.setDate(itemService.getDate());

        itemServiceTestHelper.replace(order.getId(), itemServiceId, body)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void replaceShouldBeIdempotentWhenAlreadyReplaced() throws Exception {
        //create order with service
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        var itemService = getOnlyElement(getOnlyElement(order.getItems()).getServices());
        var itemServiceId = itemService.getId();

        //replace service
        ItemServiceReplaceRequest body = new ItemServiceReplaceRequest();
        body.setYaServiceId("333222111");
        body.setStatus(ItemServiceStatus.WAITING_SLOT);
        body.setDate(itemService.getDate());
        itemServiceTestHelper.replace(order.getId(), itemServiceId, body)
                .andExpect(status().isOk());

        //Replace service again and expect success response: 200 OK
        itemServiceTestHelper.replace(order.getId(), itemServiceId, body)
                .andExpect(status().isOk());

        //check that there are only two services: old CANCELLED and new WAITING_SLOT
        Order reloadedOrder = orderService.getOrder(order.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.ITEM_SERVICES));
        List<ItemService> itemServices = reloadedOrder.getItems()
                .stream()
                .map(OrderItem::getServices)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(itemServices, hasSize(2));
        assertThat(itemServices, hasItem(hasProperty("status", equalTo(ItemServiceStatus.CANCELLED))));
        assertThat(itemServices, hasItem(hasProperty("status", equalTo(ItemServiceStatus.WAITING_SLOT))));
    }

    @Test
    public void shouldSetTotalPriceWhenItemServiceStatusIsCompleted() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        int idOfCompletedStatus = ItemServiceStatus.COMPLETED.getId();
        BigDecimal expectedTotalCost = BigDecimal.valueOf(666.66);

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/status", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalCost\": " + expectedTotalCost + "," +
                                "\"status\": " + idOfCompletedStatus + "}"))
                .andExpect(status().isOk());
        BigDecimal cost = dsl.selectFrom(ITEM_SERVICE)
                .where(ITEM_SERVICE.ID.eq(itemServiceId)).fetchOne(ITEM_SERVICE.TOTAL_COST);
        assertEquals(cost, expectedTotalCost);
    }

    @Test
    public void shouldSetNullableTotalPriceWhenItemServiceStatusIsCompleted() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        int idOfCompletedStatus = ItemServiceStatus.COMPLETED.getId();

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/status", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": " + idOfCompletedStatus + "}"))
                .andExpect(status().isOk());
        BigDecimal cost = dsl.selectFrom(ITEM_SERVICE)
                .where(ITEM_SERVICE.ID.eq(itemServiceId)).fetchOne(ITEM_SERVICE.TOTAL_COST);
        assertNull(cost);
    }

    @Test
    public void shouldNotSetTotalPriceWhenItemServiceStatusIsAnyExcludeCompleted() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Long itemServiceId = getOnlyElement(getOnlyElement(order.getItems()).getServices()).getId();

        int idOfCancelledStatus = ItemServiceStatus.CANCELLED.getId();
        BigDecimal expectedTotalCost = BigDecimal.valueOf(666.66);

        mockMvc.perform(
                post("/orders/{orderId}/services/{itemServiceId}/status", order.getId(), itemServiceId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalCost\": " + expectedTotalCost + "," +
                                "\"status\": " + idOfCancelledStatus + "}"))
                .andExpect(status().isOk());

        BigDecimal cost = dsl.selectFrom(ITEM_SERVICE)
                .where(ITEM_SERVICE.ID.eq(itemServiceId)).fetchOne(ITEM_SERVICE.TOTAL_COST);
        assertNull(cost);
    }
}
