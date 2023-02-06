package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerStatusCannotCancelEdaOrderTest extends AbstractWebTestBase {

    private static final Long EDA_SHOP_ID = 1L;

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.SHOP_FAILED,
                        EDA_SHOP_ID,
                        ClientRole.SHOP,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.SHOP_FAILED,
                        EDA_SHOP_ID,
                        ClientRole.BUSINESS,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.SHOP_FAILED,
                        EDA_SHOP_ID,
                        ClientRole.SHOP_USER,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.SHOP_FAILED,
                        EDA_SHOP_ID,
                        ClientRole.BUSINESS_USER,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.USER_CHANGED_MIND,
                        EDA_SHOP_ID,
                        ClientRole.CALL_CENTER_OPERATOR,
                        false,
                },
                new Object[]{
                        OrderStatus.PROCESSING,
                        OrderSubstatus.USER_CHANGED_MIND,
                        EDA_SHOP_ID,
                        ClientRole.USER,
                        true,
                }
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testChangeStatusToCancelledFromProcessing(OrderStatus statusFrom,
                                                          OrderSubstatus substatus,
                                                          Long shopId,
                                                          ClientRole clientRole,
                                                          boolean expectError) {
        //setup
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setShopId(shopId);
        parameters.getReportParameters().setIsEda(true);
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
                OrderStatus.CANCELLED,
                substatus,
                resultActionsContainer,
                null
        );
        if (!expectError) {
            assertThat(updated.getStatus(), is(OrderStatus.CANCELLED));
        }
    }

    private ClientInfo generateClientInfo(ClientRole role,
                                          Order order) {
        long id = 1L;
        long shopId = 1L;
        Long businessId = null;
        switch (role) {
            case SHOP:
            case SHOP_USER:
                id = order.getShopId();
                shopId = order.getShopId();
                break;
            case BUSINESS:
                id = order.getBusinessId();
                businessId = order.getBusinessId();
                break;
            case BUSINESS_USER:
                id = order.getBuyer().getUid();
                businessId = order.getBusinessId();
                break;
            case USER:
                id = order.getBuyer().getUid();
                break;
            default:
        }
        return new ClientInfo(role, id, shopId, businessId);
    }
}
