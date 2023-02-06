package ru.yandex.market.checkout.checkouter.pay;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CardInfo;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.PaymentFeature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YaCardPaymentTest extends AbstractWebTestBase {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    public void createOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuiltMultiCart().setCardInfo(crateYaCardInfo());
        parameters.setMockLoyalty(true);
        order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), equalTo(OrderStatus.UNPAID));
        trustMockConfigurer.mockWholeTrust();
    }

    @Test
    public void shouldSaveYaCardPaymentProperties() throws Exception {
        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyYaCardCrear(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        checkPaymentProperties(payment);
        order = orderService.getOrder(order.getId());
        checkOrderPropery(order);
    }

    @Test
    public void checkLoyaltyParamsTransfer() {
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        List<ServeEvent> calcEvents = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl()))
                .collect(Collectors.toList());
        ServeEvent calcEvent = calcEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );
        assertNotNull(discountRequest.getPaymentInfo());
        assertTrue(discountRequest.getPaymentInfo().getPaymentFeatures().contains(PaymentFeature.YA_BANK));
        assertNotNull(discountRequest.getPaymentInfo().getPaymentMethodId());
    }

    private void checkOrderPropery(Order order) {
        assertNotNull(order.getProperty(OrderPropertyType.YA_CARD));
        assertTrue(order.getProperty(OrderPropertyType.YA_CARD));
    }

    private void checkPaymentProperties(Payment payment) {
        assertNotNull(payment.getProperties());
        assertTrue(payment.getProperties().isYaCard());
        assertNotNull(payment.getProperties().getRRN());
        assertNotNull(payment.getProperties().getApprovalCode());
        assertNotNull(payment.getProperties().getPaymentMethodId());
    }

    private CardInfo crateYaCardInfo() {
        CardInfo info = new CardInfo();
        info.setYaCardSelected(true);
        info.setSelectedCardId("card-1234xyz");
        return info;
    }
}
