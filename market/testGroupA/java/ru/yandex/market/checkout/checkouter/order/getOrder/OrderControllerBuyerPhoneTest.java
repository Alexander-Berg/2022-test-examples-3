package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;
import static ru.yandex.market.checkout.common.DataNormalizeUtil.normalizePhone;

/**
 * Тесты обратной совместимости, использующие открытый номера телефона покупателя.
 * Удалить в MARKETCHECKOUT-27094
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderControllerBuyerPhoneTest extends AbstractWebTestBase {

    private static final int FIRST_BUYER_PHONE_COUNT = 2;
    private static final int SECOND_BUYER_PHONE_COUNT = 3;
    private static final String BUYER_PHONE_1 = "buyerPhone1 8800 234 55 66";
    private static final String BUYER_PHONE_2 = "buyerPhone2 +7 (916) 444 55 55";

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @BeforeAll
    public void setUp() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        Map<String, Integer> phoneData = new HashMap<String, Integer>() {{
            put(BUYER_PHONE_1, FIRST_BUYER_PHONE_COUNT);
            put(BUYER_PHONE_2, SECOND_BUYER_PHONE_COUNT);
        }};
        phoneData.forEach((phoneNumber, value) -> {
            int ordersCount = value;
            for (int i = 0; i < ordersCount; i++) {
                Order order = OrderProvider.getBlueOrder(o -> {
                    o.getBuyer().setPhone(phoneNumber);
                    o.getBuyer().setNormalizedPhone(normalizePhone(phoneNumber));
                });
                orderServiceHelper.saveOrder(order);
            }
        });
    }

    @Override
    @AfterEach
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }


    @DisplayName("Ручка /orders/count должна фильтровать по buyerPhone")
    @Tag(Tags.AUTO)
    @Test
    public void shouldCountByBuyerPhoneWithApi() throws Exception {
        mockMvc.perform(get("/orders/count")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_1)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(jsonPath("$.value").value(FIRST_BUYER_PHONE_COUNT));


        mockMvc.perform(get("/orders/count")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_2)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(jsonPath("$.value").value(SECOND_BUYER_PHONE_COUNT));
    }

    @DisplayName("Ручка /orders/count должна фильтровать по buyerPhone")
    @Tag(Tags.AUTO)
    @Test
    public void shouldCountByBuyerPhoneWithClient() throws Exception {
        OrderSearchRequest first = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withBuyerPhone(BUYER_PHONE_1)
                .build();
        int firstCount = client.getOrdersCount(first, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(FIRST_BUYER_PHONE_COUNT, firstCount);

        OrderSearchRequest second = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withBuyerPhone(BUYER_PHONE_2)
                .build();
        int secondCount = client.getOrdersCount(second, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(SECOND_BUYER_PHONE_COUNT, secondCount);
    }

    @DisplayName("Ручка /orders должна фильтровать по buyerPhone")
    @Tag(Tags.AUTO)
    @Test
    public void shouldFilterByBuyerPhone() throws Exception {
        mockMvc.perform(get("/orders")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_1)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andDo(log())
                .andExpect(jsonPath("$.orders.length()").value(FIRST_BUYER_PHONE_COUNT));

        mockMvc.perform(get("/orders")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_2)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andDo(log())
                .andExpect(jsonPath("$.orders.length()").value(SECOND_BUYER_PHONE_COUNT));
    }

    @DisplayName("Ручка /orders/by-uid должна фильтровать по buyerPhone")
    @Tag(Tags.AUTO)
    @Test
    public void orderByUidShouldFilterByBuyerPhone() throws Exception {
        Long uid = BuyerProvider.getBuyer().getUid();

        mockMvc.perform(get("/orders/by-uid/{uid}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_1))
                .andDo(log())
                .andExpect(jsonPath("$.orders.length()").value(FIRST_BUYER_PHONE_COUNT));

        mockMvc.perform(get("/orders/by-uid/{uid}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.BUYER_PHONE, BUYER_PHONE_2))
                .andDo(log())
                .andExpect(jsonPath("$.orders.length()").value(SECOND_BUYER_PHONE_COUNT));
    }
}
