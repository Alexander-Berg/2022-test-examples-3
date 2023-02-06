package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
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
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.HAS_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

public abstract class GetOrdersByHasDeliveryServiceIdTestBase {

    public static class OrdersTest extends AbstractDeliveryFiltersTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{
                            "GET /orders",
                            parameterizedGetRequest("/orders" + "?" + HAS_DELIVERY_SERVICE_ID +
                                    "={hasDeliveryServiceId}" +
                                    "&rgb=BLUE,WHITE")
                    },
                    new Object[]{
                            "GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" +
                                    BuyerProvider.UID + "?" + HAS_DELIVERY_SERVICE_ID + "={hasDeliveryServiceId}" +
                                    "&rgb=BLUE,WHITE")
                    },
                    new Object[]{
                            "POST /get-orders",
                            parameterizedPostRequest("/get-orders", "{\"rgbs\":[\"BLUE\",\"WHITE\"]," +
                                    "\"hasDeliveryServiceId\": %s}")
                    }
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по hasDeliveryServiceId")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByHasDeliveryServiceIdTest(String caseName,
                                                  GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest
                            .build(Boolean.FALSE)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.orders[*]",
                                    contains(hasEntry("id", orderWithoutShipment.getId().intValue()))
                            )
                    );
            mockMvc.perform(
                    parameterizedRequest
                            .build(Boolean.TRUE)
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
        @DisplayName("Посчитать заказы по hasDeliveryServiceId")
        @Test
        public void getByHasDeliveryServiceIdTest() throws Exception {
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(HAS_DELIVERY_SERVICE_ID, Boolean.FALSE.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(HAS_DELIVERY_SERVICE_ID, Boolean.TRUE.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }
    }
}
