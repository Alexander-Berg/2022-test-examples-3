package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.getBuyerWithPhone;

/**
 * https://testpalm.yandex-team.ru/testcase/checkouter-174
 *
 * @author asafev
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByBuyerPhoneTestBase extends AbstractWebTestBase {

    public static final String PHONE = "+70000000000";

    protected Order orderWithPhone;

    @BeforeAll
    public void init() {
        super.setUpBase();
        var params = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        var buyer = getBuyerWithPhone(PHONE);
        params.setBuyer(buyer);
        orderWithPhone = orderCreateHelper.createOrder(params);
    }

    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersByBuyerPhoneTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders",
                            parameterizedGetRequest("/orders?buyerPhone={phone}")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID_WITH_PHONE + "?buyerPhone" +
                                    "={phone}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"buyerPhone\":%s}")}
            ).stream().map(Arguments::of);
        }

        @DisplayName("Получить заказы по телефону покупателя (buyerPhone)")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterOrdersWithPhoneTest(String caseName,
                                              GetOrdersUtils.ParameterizedRequest<String> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(PHONE)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                    .andExpect(jsonPath("$.orders[*]", contains(hasEntry("id", orderWithPhone.getId().intValue()))));
        }
    }

    public static class CountTest extends GetOrdersByBuyerPhoneTestBase {

        @DisplayName("Посчитать заказы с телефоном покупателя")
        @Test
        public void nonFulfilmentCountTest() throws Exception {
            mockMvc.perform(get("/orders/count/")
                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.BUYER_PHONE, PHONE)
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }
    }
}
