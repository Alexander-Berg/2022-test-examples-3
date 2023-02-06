package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Из PROCESSING в CANCELLED_WITHOUT_REFUND может саппорт и магазин.
 * Пользователь из PROCESSING не может отменить
 * <p>
 * user: gelvy
 * date: 19.02.2021
 */
public class OrderControllerStatusChangeCancelledWithoutRefundTest extends AbstractWebTestBase {

    private static final Long EDA_SHOP_ID = 1L;

    private static final Long SHOP_ID = 2L;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{
                        OrderStatus.UNPAID,
                        EDA_SHOP_ID,
                        ClientRole.SHOP,
                        true,
                        true,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        EDA_SHOP_ID,
                        ClientRole.SHOP,
                        true,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        EDA_SHOP_ID,
                        ClientRole.SHOP_USER,
                        true,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        EDA_SHOP_ID,
                        ClientRole.CALL_CENTER_OPERATOR,
                        true,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        EDA_SHOP_ID,
                        ClientRole.USER,
                        true,
                        true,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        EDA_SHOP_ID,
                        ClientRole.SYSTEM,
                        true,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        SHOP_ID,
                        ClientRole.SHOP,
                        false,
                        true,
                },
                new Object[]{
                        OrderStatus.DELIVERY,
                        EDA_SHOP_ID,
                        ClientRole.SHOP,
                        true,
                        true,
                },
                new Object[]{
                        OrderStatus.DELIVERED,
                        EDA_SHOP_ID,
                        ClientRole.SHOP,
                        true,
                        true,
                }

        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testChangeStatusToCancelledWithoutRefundByShop(OrderStatus statusFrom,
                                                               Long shopId,
                                                               ClientRole clientRole,
                                                               boolean isEda,
                                                               boolean expectError) {
        //setup
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setShopId(shopId);
        parameters.getReportParameters().setIsEda(isEda);

        if (statusFrom == OrderStatus.PICKUP) {
            parameters.setDeliveryType(DeliveryType.PICKUP);
            parameters.setOutletId(419584L);
        } else {
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
            parameters.setDeliveryType(DeliveryType.DELIVERY);
        }

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, statusFrom);

        ResultActionsContainer resultActionsContainer = null;
        if (expectError) {
            resultActionsContainer = new ResultActionsContainer();

            resultActionsContainer.andExpect(status().is4xxClientError());
        }

        ClientInfo clientInfo = generateClientInfo(clientRole, order);

        //dotest
        Order updated = orderStatusHelper.updateOrderStatus(
                order.getId(),
                clientInfo,
                OrderStatus.CANCELLED_WITHOUT_REFUND,
                null,
                resultActionsContainer,
                null
        );
        if (!expectError) {
            assertThat(updated.getStatus(), is(OrderStatus.CANCELLED_WITHOUT_REFUND));
        }
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private ClientInfo generateClientInfo(ClientRole role,
                                          Order order) {
        switch (role) {
            case SHOP:
                return new ClientInfo(role, order.getShopId());
            case SHOP_USER:
                return new ClientInfo(role, 1L, order.getShopId());
            case USER:
                return new ClientInfo(role, order.getBuyer().getUid());
        }
        return new ClientInfo(role, 1L);
    }
}
