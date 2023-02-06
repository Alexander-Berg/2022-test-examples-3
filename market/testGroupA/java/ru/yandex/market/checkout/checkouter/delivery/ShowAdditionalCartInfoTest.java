package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class ShowAdditionalCartInfoTest extends AbstractWebTestBase {

    @Test
    @DisplayName("Проверяем, что дополнительная информация по корзине сериализуется вы выдаче /cart")
    public void checkSerializeAdditionalCartInfo() {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.BLUE);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        parameters.setCheckCartErrors(false);

        parameters.cartResultActions()
                .andExpect(MockMvcResultMatchers.jsonPath("$.carts[*].additionalCartInfo").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.carts[*].additionalCartInfo[0]").exists())
                .andExpect(jsonPath("$.carts[0].additionalCartInfo[0].weight").value(10000))
                .andExpect(jsonPath("$.carts[0].additionalCartInfo[0].width").value(20))
                .andExpect(jsonPath("$.carts[0].additionalCartInfo[0].height").value(30))
                .andExpect(jsonPath("$.carts[0].additionalCartInfo[0].depth").value(40));

        orderCreateHelper.cart(parameters);
    }
}
