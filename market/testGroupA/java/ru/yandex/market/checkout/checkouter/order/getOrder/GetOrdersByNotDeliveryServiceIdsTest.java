package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrdersByNotDeliveryServiceIdsTest extends AbstractWebTestBase {

    private final Map<Boolean, Collection<Order>> orders = new HashMap<>() {{
        put(Boolean.TRUE, new ArrayList<>());
        put(Boolean.FALSE, new ArrayList<>());
    }};
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;

    @BeforeEach
    public void init() {
        for (int i = 0; i < 4; ++i) {
            Order orderWithShipment = orderControllerTestHelper.createOrderWithPartnerDelivery(OrderProvider.SHOP_ID);
            orders.get(Boolean.TRUE).add(orderWithShipment);
        }

        for (int i = 0; i < 2; ++i) {
            Order orderWithoutShipment = orderCreateHelper.createOrder(
                    BlueParametersProvider.defaultBlueOrderParameters());
            orders.get(Boolean.FALSE).add(orderWithoutShipment);
        }
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы с исключением заданных notDeliveryServiceIds")
    @Test
    public void getByNotDeliveryServiceIdsTest() throws Exception {
        List<Long> deliveryServiceIds = Stream.concat(orders.get(Boolean.TRUE).stream(),
                orders.get(Boolean.FALSE).stream())
                .map(Order::getDelivery)
                .filter(d -> d.getParcels() != null)
                .flatMap(d -> d.getParcels().stream())
                .filter(p -> p.getTracks() != null)
                .flatMap(p -> p.getTracks().stream())
                .map(Track::getDeliveryServiceId)
                .distinct()
                .collect(Collectors.toList());

        Matcher[] hasDeliveryServiceIdsMatchers = orders.get(Boolean.TRUE).stream().map(order -> hasEntry("id",
                order.getId().intValue())).toArray(Matcher[]::new);
        String deliveryServiceIdsString = mapper.writeValueAsString(deliveryServiceIds);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/get-orders")
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"deliveryServiceIds\": %s}",
                                deliveryServiceIdsString))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                containsInAnyOrder(hasDeliveryServiceIdsMatchers)
                        )
                );

        List<Long> deliveryServiceIdsToExclude = orders.get(Boolean.TRUE).stream().skip(2)
                .flatMap(order -> order.getDelivery().getParcels().stream())
                .flatMap(shipment -> shipment.getTracks().stream())
                .map(Track::getDeliveryServiceId)
                .collect(Collectors.toList());
        String deliveryServiceIdsToExcludeString = mapper.writeValueAsString(deliveryServiceIdsToExclude);
        Matcher[] filteredDeliveryServiceIdsMatchers =
                orders.get(Boolean.TRUE).stream().limit(2).map(order -> hasEntry("id", order.getId().intValue()))
                        .toArray(Matcher[]::new);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/get-orders")
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"deliveryServiceIds\": %s, " +
                                "\"notDeliveryServiceIds\": %s}", deliveryServiceIdsString,
                                deliveryServiceIdsToExcludeString))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                containsInAnyOrder(filteredDeliveryServiceIdsMatchers)
                        )
                );
    }
}
