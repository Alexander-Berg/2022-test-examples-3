package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrder;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FRAUD;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_EMIT_CASHBACK;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;


public class BnplOrderTest extends AbstractWebTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "DELIVERY", "PICKUP"})
    void bnplIsAbleToCancelOrder(OrderStatus status) {
        Parameters parameters = defaultBnplParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, status);

        var cancellationRequest = new CompatibleCancellationRequest(USER_FRAUD.name(), null);
        order = client.createCancellationRequest(order.getId(),
                cancellationRequest, ClientRole.ANTIFRAUD_BNPL, 0L);

        assertThat(order.getCancellationRequest().getSubstatus(), equalTo(USER_FRAUD));
    }

    @Test
    public void bnplOrderChackbackTest() throws IOException {
        Parameters parameters = defaultBnplParameters();
        var order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        var bnplOrder = createMockForGetBnplOrder();
        bnplOrder.setOrderStatus(BnplOrderStatus.COMMITED);
        bnplMockConfigurer.mockGetBnplOrder(bnplOrder);
        orderPayHelper.notifyBnplFinished(payment);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
    }

    private BnplOrder createMockForGetBnplOrder() throws IOException {
        var mockBnplOrder = bnplMockConfigurer.getDefaultBnplOrderResponse();
        return bnplMockConfigurer
                .findEventsByStubName(BnplMockConfigurer.POST_ORDER_CREATE)
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .findFirst()
                .map(request -> parse(request, BnplOrder.class))
                .map(request -> {
                            mockBnplOrder.setOrderServices(request.getOrderServices());
                            mockBnplOrder.setUserId(request.getUserId());
                            return mockBnplOrder;
                        }
                ).orElseThrow();
    }

    @Nullable
    private <T> T parse(String request, Class<T> clazz) {
        try {
            return checkouterAnnotationObjectMapper.readValue(request, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e);
            return null;
        }
    }
}
