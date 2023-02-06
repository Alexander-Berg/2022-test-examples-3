package ru.yandex.market.checkout.checkouter.actualization.multicart.processor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.common.report.model.filter.Filter;
import ru.yandex.market.fintech.fintechutils.client.PostpaidControllerApi;
import ru.yandex.market.fintech.fintechutils.client.model.DecisionResultDto;
import ru.yandex.market.fintech.fintechutils.client.model.PostPaidDecideResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.RESALE_FILTERS;
import static ru.yandex.market.checkout.test.providers.OfferFilterProvider.getFilter;

class PostPaidMultiCartProcessorTest extends AbstractWebTestBase {

    private static final Set<PaymentMethod> DEFAULT_PAYMENT_OPTIONS = Set.of(
            PaymentMethod.YANDEX,
            PaymentMethod.GOOGLE_PAY,
            PaymentMethod.CASH_ON_DELIVERY,
            PaymentMethod.CARD_ON_DELIVERY
    );


    @Autowired
    private PostPaidMultiCartProcessor processor;

    @Autowired
    private PostpaidControllerApi postpaidControllerClient;

    @Autowired
    private CheckouterFeatureWriter featureWriter;

    @BeforeEach
    void setUp() {
        trustMockConfigurer.mockWholeTrust();
        featureWriter.writeValue(BooleanFeatureType.ENABLE_FINTECH_POST_PAID_ACTUALIZATION, true);
        featureWriter.writeValue(BooleanFeatureType.FORBID_FBY_POSTPAY, true);
        featureWriter.writeValue(BooleanFeatureType.FORBID_POSTPAY_FOR_PREPAY_SHOPS, true);
        featureWriter.writeValue(BooleanFeatureType.FORBID_FBS_POSTPAY_IN_SIBERIA, true);
        featureWriter.writeValue(CollectionFeatureType.PREPAY_ONLY_FBS_REGION_IDS, Set.of(213));
        Mockito.clearInvocations(postpaidControllerClient);

    }

