package ru.yandex.market.checkout.checkouter.controllers.oms;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.json.PaymentJsonHandlerTest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;

public class OrderEditControllerTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Autowired
    private ObjectMapper actualizationLoggingMapper;

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Test
    public void testWithIgnore() throws Exception {
        Order order = createOrder();

        orderPayHelper.payForOrder(order);
        orderUpdateService.updateOrderStatus(order.getId(),
                StatusAndSubstatus.of(OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                ClientInfo.SYSTEM);

        OrderEditOptions orderEditOptions2 = request(order.getId());

        assertEquals(2, orderEditOptions2.getDeliveryOptions().size());
    }

    @Test
    public void testWithoutIgnore() throws Exception {
        Order order = createOrder();

        // 1) отсекается неравные deliveryServiceId -> остается 1 MOCK_INTAKE_DELIVERY_SERVICE_ID
        // 2) идем по ветке проверки ShipmentDay || ShipmentDate, а они по тесту null -> остается 0
        OrderEditOptions orderEditOptions1 = request(order.getId());
        assertTrue(orderEditOptions1.getDeliveryOptions().isEmpty());
    }

    private Order createOrder() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                Collections.singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.getOrder().setPreorder(true);
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.PREORDER_OK);

        OrderItem orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        FoundOfferBuilder from = FoundOfferBuilder.createFrom(orderItem).preorder(true);
        parameters.getReportParameters().setOffers(List.of(from.build()));
        parameters.getReportParameters().getActualDelivery().getResults()
                .get(0).setDelivery(Arrays.asList(buildDeliveryOption(MOCK_DELIVERY_SERVICE_ID),
                buildDeliveryOption(MOCK_INTAKE_DELIVERY_SERVICE_ID)));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        multiCart.getCarts().get(0).setPreorder(true);
        multiCart.getCarts().get(0).setPayment(getPayment());
        reportMock.resetRequests();
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        return multiOrder.getOrders().get(0);
    }

    private OrderEditOptions request(Long orderId) throws Exception {
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/edit-options", orderId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .content(actualizationLoggingMapper.writeValueAsString(orderEditOptionsRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        String contentAsString = mockMvc.perform(builder)
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return actualizationLoggingMapper.readValue(contentAsString, OrderEditOptions.class);
    }

    private ActualDeliveryOption buildDeliveryOption(long deliveryServiceId) {
        ActualDeliveryOption actualDeliveryOption = new ActualDeliveryOption();
        actualDeliveryOption.setDeliveryServiceId(deliveryServiceId);
        actualDeliveryOption.setCost(BigDecimal.TEN);
        actualDeliveryOption.setDayFrom(1);
        actualDeliveryOption.setDayTo(2);
        actualDeliveryOption.setCurrency(Currency.RUR);
        actualDeliveryOption.setTimeIntervals(Collections.singletonList(new DeliveryTimeInterval(LocalTime.MIN,
                LocalTime.MAX)));
        actualDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        return actualDeliveryOption;
    }

    public static Payment getPayment() {
        Payment payment = new Payment();
        payment.setId(PaymentJsonHandlerTest.ID);
        payment.setOrderId(PaymentJsonHandlerTest.ORDER_ID);
        payment.setBasketId(PaymentJsonHandlerTest.BASKET_ID);
        payment.setFake(PaymentJsonHandlerTest.FAKE);
        payment.setStatus(PaymentJsonHandlerTest.STATUS);
        payment.setSubstatus(PaymentJsonHandlerTest.SUBSTATUS);
        payment.setFailReason(PaymentJsonHandlerTest.FAIL_REASON);
        payment.setUid(PaymentJsonHandlerTest.UID);
        return payment;
    }

}
