package ru.yandex.market.checkout.checkouter.pay;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.creditbroker.CreditBrokerContext;
import ru.yandex.market.checkout.checkouter.pay.creditbroker.CreditBrokerContextFactory;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_CREDIT_BROKER;

public class CreditBrokerPaymentTest extends AbstractWebTestBase {

    @Autowired
    protected PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private CreditBrokerContextFactory creditBrokerContextFactory;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @BeforeEach
    public void setup() {
        checkouterFeatureWriter.writeValue(ENABLE_CREDIT_BROKER, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actualizeShowCreditBrokerFlagTest(boolean showCreditBroker) {
        Parameters parameters = getCreditBrokerParameters(showCreditBroker);
        MultiCart cart = orderCreateHelper.cart(parameters);

        if (showCreditBroker) {
            assertThat(cart.getCreditInformation(), notNullValue());
            validateCreditInformationOptions(cart.getCreditInformation());
        } else {
            assertThat(cart.getCreditInformation(), notNullValue());
            assertThat(cart.getCreditInformation().getOptions(), nullValue());
        }
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "3:RUR:3614",
                    "6:RUR:1917",
                    "12:RUR:1075",
                    "24:RUR:667",
            },
            delimiter = ':'
    )
    void selectedCreditOptionShouldBeProcessedInCartTest(String term, String expectedMonthlyPaymentCurrency,
                                                         String expectedMonthlyPaymentValue) {
        Parameters parameters = getCreditBrokerParameters(true);
        CreditInformation creditInformation = new CreditInformation(new CreditOption(term, null));
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);

        MultiCart cart = orderCreateHelper.cart(parameters);

        validateCreditInformationOptions(cart.getCreditInformation());
        CreditOption selectedCreditOption = cart.getCreditInformation().getSelected();
        assertNotNull(selectedCreditOption);
        assertEquals(term, selectedCreditOption.getTerm());
        MonthlyPayment expectedMonthlyPayment = new MonthlyPayment(Currency.findByName(expectedMonthlyPaymentCurrency),
                expectedMonthlyPaymentValue);
        assertEquals(expectedMonthlyPayment, selectedCreditOption.getMonthlyPayment());
    }

