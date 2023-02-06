package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.json.Names.Track.DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

public abstract class GetOrdersByDeliveryServiceIdsTestBase {

    public static class OrdersTest extends AbstractDeliveryFiltersTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{
                            "GET /orders",
                            parameterizedGetRequest("/orders?rgb=BLUE,WHITE", DELIVERY_SERVICE_ID)
                    },
                    new Object[]{
                            "GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?rgb=BLUE,WHITE",
                                    DELIVERY_SERVICE_ID)
                    },
                    new Object[]{
                            "POST /get-orders",
                            parameterizedPostRequest("/get-orders",
                                    "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"deliveryServiceIds\": %s}")
                    }
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по deliveryServiceId")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByDeliveryServiceIdsTest(
                String caseName,
                GetOrdersUtils.ParameterizedRequest<Collection<Long>> parameterizedRequest) throws Exception {
            List<Long> serviceIds = Stream.of(orderWithShipment1, orderWithShipment2, orderWithoutShipment)
                    .filter(order -> order.getDelivery().getParcels() != null)
                    .flatMap(order -> order.getDelivery().getParcels().stream())
                    .filter(shipment -> shipment.getTracks() != null)
                    .flatMap(shipment -> shipment.getTracks().stream())
                    .map(Track::getDeliveryServiceId)
                    .collect(Collectors.toList());
            mockMvc.perform(
                    parameterizedRequest
                            .build(serviceIds)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.orders[*]",
                                    containsInAnyOrder(
                                            hasEntry("id", orderWithShipment1.getId().intValue()),
                                            hasEntry("id", orderWithShipment2.getId().intValue()))
                            )
                    );
        }
    }

    public static class CountsTest extends AbstractDeliveryFiltersTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по deliveryServiceId")
        @Test
        public void getByDeliveryServiceIdsTest() throws Exception {
            String[] serviceIds = Stream.of(orderWithShipment1, orderWithShipment2, orderWithoutShipment)
                    .filter(order -> order.getDelivery().getParcels() != null)
                    .flatMap(order -> order.getDelivery().getParcels().stream())
                    .filter(shipment -> shipment.getTracks() != null)
                    .flatMap(shipment -> shipment.getTracks().stream())
                    .map(Track::getDeliveryServiceId)
                    .map(String::valueOf)
                    .toArray(String[]::new);
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(DELIVERY_SERVICE_ID, serviceIds)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }
    }
}
