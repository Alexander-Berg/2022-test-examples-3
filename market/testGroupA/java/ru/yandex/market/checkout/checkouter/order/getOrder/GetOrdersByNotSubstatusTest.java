package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByNotSubstatusTest extends AbstractWebTestBase {

    private static final GetOrdersUtils.ParameterizedRequest<Object> REQUEST = parameterizedGetRequest(
            "/orders?rgb=BLUE&notSubstatus={notSubstatus}"
    );

    @BeforeAll
    public void init() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.createOrder(parameters);
    }

    @DisplayName("Получить заказы без подстатуса WAITING_USER_INPUT")
    @Test
    public void shouldReturnOrdersWithoutWaitingUserInput() throws Exception {
        mockMvc.perform(
                REQUEST.build(OrderSubstatus.WAITING_USER_INPUT)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(0)));
    }
}
