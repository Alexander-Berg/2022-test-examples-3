package ru.yandex.market.checkout.checkouter.pay;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CARD;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.REGION_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SESSION_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.UID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.USER_IP;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkListPaymentMethodsCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkUnbindCardCall;


/**
 * Created by disproper on 17/04/06.
 */
public class PaymentControllerRequestsTest extends AbstractWebTestBase {

    @Autowired
    private ShopService shopService;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_PAYMENT)
    @DisplayName("testGetPaymentById")
    @Test
    public void testGetPaymentById() throws Exception {
        trustMockConfigurer.mockListPaymentMethods();
        Order order = OrderProvider.getPrepaidOrder();
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.UNPAID, OrderSubstatus.WAITING_USER_INPUT);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        mockMvc.perform(MockMvcRequestBuilders.post("/orders/{orderId}/payment?uid=" + order.getBuyer().getUid() +
                "&returnPath=1", orderId))
                .andExpect(MockMvcResultMatchers.status().is(424));

    }

    @DisplayName("/user-card/unbind-card должен ходить в баланс с переданными параметрами.")
    @Tag(Tags.GLOBAL)
    @Test
    public void paymentMethodsUnbindCardTest() throws Exception {
        trustMockConfigurer.mockUnbindCard();
        final String sessionId = "PASSPORT_OAUTH_TOKEN";
        final String userIp = "127.127.127.4";
        final String cardId = "card_id_string";

        mockMvc.perform(put("/user-card/unbind-card").
                param(SESSION_ID, sessionId).
                param(USER_IP, userIp).
                param(CARD, cardId)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(
                        MockMvcResultMatchers
                                .content()
                                .json("{\"status\":\"success\"}")
                );

        checkUnbindCardCall(trustMockConfigurer.eventsIterator(), sessionId, userIp, cardId);
    }

    @DisplayName("/user-card/list Получаем список привязанных карт пользователя для оплаты.")
    @Tag(Tags.GLOBAL)
    @Test
    public void paymentMethodsUserCardListTest() throws Exception {
        trustMockConfigurer.mockListPaymentMethods();
        final String uid = "1234567898765";
        final String userIp = "127.127.127.4";

        mockMvc.perform(MockMvcRequestBuilders.get("/user-card/list").
                param(UID, uid).
                param(USER_IP, userIp)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(
                        MockMvcResultMatchers
                                .content()
                                .json("{'status':'success'}")
                ).andExpect(
                MockMvcResultMatchers
                        .content()
                        .json("{'status':'success','payment_methods':{'card-x5991b708795be261c6c5f2bd':{'type':'card" +
                                "'," +
                                "'id':'card-x5991b708795be261c6c5f2bd','region_id':'225'," +
                                "'number':'411111****1111','system':'VISA'}}}")

        );

        checkListPaymentMethodsCall(trustMockConfigurer.eventsIterator(), uid, userIp, null);
    }

    @DisplayName("/user-card/list Получаем список привязанных карт пользователя для оплаты. Передаем region_id.")
    @Tag(Tags.GLOBAL)
    @Test
    public void paymentMethodsUserCardListRegionIdTest() throws Exception {
        trustMockConfigurer.mockListPaymentMethods();
        final String uid = "1234567898765";
        final String userIp = "127.127.127.4";
        final Long regionId = 333L;

        mockMvc.perform(MockMvcRequestBuilders.get("/user-card/list").
                param(UID, uid).
                param(USER_IP, userIp).
                param(REGION_ID, regionId.toString())
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(
                        MockMvcResultMatchers
                                .content()
                                .json("{'status':'success'}")
                ).andExpect(
                MockMvcResultMatchers
                        .content()
                        .json("{'status':'success','payment_methods':{'card-x5991b708795be261c6c5f2bd':{'type':'card" +
                                "'," +
                                "'id':'card-x5991b708795be261c6c5f2bd','region_id':'225'," +
                                "'number':'411111****1111','system':'VISA'}}}")

        );

        checkListPaymentMethodsCall(trustMockConfigurer.eventsIterator(), uid, userIp, regionId.toString());
    }

    @DisplayName("/user-card/list Корректно обрабатываем ошибку no_auth от баланса.")
    @Tag(Tags.GLOBAL)
    @Test
    public void paymentNoMethodsUserCardListTest() throws Exception {
        trustMockConfigurer.mockListPaymentMethods("listPaymentMethods_no_match.json");
        final String uid = "1234567898765";
        final String userIp = "127.127.127.4";

        mockMvc.perform(MockMvcRequestBuilders.get("/user-card/list").
                param(UID, uid).
                param(USER_IP, userIp)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(
                        MockMvcResultMatchers
                                .content()
                                .json("{'status':'no_auth'}")
                );

        checkListPaymentMethodsCall(trustMockConfigurer.eventsIterator(), uid, userIp, null);
    }
}
