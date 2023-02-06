package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrderByUidTest extends AbstractWebTestBase {

    private Order firstOrder;
    private Order secondOrder;
    private Order thirdOrder;
    private Order forthOrder;
    private Long uid;

    @BeforeEach
    void setUp() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        uid = parameters.getBuyer().getUid();
        firstOrder = orderCreateHelper.createOrder(parameters);

        LocalDateTime localDateTime = LocalDateTime.now().plusDays(35);
        setFixedTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Parameters secondParameters = BlueParametersProvider.defaultBlueOrderParameters();
        secondParameters.getBuyer().setUid(uid);
        secondOrder = orderCreateHelper.createOrder(secondParameters);

        localDateTime = LocalDateTime.now().plusDays(70);
        setFixedTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Parameters thirdParameters = BlueParametersProvider.defaultBlueOrderParameters();
        thirdParameters.getBuyer().setUid(uid);
        thirdOrder = orderCreateHelper.createOrder(thirdParameters);
    }

    @Test
    void shouldFilterUsingToDateFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.TO_TIMESTAMP,
                        String.valueOf(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli())
                )
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, Boolean.TRUE.toString())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].id").value(firstOrder.getId()));
    }

    @Test
    void shouldFilterUsingFromDateFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.FROM_TIMESTAMP,
                        String.valueOf(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli())
                )
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, Boolean.TRUE.toString())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                .andExpect(jsonPath("$.orders[0].id").value(thirdOrder.getId()));
    }

    @Test
    void shouldFilterUsingToOrderId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.TO_ORDER_ID, String.valueOf(thirdOrder.getId()))
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, Boolean.TRUE.toString())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                .andExpect(jsonPath("$.orders[0].id").value(secondOrder.getId()));
    }

    @Test
    void shouldFilterUsingFromOrderId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.FROM_ORDER_ID, String.valueOf(thirdOrder.getId()))
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, Boolean.TRUE.toString())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].id").value(thirdOrder.getId()));
    }

    @DisplayName("Estimated: получить в истории информацию, что доставка является неточной")
    @Test
    public void orderDeliveryIsEstimatedTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        Long estimatedUid = parameters.getBuyer().getUid() + 1;
        parameters.getBuyer().setUid(estimatedUid);

        forthOrder = orderCreateHelper.createOrder(parameters);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/orders/by-uid/{userId}", estimatedUid)
                                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                .param(CheckouterClientParams.FROM_ORDER_ID, String.valueOf(forthOrder.getId()))
                                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, Boolean.TRUE.toString()))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].delivery.estimated").value(true));
    }
}
