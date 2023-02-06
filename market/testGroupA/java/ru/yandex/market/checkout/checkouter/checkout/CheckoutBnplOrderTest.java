package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.BnplPlanDetails;
import ru.yandex.market.checkout.checkouter.cart.BnplRegularPayment;
import ru.yandex.market.checkout.checkouter.cart.BnplVisualProperties;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderService;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplServiceType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.json.BnplDenialReason;
import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;
import ru.yandex.market.common.report.model.json.credit.CreditOffer;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.checkout.multiware.CommonPaymentServiceTest.CASHBACK_AMOUNT;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.REPORT_EXPERIMENTS_PARAM;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplServiceType.LOAN;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplServiceType.PAYMENT;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;
import static ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder.DELIVERY_TITLE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.FF_SHOP_ID;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SKU;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.defaultWhiteParameters;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.dsbsOrderItem;
import static ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer.POST_PLAN_CHECK;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.PRE_ORDER;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_CHEAP;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_EXPENSIVE;

public class CheckoutBnplOrderTest extends AbstractWebTestBase {

    public static final List<String> SHOW_INFO_LIST = List.of(
            OrderItemProvider.SHOW_INFO,
            OrderItemProvider.ANOTHER_SHOW_INFO,
            OrderItemProvider.OTHER_SHOW_INFO
    );


    private static final List<Integer> HARDCODED_ALLOWED_HID = List.of(
            90864,
            90857,
            90855
    );

    private static final Long ANOTHER_SUPPLIER_ID = 999L;

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Value("${market.checkouter.BnplProcessor.bnplPlanCheck.timeout.milliseconds:50}")
    private Integer bnplPlanCheckTimeoutMilliseconds;

