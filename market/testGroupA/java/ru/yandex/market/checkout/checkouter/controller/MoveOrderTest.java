package ru.yandex.market.checkout.checkouter.controller;

import java.util.Arrays;

import javax.servlet.http.Cookie;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.MUID;

// TODO - можно попробовать завязаться на энум MoveOrderStatus в ответах и энум clientRole в запросах

public class MoveOrderTest extends AbstractWebTestBase {

    private String moveUrlTemplate = "/move-orders";
    private Buyer buyer = BuyerProvider.getBuyer();
    private Buyer buyer1 = BuyerProvider.getBuyer();
    private Long to = buyer1.getUid();
    private Long to2 = 4001119628L;
    private Parameters parameters;

    @Autowired
    private AuthHelper authHelper;

    @BeforeEach
    public void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_COIN_BINDING_ON_MOVE_ORDER, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.THROW_ERROR_ON_WRONG_USER_MUID, true);
    }

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_COIN_BINDING_ON_MOVE_ORDER, false);
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация заказа")
    @Test
    public void moveOrder() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie(MUID, responseCookie);
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .param("rgb", Color.GREEN.toString())
                        .cookie(cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId))
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"));

        mockMvc.perform(
                get("/orders/" + orderId)
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", to.toString())
        )
                .andExpect(jsonPath("$.buyer.uid").value(to))
                .andExpect(jsonPath("$.buyer.muid").value(from));

    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация заказа без куки")
    @Test
    public void moveOrderWithoutCookie() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("muid cookie was not specified"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация заказа из под роли SHOP")
    @Test
    public void moveOrderByShop() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();
        Long shopId = order.getShopId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", "SHOP")
                        .param("clientId", shopId.toString())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("Access denied for clientRole==SHOP"));

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", "SHOP_USER")
                        .param("clientId", shopId.toString())
                        .param("shopId", shopId.toString())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("Access denied for clientRole==SHOP_USER"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Перенос авторизованного заказа из под роли USER")
    @Test
    public void moveOrderFromUidByUser() throws Exception { // - ошибка как в тесте без куки - узнать можно ли что-то
        // с этим сделать чотбы проверить
        buyer.setUid(to);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", to.toString())
                        .param("toUid", to2.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", to2.toString())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("muid cookie was not specified"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Перенос авторизованного заказа из под роли SYSTEM")
    @Test
    public void moveOrderFromUidBySystem() throws Exception {
        doTestMoveOrderFromUidByRole(new ClientInfo(ClientRole.SYSTEM, to2));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Перенос авторизованного заказа из под роли CALL_CENTER_OPERATOR")
    @Test
    public void moveOrderFromUidByCallCenterOperator() throws Exception {
        doTestMoveOrderFromUidByRole(new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 12L));
    }

    private void doTestMoveOrderFromUidByRole(ClientInfo clientInfo) throws Exception {
        buyer.setUid(to);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", to.toString())
                        .param("toUid", to2.toString())
                        .param("clientRole", clientInfo.getRole().name())
                        .param("clientId", String.valueOf(clientInfo.getId()))
                        .param("rgb", Color.GREEN.toString())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId))
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"));

        mockMvc.perform(
                get("/orders/" + orderId)
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", to2.toString())
        )
                .andExpect(jsonPath("$.buyer.uid").value(to2))
                .andExpect(jsonPath("$.buyer.muid").doesNotExist());
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация левого заказа")
    @Test
    public void moveOrderThatNotFound() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId + 1))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .cookie(cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId + 1))
                .andExpect(jsonPath("$.[0].status").value("ORDER_NOT_FOUND"));

        AuthInfo authInfo2 = authHelper.getAuthInfo();
        Long from2 = authInfo2.getMuid();
        String responseCookie2 = authInfo2.getCookie();
        Cookie cookie2 = new Cookie("muid", responseCookie2);

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from2.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .cookie(cookie2)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId))
                .andExpect(jsonPath("$.[0].status").value("ORDER_UID_NOT_EQUAL"));

    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация заказа c левой кукой")
    @Test
    public void moveOrderWithWrongCookie() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();
        from += 1;

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", (from).toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .cookie(cookie)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Parameter fromMuid not equals to the decrypted muid"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация нескольких заказов")
    @Test
    public void moveSeveralOrders() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        Long orderId1 = order1.getId();
        Long orderId2 = order2.getId();

        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d, %d]}", orderId1, orderId2))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .param("rgb", Color.GREEN.toString())
                        .cookie(cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId1))
                .andExpect(jsonPath("$.[1].id").value(orderId2))
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.[1].status").value("SUCCESS"));

        mockMvc.perform(
                get("/orders/" + orderId1)
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", to.toString())
        )
                .andExpect(jsonPath("$.buyer.uid").value(to))
                .andExpect(jsonPath("$.buyer.muid").value(from));

        mockMvc.perform(
                get("/orders/" + orderId2)
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", to.toString())
        )
                .andExpect(jsonPath("$.buyer.uid").value(to))
                .andExpect(jsonPath("$.buyer.muid").value(from));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация нескольких заказов и синхронная привязка монеток")
    @Test
    public void moveSeveralOrdersWithCoinBinding() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        Long orderId1 = order1.getId();
        Long orderId2 = order2.getId();

        loyaltyConfigurer.resetAll();
        loyaltyConfigurer.mockCoinBinding(Arrays.asList(orderId1, orderId2), to, 200);
        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d, %d]}", orderId1, orderId2))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .param("rgb", Color.GREEN.toString())
                        .cookie(new Cookie("muid", responseCookie))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId1))
                .andExpect(jsonPath("$.[1].id").value(orderId2))
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$.[0].coinBindingResult.totalOrderCoinsCount").value(2))
                .andExpect(jsonPath("$.[1].coinBindingResult.totalOrderCoinsCount").value(2))
                .andExpect(jsonPath("$.[0].coinBindingResult.successfulOrderCoinBindingCount").value(1))
                .andExpect(jsonPath("$.[1].coinBindingResult.successfulOrderCoinBindingCount").value(1));
        loyaltyConfigurer.verify(
                WireMock.putRequestedFor(urlPathEqualTo("/coins/bindByOrderId"))
                        .withQueryParam("orderId", containing(Long.toString(orderId1)))
        );
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Доавторизация нескольких заказов и синхронная привязка монеток")
    @Test
    public void moveSeveralOrdersWithCoinBindingFailure() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        Long orderId1 = order1.getId();
        Long orderId2 = order2.getId();

        loyaltyConfigurer.resetAll();
        loyaltyConfigurer.mockCoinBinding(Arrays.asList(orderId1, orderId2), to, 500);
        mockMvc.perform(
                post(moveUrlTemplate)
                        .content(String.format("{\"orders\": [%d, %d]}", orderId1, orderId2))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .param("rgb", Color.GREEN.toString())
                        .cookie(new Cookie("muid", responseCookie))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(orderId1))
                .andExpect(jsonPath("$.[1].id").value(orderId2))
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$.[0].coinBindingResult").doesNotExist());
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /move-orders: Повторная доавторизация")
    @Test
    public void moveRecentlyMovedOrder() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        buyer.setUid(from);
        parameters = new Parameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        mockMvc.perform(post("/move-orders")
                .content(String.format("{\"orders\":[%d]}", orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .param("fromMuid", from.toString())
                .param("toUid", to.toString())
                .param("clientRole", ClientRole.USER.name())
                .param("clientId", from.toString())
                .param("rgb", Color.GREEN.toString())
                .cookie(cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].status").value("SUCCESS"));

        mockMvc.perform(post("/move-orders")
                .content(String.format("{\"orders\":[%d]}", orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .param("fromMuid", from.toString())
                .param("toUid", to.toString())
                .param("clientRole", ClientRole.USER.name())
                .param("clientId", from.toString())
                .cookie(cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].status").value("ORDER_NOT_FOUND"));

    }
}
