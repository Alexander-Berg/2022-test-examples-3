package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.EnumMap;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public abstract class AbstractCancellationRequestNegativeSubstatusTestBase extends AbstractWebTestBase {

    private static final String NOTES = "notes";
    private final EnumMap<OrderStatus, Order> ordersMap = new EnumMap<>(OrderStatus.class);
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    @BeforeEach
    public void createOrders() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        for (OrderStatus status : new OrderStatus[]{OrderStatus.UNPAID, OrderStatus.PENDING,
                OrderStatus.PROCESSING, OrderStatus.DELIVERY,
                OrderStatus.PICKUP}) {
            Order order;
            if (status == OrderStatus.PENDING) {
                Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
                parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
                parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

                Long orderId = orderCreateHelper.createOrder(parameters).getId();
                order = orderService.getOrder(orderId);
            } else {
                order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                        .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                        .withDeliveryType(DeliveryType.PICKUP)
                        .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                        .withColor(BLUE)
                        .withPartnerInterface(false)
                        .build();
                orderStatusHelper.proceedOrderToStatus(order, status);
            }

            ordersMap.put(status, order);
        }
    }

    protected void createCancellationRequest(ClientInfo clientInfo, OrderStatus status, OrderSubstatus substatus,
                                             boolean isCreateByOrderEditApi) throws Exception {
        Order order = Objects.requireNonNull(ordersMap.get(status));
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, NOTES);
        ResultActionsContainer resultActionsContainer = new ResultActionsContainer();
        resultActionsContainer.andExpect(status().is(400));
        resultActionsContainer.andExpect(content().json("{\"status\":400," +
                "\"code\":\"CANCELLATION_SUBSTATUS_NOT_ALLOWED\"," +
                "\"message\":\"Cancellation request substatus " + substatus +
                " is not available for status " + status +
                " and role " + clientInfo.getRole() + " for order " + order.getId() + "\"}"));

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo, resultActionsContainer);
        } else {
            cancellationRequestHelper.createCancellationRequest(order.getId(), cancellationRequest,
                    clientInfo, resultActionsContainer);
        }
    }
}
