package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DescribedSubstatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FORGOT_TO_USE_BONUS;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * Тест проверяет, что на все подстатусы имеющиеся в системе есть тексты, которые показываются на фронте.
 * Если у вас упал тест и вы добавляли новый подстатус - нужно дойти до редакторов и получить для него текст.
 * Параллельно стоит подумать хотите ли вы чтобы пользователь видел этот подстатус вообще.
 *
 * @author mmetlov
 */

public class SubstatusTextTest extends AbstractWebTestBase {

    public static final String USER_CHANGED_MIND_TEXT = "Товар больше не нужен";
    public static final String USER_FORGOT_TO_USE_BONUS_TEXT = "Хочу использовать бонус или промокод";
    private static final String NOTES = "notes";
    @Autowired
    private OrderStatusGraph orderStatusGraph;

    public static Stream<Arguments> parameterizedTestData() {
        EnumSet<OrderSubstatus> substatuses = EnumSet.allOf(OrderSubstatus.class);
        substatuses.remove(OrderSubstatus.UNKNOWN);
        substatuses.removeIf(os -> os.getStatus() != OrderStatus.CANCELLED);
        return substatuses.stream().map(s -> new Object[]{s}).collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldHaveTextForSubstatusTest(OrderSubstatus substatus) {
        Set<DescribedSubstatus> describedSubstatuses =
                orderStatusGraph.toDescribedSubstatuses(Collections.singleton(substatus));
        assertEquals(1, describedSubstatuses.size());
        DescribedSubstatus describedSubstatus = describedSubstatuses.stream().findFirst().get();
        assertNotNull(describedSubstatus.getText());
        assertFalse(describedSubstatus.getText().isEmpty());
    }


    public static class OrdersTest extends AbstractWebTestBase {

        @Autowired
        private CancellationRequestHelper cancellationRequestHelper;
        @Autowired
        private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

        private Order order;

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?rgb=BLUE&id={id}")},
                    new Object[]{"GET /orders/by-uid/{uid}", parameterizedGetRequest("/orders/by-uid/"
                            + BuyerProvider.UID + "?rgb=BLUE&id={id}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest(
                            "/get-orders", "{\"rgbs\":[\"BLUE\"],\"orderIds\":[%s]}")}
            ).stream().map(Arguments::of);
        }

        @BeforeEach
        public void init() throws Exception {
            setFixedTime(INTAKE_AVAILABLE_DATE);
            order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                    .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .withDeliveryType(DeliveryType.DELIVERY)
                    .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                    .withColor(Color.BLUE)
                    .build();
            CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
            order = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID));
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void test(String caseName, GetOrdersUtils.ParameterizedRequest<Long> parameterizedRequest)
                throws Exception {
            ResultActions resultActions = mockMvc.perform(
                    parameterizedRequest.build(order.getId())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].substatusText").value(USER_CHANGED_MIND_TEXT));
            if (caseName.contains("by-uid")) {
                resultActions.andExpect(jsonPath("$.orders[0].cancellationRequest").doesNotExist());
            } else {
                resultActions.andExpect(jsonPath("$.orders[0].cancellationRequest.substatusText")
                        .value(USER_CHANGED_MIND_TEXT));
            }
        }
    }

    public static class OrderTest extends AbstractWebTestBase {

        @Autowired
        private CancellationRequestHelper cancellationRequestHelper;
        @Autowired
        private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

        public static Stream<Arguments> parameterizedTestData() {
            return Stream.of(
                    new Object[]{USER_CHANGED_MIND, USER_CHANGED_MIND_TEXT},
                    new Object[]{USER_FORGOT_TO_USE_BONUS, USER_FORGOT_TO_USE_BONUS_TEXT}
            ).map(Arguments::of);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void shouldAddTextAboutChangedMindToOrder(OrderSubstatus expectedSubStatus, String expectedText)
                throws Exception {
            setFixedTime(INTAKE_AVAILABLE_DATE);
            Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                    .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .withDeliveryType(DeliveryType.DELIVERY)
                    .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                    .withColor(Color.BLUE)
                    .build();
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
            CancellationRequest cancellationRequest = new CancellationRequest(expectedSubStatus, NOTES);
            order = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID));
            assertEquals(expectedText, order.getCancellationRequest().getSubstatusText());
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
            assertEquals(expectedText, order.getSubstatusText());
            assertEquals(expectedText, order.getCancellationRequest().getSubstatusText());
            order = orderService.getOrder(order.getId());
            assertEquals(expectedText, order.getSubstatusText());
            assertEquals(expectedText, order.getCancellationRequest().getSubstatusText());
        }
    }
}
