package ru.yandex.market.checkout.checkouter.degradation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.TrustService;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.saturn.ScoringRequest;
import ru.yandex.market.checkout.checkouter.saturn.ScoringRequestBasket;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.saturn.SaturnMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

@MockBean(classes = {TrustService.class})
public class TrustPayDegradationServiceTest extends AbstractWebTestBase {

    private static final Set<PaymentMethod> SUPPORTED_METHODS = Set.of(
            PaymentMethod.YANDEX,
            PaymentMethod.CASH_ON_DELIVERY,
            PaymentMethod.CARD_ON_DELIVERY
    );
    private static final Set<PaymentMethod> CASH_METHODS = Set.of(
            PaymentMethod.CASH_ON_DELIVERY,
            PaymentMethod.CARD_ON_DELIVERY
    );

    @Autowired
    private SaturnMockConfigurer configurer;

    @AfterEach
    void setDefaults() {
        checkouterProperties.setEnableTrustPayDegradationStrategy(false);
        checkouterProperties.setTrustPayDegradationStrategyUsers(Set.of());
    }

    @Test
    @DisplayName("POSITIVE: Траст доступен, заказ создался в статусе UNPAID - WAITING_USER_INPUT")
    void positiveTrustAvailableCheckoutStatusTest() {
        Parameters parameters = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertEquals(OrderSubstatus.WAITING_USER_INPUT, order.getSubstatus());
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен, заказ создался в статусе UNPAID - AWAIT_PAYMENT")
    void positiveTrustUnavailableCheckoutStatusTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, 0.5);
        trustMockConfigurer.mockBindings();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_PAYMENT, order.getSubstatus());
    }

    @Test
    @DisplayName("NEGATIVE: Траст недоступен. В заказе выбран неподдерживаемый, во время деградации, способ оплаты")
    void positiveTrustUnavailableCheckoutUnsupportedPaymentMethodTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, 0.5);
        trustMockConfigurer.mockBindings();

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        parameters.configuration()
                .checkout()
                .response()
                .setErrorMatcher(jsonPath("$.orderFailures[0].errorDetails")
                        .value("Actualization error: no available payment methods during trust degradation."));
        parameters.configuration().checkout().response().setUseErrorMatcher(true);
        parameters.configuration().checkout().response().setCheckOrderCreateErrors(false);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен. Актуализация удаляет неподдерживаемые способы оплаты")
    void positiveTrustUnavailableCartUnsupportedPaymentMethodTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, 0.5);
        trustMockConfigurer.mockBindings();

        Parameters parameters = defaultBlueOrderParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.getCarts().forEach(it -> assertTrue(SUPPORTED_METHODS.containsAll(it.getPaymentOptions())));
        cart.getCarts().forEach(it -> assertTrue(it.getDeliveryOptions()
                .stream()
                .map(Delivery::getPaymentOptions)
                .flatMap(Collection::stream)
                .allMatch(SUPPORTED_METHODS::contains)
        ));
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен. У пользователя есть привязанная карта и неудовлетворительный скоринг")
    void positiveTrustCartNegativeScoringTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, -0.5);
        trustMockConfigurer.mockBindings();

        Parameters parameters = defaultBlueOrderParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.getCarts().forEach(it -> assertTrue(CASH_METHODS.containsAll(it.getPaymentOptions())));
        cart.getCarts().forEach(it -> assertTrue(it.getDeliveryOptions()
                .stream()
                .map(Delivery::getPaymentOptions)
                .flatMap(Collection::stream)
                .allMatch(CASH_METHODS::contains)
        ));
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен. У пользователя нет привязанной карты")
    void positiveTrustCartNoCardsTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, -0.5);
        trustMockConfigurer.mockBindings("bindings_response_without_cards.json");

        Parameters parameters = defaultBlueOrderParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.getCarts().forEach(it -> assertTrue(CASH_METHODS.containsAll(it.getPaymentOptions())));
        cart.getCarts().forEach(it -> assertTrue(it.getDeliveryOptions()
                .stream()
                .map(Delivery::getPaymentOptions)
                .flatMap(Collection::stream)
                .allMatch(CASH_METHODS::contains)
        ));
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен. Пользователь не состоит в списке деградации")
    void positiveTrustCartNotTargetUserTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);
        checkouterProperties.setTrustPayDegradationStrategyUsers(Set.of(0L));

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, 0.5);
        trustMockConfigurer.mockBindings();

        Buyer buyer = new Buyer(1L);
        buyer.setFirstName(RandomStringUtils.randomAlphabetic(10));
        buyer.setLastName(RandomStringUtils.randomAlphabetic(10));
        buyer.setPhone("+79000010101");
        buyer.setEmail("a@a.ru");
        buyer.setRegionId(213L);
        buyer.setYandexEmployee(false);

        Parameters parameters = defaultBlueOrderParameters(buyer);

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertEquals(OrderSubstatus.WAITING_USER_INPUT, order.getSubstatus());
    }

    @Test
    @DisplayName("POSITIVE: Траст недоступен. Пользователь состоит в списке деградации")
    void positiveTrustCartTargetUserTest() throws IOException {
        checkouterProperties.setEnableTrustPayDegradationStrategy(true);
        checkouterProperties.setTrustPayDegradationStrategyUsers(Set.of(1L));

        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                RandomUtils.nextLong(),
                new ScoringRequestBasket(new BigDecimal(RandomUtils.nextInt()), new BigDecimal(RandomUtils.nextInt())));

        configurer.mockScoring(request, 0.5);
        trustMockConfigurer.mockBindings();

        Buyer buyer = new Buyer(1L);
        buyer.setFirstName(RandomStringUtils.randomAlphabetic(10));
        buyer.setLastName(RandomStringUtils.randomAlphabetic(10));
        buyer.setPhone("+79000010101");
        buyer.setEmail("a@a.ru");
        buyer.setRegionId(213L);
        buyer.setYandexEmployee(false);

        Parameters parameters = defaultBlueOrderParameters(buyer);

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_PAYMENT, order.getSubstatus());
    }
}
