package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByWithCisesTest extends AbstractWebTestBase {

    private static final String RGB = "WHITE,BLUE";
    private static final GetOrdersUtils.ParameterizedRequest<Object> REQUEST = parameterizedGetRequest(
            "/orders?rgb=" + RGB + "&withCises={withCises}");
    private Order orderWithCises;
    private Order orderWithoutCises;

    @BeforeAll
    public void init() throws Exception {
        OrderItem itemWithCises = OrderItemProvider.orderItemWithSortingCenter()
                .offer("off1")
                .build();
        itemWithCises.setCargoTypes(Set.of(980));
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.getOrder().setItems(Collections.singleton(itemWithCises));
        orderWithCises = orderCreateHelper.createOrder(parameters);

        long itemId = orderWithCises.getItems().iterator().next().getId();
        final String cis = "010641944023860921-DLdnD)pMAC1t";
        client.putOrderItemInstances(orderWithCises.getId(), itemId, ClientRole.SYSTEM, 0L, List.of(cis));

        Parameters usualParameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        orderWithoutCises = orderCreateHelper.createOrder(usualParameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @DisplayName("Получить только заказы с КИЗами")
    @Test
    public void shouldReturnCisesOnlyWhenCisesIsTrue() throws Exception {
        mockMvc.perform(
                REQUEST.build(Boolean.TRUE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                .andExpect(jsonPath("$.orders[*]",
                        contains(hasEntry("id", orderWithCises.getId().intValue()))));
    }

    @DisplayName("Получить все заказы, включая те, что без КИЗов")
    @Test
    public void shouldReturnAllOrdersWhenCisesIsFalse() throws Exception {
        mockMvc.perform(
                REQUEST.build(Boolean.FALSE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                .andExpect(jsonPath("$.orders[*]", containsInAnyOrder(
                        hasEntry("id", orderWithCises.getId().intValue()),
                        hasEntry("id", orderWithoutCises.getId().intValue()))));
    }
}
