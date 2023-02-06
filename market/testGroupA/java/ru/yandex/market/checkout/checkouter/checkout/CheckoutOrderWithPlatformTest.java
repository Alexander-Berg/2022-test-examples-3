package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;

public class CheckoutOrderWithPlatformTest extends AbstractWebTestBase {

    public static Stream<Arguments> parameterizedTestData() {
        return Arrays.stream(Platform.values()).map(p -> new Object[]{p}).collect(Collectors.toList()).stream()
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createOrderWithPlatform(Platform platform) throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPlatform(platform);
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(
                platform,
                orderService.getOrder(order.getId()).getProperty(OrderPropertyType.PLATFORM)
        );

        mockMvc.perform(get("/orders/{orderId}", order.getId())
                .param(CLIENT_ROLE, "SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.platform").value(platform.name()));
    }
}
