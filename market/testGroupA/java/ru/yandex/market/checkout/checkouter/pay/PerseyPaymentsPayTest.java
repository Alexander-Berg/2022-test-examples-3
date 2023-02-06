package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.persey.model.InternalPayPaymentTech;
import ru.yandex.market.checkout.checkouter.persey.model.PayOrderDonationRequest;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.perseypayments.PerseyMockConfigurer;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.firstOrNull;

public class PerseyPaymentsPayTest extends AbstractWebTestBase {

    @Autowired
    private BalanceMockHelper balanceMockHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PerseyMockConfigurer perseyMockConfigurer;
    @Autowired
    private WireMockServer perseyPaymentsMock;
    @Autowired
    private ObjectMapper perseyObjectMapper;

    @BeforeEach
    public void setup() {
        perseyPaymentsMock.resetRequests();

        perseyMockConfigurer.mockPay();
        balanceMockHelper.mockWholeBalance();
    }

    @Test
    public void perseyPaymentShouldBeDisabledByDefault() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payHelper.payForOrder(order);

        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"card-x12345", "new_card", "apple_token", "google_pay"})
    public void testPerseyPaymentEnabled(String payMethodId) {
        checkouterProperties.setEnableHelpingHandPay(true);

        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payHelper.payForOrderWithoutNotification(order);
        Order paidOrder = orderService.getOrder(order.getId());
        Payment payment = paidOrder.getPayment();

        CheckBasketParams config = new CheckBasketParams();
        config.setPayMethodId(payMethodId);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        payHelper.notifyPayment(payment);

        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(payMethodId.startsWith("card") ? 1 : 0));
    }

    @Test
    public void testShouldSendPayForCompositePayment() {
        checkouterProperties.setEnableHelpingHandPay(true);

        // создание ордера со списанием кешбэка, чтобы в последствии был payment partition кешбэчный
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(
                parameters
        );
        Order order = firstOrNull(emptyIfNull(multiOrder.getCarts()));

        payHelper.payForOrderWithoutNotification(order);
        Order paidOrder = orderService.getOrder(order.getId());
        Payment payment = paidOrder.getPayment();

        // баланс возвращает payMethodType=composite
        CheckBasketParams config = new CheckBasketParams();
        config.setPayMethod("composite");
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        payHelper.notifyPayment(payment);

        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(1));
    }

    @Test
    public void testWithRemoteIpHeader() throws IOException {
        checkouterProperties.setEnableHelpingHandPay(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setUid(1L);
        parameters.getBuyer().setIp("1.1.1.1");
        parameters.setPlatform(Platform.IOS);

        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(1));
        ServeEvent event = events.get(0);
        LoggedRequest actualRequest = event.getRequest();
        assertEquals("1.1.1.1", actualRequest.getHeader("X-Remote-Ip"));
        assertEquals("1", actualRequest.getHeader("X-Yandex-UID"));
        assertEquals(Platform.IOS.toString(), actualRequest.getHeader("X-Application"));

        PayOrderDonationRequest actualBody = perseyObjectMapper.readValue(actualRequest.getBodyAsString(),
                PayOrderDonationRequest.class);

        checkPayOrderDonationRequest(actualBody, order.getId().toString());
    }

    @Test
    public void testWithoutRemoteIpHeader() throws IOException {
        checkouterProperties.setEnableHelpingHandPay(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setUid(1L);
        parameters.setPlatform(Platform.YANDEX_GO_ANDROID);

        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(1));
        ServeEvent event = events.get(0);
        LoggedRequest actualRequest = event.getRequest();
        assertEquals("127.0.0.1", actualRequest.getHeader("X-Remote-Ip"));
        assertEquals("1", actualRequest.getHeader("X-Yandex-UID"));
        assertEquals(Platform.YANDEX_GO_ANDROID.toString(), actualRequest.getHeader("X-Application"));

        PayOrderDonationRequest actualBody = perseyObjectMapper.readValue(actualRequest.getBodyAsString(),
                PayOrderDonationRequest.class);

        checkPayOrderDonationRequest(actualBody, order.getId().toString());
    }

    private void checkPayOrderDonationRequest(PayOrderDonationRequest actualRequest, String expectedOrderId) {
        assertEquals(expectedOrderId, actualRequest.getOrderId());
        assertThat(actualRequest.getRideCost().getAmount(), comparesEqualTo(BigDecimal.valueOf(350L)));
        assertEquals("RUB", actualRequest.getRideCost().getCurrencyCode());
        assertEquals(InternalPayPaymentTech.PaymentTypeEnum.CARD, actualRequest.getPaymentTech().getPaymentType());
        assertEquals("card-x", actualRequest.getPaymentTech().getPaymentMethodId());
    }
}