    @Test
    void test1pFashion() {


        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        // 1p fashion
        parameters.getItems().forEach(item -> {
            item.setSupplierType(SupplierType.FIRST_PARTY);
            item.setCargoTypes(Set.of(OrderTypeUtils.FASHION_CARGO_TYPE));
        });

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(Boolean.TRUE, order.isFulfilment());
        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());

        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cartContainsPostPaid(cart), "Should have postpaid");
        Assertions.assertTrue(deliveryContainsPostPaid(cart));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }


    @Test
    void filterDbsTest() {

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertFalse(cartContainsPostPaid(cart), "Should not have postpaid");
        Assertions.assertFalse(deliveryContainsPostPaid(cart));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }


    @Test
    void testProcessFilterDbs() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(Boolean.TRUE, OrderTypeUtils.isDbs(order));
        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());

        parameters.setCheckCartErrors(false);
        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_OFF_25_EXP, ""));
        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertFalse(cartContainsPostPaid(cart), "Should not have postpaid");
        Assertions.assertFalse(deliveryContainsPostPaid(cart));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    public void test3pWithTrain() {

        Parameters parameters = create3pParameters();

        Order order = parameters.getOrder();

        mockFintechUtilsAnswer(order, false);

        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_TRAIN, ""));

        MultiCart cart = orderCreateHelper.cart(parameters);

        Order actualOrder = cart.getCarts().get(0);
        assertTrue(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));

        Delivery actualDelivery = actualOrder.getDeliveryOptions().get(0);
        assertTrue(actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    public void testFintechUtilsAnswersPostpaidOff() {

        Parameters parameters = create1pParameters();

        OrderItem item = parameters.getItems().iterator().next();

        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_OFF, ""));
        item.setPrepayEnabled(true);

        MultiCart cart = orderCreateHelper.cart(parameters);

        Order actualOrder = cart.getCarts().get(0);
        assertFalse(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        assertFalse(actualOrder.getPaymentOptions().isEmpty());

        Delivery actualDelivery = actualOrder.getDeliveryOptions().get(0);
        assertFalse(actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        assertFalse(actualDelivery.getPaymentOptions().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postpaidOn1PTest(boolean withTrainFlag) {
        Set<PaymentMethod> paymentOptions = Set.of(PaymentMethod.GOOGLE_PAY, PaymentMethod.CASH_ON_DELIVERY,
                PaymentMethod.CARD_ON_DELIVERY);

        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        Order order = parameters.getOrder();
        tuneYandexDelivery(order, paymentOptions);

        mockFintechUtilsAnswer(order, true);

        featureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_ON_1P, true);
        if (withTrainFlag) {
            parameters.setExperiments(Experiments.of(Experiments.POSTPAID_TRAIN, ""));
        }

        // 3p exps should not affect result
        parameters.addExperiment(Experiments.POSTPAID_OFF, "");

        // 1p
        parameters.getItems().forEach(item -> item.setSupplierType(SupplierType.FIRST_PARTY));

        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);

        Assertions.assertTrue(cartContainsPostPaid(cart), "Should have postpaid");
        Assertions.assertTrue(deliveryContainsPostPaid(cart));

        int wantedNumber = withTrainFlag ? 0 : 1;
        Mockito.verify(postpaidControllerClient, Mockito.times(wantedNumber))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    void testMultiCartTrainFlow() {
        Parameters params1p = create1pParameters();
        Order order1P = params1p.getOrder();
        Parameters params3p = create3pParameters();
        Order order3P = params3p.getOrder();

        Parameters multiParams = params1p.addOrder(params3p);

        featureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_ON_1P, true);
        multiParams.setExperiments(
                Experiments.of(Experiments.POSTPAID_TRAIN, "")
        );

        mockFintechUtilsAnswer(Map.of(order1P.getLabel(), false, order3P.getLabel(), false));

        MultiCart multiCart = orderCreateHelper.cart(multiParams);

        multiCart.getCarts().forEach(cart -> {
                    assertTrue(cart.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
                    assertFalse(cart.getPaymentOptions().isEmpty());
                    Delivery actualDelivery = cart.getDeliveryOptions().get(0);
                    assertTrue(actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
                    assertFalse(actualDelivery.getPaymentOptions().isEmpty());
                }
        );

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());

    }

    @ParameterizedTest(name = "Test 3p exp combinations, combination {0}")
    @ValueSource(bytes = {0, 1, 2, 3})
    void testMultiCartWith3pExp(byte arg) {
        // не ну а чо 00 01 10 11
        boolean allow1p = (arg & 1) == 1;
        boolean allow3p = (arg & 3) == 1;

        Parameters params1p = create1pParameters();
        Order order1P = params1p.getOrder();
        Parameters params3p = create3pParameters();
        Order order3P = params3p.getOrder();

        Parameters multiParams = params1p.addOrder(params3p);

        featureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_ON_1P, true);
        multiParams.setExperiments(
                Experiments.of(Experiments.POSTPAID_OFF_50_EXP, "")
        );
        Map<String, Boolean> allowMap = Map.of(order1P.getLabel(), allow1p, order3P.getLabel(), allow3p);
        mockFintechUtilsAnswer(allowMap);

        MultiCart multiCart = orderCreateHelper.cart(multiParams);

        multiCart.getCarts().forEach(cart -> {
                    boolean expected = allowMap.get(cart.getLabel());
                    assertEquals(expected, cart.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
                    assertFalse(cart.getPaymentOptions().isEmpty());
                    Delivery actualDelivery = cart.getDeliveryOptions().get(0);
                    assertEquals(expected,
                            actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
                    assertFalse(actualDelivery.getPaymentOptions().isEmpty());
                }
        );

        // one time for multicart
        Mockito.verify(postpaidControllerClient, Mockito.times(1))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }


    @Test
    void postpaidOn100Test() {

        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_ON_100, ""));
        // 3p
        parameters.getItems().forEach(item -> item.setSupplierType(SupplierType.THIRD_PARTY));


        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cartContainsPostPaid(cart), "Should have postpaid");
        Assertions.assertTrue(deliveryContainsPostPaid(cart));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }


    @Test
    public void testFintechUtilsAnswersDisable() {

        Set<PaymentMethod> paymentOptions = Set.of(PaymentMethod.GOOGLE_PAY, PaymentMethod.CASH_ON_DELIVERY,
                PaymentMethod.CARD_ON_DELIVERY);

        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.configuration().cart().response().setCheckCartErrors(false);
        Order order = parameters.getOrder();

        tuneYandexDelivery(order, paymentOptions);
        mockFintechUtilsAnswer(order, false);

        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_OFF_25_EXP, ""));
        parameters.getItems().forEach(item -> {
            item.setSupplierType(SupplierType.THIRD_PARTY);
            item.setPrepayEnabled(true);
        });
        order.setPaymentOptions(paymentOptions);
        MultiCart cart = orderCreateHelper.cart(parameters);

        Order actualOrder = cart.getCarts().get(0);
        assertFalse(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        assertFalse(actualOrder.getPaymentOptions().isEmpty());

        Delivery actualDelivery = actualOrder.getDeliveryOptions().get(0);
        assertFalse(actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        assertFalse(actualDelivery.getPaymentOptions().isEmpty());

        Mockito.verify(postpaidControllerClient, Mockito.times(1))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    public void testFintechUtilsAnswersEnable() {

        Parameters parameters = create3pParameters();

        mockFintechUtilsAnswer(parameters.getOrder(), true);

        parameters.setExperiments(Experiments.of(Experiments.POSTPAID_OFF_25_EXP, ""));

        MultiCart cart = orderCreateHelper.cart(parameters);

        Order actualOrder = cart.getCarts().get(0);
        assertTrue(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));

        Delivery actualDelivery = actualOrder.getDeliveryOptions().get(0);
        assertTrue(actualDelivery.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));

        Mockito.verify(postpaidControllerClient, Mockito.times(1))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testFbsPostpaidExperimentEffect(boolean postpaidOn100) {
        var fbsParams = DropshipDeliveryHelper.getDropshipPostpaidParameters();
        if (postpaidOn100) {
            fbsParams.setExperiments(Experiments.POSTPAID_ON_100);
        }
        MultiCart cart = orderCreateHelper.cart(fbsParams);

        Order actualOrder = cart.getCarts().get(0);
        if (postpaidOn100) {
            assertTrue(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        } else {
            assertFalse(actualOrder.getPaymentOptions().stream().anyMatch(PaymentMethod::isPostPaid));
        }
        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldAddPostPaidIfOrderIsResale() throws IOException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_FOR_RESALE, true);
        //arrange
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        String filterId = "validId";
        String filterValueId = "validValueId";
        checkouterFeatureWriter.writeValue(RESALE_FILTERS, Map.of(filterId, List.of(filterValueId),
                "anotherId", Collections.emptyList()));
        Filter filter = getFilter(filterId, filterValueId, null);
        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        parameters.getReportParameters().setReportFiltersValue(String.format("[%s]",
                writer.writeValueAsString(filter)));
        parameters.setCheckCartErrors(false);

        //act
        MultiCart cart = orderCreateHelper.cart(parameters);

        //assert
        Assertions.assertTrue(cartContainsPostPaid(cart), "Should have postpaid");
        Assertions.assertTrue(deliveryContainsPostPaid(cart));

        Mockito.verify(postpaidControllerClient, Mockito.times(0))
                .postpaidDecide(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldNotAddPostPaidIfOrderIsResaleButToggleIsOff() throws IOException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_FOR_RESALE, false);
        //arrange
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        String filterId = "validId";
        String filterValueId = "validValueId";
        checkouterFeatureWriter.writeValue(RESALE_FILTERS, Map.of(filterId, List.of(filterValueId),
                "anotherId", Collections.emptyList()));
        Filter filter = getFilter(filterId, filterValueId, null);
        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        parameters.getReportParameters().setReportFiltersValue(String.format("[%s]",
                writer.writeValueAsString(filter)));
        parameters.setCheckCartErrors(false);

        //act
        MultiCart cart = orderCreateHelper.cart(parameters);

        //assert
        Assertions.assertFalse(cartContainsPostPaid(cart));
        Assertions.assertFalse(deliveryContainsPostPaid(cart));
    }

    @Test
    void shouldNotAddPostPaidIfOrderIsNotResaleButToggleIsOn() throws IOException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POSTPAID_FOR_RESALE, true);
        //arrange
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        String filterId = "validId";
        String filterValueId = "validValueId";
        checkouterFeatureWriter.writeValue(RESALE_FILTERS, Map.of(filterId, List.of(filterValueId),
                "anotherId", Collections.emptyList()));
        Filter filter = getFilter("wrongId", "wrongValueId", null);
        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        parameters.getReportParameters().setReportFiltersValue(String.format("[%s]",
                writer.writeValueAsString(filter)));
        parameters.setCheckCartErrors(false);

        //act
        MultiCart cart = orderCreateHelper.cart(parameters);

        //assert
        Assertions.assertFalse(cartContainsPostPaid(cart));
        Assertions.assertFalse(deliveryContainsPostPaid(cart));
    }

    private boolean cartContainsPostPaid(MultiCart cart) {
        return cart.getPaymentOptions().stream().anyMatch(pm -> pm.getPaymentType() == PaymentType.POSTPAID);
    }

    private boolean deliveryContainsPostPaid(MultiCart cart) {
        return cart.getCarts().stream()
                .flatMap(c -> c.getDeliveryOptions().stream())
                .map(Delivery::getPaymentOptions)
                .anyMatch(d -> d.contains(PaymentMethod.CARD_ON_DELIVERY) &&
                        d.contains(PaymentMethod.CASH_ON_DELIVERY));
    }

    private void mockFintechUtilsAnswer(Order order, boolean allow) {
        Mockito.doReturn(new PostPaidDecideResponse()
                .decideResults(List.of(new DecisionResultDto()
                        .label(order.getLabel())
                        .orderId(order.getId())
                        .isPostpaidAllowed(allow)))
        ).when(postpaidControllerClient).postpaidDecide(Mockito.any(), Mockito.any());
    }

    private void mockFintechUtilsAnswer(Map<String, Boolean> allowMapping) {
        Mockito.doReturn(new PostPaidDecideResponse()
                .decideResults(
                        allowMapping.entrySet().stream()
                                .map(
                                        entry -> new DecisionResultDto()
                                                .label(entry.getKey())
                                                .isPostpaidAllowed(entry.getValue())
                                )
                                .collect(Collectors.toList())
                )
        ).when(postpaidControllerClient).postpaidDecide(Mockito.any(), Mockito.any());
    }


    /**
     * 1p order = ALL items 1p
     *
     * @return
     */
    private Parameters create1pParameters() {
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.configuration().cart().response().setCheckCartErrors(false);
        Order order = parameters.getOrder();
        order.setPaymentOptions(DEFAULT_PAYMENT_OPTIONS);

        tuneYandexDelivery(order, DEFAULT_PAYMENT_OPTIONS);
        return parameters;
    }

    /**
     * 3p order = AT LEAST 1 item 3p
     *
     * @return
     */
    private Parameters create3pParameters() {

        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        parameters.configuration().cart().response().setCheckCartErrors(false);
        Order order = parameters.getOrder();
        order.setPaymentOptions(DEFAULT_PAYMENT_OPTIONS);
        tuneYandexDelivery(order, DEFAULT_PAYMENT_OPTIONS);
        //3p
        parameters.getItems().forEach(item -> {
            item.setSupplierType(SupplierType.THIRD_PARTY);
            item.setPrepayEnabled(true);
        });
        return parameters;
    }

    private void tuneYandexDelivery(Order order, Set<PaymentMethod> paymentOptions) {
        Delivery delivery = order.getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryPartnerType(YANDEX_MARKET);
        delivery.setPaymentOptions(paymentOptions);
        delivery.setDeliveryDates(
                new DeliveryDates(
                        Date.from(LocalDate.of(2022, 1, 1)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant()),
                        Date.from(LocalDate.of(2022, 1, 5)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant())
                )
        );
    }

    @TestConfiguration
    public static class Configuration {

        @Bean
        @Primary
        public PostpaidControllerApi postpaidControllerClientSpy(PostpaidControllerApi postpaidControllerClient) {
            return Mockito.spy(postpaidControllerClient);
        }
    }
}
