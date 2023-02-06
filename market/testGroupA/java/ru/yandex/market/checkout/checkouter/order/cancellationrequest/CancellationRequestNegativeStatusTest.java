package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public class CancellationRequestNegativeStatusTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{OrderStatus.DELIVERED, false},
                new Object[]{OrderStatus.DELIVERED, true},
                new Object[]{CANCELLED, false},
                new Object[]{CANCELLED, true}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequestWithoutSubstatusMap(OrderStatus status, boolean isCreateByOrderEditApi)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, status);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID),
                    new ResultActionsContainer()
                            .andExpect(status().is(400)));
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID),
                    new ResultActionsContainer()
                            .andExpect(status().is(400)));
        }
    }
}
