package ru.yandex.market.checkout.pushapi.web;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderStatusHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderStatusParameters;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderStatusTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(OrderStatusTest.class);

    @Autowired
    private PushApiOrderStatusHelper orderStatusHelper;
    @Autowired
    private SettingsService settingsService;

    @Test
    public void orderStatusTest() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.setEacc("123456");
        orderStatusForActions(parameters).andExpect(status().isOk());

        List<ServeEvent> serveEvents = orderStatusHelper.getServeEvents();
        assertThat(serveEvents, Matchers.hasSize(1));

        ServeEvent event = Iterables.getOnlyElement(serveEvents);

        String token = ServeEventUtils.extractTokenParameter(event);
        Assertions.assertEquals(SettingsProvider.DEFAULT_TOKEN, token);

        byte[] body = event.getRequest().getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> object = (Map<Object, Object>) objectMapper.readValue(body, Map.class);
        Assertions.assertNotNull(JsonUtil.getByPath(object, "/order/buyer"));
        Assertions.assertNotNull(JsonUtil.getByPath(object, "/order/notes"));
    }

    @Test
    public void shouldHideBuyerInPushOrderStatus() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.getOrderChange().setStatus(OrderStatus.UNPAID);
        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        List<ServeEvent> events = orderStatusHelper.getServeEvents();
        ServeEvent event = Iterables.getOnlyElement(events);

        byte[] body = event.getRequest().getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> object = (Map<Object, Object>) objectMapper.readValue(body, Map.class);
        Assertions.assertNull(JsonUtil.getByPath(object, "/order/buyer"));
        Assertions.assertNull(JsonUtil.getByPath(object, "/order/notes"));
    }

    @Test
    public void shouldPushOrderAndItemsSubsidy() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.getOrderChange().getPromoPrices().setSubsidyTotal(new BigDecimal("20"));
        parameters.getOrderChange().getItems().forEach(oi -> {
            oi.getPrices().setSubsidy(BigDecimal.TEN);
        });

        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        List<ServeEvent> events = orderStatusHelper.getServeEvents();
        ServeEvent event = Iterables.getOnlyElement(events);

        byte[] body = event.getRequest().getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> object = (Map<Object, Object>) objectMapper.readValue(body, Map.class);
        Assertions.assertEquals(
                JsonUtil.getByPath(object, "/order/subsidyTotal").toString(),
                new BigDecimal("20").toString()
        );
    }

    @Test
    public void shouldPushOrderAndItemsSubsidyJson() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.setDataType(DataType.JSON);
        parameters.getOrderChange().getPromoPrices().setSubsidyTotal(new BigDecimal("20"));
        parameters.getOrderChange().getItems().forEach(oi -> {
            oi.getPrices().setSubsidy(BigDecimal.TEN);
        });

        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        List<ServeEvent> events = orderStatusHelper.getServeEvents();
        ServeEvent event = Iterables.getOnlyElement(events);

        String body = event.getRequest().getBodyAsString();

        JsonPathUtils.jpath("$.order.subsidyTotal")
                .assertValue(body, new BigDecimal("20"));
        JsonPathUtils.jpath("$.order.items[*].subsidy")
                .assertValue(body, CoreMatchers.everyItem(CoreMatchers.equalTo(10)));
    }

    @Test
    public void shouldFillParcelFields() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.setDataType(DataType.JSON);
        ParcelBoxItem boxItem = new ParcelBoxItem() {{
            setItemId(789L);
            setCount(1);
        }};
        ParcelBox box = new ParcelBox() {{
            setId(456L);
            setWeight(100L);
            setWidth(1L);
            setHeight(1L);
            setDepth(1L);
            setItems(Collections.singletonList(boxItem));
        }};
        Parcel parcel = new Parcel() {{
            setId(123L);
            setWeight(1L);
            setWidth(1L);
            setHeight(1L);
            setDepth(1L);
            setStatus(ParcelStatus.CREATED);
            setBoxes(Collections.singletonList(box));
        }};
        parameters.getOrderChange().getDelivery().setParcels(Collections.singletonList(parcel));

        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        List<ServeEvent> events = orderStatusHelper.getServeEvents();
        ServeEvent event = Iterables.getOnlyElement(events);

        String body = event.getRequest().getBodyAsString();

        LOG.debug(body);

        JsonPathUtils.jpath("$.order.delivery.shipments[*].id")
                .assertValue(body, CoreMatchers.everyItem(CoreMatchers.equalTo(123)));
        JsonPathUtils.jpath("$.order.delivery.shipments[*].boxes[*].id")
                .assertValue(body, CoreMatchers.everyItem(CoreMatchers.equalTo(456)));
        JsonPathUtils.jpath("$.order.delivery.shipments[*].boxes[*].items[*].id")
                .assertValue(body, CoreMatchers.everyItem(CoreMatchers.equalTo(789)));
    }

    @Test
    public void shouldNotFailOnDeliveredSubstatus() throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        parameters.getOrderChange().setStatus(OrderStatus.DELIVERED);
        parameters.getOrderChange().setSubstatus(OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        List<ServeEvent> serveEvents = orderStatusHelper.getServeEvents();
        assertThat(serveEvents, Matchers.hasSize(1));

        ServeEvent event = Iterables.getOnlyElement(serveEvents);

        String token = ServeEventUtils.extractTokenParameter(event);
        Assertions.assertEquals(SettingsProvider.DEFAULT_TOKEN, token);

        byte[] body = event.getRequest().getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> object = (Map<Object, Object>) objectMapper.readValue(body, Map.class);
        Assertions.assertNull(
                JsonUtil.getByPath(object, "/order/substatus")
        );
    }
}