    private static OrderItem createOrderItem(
            int index,
            String offerId,
            double price,
            int count,
            SupplierType supplierType,
            Integer categoryId
    ) {
        var orderItem = OrderItemProvider.buildOrderItem(offerId, new BigDecimal(price), count);
        orderItem.setId(null);
        orderItem.setOfferName(offerId);
        orderItem.setCategoryId(categoryId);
        orderItem.setFeedId(System.currentTimeMillis());
        orderItem.setMsku(System.currentTimeMillis());
        orderItem.setShopSku("sku-" + offerId);
        orderItem.setSku(orderItem.getMsku().toString());
        orderItem.setSupplierId(System.currentTimeMillis());

        orderItem.setWareMd5(OrderItemProvider.DEFAULT_WARE_MD5);
        orderItem.setShowInfo(SHOW_INFO_LIST.get(index % SHOW_INFO_LIST.size()));
        orderItem.setSupplierType(supplierType);
        return orderItem;
    }

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
        reportConfigurer.mockDefaultCreditInfo();
    }

    @AfterEach
    public void restoreReportConfig() {
        reportConfigurer.mockDefaultCreditInfo();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutTest(boolean bnplSelected) throws Exception {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);

        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(2);
    }

    @Test
    public void checkoutTestWithNullDeliveryPrice() {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(false);
        expectedBnplInfo.setAvailable(false);

        Parameters parameters = defaultBnplParameters();
        parameters.addOrder(defaultBnplParameters());
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getOrders().forEach(order -> {
            order.getDelivery().setPrice(null);
            order.getDelivery().setBuyerPrice(null);
        });

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
    }

    @Test
    public void cartWithBnplAvailableFalseTest() throws IOException, ProcessingException {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setAvailable(false);
        expectedBnplInfo.setSelected(false);

        Parameters parameters = defaultBnplParameters();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);
        parameters.getBuiltMultiCart().setPaymentMethod(null);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(1);
    }

    @Test
    public void cartWithoutBuyerCurrencyTest() throws IOException, ProcessingException {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setAvailable(false);
        expectedBnplInfo.setSelected(false);

        Parameters parameters = defaultBnplParameters();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);
        parameters.getBuiltMultiCart().setPaymentMethod(null);
        parameters.getBuiltMultiCart().setBuyerCurrency(null);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(1);
    }

    @Test
    public void planCheckTimeoutCheckoutTest() throws Exception {
        bnplMockConfigurer.mockPlanCheck(YANDEX_UID_HEADER, bnplPlanCheckTimeoutMilliseconds * 2);

        var expectedBnplInfo = new BnplInfo();

        Parameters parameters = defaultBnplParameters();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
    }

    @Test
    public void bnplDisabledTest() throws Exception {
        checkouterProperties.setEnableBnpl(false);

        var expectedBnplInfo = new BnplInfo();
        expectedBnplInfo.setBnplPlanDetails(null);
        expectedBnplInfo.setSelected(false);
        expectedBnplInfo.setAvailable(false);

        Parameters parameters = defaultBnplParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(0);
    }

    @Test
    public void planCheckEmptyResponseCheckoutTest() throws Exception {
        bnplMockConfigurer.mockEmptyPlanCheck(YANDEX_UID_HEADER);

        var expectedBnplInfo = new BnplInfo();

        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
    }

    @Test
    public void bnplIsAbsentInReportTest() throws Exception {
        reportConfigurer.mockCreditInfoWithoutOffers();

        var expectedBnplInfo = new BnplInfo();

        Parameters parameters = defaultBnplParameters();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutTestWithCashback(boolean bnplSelected) throws Exception {
        trustMockConfigurer.mockListWalletBalanceResponse();
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);

        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            var expectedPrice = new BigDecimal("220.0000");
            var wasDiscountApplied = bnplPlanCheckRequestBody.getServices()
                    .stream()
                    .map(BnplOrderService::getItems)
                    .flatMap(Collection::stream)
                    .map(BnplItem::getPrice)
                    .anyMatch(expectedPrice::equals);
            assertThat(wasDiscountApplied).isTrue();
        };
        validatePlanCheckBnplRequest(2, validator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutTestWithPaymentType(boolean bnplSelected) throws Exception {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);
        var index = 0;

        Parameters parameters = defaultBlueOrderParametersWithItems(
                createOrderItem(index++, "bnpl-1", 30.00, 1, SupplierType.FIRST_PARTY, 90864),
                createOrderItem(index++, "payment-1", 100.00, 1, SupplierType.FIRST_PARTY, 1)
        );
        parameters.addItemService();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        validatePlanCheckBnplRequest(1, bnplPlanCheckRequestBody ->
                validateLoanPaymentTypes(bnplPlanCheckRequestBody, multiCart.getCarts().get(0), false));
        bnplMockConfigurer.resetRequests();

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);

        validatePlanCheckBnplRequest(1, bnplPlanCheckRequestBody ->
                validateLoanPaymentTypes(bnplPlanCheckRequestBody, multiOrder.getOrders().get(0), true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutTestWithItemCountGreaterThanOneAndCashback(boolean bnplSelected) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);

        trustMockConfigurer.mockListWalletBalanceResponse();
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 250.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        validatePlanCheckBnplRequest(1);
        bnplMockConfigurer.resetRequests();

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            // 250 price - (30 max cashbask spent / 3 count) = 240.00
            var expectedPrice = new BigDecimal("240.0000");
            // 240 price x 3 count + 100 delivery + 150 itemServicePrice x 3 count = 1270.00
            var expectedAmount = new BigDecimal("1270.0000");
            var wasDiscountApplied = bnplPlanCheckRequestBody.getServices()
                    .stream()
                    .map(BnplOrderService::getItems)
                    .flatMap(Collection::stream)
                    .map(BnplItem::getPrice)
                    .anyMatch(expectedPrice::equals);
            assertThat(wasDiscountApplied).isTrue();
            assertThat(
                    bnplPlanCheckRequestBody.getServices()
                            .stream()
                            .map(BnplOrderService::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            ).isEqualTo(expectedAmount);
        };
        validatePlanCheckBnplRequest(1, validator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutTestWithItemCountGreaterThanOneAndCashbackWithSingleServicePerItems(boolean bnplSelected)
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);

        trustMockConfigurer.mockListWalletBalanceResponse();
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 250.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        validatePlanCheckBnplRequest(1);
        bnplMockConfigurer.resetRequests();

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            // 250 price - (30 max cashbask spent / 3 count) = 240.00
            var expectedPrice = new BigDecimal("240.0000");
            // 240 price x 3 count + 100 delivery + 150 itemServicePrice x 1 count = 970.00
            var expectedAmount = new BigDecimal("970.0000");
            var wasDiscountApplied = bnplPlanCheckRequestBody.getServices()
                    .stream()
                    .map(BnplOrderService::getItems)
                    .flatMap(Collection::stream)
                    .map(BnplItem::getPrice)
                    .anyMatch(expectedPrice::equals);
            assertThat(wasDiscountApplied).isTrue();
            assertThat(
                    bnplPlanCheckRequestBody.getServices()
                            .stream()
                            .map(BnplOrderService::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            ).isEqualTo(expectedAmount);
        };
        validatePlanCheckBnplRequest(1, validator);
    }

    @Test
    public void checkoutPostpaidOrderTest() throws Exception {
        var expectedBnplInfo = new BnplInfo();

        OrderItem anotherOrderItem = OrderItemProvider.getAnotherOrderItem();
        anotherOrderItem.setSupplierId(ANOTHER_SUPPLIER_ID);
        anotherOrderItem.setOfferName("POSTPAID_OFFER");
        Parameters cashOnlyParameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(anotherOrderItem);
        cashOnlyParameters.getReportParameters().setDeliveryPartnerTypes(singletonList("SHOP"));
        cashOnlyParameters.getOrder().setItems(Collections.singleton(anotherOrderItem));
        cashOnlyParameters.addItemService();
        cashOnlyParameters.setupFulfillment(new ItemInfo.Fulfilment(ANOTHER_SUPPLIER_ID, TEST_SKU, TEST_SHOP_SKU));
        cashOnlyParameters.getOrder().getItems().forEach(oi -> oi.setSupplierId(ANOTHER_SUPPLIER_ID));
        cashOnlyParameters.addShopMetaData(ANOTHER_SUPPLIER_ID, ShopSettingsHelper.getPostpayMeta());
        cashOnlyParameters.makeCashOnly();
        cashOnlyParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(cashOnlyParameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
        var multiOrder = orderCreateHelper.checkout(multiCart, cashOnlyParameters);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);
        validatePlanCheckBnplRequest(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutMultiCartWithPostpaidAndPrepaidOrdersTest(boolean bnplSelected) throws Exception {
        var expectedBnplInfo = getExpectedBnplInfo();

        // 1st cart
        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        // 2nd cart
        OrderItem anotherOrderItem = OrderItemProvider.getAnotherOrderItem();
        anotherOrderItem.setSupplierId(ANOTHER_SUPPLIER_ID);
        anotherOrderItem.setOfferName("POSTPAID_OFFER");
        Parameters cashOnlyParameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(anotherOrderItem);
        cashOnlyParameters.addItemService();
        cashOnlyParameters.getReportParameters().setDeliveryPartnerTypes(singletonList("SHOP"));
        cashOnlyParameters.getOrder().setItems(Collections.singleton(anotherOrderItem));
        cashOnlyParameters.setupFulfillment(new ItemInfo.Fulfilment(ANOTHER_SUPPLIER_ID, TEST_SKU, TEST_SHOP_SKU));
        cashOnlyParameters.getOrder().getItems().forEach(oi -> oi.setSupplierId(ANOTHER_SUPPLIER_ID));
        cashOnlyParameters.addShopMetaData(ANOTHER_SUPPLIER_ID, ShopSettingsHelper.getPostpayMeta());
        cashOnlyParameters.makeCashOnly();
        cashOnlyParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.addOrder(cashOnlyParameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        expectedBnplInfo.setAvailable(false);
        expectedBnplInfo.setSelected(false);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        multiCart.getBnplInfo().setSelected(bnplSelected);

        var multiOrder = checkoutWithPostpayPessimization(multiCart, parameters);
        expectedBnplInfo.setSelected(bnplSelected);
        expectedBnplInfo.setAvailable(true);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);

        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            bnplItemValidator(bnplPlanCheckRequestBody, bnplOrderService -> true, "POSTPAID_OFFER");
        };

        validatePlanCheckBnplRequest(2, validator);
    }

    private void bnplItemValidator(
            BnplPlanCheckRequestBody bnplPlanCheckRequestBody,
            Predicate<BnplOrderService> bnplOrderServicePredicate,
            String excludedBnplItemTitle
    ) {
        validateBnplItems(bnplPlanCheckRequestBody, bnplOrderServicePredicate, excludedBnplItemTitle);
        validateAmount(bnplPlanCheckRequestBody);
    }

    private void validateBnplItems(BnplPlanCheckRequestBody bnplPlanCheckRequestBody,
                                   Predicate<BnplOrderService> bnplOrderServicePredicate,
                                   String excludedBnplItemTitle) {
        var containsOnlyPostpaidItems = bnplPlanCheckRequestBody.getServices()
                .stream()
                .filter(bnplOrderServicePredicate)
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .noneMatch(excludedBnplItemTitle::equals);
        assertThat(containsOnlyPostpaidItems).isTrue();
    }

    private void validateAmount(BnplPlanCheckRequestBody bnplPlanCheckRequestBody) {
        var itemSum = bnplPlanCheckRequestBody.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var amountSum = bnplPlanCheckRequestBody.getServices().stream()
                .map(BnplOrderService::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amountSum).isEqualTo(itemSum);
    }

    @Test
    public void checkoutDsbsOrderTest() throws Exception {
        Parameters parameters = createDsbsParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo().isAvailable()).isTrue();

        multiCart.setPaymentMethod(PaymentMethod.YANDEX);
        multiCart.getCarts().forEach(order -> order.setPaymentMethod(PaymentMethod.YANDEX));

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiCart.getBnplInfo().isAvailable()).isTrue();
    }

    @Test
    public void cartWithMarketForceWhiteOnExperimentTest() {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(false);

        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(false);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.addExperiment("market_force_white_on", "13,14,15,16,17,19,23,25,26");

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        assertReportRequestHasNotContainsMarketForceWhiteOnExperiment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkoutMultiCartOrderWithDsbsOrderTest(boolean bnplSelected) throws Exception {
        var expectedBnplInfo = getExpectedBnplInfo();

        Parameters parameters = createDsbsParameters();
        // 2nd cart - bnpl
        parameters.addOrder(defaultBnplParameters(
                createOrderItem(0, "BNPL", 250.00, 3, SupplierType.FIRST_PARTY, 90864)
        ));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        expectedBnplInfo.setAvailable(false);
        expectedBnplInfo.setSelected(false);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);

        multiCart.getBnplInfo().setSelected(bnplSelected);

        multiCart.setPaymentMethod(PaymentMethod.YANDEX);
        multiCart.getCarts().forEach(order -> order.setPaymentMethod(PaymentMethod.YANDEX));

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        expectedBnplInfo.setSelected(bnplSelected);
        expectedBnplInfo.setAvailable(true);
        assertThat(multiOrder.getBnplInfo()).isEqualTo(expectedBnplInfo);

        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            // loan service doesn't contain DSBS item
            validateBnplItems(bnplPlanCheckRequestBody,
                    bnplOrderService -> bnplOrderService.getType() == LOAN,
                    "DSBS");
            // payment service doesn't contain BNPL item
            validateBnplItems(bnplPlanCheckRequestBody,
                    bnplOrderService -> bnplOrderService.getType() == PAYMENT,
                    "BNPL");
            validateAmount(bnplPlanCheckRequestBody);
        };

        validatePlanCheckBnplRequest(2, validator);
    }

    private BnplInfo getExpectedBnplInfo() {
        var bnplInfo = new BnplInfo();
        bnplInfo.setAvailable(true);
        bnplInfo.setSelected(true);
        bnplInfo.setSelectedPlan("split_4_month");
        BnplPlanDetails bnplPlanDetails1 = BnplPlanDetails.builder()
                .withDeposit(BigDecimal.valueOf(555))
                .withPayments(List.of(
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(300))
                                .withDateTime(Instant.parse("2021-06-24T12:03:46.751644Z"))
                                .build(),
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(255))
                                .withDateTime(Instant.parse("2021-07-08T12:03:46.751644Z"))
                                .build()
                ))
                .withType("base_split")
                .withFee(BigDecimal.ZERO)
                .withConstructor("split_2_month")
                .withDetailsUrl("https://split.yandex.ru/market")
                .withVisualProperties(BnplVisualProperties.builder()
                        .withNextDatesDescription("далее 1 платежей")
                        .withNextPaymentsDescription("по 255")
                        .withColors(Map.of("canceled", "#E1E3E8", "paid", "#4AC2A8"))
                        .withShortTitle("сплит 2")
                        .build())
                .build();
        BnplPlanDetails bnplPlanDetails2 = BnplPlanDetails.builder()
                .withDeposit(BigDecimal.valueOf(344))
                .withPayments(List.of(
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(430))
                                .withDateTime(Instant.parse("2021-06-24T12:03:46.751644Z"))
                                .build(),
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(86))
                                .withDateTime(Instant.parse("2021-07-08T12:03:46.751644Z"))
                                .build(),
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(86))
                                .withDateTime(Instant.parse("2021-07-22T12:03:46.751644Z"))
                                .build(),
                        BnplRegularPayment.builder()
                                .withAmount(BigDecimal.valueOf(86))
                                .withDateTime(Instant.parse("2021-08-05T12:03:46.751644Z"))
                                .build()
                ))
                .withType("long_split")
                .withFee(BigDecimal.ZERO)
                .withConstructor("split_4_month")
                .withDetailsUrl("https://split.yandex.ru/#longsplit")
                .withVisualProperties(BnplVisualProperties.builder()
                        .withNextDatesDescription("далее 3 платежей")
                        .withNextPaymentsDescription("по 7500₽")
                        .withColors(Map.of("canceled", "#E1E3E8", "paid", "#4AC2A8"))
                        .withShortTitle("сплит 4")
                        .build())
                .build();
        bnplInfo.setBnplPlanDetails(bnplPlanDetails2);
        bnplInfo.setPlans(List.of(bnplPlanDetails1, bnplPlanDetails2));
        return bnplInfo;
    }

    private void validatePlanCheckBnplRequest(
            int expectedCount
    ) throws IOException, ProcessingException {
        validatePlanCheckBnplRequest(expectedCount, null);
    }

    private void validatePlanCheckBnplRequest(
            int expectedCount,
            Consumer<BnplPlanCheckRequestBody> bnplPlanCheckRequestBodyValidator
    ) throws IOException, ProcessingException {
        var validator = bnplMockConfigurer.createCheckPlanValidator();

        var requestBodies = bnplMockConfigurer.bnplMock.getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(request -> POST_PLAN_CHECK.equalsIgnoreCase(request.getUrl()))
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());

        assertThat(requestBodies).hasSize(expectedCount);

        for (var requestBody : requestBodies) {
            var report = validator.validate(JsonLoader.fromString(requestBody));
            SoftAssertions.assertSoftly(softly -> {
                for (ProcessingMessage processingMessage : report) {
                    softly.fail(processingMessage.toString());
                }
                softly.assertThat(report.isSuccess()).isTrue();
            });

            var bnplPlanCheckRequestBody = checkouterAnnotationObjectMapper.readValue(
                    requestBody,
                    BnplPlanCheckRequestBody.class);
            var isDeliveryBnplItemExist = bnplPlanCheckRequestBody.getServices().stream()
                    .map(BnplOrderService::getItems)
                    .flatMap(Collection::stream)
                    .anyMatch(bnplItem -> DELIVERY_TITLE.equalsIgnoreCase(bnplItem.getCategory()));
            assertThat(isDeliveryBnplItemExist).isTrue();

            var containsBnplItemWithEmptyTitle = bnplPlanCheckRequestBody.getServices().stream()
                    .map(BnplOrderService::getItems)
                    .flatMap(Collection::stream)
                    .map(BnplItem::getTitle)
                    .anyMatch(title -> title == null || title.isBlank());
            assertThat(containsBnplItemWithEmptyTitle).isFalse();

            if (bnplPlanCheckRequestBodyValidator != null) {
                bnplPlanCheckRequestBodyValidator.accept(bnplPlanCheckRequestBody);
            }
        }
    }

    private void validateLoanPaymentTypes(
            BnplPlanCheckRequestBody bnplPlanCheckRequestBody,
            Order finalOrder,
            boolean deliveryAvailable
    ) {
        var planCheckLoanItems = getBnplItemsByType(bnplPlanCheckRequestBody, LOAN);
        var planCheckPaymentItems = getBnplItemsByType(bnplPlanCheckRequestBody, BnplServiceType.PAYMENT);
        if (planCheckPaymentItems.isEmpty()) {
            assertThat(bnplPlanCheckRequestBody.getServices()).hasSize(1);
        }
        var expectedLoanBnplItems = finalOrder.getItems().stream()
                .filter(item -> HARDCODED_ALLOWED_HID.contains(item.getCategoryId()))
                .map(OfferItem::getOfferId)
                .collect(Collectors.toSet());

        assertThat(planCheckLoanItems).containsAll(expectedLoanBnplItems);
        assertThat(planCheckPaymentItems).containsAll(finalOrder.getItems().stream()
                .map(OfferItem::getOfferId)
                .filter(offerId -> !expectedLoanBnplItems.contains(offerId))
                .collect(Collectors.toSet())
        );

        if (deliveryAvailable) {
            assertTrue(planCheckLoanItems.stream().anyMatch(item -> item.startsWith(DELIVERY_TITLE)));
        }
    }

    @Nonnull
    private Set<String> getBnplItemsByType(BnplPlanCheckRequestBody request, BnplServiceType loan) {
        return request.getServices().stream()
                .filter(bnplOrderService -> bnplOrderService.getType() == loan)
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .collect(Collectors.toSet());
    }

    private MultiOrder checkoutWithPostpayPessimization(MultiCart cart, Parameters parameters) throws Exception {
        cart.setPaymentMethod(null);
        cart.setPaymentType(null);
        cart.getCarts().forEach(o -> {
            PaymentMethod paymentMethod = o.getPaymentOptions().stream()
                    // Фильтрую по методу, а не по типу, чтобы в тесте не было экзотики вроде APPLE_PAY
                    .filter(po -> po == PaymentMethod.YANDEX)
                    .findAny()
                    .orElse(
                            o.getPaymentOptions().stream()
                                    .findAny()
                                    .orElseThrow(() -> new RuntimeException("No payment options"))
                    );
            o.setPaymentMethod(paymentMethod);
            o.setPaymentType(paymentMethod.getPaymentType());
        });
        return orderCreateHelper.checkout(cart, parameters);
    }

    private Parameters createDsbsParameters() {
        Parameters parameters = defaultWhiteParameters();
        parameters.addItemService();
        parameters.setShopId(FF_SHOP_ID);
        parameters.getOrder().setItems(Collections.singletonList(
                dsbsOrderItem()
                        .offer("DSBS")
                        .name("DSBS")
                        .categoryId(90864)
                        .atSupplierWarehouse(true)
                        .supplierType(SupplierType.THIRD_PARTY)
                        .build()));
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU, null, false));
        return parameters;
    }

    private void assertReportRequestHasNotContainsMarketForceWhiteOnExperiment() {
        var containsMarketForceWhiteOnExperiment = reportMock.getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.queryParameter("place").containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                .map(request -> request.queryParameter(REPORT_EXPERIMENTS_PARAM))
                .filter(QueryParameter::isPresent)
                .anyMatch(queryParameter -> queryParameter.values().stream()
                        .anyMatch(value -> value.contains("market_force_white_on")));

        assertFalse(containsMarketForceWhiteOnExperiment,
                "market_force_white_on experiment must be excluded from report request");
    }

    @Test
    public void checkoutTestWithPaidSplitFeatureOn() throws Exception {
        Parameters parameters = defaultBnplParameters();
        parameters.setBnplFeatures(List.of("paid_split"));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            List<String> features = bnplPlanCheckRequestBody.getFeatures();
            assertThat(features).contains("paid_split");
        };
        validatePlanCheckBnplRequest(2, validator);
    }

    @Test
    public void checkoutTestWithPaidSplitFeatureOff() throws Exception {
        Parameters parameters = defaultBnplParameters();
        parameters.setBnplFeatures(null);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            List<String> features = bnplPlanCheckRequestBody.getFeatures();
            assertThat(features).isNull();
        };
        validatePlanCheckBnplRequest(2, validator);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterDataForTooExpensiveOfferTest")
    public void checkTooExpensiveOffer(String name, boolean toggle, boolean mock, boolean enabled,
                                       BnplDenialReason reason, int expectedCount) throws Exception {
        // setup
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_BNPL_FOR_EXPENSIVE_OFFER, toggle);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TREAT_REPORT_BNPL_ANSWER_AS_CONST_TOO_EXPENSIVE, mock);
        Parameters parameters = defaultBnplParameters();
        CreditInfo creditInfo = new CreditInfo();
        OrderItem orderItem = parameters.getItems().stream().findFirst().orElseThrow();
        creditInfo.setCreditOffers(Collections.singletonList(createCreditOffer(orderItem, enabled, reason, mock)));
        parameters.getReportParameters().setCreditInfo(creditInfo);

        // act
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        // verify
        validatePlanCheckBnplRequest(expectedCount);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterDataForTooCheapOfferTest")
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void checkTooCheapOffer(String name, int minBasketPrice, double item1Price, double item2Price,
                                   boolean enabled1, BnplDenialReason reason1, boolean enabled2,
                                   BnplDenialReason reason2, int expectedCount, int loanAmont, int paymentAmont)
            throws Exception {
        // setup
        checkouterFeatureWriter.writeValue(IntegerFeatureType.MIN_BASKET_PRICE_FOR_BNPL, minBasketPrice);
        OrderItem item1 = createOrderItem(1, "offer-1", item1Price, 2, SupplierType.FIRST_PARTY, 90864);
        OrderItem item2 = createOrderItem(2, "offer-2", item2Price, 1, SupplierType.FIRST_PARTY, 90857);
        Parameters parameters = defaultBnplParameters(item1, item2);
        item1.setCategoryId(90864);
        item2.setCategoryId(90857);
        CreditInfo creditInfo = new CreditInfo();
        creditInfo.setCreditOffers(Arrays.asList(createCreditOffer(item1, enabled1, reason1, false),
                createCreditOffer(item2, enabled2, reason2, false)));
        parameters.getReportParameters().setCreditInfo(creditInfo);

        // act
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        // verify
        Consumer<BnplPlanCheckRequestBody> validator = bnplPlanCheckRequestBody -> {
            if (loanAmont > 0) {
                BnplOrderService loanService = bnplPlanCheckRequestBody.getServices().stream()
                        .filter(service -> service.getType() == LOAN)
                        .findFirst()
                        .orElseThrow();
                BigDecimal delivery = getDeliveryPrice(loanService);
                assertThat(loanService.getAmount().subtract(delivery).longValue())
                        .isEqualTo(new BigDecimal(loanAmont).longValue());
            } else {
                assertTrue(bnplPlanCheckRequestBody.getServices().stream()
                        .noneMatch(service -> service.getType() == LOAN));
            }
            if (paymentAmont > 0) {
                BnplOrderService paymentService = bnplPlanCheckRequestBody.getServices().stream()
                        .filter(service -> service.getType() == PAYMENT)
                        .findFirst()
                        .orElseThrow();
                BigDecimal delivery = getDeliveryPrice(paymentService);
                assertThat(paymentService.getAmount().subtract(delivery).longValue())
                        .isEqualTo(new BigDecimal(paymentAmont).longValue());
            } else {
                assertTrue(bnplPlanCheckRequestBody.getServices().stream()
                        .noneMatch(service -> service.getType() == PAYMENT));
            }
        };
        validatePlanCheckBnplRequest(expectedCount, validator);
    }

    private BigDecimal getDeliveryPrice(BnplOrderService loanService) {
        BigDecimal delivery = loanService.getItems().stream()
                .filter(item -> item.getCategory().equals("Доставка"))
                .findFirst().map(BnplItem::getTotal)
                .orElse(BigDecimal.ZERO);
        return delivery;
    }

    public static Stream<Arguments> parameterDataForTooCheapOfferTest() {
        return Stream.of(
                Arguments.of("порог задан, 2 недорогих товара в сумме дают 1000: проходят оба",
                        1000, 250, 500, false, TOO_CHEAP, false, TOO_CHEAP, 2, 1000, 0),
                Arguments.of("порог НЕ задан, 2 недорогих товара в сумме дают 1000: НЕ проходят оба",
                        0, 500, 500, false, TOO_CHEAP, false, TOO_CHEAP, 0, 0, 0),
                Arguments.of("порог задан, 2 недорогих товара в сумме НЕ проходят порог: НЕ проходят оба",
                        1000, 250, 499, false, TOO_CHEAP, false, TOO_CHEAP, 0, 0, 0),
                Arguments.of("порог задан, 2 товара с разными причинами отказа НЕ проходят порог",
                        1000, 499, 1500, false, TOO_CHEAP, false, PRE_ORDER, 0, 0, 0),
                Arguments.of("порог задан, один недорогой и один дорогой товар: проходят оба",
                        1000, 500, 1500, false, TOO_CHEAP, true, null, 2, 2500, 0),
                Arguments.of("порог НЕ задан, один недорогой и один дорогой товар: проходит один",
                        0, 500, 1500, false, TOO_CHEAP, true, null, 2, 1500, 1000)
        );
    }

    @Test
    public void checkoutShouldRetainSelectedPlan() throws Exception {
        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelectedPlan("split_2_month");

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo().getSelectedPlan()).isEqualTo("split_2_month");
        assertThat(multiCart.getBnplInfo().getBnplPlanDetails().getConstructor()).isEqualTo("split_2_month");
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo().getSelectedPlan()).isEqualTo("split_2_month");
        assertThat(multiCart.getBnplInfo().getBnplPlanDetails().getConstructor()).isEqualTo("split_2_month");
    }

    @Test
    public void checkoutShouldSelectDefaultPlanWhenSelectedPlanIsMissing() throws Exception {
        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelectedPlan("fake_plan");

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getBnplInfo().getSelectedPlan()).isEqualTo("split_4_month");
        assertThat(multiCart.getBnplInfo().getBnplPlanDetails().getConstructor()).isEqualTo("split_4_month");
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getBnplInfo().getSelectedPlan()).isEqualTo("split_4_month");
        assertThat(multiCart.getBnplInfo().getBnplPlanDetails().getConstructor()).isEqualTo("split_4_month");
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void cartV2Test(boolean bnplSelected) {
        var expectedBnplInfo = getExpectedBnplInfo();
        expectedBnplInfo.setSelected(bnplSelected);

        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        MultiCart multiCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        assertThat(multiCart.getBnplInfo()).isEqualTo(expectedBnplInfo);
    }

    public static Stream<Arguments> parameterDataForTooExpensiveOfferTest() {
        return Stream.of(
                Arguments.of("Check too expensive offer (bnpl is enabled)",
                        true, false, true, null, 2),
                Arguments.of("Check too expensive offer (offer is too cheap, toggle is on)",
                        true, false, false, TOO_CHEAP, 0),
                Arguments.of("Check too expensive offer (offer is too expensive, toggle is off)",
                        false, false, false, TOO_EXPENSIVE, 0),
                Arguments.of("Check too expensive offer (offer is too expensive, toggle is on)",
                        true, false, false, TOO_EXPENSIVE, 2),
                Arguments.of("Check too expensive offer (offer is not expensive, toggle is on, mock report answer)",
                        true, true, false, null, 2),
                Arguments.of("Check too expensive offer (offer is not expensive, toggle is off, mock report answer)",
                        false, true, false, null, 0)
        );
    }

    private static CreditOffer createCreditOffer(OrderItem orderItem, boolean enabled, BnplDenialReason reason,
                                                 boolean mock) {
        var creditOffer = new CreditOffer();
        creditOffer.setEntity(orderItem.getOfferId());
        creditOffer.setHid(orderItem.getCategoryId());
        creditOffer.setWareId(orderItem.getWareMd5());
        var bnplInfo = new YandexBnplInfo();
        bnplInfo.setEnabled(enabled);
        if (!enabled && !mock) {
            BnplDenial bnplDenial = new BnplDenial();
            bnplDenial.setReason(reason);
            bnplDenial.setThreshold(new BigDecimal(1000));
            bnplInfo.setBnplDenial(bnplDenial);
        }
        creditOffer.setYandexBnplInfo(bnplInfo);
        return creditOffer;
    }
}
