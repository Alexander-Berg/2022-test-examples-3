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
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrdersByNotTrackCodesTest extends AbstractWebTestBase {

    private static final Map<Boolean, Collection<Order>> ORDERS = new HashMap<>() {{
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
            ORDERS.get(Boolean.TRUE).add(orderWithShipment);
        }

        for (int i = 0; i < 2; ++i) {
            Order orderWithoutShipment = orderCreateHelper.createOrder(new Parameters());
            ORDERS.get(Boolean.FALSE).add(orderWithoutShipment);
        }
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы с исключением заданных trackCodes")
    @Test
    public void getByNotTrackCodesTest() throws Exception {
        List<String> trackCodes = Stream.concat(ORDERS.get(Boolean.TRUE).stream(), ORDERS.get(Boolean.FALSE).stream())
                .map(Order::getDelivery)
                .filter(d -> d.getParcels() != null)
                .flatMap(d -> d.getParcels().stream())
                .filter(p -> p.getTracks() != null)
                .flatMap(p -> p.getTracks().stream())
                .map(Track::getTrackCode)
                .collect(Collectors.toList());

        Matcher[] hasTrackCodeMatchers = ORDERS.get(Boolean.TRUE).stream().map(order -> hasEntry("id",
                order.getId().intValue())).toArray(Matcher[]::new);
        String trackCodesString = mapper.writeValueAsString(trackCodes);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/get-orders")
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"trackCodes\": %s}", trackCodesString))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                containsInAnyOrder(hasTrackCodeMatchers)
                        )
                );

        List<String> trackCodesToExclude = ORDERS.get(Boolean.TRUE).stream().skip(2)
                .flatMap(order -> order.getDelivery().getParcels().stream())
                .flatMap(shipment -> shipment.getTracks().stream())
                .map(Track::getTrackCode)
                .collect(Collectors.toList());
        String trackCodesToExcludeString = mapper.writeValueAsString(trackCodesToExclude);
        Matcher[] filteredTrackCodeMatchers = ORDERS.get(Boolean.TRUE).stream().limit(2).map(order -> hasEntry("id",
                order.getId().intValue())).toArray(Matcher[]::new);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/get-orders")
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"trackCodes\": %s, \"notTrackCodes\":" +
                                " %s}", trackCodesString, trackCodesToExcludeString))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                containsInAnyOrder(filteredTrackCodeMatchers)
                        )
                );
    }
}