    @Test
    void selectedCreditOptionShouldNotCauseProblemsWhenShowCreditBrokerIsOff() {
        Parameters parameters = getCreditBrokerParameters(false);
        CreditInformation creditInformation = new CreditInformation(new CreditOption("3", null));
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCreditInformation(), notNullValue());
        assertThat(cart.getCreditInformation().getOptions(), nullValue());
    }

    @Test
    void shouldPayViaCreditStrategy() throws Exception {
        Parameters parameters = getCreditBrokerParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateCreditInformationOptions(multiCart.getCreditInformation());

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(multiCart.getCreditInformation().getOptions().get(0));
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = multiOrder.getOrders().get(0);
        Payment payment = orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        Payment storedPayment = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentMethod.TINKOFF_CREDIT, order.getPaymentMethod());
        assertEquals(PaymentGoal.TINKOFF_CREDIT, storedPayment.getType());
        assertEquals(3, storedPayment.getProperties().getCreditDurationInMonths());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(value = {"3", "6", "12", "24"})
    void checkoutWithVariousTerms(String term) throws Exception {
        Parameters parameters = getCreditBrokerParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateCreditInformationOptions(multiCart.getCreditInformation());

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(
                multiCart.getCreditInformation().getOptions().stream()
                        .filter(option -> term.equals(option.getTerm()))
                        .findAny()
                        .orElseThrow());
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = multiOrder.getOrders().get(0);
        Assertions.assertThat(order.getPaymentSubmethod().name()).isEqualTo("CREDIT_BROKER_" + term);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        Payment payment = order.getPayment();
        assertEquals(PaymentMethod.TINKOFF_CREDIT, order.getPaymentMethod());
        assertEquals(PaymentGoal.TINKOFF_CREDIT, payment.getType());
        Assertions.assertThat(order.getPaymentSubmethod().name()).isEqualTo("CREDIT_BROKER_" + term);
    }

    @Test
    public void testEmptySelectedOnCheckout() throws Exception {
        Parameters parameters = getCreditBrokerParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateCreditInformationOptions(multiCart.getCreditInformation());

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(null);
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertTrue(multiOrder.hasErrors());
    }

    @Test
    public void testIllegalSelectedOnCheckout() throws Exception {
        Parameters parameters = getCreditBrokerParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateCreditInformationOptions(multiCart.getCreditInformation());

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(new CreditOption("5", new MonthlyPayment(Currency.RUR, "3450")));
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertTrue(multiOrder.hasOrderErrors());
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "true:market_credit_broker_processing",
                    "false:tinkoff_credit_processing",
            },
            delimiter = ':'
    )
    public void testCreditBrokerPreferredProcessingCcPassedToTrust(
            boolean showCreditBroker, String expectedPreferredProcessingCc) throws Exception {
        Parameters parameters = getCreditBrokerParameters(showCreditBroker);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        if (showCreditBroker) {
            CreditOption selectedOption = multiCart.getCreditInformation().getOptions().get(0);
            parameters.getBuiltMultiCart().setCreditInformation(new CreditInformation(selectedOption));
        }
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = multiOrder.getOrders().get(0);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        orderPayHelper.payForOrder(order);

        JsonObject creditJsonRequest =
                TrustCallsChecker.getRequestBodyAsJson(trustMockConfigurer.servedEvents().stream()
                        .filter(e -> TrustMockConfigurer.CREATE_CREDIT.equals(e.getStubMapping().getName()))
                        .findFirst().orElseGet(() -> fail("creditJsonRequest not found")));
        assertEquals(expectedPreferredProcessingCc,
                creditJsonRequest.getAsJsonObject("pass_params").getAsJsonObject("terminal_route_data").get(
                        "preferred_processing_cc").getAsString());
    }

    @Test
    public void testCreditBrokerParamsPassedToTrustAndCorrectFormLinkReturned() throws Exception {
        String applicationId = "1c6cf5af-4206-4039-a3b2-902611adcdf0";
        String formLink = "https://something.ru/credit-application?id=1c6cf5af-4206-4039-a3b2-902611adcdf0";
        Integer creditDurationInMonths = 3;
        mockCreditBrokerContext(applicationId, formLink, creditDurationInMonths);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setShowCreditBroker(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        CreditOption selectedOption = multiCart.getCreditInformation().getOptions().get(0);
        parameters.getBuiltMultiCart().setCreditInformation(new CreditInformation(selectedOption));
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = multiOrder.getOrders().get(0);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        Payment payment = orderPayHelper.payForOrder(order);
        assertEquals(formLink, payment.getPaymentUrl());
        assertEquals(new PaymentForm(formLink), payment.getPaymentForm());

        JsonObject creditJsonRequest =
                TrustCallsChecker.getRequestBodyAsJson(trustMockConfigurer.servedEvents().stream()
                        .filter(e -> TrustMockConfigurer.CREATE_CREDIT.equals(e.getStubMapping().getName()))
                        .findFirst().orElseGet(() -> fail("creditJsonRequest not found")));
        assertEquals(creditDurationInMonths,
                creditJsonRequest.getAsJsonObject("pass_params").getAsJsonObject("credit").get(
                        "credit_duration_in_months").getAsInt());
        assertEquals(applicationId,
                creditJsonRequest.getAsJsonObject("pass_params").getAsJsonObject("credit").get(
                        "application_id").getAsString());
    }

    private void validateCreditInformationOptions(CreditInformation creditInformation) {
        assertThat(creditInformation.getOptions(), hasSize(4));
        assertThat(creditInformation.getOptions(), containsInAnyOrder(
                allOf(
                        hasProperty("term", is("3")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("3614"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("6")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("1917"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("12")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("1075"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("24")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("667"))
                        ))
                )
        ));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "11111111,https://m.market.yandex.ru/",
                    "22222222,https://some-custom-url.yandex.ru/",
                    "33333333,https://another-custom-url.yandex.ru/",
            }
    )
    public void customBaseUrlTest(long uid, String expectedBaseUrl) throws Exception {
        checkouterFeatureWriter.writeValue(MapFeatureType.CREDIT_BROKER_CUSTOM_BASE_URLS,
                Map.of(22222222L, "https://some-custom-url.yandex.ru/",
                        33333333L, "https://another-custom-url.yandex.ru/"));

        Parameters parameters = getCreditBrokerParameters();
        parameters.getBuyer().setUid(uid);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateCreditInformationOptions(multiCart.getCreditInformation());

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(multiCart.getCreditInformation().getOptions().get(0));
        parameters.getBuiltMultiCart().setCreditInformation(creditInformation);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = multiOrder.getOrders().get(0);
        Payment payment = orderPayHelper.payForOrder(order);

        assertTrue(payment.getPaymentUrl().startsWith(expectedBaseUrl));
    }

    private void mockCreditBrokerContext(String applicationId, String formLink, Integer creditDurationInMonths) {
        CreditBrokerContext creditBrokerContext = new CreditBrokerContext(UUID.fromString(applicationId),
                URI.create(formLink), creditDurationInMonths);
        doReturn(creditBrokerContext).when(creditBrokerContextFactory).construct(any());
    }

    private Parameters getCreditBrokerParameters() {
        return getCreditBrokerParameters(true);
    }

    private Parameters getCreditBrokerParameters(boolean showCreditBroker) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setShowCreditBroker(showCreditBroker);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        return parameters;
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public CreditBrokerContextFactory creditBrokerContextFactory(
                @Autowired CreditBrokerContextFactory creditBrokerContextFactory) {
            return Mockito.spy(creditBrokerContextFactory);
        }
    }
}
