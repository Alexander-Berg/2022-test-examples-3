package ru.yandex.market.checkout.checkouter.items;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrderNotFound;

/**
 * @author mmetlov
 */
public class GetOrderItemsClientRoleTest extends AbstractWebTestBase {

    private static final long SHOP_CLIENT_ID = 1234L;
    private static final long REFEREE_CLIENT_ID = 5678L;

    private Order order;

    @BeforeEach
    public void init() throws Exception {
        Parameters parameters = new Parameters();

        order = orderCreateHelper.createOrder(parameters);
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа без роли")
    @Test
    public void testNoRole() throws Exception {
        mockMvc.perform(builderWithClientRoleParams(null, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью UNKNOWN")
    @Test
    public void testСlientRoleUnknown() throws Exception {
        mockMvc.perform(builderWithClientRoleParams(ClientRole.UNKNOWN, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью SYSTEM")
    @Test
    public void testClientRoleSystem() throws Exception {
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SYSTEM, null, null, null))
                .andExpect(itemsPresent());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью SHOP")
    @Test
    public void testClientRoleShop() throws Exception {
        //магазин видит состав своего заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, OrderProvider.SHOP_ID, null, null))
                .andExpect(itemsPresent());

        //магазин не видит состав чужого заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, Long.MAX_VALUE, null, null))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        // без shopId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью BUSINESS")
    @Test
    public void testClientRoleBusiness() throws Exception {
        //магазин видит состав своего заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS, OrderProvider.BUSINESS_ID, null, null))
                .andExpect(itemsPresent());

        //магазин не видит состав чужого заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS, Long.MAX_VALUE, null, Long.MAX_VALUE))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        // без businessId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью BUSINESS_USER")
    @Test
    public void testClientRoleBusinessUser() throws Exception {
        //магазин видит состав своего заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS_USER, SHOP_CLIENT_ID, null,
                OrderProvider.BUSINESS_ID)).andExpect(itemsPresent());

        //магазин не видит состав чужого заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS_USER, SHOP_CLIENT_ID, null, Long.MAX_VALUE))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        // без clientId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS_USER, null, null, OrderProvider.BUSINESS_ID))
                .andExpect(status().is4xxClientError());

        // без businessId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.BUSINESS_USER, SHOP_CLIENT_ID, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью SHOP_USER")
    @Test
    public void testClientRoleShopUser() throws Exception {
        //магазин видит состав своего заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, OrderProvider.SHOP_ID, null))
                .andExpect(itemsPresent());

        //магазин не видит состав чужого заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, Long.MAX_VALUE, null))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        // без clientId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, null, OrderProvider.SHOP_ID, null))
                .andExpect(status().is4xxClientError());

        // без shopId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью USER")
    @Test
    public void testClientRoleUser() throws Exception {
        //пользователь видит состав своего заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, BuyerProvider.UID, null, null))
                .andExpect(itemsPresent());

        //пользователь не видит состав чужого заказа
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, Long.MAX_VALUE, null, null))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        // без clientId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить состав заказа с ролью REFEREE")
    @Test
    public void testClientRoleReferee() throws Exception {
        mockMvc.perform(builderWithClientRoleParams(ClientRole.REFEREE, REFEREE_CLIENT_ID, null, null))
                .andExpect(itemsPresent());

        // без clientId 400
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, null, null, null))
                .andExpect(status().is4xxClientError());
    }

    private MockHttpServletRequestBuilder builderWithClientRoleParams(ClientRole role,
                                                                      Long clientId,
                                                                      Long shopId,
                                                                      Long businessId) {
        MockHttpServletRequestBuilder res = MockMvcRequestBuilders.get("/orders/{orderId}/items", order.getId());

        if (role != null) {
            res.param(CheckouterClientParams.CLIENT_ROLE, role.name());
        }

        if (clientId != null) {
            res.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }

        if (shopId != null) {
            res.param(CheckouterClientParams.SHOP_ID, shopId.toString());
        }
        if (businessId != null) {
            res.param(CheckouterClientParams.BUSINESS_ID, businessId.toString());
        }

        return res;
    }

    private ResultMatcher itemsPresent() {
        return result -> {
            status().isOk().match(result);
            jsonPath("$.items[*]",
                    containsInAnyOrder(
                            order.getItems().stream().map(i -> hasEntry("id", i.getId().intValue()))
                                    .toArray(Matcher[]::new)))
                    .match(result);
        };
    }
}
