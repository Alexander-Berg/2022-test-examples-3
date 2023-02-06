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
import static ru.yandex.market.checkout.checkouter.json.Names.Track.TRACK_CODE;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

public abstract class GetOrdersByTrackCodesTestBase {


    public static class OrdersTest extends AbstractDeliveryFiltersTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{
                            "GET /orders",
                            parameterizedGetRequest("/orders?rgb=BLUE,WHITE", TRACK_CODE)
                    },
                    new Object[]{
                            "GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID +
                                    "?rgb=BLUE,WHITE", TRACK_CODE)
                    },
                    new Object[]{
                            "POST /get-orders",
                            parameterizedPostRequest("/get-orders",
                                    "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"trackCodes\": %s}")
                    }
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по trackCodes")
        @ParameterizedTest(name = "method = {0}")
        @MethodSource("parameterizedTestData")
        public void getByTrackCodesTest(String caseName,
                                        GetOrdersUtils.ParameterizedRequest<Collection<String>> parameterizedRequest)
                throws Exception {
            List<String> trackCodes = Stream.of(orderWithShipment1, orderWithShipment2, orderWithoutShipment)
                    .filter(order -> order.getDelivery().getParcels() != null)
                    .flatMap(order -> order.getDelivery().getParcels().stream())
                    .filter(shipment -> shipment.getTracks() != null)
                    .flatMap(shipment -> shipment.getTracks().stream())
                    .map(Track::getTrackCode)
                    .collect(Collectors.toList());
            mockMvc.perform(
                    parameterizedRequest
                            .build(trackCodes)
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
        @DisplayName("Посчитать заказы по trackCodes")
        @Test
        public void getByTrackCodesTest() throws Exception {
            String[] trackCodes = Stream.of(orderWithShipment1, orderWithShipment2, orderWithoutShipment)
                    .filter(order -> order.getDelivery().getParcels() != null)
                    .flatMap(order -> order.getDelivery().getParcels().stream())
                    .filter(shipment -> shipment.getTracks() != null)
                    .flatMap(shipment -> shipment.getTracks().stream())
                    .map(Track::getTrackCode)
                    .toArray(String[]::new);
            mockMvc.perform(
                    get("/orders/count")
                            .param(TRACK_CODE, trackCodes)
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }
    }
}
