package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.validation.LimitMaxAmountResult;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;

/**
 * Created by asafev on 01/09/2017.
 */
public class CreateOrderFailureTest extends AbstractWebTestBase {

    private static final long ANOTHER_SHOP_ID = 1774L;
    private static final String DIFFERENT_HASH =
            "vujQrQNMAdOzZmnKZ6gFMPXWOueGz50jupRcKN87Azi6lFwo3zsDOGTa22maa4ZBZNrbaZprhkG5atQBWNJmD4YW6HNFGsfvYHV" +
                    "+A5lGEe0=";

    /**
     * checkouter-14: Создание постоплатного Глобал заказа
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-14
     */

    @Tag(Tags.GLOBAL)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание постоплатного Глобал заказа")
    @Test
    public void createPostPaidOrder() {
        Parameters parameters = new Parameters();
        parameters.turnOffErrorChecks();
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
        });
        parameters.setMultiCartAction(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            multiCart.setPaymentType(PaymentType.POSTPAID);
        });
        addShopMetaData(parameters, ShopSettingsHelper.getRedPrepayMeta());

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        checkCartFailures(order);
    }

    /**
     * checkouter-16: Создание заказа в магазине без предоплаты
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-14
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа в магазине без предоплаты")
    @Test
    public void createPrePaidOrder() {
        Parameters parameters = new Parameters();
        parameters.turnOffErrorChecks();
        parameters.setMultiCartAction(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
        });
        addShopMetaData(parameters, ShopSettingsHelper.getPostpayMeta());

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        checkCartFailures(order);
    }

    /**
     * checkouter-4: Создание заказа с ошибкой, 'changes': ['DELIVERY']
     * <p>
     * Step 1. Создать заказ со старым hash доставки (дёрнуть /cart, запомнить hash, дёрнуть /cart ещё раз, а в
     * создании заказа использовать запомненный hash)
     * Expectation: 200 ОК, заказ не создан, “error”: “OUT_OF_DATE”, “changes”: [ “DELIVERY” ]
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-4
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с ошибкой, 'changes': ['DELIVERY']")
    @Test
    public void createOrderWithOldDeliveryHash() {
        Parameters parameters = new Parameters();
        parameters.setMultiCartAction((multiCart) -> multiCart.getCarts().get(0).getDelivery().setHash(DIFFERENT_HASH));
        parameters.turnOffErrorChecks();

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        checkOrderFailures(order);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверка, что Order.label сохраняется и для failure")
    @Test
    public void checkFailedCartLabel() throws Exception {
        String testLabel = "test_label_123";
        Parameters parameters = new Parameters(
                OrderProvider.getBlueOrder(o -> o.setLabel(testLabel))
        );
        parameters.setMultiCartAction((multiCart) -> multiCart.getCarts().get(0).getDelivery().setHash(DIFFERENT_HASH));
        parameters.turnOffErrorChecks();

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        checkOrderFailures(order);
        assertThat(order.getOrderFailures().iterator().next().getOrder().getLabel(), equalTo(testLabel));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с суммой превышающей лимит")
    @Step("Создать заказ с суммой превышающей лимит")
    @Test
    public void createOrderWithTooLargeSum() {
        Parameters parameter = new Parameters();
        parameter.setPaymentMethod(PaymentMethod.YANDEX);
        parameter.getOrder().getItems().forEach(oi -> {
            oi.setBuyerPrice(new BigDecimal("250001"));
            oi.setPrice(new BigDecimal("250001"));
        });
        parameter.turnOffErrorChecks();

        MultiOrder order = orderCreateHelper.createMultiOrder(parameter);

        List<ValidationResult> validationErrors = order.getValidationErrors();

        assertThat(validationErrors, allOf(is(not(nullValue())), is(not(empty()))));
        assertThat(validationErrors.size(), is(1));

        ValidationResult validationError = validationErrors.get(0);
        assertThat(validationError.getSeverity(), is(ValidationResult.Severity.ERROR));
        assertThat(validationError.getType(), is("limitMaxAmount"));
        assertThat(validationError.getCode(), is("payment_amount_limit_exceeded"));

        assertThat(validationError, instanceOf(LimitMaxAmountResult.class));

        LimitMaxAmountResult limitMaxAmountResult = (LimitMaxAmountResult) validationError;
        assertThat(limitMaxAmountResult.getCurrency(), is(Currency.RUR));
        assertThat(limitMaxAmountResult.getMaxAmount(), is(new BigDecimal("250000")));
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-63
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с ошибкой, \"changes\": [\"MISSING\"]")
    @Test
    public void createOrderWithMissingItems() throws Exception {
        Parameters parameters = new Parameters();

        OrderItem onlyItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        FeedOfferId firstItemFeedOfferId = onlyItem.getFeedOfferId();
        parameters.getReportParameters().overrideItemInfo(firstItemFeedOfferId).setHideOffer(true);
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        OrderItem firstCartFirstItem = cart.getCarts().get(0).getItem(firstItemFeedOfferId);
        assertThat(firstCartFirstItem.getCount(), is(0));
        assertThat(firstCartFirstItem.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание синего заказа с ошибкой \"changes\": [\"MISSING\"]")
    @Test
    public void createBlueOrderWithMissingItems() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        OrderItem onlyItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        FeedOfferId firstItemFeedOfferId = onlyItem.getFeedOfferId();
        parameters.getReportParameters().overrideItemInfo(firstItemFeedOfferId).setHideOffer(true);
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        OrderItem firstCartFirstItem = cart.getCarts().get(0).getItem(firstItemFeedOfferId);
        assertThat(firstCartFirstItem.getCount(), is(0));
        assertThat(firstCartFirstItem.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание синего заказа с ошибкой \"changes\": [\"COUNT\"]")
    @Test
    public void createBlueOrderWithMissingItemsInPushApi() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setCount(2);
        var ssItem = SSItem.of(orderItem.getShopSku(), 667, orderItem.getWarehouseId());
        parameters.configuration().cart(parameters.getOrder())
                .setStockStorageResponse(List.of(SSItemAmount.of(ssItem, 1)));
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        assertThat(Iterables.getOnlyElement(cart.getCarts()).getValidationErrors(), anyOf(nullValue(), empty()));
        OrderItem firstCartFirstItem = Iterables.getOnlyElement(Iterables.getOnlyElement(cart.getCarts()).getItems());
        assertThat(firstCartFirstItem.getCount(), is(1));
        assertThat(firstCartFirstItem.getChanges(), containsInAnyOrder(ItemChange.COUNT));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание синего заказа с ошибкой \"changes\": [\"MISSING\"] только одного товара")
    @Test
    public void createBlueOrderWithOneMissingItems() throws Exception {
        Order order = OrderProvider.getBlueOrder(configurableOrder -> {
            String offerId = Iterables.getOnlyElement(configurableOrder.getItems()).getOfferId();
            OrderItem anotherItem = OrderItemProvider.buildOrderItem(
                    new FeedOfferId(
                            String.valueOf(Long.parseLong(offerId) + 1),
                            FEED_ID
                    )
            );
            configurableOrder.addItem(anotherItem);
        });
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);

        OrderItem onlyItem = Optional.ofNullable(
                Iterables.getFirst(parameters.getOrder().getItems(), null)
        ).orElseThrow(IllegalStateException::new);
        FeedOfferId firstItemFeedOfferId = onlyItem.getFeedOfferId();
        parameters.getReportParameters().overrideItemInfo(firstItemFeedOfferId).setHideOffer(true);
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        OrderItem firstCartFirstItem = order.getItem(firstItemFeedOfferId);
        assertThat(firstCartFirstItem.getCount(), is(0));
        assertThat(firstCartFirstItem.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void createBlueOrderWithOneItemWithoutStocks() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.setStockStorageResponse(Collections.singletonList(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(),
                        ObjectUtils.firstNonNull(item.getWarehouseId(), 1)), 0)
        ));

        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        item = order.getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(0));
        assertThat(item.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void cartBlueOrderWithInsufficientStocks() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        item.setCount(3);

        OrderItem pushApiItem = new OrderItem(item);
        pushApiItem.setCount(2);
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(
                Collections.singletonList(pushApiItem),
                parameters.getOrder().getShopId(),
                null,
                parameters.getOrder().getAcceptMethod(),
                false
        );
        parameters.setStockStorageResponse(Collections.singletonList(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(),
                        ObjectUtils.firstNonNull(item.getWarehouseId(), 1)), 2)
        ));
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        item = order.getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(2));
        assertThat(item.getChanges(), hasItem(ItemChange.COUNT));
    }

    @Test
    public void checkoutBlueOrderWithInsufficientStocks() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        item.setCount(3);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        OrderItem cartItem = order.getItem(item.getFeedOfferId());
        assertThat(cartItem.getCount(), is(3));
        assertThat(cartItem.getChanges(), anyOf(nullValue(), empty()));

        parameters.setStockStorageResponse(Collections.singletonList(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(),
                        ObjectUtils.firstNonNull(item.getWarehouseId(), 1)), 2)
        ));
        pushApiConfigurer.mockCart(
                Collections.singletonList(item),
                parameters.getOrder().getShopId(),
                null,
                parameters.getOrder().getAcceptMethod(),
                false
        );
        parameters.turnOffErrorChecks();
        orderCreateHelper.initializeMock(parameters);

        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getOrderFailures(), hasSize(1));
        OrderFailure orderFailure = Iterables.getOnlyElement(checkout.getOrderFailures());
        assertThat(orderFailure.getErrorCode(), is(OrderFailure.Code.OUT_OF_DATE));
        item = orderFailure.getOrder().getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(2));
        assertThat(item.getChanges(), hasItem(ItemChange.COUNT));
    }

    @Test
    public void checkoutBlueOrderWithZeroStocks() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        item.setCount(3);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        OrderItem cartItem = order.getItem(item.getFeedOfferId());
        assertThat(cartItem.getCount(), is(3));
        assertThat(cartItem.getChanges(), anyOf(nullValue(), empty()));

        parameters.setStockStorageResponse(Collections.singletonList(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(),
                        ObjectUtils.firstNonNull(item.getWarehouseId(), 1)), -5)
        ));
        pushApiConfigurer.mockCart(
                Collections.singletonList(item),
                parameters.getOrder().getShopId(),
                null,
                parameters.getOrder().getAcceptMethod(),
                false
        );
        parameters.turnOffErrorChecks();
        orderCreateHelper.initializeMock(parameters);

        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getOrderFailures(), hasSize(1));
        OrderFailure orderFailure = Iterables.getOnlyElement(checkout.getOrderFailures());
        assertThat(orderFailure.getErrorCode(), is(OrderFailure.Code.OUT_OF_DATE));
        item = orderFailure.getOrder().getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(0));
        assertThat(item.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void createBlueOrderWithNegativeStocks() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.setStockStorageResponse(Collections.singletonList(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(),
                        ObjectUtils.firstNonNull(item.getWarehouseId(), 1)), -101)
        ));
        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));

        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        item = order.getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(0));
        assertThat(item.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void shouldNotFailIfNoItemsAreAvailableInReport() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setHideOffer(true);
        parameters.getReportParameters().setIgnoreStocks(false);

        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCartFailures(), anyOf(nullValue(), empty()));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        assertThat(order.getValidationErrors(), anyOf(nullValue(), empty()));
        item = order.getItem(item.getFeedOfferId());
        assertThat(item.getCount(), is(0));
        assertThat(item.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("/cart с пустым полем carts")
    @Issue("MARKETCHECKOUT-6803")
    @Test
    public void cartWithoutCartsField() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuiltMultiCart().setCarts(null);
        parameters.turnOffErrorChecks();

        try {
            client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid());
            fail("/cart should fail!");
        } catch (ErrorCodeException e) {
            assertEquals("INVALID_REQUEST", e.getCode());
            assertEquals("Collection of carts must not be empty", e.getMessage());
        }
    }

    @Test
    public void shouldFailIfAskedForOfferOfAnotherShop() throws Exception {
        Parameters parameters = new Parameters();
        parameters.getReportParameters().setResponseShopId(ANOTHER_SHOP_ID);
        parameters.setCheckCartErrors(false);
        OrderItem onlyItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        FeedOfferId itemFeedOfferId = onlyItem.getFeedOfferId();

        MultiCart cart = orderCreateHelper.cart(parameters);

        List<Order> carts = cart.getCarts();
        assertThat(carts, not(empty()));
        assertThat(carts, hasSize(1));

        Order cartFailure = Iterables.getOnlyElement(carts);
        assertThat(cartFailure.getItem(itemFeedOfferId).getChanges(), not(empty()));
        assertThat(cartFailure.getItem(itemFeedOfferId).getChanges(), contains(ItemChange.MISSING));
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-38
     * <p>
     * Case 2: неправильная дата
     */
    @Test
    public void shouldReturnChangesDeliveryIfWrongDate() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(o -> {
                parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.yandexDelivery()
                                .courier(false)
                                .nextDays(14, 21)
                                .buildActualDeliveryOption(getClock()))
                        .build());
                reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
            });
        });
        parameters.setCheckCartErrors(false);
        parameters.setUseErrorMatcher(false);
        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        checkOrderFailures(multiOrder);
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-38
     * <p>
     * Case 3: неправильный интервал
     */
    @Test
    public void shouldReturnChangesDeliveryIfWrongInterval() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildDeliveryResponseWithIntervals());
        setWrongInterval(parameters);
        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        checkOrderFailures(multiOrder);
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-41
     */
    @Test
    public void shouldReturnChangesDeliveryIfShopDidNotReturnIntervals() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.getDeliveryDates().setToDate(deliveryResponse.getDeliveryDates().getFromDate());

        Parameters parameters = new Parameters();
        parameters.setPushApiDeliveryResponse(deliveryResponse);
        setWrongInterval(parameters);
        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        checkOrderFailures(multiOrder);
    }

    @Test
    public void shouldFailWithUnknownErrorWhenActualizationSuccessfulButAcceptFails() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAcceptFailure(parameters.getOrder());

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.UNKNOWN_ERROR));

        checkOrderCreatedAndCanceled(multiOrder.getCartFailures().get(0).getOrder().getId());
    }

    @Test
    public void shouldFailWithShopErrorWhenActualizationSuccessfulButAcceptFails() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAcceptShopFailure(parameters.getOrder());

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.SHOP_ERROR));

        checkOrderCreatedAndCanceled(multiOrder.getCartFailures().get(0).getOrder().getId());
    }

    @Test
    public void shouldFailWithShopErrorWhenActualizationSuccessfulButNotAccept() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setAcceptOrder(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAccept(parameters.getOrder(), false);

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.SHOP_ERROR));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorReason(), equalTo(OrderFailure.Reason.SHOP_IS_TRICKY));

        checkOrderCreatedAndCanceled(multiOrder.getCartFailures().get(0).getOrder().getId());
    }

    @Test
    public void shouldFailWhenMissingInReport() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        OrderItem onlyItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        onlyItem.setShopSku(null);
        onlyItem.setSku(null);
        onlyItem.setWarehouseId(null);

        OrderItem anotherItem = OrderItemProvider.getAnotherOrderItem();
        parameters.getOrder().addItem(anotherItem);

        FeedOfferId firstItemFeedOfferId = onlyItem.getFeedOfferId();

        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(firstItemFeedOfferId);
        itemInfo.setHideOffer(true);

        parameters.getReportParameters().overrideItemInfo(anotherItem.getFeedOfferId())
                .setFulfilment(new ItemInfo.Fulfilment(FulfilmentProvider.ANOTHER_FF_SHOP_ID,
                        FulfilmentProvider.ANOTHER_TEST_SKU, FulfilmentProvider.ANOTHER_TEST_SHOP_SKU));

        parameters.turnOffErrorChecks();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        OrderItem firstCartFirstItem = cart.getCarts().get(0).getItem(firstItemFeedOfferId);
        assertThat(firstCartFirstItem.getCount(), is(0));
        assertThat(firstCartFirstItem.getChanges(), hasItem(ItemChange.MISSING));
    }

    @Test
    public void shouldReturnChangesItemPriceIfReportPriceDiffers() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        OrderItem item = cart.getCarts().get(0).getItems().iterator().next();
        assertThat(item.getChanges(), is(nullValue()));

        BigDecimal clientBuyerPrice = item.getBuyerPrice();
        assertThat(item.getPrices().getBuyerPriceNominal(), equalTo(clientBuyerPrice));

        BigDecimal newReportPrice = item.getBuyerPrice().add(BigDecimal.TEN);
        parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).getPrices().value = newReportPrice;

        parameters.turnOffErrorChecks();
        cart = orderCreateHelper.cart(parameters);
        item = cart.getCarts().get(0).getItems().iterator().next();
        assertThat(item.getChanges(), CoreMatchers.hasItem(ItemChange.PRICE));

        assertThat(item.getBuyerPrice(), equalTo(newReportPrice));
        assertThat(item.getPrices().getBuyerPriceNominal(), equalTo(newReportPrice));
    }

    @Test
    public void whenPostpaidWithDeferredCourier_shouldReturnValidationError() {
        // Assign
        Parameters parameters = BlueParametersProvider.blueOrderWithDeferredCourierDelivery();
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        parameters.turnOffErrorChecks();

        // Act
        var order = orderCreateHelper.createOrder(parameters);

        // Assert
        List<ValidationResult> validationErrors = order.getValidationErrors();

        assertThat(validationErrors, allOf(is(not(nullValue())), is(not(empty()))));
        assertThat(validationErrors.size(), is(1));

        ValidationResult validationError = validationErrors.get(0);
        assertThat(validationError.getSeverity(), is(ValidationResult.Severity.ERROR));
        assertThat(validationError.getCode(), is("POSTPAID_DEFERRED_COURIER_ERROR"));
    }

    private void checkOrderCreatedAndCanceled(long orderId) throws Exception {
        setFixedTime(getClock().instant().plus(10, ChronoUnit.MINUTES));
        tmsTaskHelper.runExpireOrderTaskV2();

        Order order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderSubstatus.RESERVATION_FAILED, order.getSubstatus());

    }

    private void checkCartFailures(MultiOrder order) {
        assertThat(order.getCartFailures(), is(not(empty())));
        assertThat(order.getCartFailures().size(), is(equalTo(1)));
        assertThat(order.getCartFailures().get(0).getErrorCode(), is(OrderFailure.Code.OUT_OF_DATE));
        assertThat(order.getCartFailures().get(0).getOrder(), is(notNullValue()));
        assertThat(order.getCartFailures().get(0).getOrder().getChanges(), is(not(empty())));
        assertThat(order.getCartFailures().get(0).getOrder().getChanges(), contains(CartChange.PAYMENT));
        assertThat(order.getCartFailures().get(0).getOrder().getChangesReasons(), hasKey(CartChange.PAYMENT));
    }

    private void checkOrderFailures(MultiOrder order) {
        assertThat(order.getOrderFailures(), hasSize(1));
        assertThat(order.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.OUT_OF_DATE));
        assertThat(order.getOrderFailures().get(0).getOrder().getChanges(), contains(CartChange.DELIVERY));
        assertThat(order.getOrderFailures().get(0).getOrder().getChangesReasons(), hasKey(CartChange.DELIVERY));
    }

    private void setWrongInterval(Parameters parameters) {
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(o -> {
                DeliveryDates deliveryDates = o.getDelivery().getDeliveryDates();
                deliveryDates.setFromTime(LocalTime.of(12, 0));
                deliveryDates.setToTime(LocalTime.of(16, 0));
            });
        });
    }

    private void addShopMetaData(Parameters parameters,
                                 ShopMetaData shopMetaData) {
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                shopMetaData
        );
        parameters.getOrder().getItems().stream()
                .map(OrderItem::getSupplierId)
                .filter(Objects::nonNull)
                .forEach(supplierId -> parameters.addShopMetaData(supplierId, shopMetaData));
    }
}
