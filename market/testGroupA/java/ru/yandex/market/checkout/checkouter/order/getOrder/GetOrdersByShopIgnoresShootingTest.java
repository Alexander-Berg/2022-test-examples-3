package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrdersByShopIgnoresShootingTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    @Test
    void getOrdersByShopIgnoresShooting() throws Exception {
        var normalOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.createOrder(normalOrderParameters);

        var shootingOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        shootingOrderParameters.getBuyer().setUid(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint());
        orderCreateHelper.createOrder(shootingOrderParameters);

        MockHttpServletRequestBuilder req = post("/get-orders")
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP.name())
                .param(CheckouterClientParams.CLIENT_ID, "774")
                .content(testSerializationService.serializeCheckouterObject(OrderSearchRequest.builder()
                        .withRgbs(Color.BLUE)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)));
    }
}
