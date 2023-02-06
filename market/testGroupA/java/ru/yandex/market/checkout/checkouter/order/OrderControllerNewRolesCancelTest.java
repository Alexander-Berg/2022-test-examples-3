package ru.yandex.market.checkout.checkouter.order;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class OrderControllerNewRolesCancelTest extends AbstractWebTestBase {


    public static Stream<Arguments> parameterizedTestData() {

        return statues()
                .flatMap(statues -> roles().map(role -> new Object[]{statues, role}))
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    public static Stream<OrderStatus[]> statues() {
        return Stream.of(
                new OrderStatus[0],
                new OrderStatus[]{OrderStatus.DELIVERY}
        );
    }

    public static Stream<ClientRole> roles() {
        return Stream.of(ClientRole.CALL_CENTER_OPERATOR, ClientRole.CRM_ROBOT);
    }

    @ParameterizedTest(name = "Роль {1} должна отменять из статусов {0}")
    @MethodSource("parameterizedTestData")
    public void callCenterOperatorShouldBeAbleToCancelOrderFromProcessing(OrderStatus[] statuses, ClientRole role)
            throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(o -> {
        }, DeliveryProvider.getSelfDelivery(), statuses);

        mockMvc.perform(post("/orders/{orderId}/status", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, role.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(135135L))
                .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                .param(CheckouterClientParams.SUBSTATUS, OrderSubstatus.USER_CHANGED_MIND.name()))
                .andExpect(status().isOk());
    }
}
