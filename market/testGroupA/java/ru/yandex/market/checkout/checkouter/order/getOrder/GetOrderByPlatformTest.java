package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrderByPlatformTest extends AbstractWebTestBase {

    @Test
    void getOrdersCountByPlatformTest() throws Exception {
        var expressParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        expressParameters.setPlatform(Platform.YANDEX_GO_IOS);
        orderCreateHelper.createOrder(expressParameters);
        Long uid = expressParameters.getBuyer().getUid();

        var blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        blueOrderParameters.getBuyer().setUid(uid);
        blueOrderParameters.setPlatform(Platform.IOS);
        orderCreateHelper.createOrder(blueOrderParameters);

        var secondBlueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        secondBlueOrderParameters.getBuyer().setUid(uid);
        secondBlueOrderParameters.setPlatform(Platform.ANDROID);
        orderCreateHelper.createOrder(secondBlueOrderParameters);


        mockMvc.perform(MockMvcRequestBuilders.get("/orders/count/")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.PLATFORM, Platform.YANDEX_GO_IOS.name())
                .param(CheckouterClientParams.UID, uid.toString())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                .param(CheckouterClientParams.CLIENT_ID, uid.toString())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", equalTo(1)));
    }
}
