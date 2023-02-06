package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.CoinError;
import ru.yandex.market.checkout.checkouter.order.LocalCartLabelerService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyUtils;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.checkouter.util.Utils;
import ru.yandex.market.checkout.checkouter.validation.MarketCoinValidationResult;
import ru.yandex.market.checkout.checkouter.validation.PromoCodeValidationResult;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.loyalty.response.DiscountResponseBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderResponseBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.common.report.model.PromoBound;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.common.report.model.PromoThreshold;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.loyalty.api.model.AdditionalInfo;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackOptionsDetails;
import ru.yandex.market.loyalty.api.model.CashbackOptionsDetailsGroup;
import ru.yandex.market.loyalty.api.model.CashbackOptionsDetailsSuperGroup;
import ru.yandex.market.loyalty.api.model.CashbackOptionsRequest;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackPromoRequest;
import ru.yandex.market.loyalty.api.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.DisplayNames;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.ItemCashbackRequest;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.OrderItemRequest;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundleAdditionalFlags;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.report.InternalSpec;
import ru.yandex.market.loyalty.api.model.report.Specs;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.ORDER_UI_PROMO_FLAGS;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.blueMarketPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCoinPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.util.Utils.addCartErrorsChecks;
import static ru.yandex.market.checkout.checkouter.promo.util.Utils.checkCoins;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;
import static ru.yandex.market.checkout.checkouter.validation.ValidationResult.Severity.ERROR;
import static ru.yandex.market.checkout.checkouter.validation.ValidationResult.Severity.WARNING;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.getEmptyDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.buildOrderItem;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.defaultOrderItem;
import static ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer.applyFulfilmentParams;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer.URI_REVERT;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.PROMOCODE;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.PROMOCODE_PROMO_KEY;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.TEST_ITEM_SUBSIDY_VALUE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.EXPIRED_COIN;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_PROCESSABLE_COIN;

/**
 * проверяет передачу параметров и формирование запроса в лоялти
 */
public class LoyaltyRequestTest extends AbstractWebTestBase {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();
    private static final String RANDOM_STRING = RandomStringUtils.random(32);
    private static final Set<String> SPEC_VALUES = Set.of("prescription", "baa");
    private static final String SPEC_TYPE = "spec";

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    private static ru.yandex.market.loyalty.api.model.coin.CoinError buildCoinError(
            Long coinId, MarketLoyaltyErrorCode errorCode, String message) {
        return new ru.yandex.market.loyalty.api.model.coin.CoinError(
                new IdObject(coinId),
                new MarketLoyaltyError(errorCode.name(), message, "userMessage: " + message));
    }

    private static OrderPromo coupon(String promoCode, BigDecimal buyerItemsDiscount) {
        OrderPromo orderPromo = new OrderPromo(marketCouponPromo(
                PROMOCODE_PROMO_KEY,
                null,
                null,
                promoCode,
                "LOYALTY"
        ));
        orderPromo.setBuyerItemsDiscount(buyerItemsDiscount);
        orderPromo.setBuyerSubsidy(BigDecimal.ZERO);
        orderPromo.setSubsidy(BigDecimal.ZERO);
        orderPromo.setDeliveryDiscount(BigDecimal.ZERO);
        return orderPromo;
    }

    private static OrderPromo bluePromo(BigDecimal buyerItemsDiscount) {
        OrderPromo orderPromo = new OrderPromo(blueMarketPromo());
        orderPromo.setBuyerItemsDiscount(buyerItemsDiscount);
        orderPromo.setBuyerSubsidy(BigDecimal.ZERO);
        orderPromo.setSubsidy(BigDecimal.ZERO);
        orderPromo.setDeliveryDiscount(BigDecimal.ZERO);
        return orderPromo;
    }

    private static OrderPromo coin(String promoKey, Long coinId, BigDecimal buyerItemsDiscount) {
        OrderPromo orderPromo = new OrderPromo(marketCoinPromo(promoKey, null, null, coinId, null, null));
        orderPromo.setBuyerItemsDiscount(buyerItemsDiscount);
        orderPromo.setBuyerSubsidy(BigDecimal.ZERO);
        orderPromo.setSubsidy(BigDecimal.ZERO);
        orderPromo.setDeliveryDiscount(BigDecimal.ZERO);
        return orderPromo;
    }

    @Test
    @DisplayName("Проверяем, что прокидывается флаг кгт из actual_delivery в loyalty")
    void testIsLargeSizeProvideToLoyalty() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .addDelivery(12345L)
                        .addPost(7)
                        .addLargeSize(true)
                        .build()
        );

        parameters.setMockLoyalty(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));
        OrderWithBundlesRequest orderWithDeliveriesRequest = discountRequestOrders.get(0);
        assertThat(orderWithDeliveriesRequest.getLargeSize(), equalTo(true));
    }

    @Test
    @DisplayName("Проверяем, что прокидывается флаг isOptionalRulesEnabled в loyalty")
    void testIsOptionalRulesEnabledProvideToLoyalty() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.configuration().cart().request().setIsOptionalRulesEnabled(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        assertTrue(discountRequest.getIsOptionalRulesEnabled());


        orderCreateHelper.checkout(cart, parameters);
        serveEvents = loyaltyConfigurer.servedEvents();
        List<ServeEvent> spendEvents = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_SPEND_V3.equals(event.getRequest().getUrl()))
                .collect(Collectors.toList());

        assertFalse(spendEvents.isEmpty());

        for (ServeEvent event : spendEvents) {
            discountRequest = GSON.fromJson(
                    event.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );

            assertTrue(discountRequest.getIsOptionalRulesEnabled());
        }
    }

    @Test
    @DisplayName("Проверяем, что прокидывается флаг calculateOrdersSeparately в loyalty")
    void testCalculateOrdersSeparatelyProvideToLoyalty() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.configuration().cart().request().setCalculateOrdersSeparately(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        assertTrue(discountRequest.getCalculateOrdersSeparately());
    }

    @Test
    @DisplayName("Проверяем, что если при актуализации выяснили что все айтемы кончились, то мы отправим ордер без " +
            "айтемов с купоном, и лоялти вернет ошибку (т.е. changes=COUNT - валидный, но айтемы с нулем в " +
            "количестве не шлем в лоялти)")
    public void testAllItemWithZeroCount() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setMockPushApi(false);
        parameters.setMockLoyalty(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));

        OrderItem orderItem = defaultOrderItem();
        parameters.turnOffErrorChecks();
        SSItem ssItem = SSItem.of(
                orderItem.getShopSku(), orderItem.getSupplierId(),
                ObjectUtils.firstNonNull(orderItem.getWarehouseId(), 1)
        );
        parameters.setStockStorageResponse(List.of(SSItemAmount.of(ssItem, 0)));
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> cart.setDelivery(getEmptyDelivery()));

        addCartErrorsChecks(parameters.cartResultActions(), "PROMO_CODE_ERROR",
                "COUPON_NOT_APPLICABLE", "ERROR", null);
        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        OrderItem firstCartFirstItem = Iterables.getOnlyElement(Iterables.getOnlyElement(cart.getCarts()).getItems());
        assertThat(firstCartFirstItem.getCount(), is(0));
        assertThat(firstCartFirstItem.getChanges(), containsInAnyOrder(ItemChange.MISSING));
    }

    @Test
    @DisplayName("Проверяем, что если при актуализации выяснили что один из двух айтемов кончился, то мы отправим " +
            "ордер с айтемом, у которого есть стоки и  с купоном, и лоялти не вернет ошибку " +
            "(т.е. changes=COUNT - валидный, но айтемы с нулем в количестве не  шлем в лоялти)")
    public void testItemWithZeroCount() {
        Parameters parameters = CashParametersProvider.createOrderWithTwoItems(false);
        parameters.setMockLoyalty(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMOCODE));

        Iterator<OrderItem> itemsIterator = parameters.getOrder().getItems().iterator();
        OrderItem obsoleteItem = buildOrderItem(itemsIterator.next().getFeedOfferId());
        OrderItem actualItem = itemsIterator.next();
        parameters.turnOffErrorChecks();

        SSItem ssObsoleteItem = SSItem.of(
                obsoleteItem.getShopSku(), obsoleteItem.getSupplierId(),
                ObjectUtils.firstNonNull(obsoleteItem.getWarehouseId(), 1)
        );
        SSItem ssActualItem = SSItem.of(
                actualItem.getShopSku(), actualItem.getSupplierId(),
                ObjectUtils.firstNonNull(actualItem.getWarehouseId(), 1)
        );
        parameters.setStockStorageResponse(List.of(SSItemAmount.of(ssObsoleteItem, 0),
                SSItemAmount.of(ssActualItem, 100)));
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> cart.setDelivery(getEmptyDelivery()));
        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        assertThat(Iterables.getOnlyElement(cart.getCarts()).getValidationErrors(), anyOf(nullValue(), empty()));

        OrderItem obsoleteItemResp = Iterables.getOnlyElement(cart.getCarts()).getItem(obsoleteItem.getFeedOfferId());
        assertThat(obsoleteItemResp.getCount(), is(0));
        assertThat(obsoleteItemResp.getChanges(), containsInAnyOrder(ItemChange.MISSING));

        OrderItem actualItemResp = Iterables.getOnlyElement(cart.getCarts()).getItem(actualItem.getFeedOfferId());
        assertThat(actualItemResp.getCount(), is(actualItem.getCount()));
        assertThat(actualItemResp.getChanges(), anyOf(nullValue(), empty()));
        assertThat(actualItemResp.getPromos(), hasItem(new ItemPromo(
                marketCouponPromo(
                        PROMOCODE_PROMO_KEY,
                        null,
                        null,
                        PROMOCODE,
                        "LOYALTY"
                ),
                TEST_ITEM_SUBSIDY_VALUE, TEST_ITEM_SUBSIDY_VALUE, TEST_ITEM_SUBSIDY_VALUE)
        ));

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(3));

        ServeEvent calcEvent = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl()))
                .findAny()
                .orElse(null);
        assertThat(calcEvent, notNullValue());
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );
        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));
        OrderWithBundlesRequest orderWithDeliveriesRequest = discountRequestOrders.get(0);
        assertThat(orderWithDeliveriesRequest.getItems(), hasSize(1));
        OrderItemRequest itemRequest = orderWithDeliveriesRequest.getItems().get(0);
        assertThat(itemRequest.getOfferId(), equalTo(actualItem.getOfferId()));
        assertThat(itemRequest.getFeedId(), equalTo(actualItem.getFeedId()));
    }

    @Test
    @DisplayName("Проверяем, что если был айтем с changes = MISSING, то не отправляем его в лоялти")
    public void testPromoWithMissingItems() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.getOrder().getItems().forEach(oi -> {
            oi.setWareMd5(null);
            oi.setShowInfo(null);
        });
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPromoCode(PROMOCODE);
        });

        OrderItem originalItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.getReportParameters().overrideItemInfo(originalItem.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);

        List<ServeEvent> servedCalcEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(LoyaltyConfigurer.URI_CALC_V3))
                .collect(Collectors.toList());
        assertThat(servedCalcEvents, hasSize(1));

        ServeEvent calcEvent = servedCalcEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );
        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));
        OrderWithBundlesRequest orderWithDeliveriesRequest = discountRequestOrders.get(0);
        assertThat(orderWithDeliveriesRequest.getItems(), hasSize(0));

        OrderItem item = Iterables.getOnlyElement(cart.getCarts().get(0).getItems());
        assertThat(item.getChanges(), hasItem(ItemChange.MISSING));
        assertThat(item.getPromos(), anyOf(nullValue(), empty()));
    }

    @Test
    @DisplayName("Проверяем, что в лоялти передаются из репорта поля msku (для синих), oldMinPrice и HyperCategoryId")
    public void testOrderItemFieldsToLoyalty() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMOCODE));

        //айтем один
        OrderItem expectedItem = parameters.getOrder().getItems().iterator().next();
        String msku = "11" + expectedItem.getSku();
        BigDecimal oldMinPrice = new BigDecimal(2000);
        parameters.getReportParameters().overrideItemInfo(expectedItem.getFeedOfferId()).getFulfilment().sku = msku;
        parameters.getReportParameters().overrideItemInfo(expectedItem.getFeedOfferId()).getPrices().discountOldMin =
                oldMinPrice;

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCarts().get(0).getChanges(), anyOf(nullValue(), empty()));
        assertThat(cart.getCarts().get(0).getPromos(), hasSize(2));
        assertThat(cart.getCarts().get(0).getPromos(), containsInAnyOrder(
                coupon(PROMOCODE, TEST_ITEM_SUBSIDY_VALUE),
                bluePromo(oldMinPrice.subtract(expectedItem.getBuyerPrice()))
        ));


        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(3));

        ServeEvent calcEvent = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl()))
                .findAny()
                .orElse(null);
        assertThat(calcEvent, notNullValue());
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );
        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));
        OrderWithBundlesRequest orderWithDeliveriesRequest = discountRequestOrders.get(0);
        assertThat(orderWithDeliveriesRequest.getItems(), hasSize(1));

        OrderItemRequest loyaltyItem = orderWithDeliveriesRequest.getItems().get(0);
        assertThat(loyaltyItem.getFeedId(), equalTo(expectedItem.getFeedId()));
        assertThat(loyaltyItem.getOfferId(), equalTo(expectedItem.getOfferId()));
        assertThat(loyaltyItem.getPrice(), equalTo(expectedItem.getPrice()));
        assertThat(loyaltyItem.getQuantity(), equalTo(BigDecimal.valueOf(expectedItem.getCount())));
        assertThat(loyaltyItem.getSku(), equalTo(msku));
        assertThat(loyaltyItem.getHyperCategoryId(), equalTo(expectedItem.getCategoryId()));
        assertThat(loyaltyItem.getOldMinPrice(), equalTo(oldMinPrice));
        assertThat(loyaltyItem.getVendorId(), equalTo(expectedItem.getVendorId()));
    }

    @Test
    @DisplayName("Проверяем что будет если от лоялти придет ошибка по промокоду и инфа по монеткам. все должно " +
            "передаться")
    public void shouldReturnPromoCodeErrorWithCoinErrors() {
        final String expiredCoinMessage = "истекла монеточка";
        final String wrongOwnerMessage = "не трожь чужую монеточку";
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMockLoyalty(false);
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPromoCode(PROMOCODE);
        });
        parameters.getBuiltMultiCart().setCoinIdsToUse(Lists.newArrayList(11L, 22L, 33L, 44L));

        OrderItem itemWithCoin = parameters.getOrder().getItems().iterator().next();
        List<UserCoinResponse> allCoins = EnhancedRandom.randomStreamOf(3, UserCoinResponse.class)
                .collect(Collectors.toList());

        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer ->
                        loyaltyConfigurer.mockResponse(
                                buildDiscountResponse(parameters.getOrder(), itemWithCoin, allCoins,
                                        newArrayList(11L, 22L), newArrayList(
                                                buildCoinError(33L, EXPIRED_COIN, expiredCoinMessage),
                                                buildCoinError(44L, MarketLoyaltyErrorCode.NOT_COIN_OWNER,
                                                        wrongOwnerMessage))
                                ),
                                HttpStatus.OK.value()
                        )
        );

        List<ValidationResult> expectedErrors = List.of(
                new PromoCodeValidationResult(PROMOCODE, "PROMO_NOT_ACTIVE", ERROR, RANDOM_STRING),
                new MarketCoinValidationResult("EXPIRED_COIN", ERROR, "userMessage: "
                        + expiredCoinMessage, 33L),
                new MarketCoinValidationResult("NOT_PROCESSABLE_COIN", ERROR, "userMessage: "
                        + wrongOwnerMessage, 44L)
        );

        List<ValidationResult> expectedWarnings = List.of(
                new MarketCoinValidationResult("UNUSED_COIN", WARNING, null, 11L),
                new MarketCoinValidationResult("UNUSED_COIN", WARNING, null, 22L)
        );

        parameters.setCheckCartErrors(false);

        MultiCart carts = orderCreateHelper.cart(parameters);
        assertThat(carts.getCarts(), hasSize(1));
        Order cart = carts.getCarts().get(0);

        //проверяем как сформировался блок с ошибками
        assertThat(carts.getValidationErrors(), everyItem(in(expectedErrors)));
        assertThat(carts.getValidationWarnings(), everyItem(in(expectedWarnings)));

        //проверяем что промо передалось, даже если были ошибки
        assertThat(cart.getChanges(), anyOf(nullValue(), empty()));
        assertThat(cart.getPromos(), hasSize(1));
        assertThat(cart.getPromos(), containsInAnyOrder(coin("promoKey", 0L, BigDecimal.TEN)));
        assertThat(cart.getItem(itemWithCoin.getFeedOfferId()).getPromos(),
                containsInAnyOrder(new ItemPromo(marketCoinPromo("promoKey", null, null,  0L, null, null),
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)));

        // проверяем что даже если была ошибка(но не исключение), то инфа по монеткам, которую дал лоялти передалась
        // в блоках allcoins, coinErrors, UnusedCoins
        checkCoins(carts.getCoinInfo().getAllCoins(), allCoins);
        assertThat(carts.getCoinInfo().getCoinErrors(), containsInAnyOrder(
                new CoinError(33L, EXPIRED_COIN.name(), "userMessage: " + expiredCoinMessage),
                new CoinError(44L, NOT_PROCESSABLE_COIN.name(), "userMessage: " + wrongOwnerMessage)
        ));
        assertThat(carts.getCoinInfo().getUnusedCoinIds(), containsInAnyOrder(11L, 22L));
    }

    @Test
    @DisplayName("Проверяем, что данные лоялти прокидвываются в /cart")
    public void shouldReturnLoyaltyFieldsFromCart() {
        int count = 2;
        final BigDecimal discount = BigDecimal.valueOf(100);

        Parameters parameters = defaultBlueOrderParameters();
        OrderItem orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        orderItem.setCount(count);
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(orderItem).build()));
        parameters.getLoyaltyParameters().setExpectedPromoCode(PROMOCODE);

        parameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(orderItem)
                                .promos(new ItemPromoResponse(
                                        discount,
                                        ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        null,
                                        null,
                                        true,
                                        PROMOCODE,
                                        null,
                                        null,
                                        new AdditionalInfo(CoinType.FIXED, BigDecimal.TEN, 2),
                                        new DisplayNames("Промокод " + PROMOCODE)
                                ))
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);

        parameters.setCheckCartErrors(false);

        MultiCart carts = orderCreateHelper.cart(parameters);

        assertThat(carts.getCarts(), hasSize(1));
        Order cart = carts.getCarts().get(0);
        assertThat(cart.getPromos(), hasSize(1));
        assertThat(cart.getItems(), hasSize(1));
        OrderItem item = cart.getItems().iterator().next();
        assertThat(item.getPromos(), hasSize(1));
        ItemPromo promocode = item.getPromos().iterator().next();

        assertThat(promocode.getAdditionalInfo(), allOf(
                hasProperty("coinType", equalTo(ru.yandex.market.checkout.checkouter.order.promo.CoinType.FIXED)),
                hasProperty("nominal", comparesEqualTo(BigDecimal.TEN)),
                hasProperty("quantityInBundle", equalTo(2))));

        assertThat(promocode.getTotalDiscount(), comparesEqualTo(discount.multiply(BigDecimal.valueOf(count))));

        assertThat(promocode.getDisplayNames(), allOf(
                hasProperty("details", equalTo("Промокод " + PROMOCODE))));
    }

    @Test
    public void shouldAllowCheckoutWhenRegionMismatchError() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMockLoyalty(false);
        parameters.getBuiltMultiCart().setCoinIdsToUse(List.of(11L));

        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer ->
                        loyaltyConfigurer.mockResponse(
                                buildDiscountResponseWithRegionMismatchError(parameters),
                                HttpStatus.OK.value()
                        )
        );

        List<ValidationResult> expectedWarnings = List.of(
                new MarketCoinValidationResult("REGION_MISMATCH_ERROR", WARNING, "userMessage: ", 11L)
        );

        MultiOrder orders = orderCreateHelper.createMultiOrder(parameters);
        assertThat(orders.getValidationWarnings(), everyItem(in(expectedWarnings)));
    }

    @Test
    public void testDiscountsRevertSingleCallOnReservationFailed() {
        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.setAcceptOrder(false);
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMOCODE));
        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrderFailures(), hasSize(1));
        assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(), equalTo(OrderFailure.Code.SHOP_ERROR));

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents.stream().filter(event -> event.getRequest().getAbsoluteUrl().contains(URI_REVERT))
                .count(), is(1L));
    }

    @Test
    public void sendingCargoTypesInLoyaltyCalc() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.getOrder().getItems().forEach(i -> i.setCargoTypes(Sets.newHashSet(1, 2, 3)));

        orderCreateHelper.cart(parameters);

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartDiscountRequest.class
        );

        List<OrderItemRequest> itemsInRequest = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.toList());

        assertThat(itemsInRequest, everyItem(hasProperty("cargoTypes", containsInAnyOrder(1, 2, 3))));
    }

    @Test
    public void deliveryPartnerTypeInRequest() throws Exception {
        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setMockLoyalty(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(5));

        for (ServeEvent event : serveEvents) {
            MultiCartDiscountRequest discountRequest = GSON.fromJson(
                    event.getRequest().getBodyAsString(),
                    MultiCartDiscountRequest.class
            );

            assertThat(
                    discountRequest.getOrders().stream().flatMap(o -> o.getItems().stream()).
                            collect(Collectors.toList()),
                    everyItem(hasProperty("deliveryPartnerTypes", contains("YANDEX_MARKET")))
            );
        }
    }

    @Test
    public void deviceIdInRequest() throws Exception {
        String deviceId = "{\"androidBuildModel\":\"Redmi 5A\"," +
                "\"androidDeviceId\":\"deadbeeff8f2e9fc\"," +
                "\"googleServiceId\":\"52ac44c3-37d3-47a2-a04d-e430c3ee0937\"," +
                "\"androidBuildManufacturer\":\"Xiaomi\"," +
                "\"androidHardwareSerial\":\"f0ad12345678\"}";
        final Map<String, String> deviceIdMap = GSON.fromJson(deviceId, new TypeToken<Map<String, String>>() {
        }.getType());
        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, deviceId);
        final MultiCart sourceMultiCart = parameters.getBuiltMultiCart();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);
        final Buyer multiCartBuyer = sourceMultiCart.getBuyer();

        assertThat(loyaltyConfigurer.servedEvents(), hasSize(5));

        Stream.of(LoyaltyConfigurer.URI_CALC_V3, LoyaltyConfigurer.URI_SPEND_V3)
                .map(WireMock::urlPathEqualTo)
                .map(WireMock::postRequestedFor)
                .map(loyaltyConfigurer::findAll)
                .flatMap(List::stream)
                .forEach(loggedRequest -> {
                    MultiCartDiscountRequest discountRequest = GSON.fromJson(
                            loggedRequest.getBodyAsString(),
                            MultiCartDiscountRequest.class
                    );

                    final DeviceInfoRequest deviceInfoRequest = discountRequest.getDeviceInfoRequest();
                    assertThat(
                            deviceInfoRequest.getDeviceId(), equalTo(deviceIdMap)
                    );
                    assertThat(
                            deviceInfoRequest.getEmail(), equalTo(multiCartBuyer.getEmail())
                    );
                    assertThat(
                            deviceInfoRequest.getMuid(), equalTo(multiCartBuyer.getMuid())
                    );
                    assertThat(
                            deviceInfoRequest.getPhone(), equalTo(multiCartBuyer.getPhone())
                    );
                });
    }

    @Test
    public void experimentsInRequest() throws Exception {
        final String testExperimentValue = "suchExperiment=1";
        final int requestNumber = 5;

        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.setExperiments(testExperimentValue);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        assertThat(loyaltyConfigurer.servedEvents(), hasSize(requestNumber));
        List<ServeEvent> events = loyaltyConfigurer.servedEvents();
        List<HttpHeader> experiments =
                events.stream()
                        .map(event -> event.getRequest().getHeaders().getHeader(CheckoutCommonParams.X_EXPERIMENTS))
                        .collect(Collectors.toList());
        assertThat(experiments, hasSize(requestNumber));
        assertThat(experiments.get(0), equalTo(new HttpHeader(CheckoutCommonParams.X_EXPERIMENTS,
                testExperimentValue)));
    }

    @Test
    public void deviceIdInMultiOrderRequest() throws Exception {
        String deviceId1 = "{\"androidBuildModel\":\"Redmi 5A\"," +
                "\"androidDeviceId\":\"deadbeeff8f2e9fc\"," +
                "\"googleServiceId\":\"52ac44c3-37d3-47a2-a04d-e430c3ee0937\"," +
                "\"androidHardwareSerial\":\"f0ad12345678\"}";

        String deviceId2 = "{\"androidBuildModel\":\"Redmi 5A\"," +
                "\"androidDeviceId\":\"deadbeeff8f2e9fc\"," +
                "\"googleServiceId\":\"52ac44c3-37d3-47a2-a04d-e430c3ee0937\"," +
                "\"androidBuildManufacturer\":\"Xiaomi\"}";
        final Map<String, String> deviceIdMap1 = GSON.fromJson(deviceId1, new TypeToken<Map<String, String>>() {
        }.getType());
        final Map<String, String> deviceIdMap2 = GSON.fromJson(deviceId2, new TypeToken<Map<String, String>>() {
        }.getType());
        final Map<String, String> deviceIdMergedMap = new HashMap<>(deviceIdMap1);
        deviceIdMergedMap.putAll(deviceIdMap2);
        Parameters parameters = defaultBlueOrderParameters();
        final Parameters parameters2 = defaultBlueOrderParameters();
        parameters.setMockLoyalty(true);
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, deviceId1);
        parameters2.getOrder().setProperty(OrderPropertyType.DEVICE_ID, deviceId2);
        parameters.addOrder(parameters2);
        final MultiCart sourceMultiCart = parameters.getBuiltMultiCart();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);
        final Buyer multiCartBuyer = sourceMultiCart.getBuyer();

        assertThat(loyaltyConfigurer.servedEvents(), hasSize(5));

        Stream.of(LoyaltyConfigurer.URI_CALC_V3, LoyaltyConfigurer.URI_SPEND_V3)
                .map(WireMock::urlPathEqualTo)
                .map(WireMock::postRequestedFor)
                .map(loyaltyConfigurer::findAll)
                .flatMap(List::stream)
                .forEach(loggedRequest -> {
                    MultiCartDiscountRequest discountRequest = GSON.fromJson(
                            loggedRequest.getBodyAsString(),
                            MultiCartDiscountRequest.class
                    );

                    assertThat(discountRequest.getOrders(), hasSize(2));

                    final DeviceInfoRequest deviceInfoRequest = discountRequest.getDeviceInfoRequest();
                    assertThat(
                            deviceInfoRequest.getDeviceId(), equalTo(deviceIdMergedMap)
                    );
                    assertThat(
                            deviceInfoRequest.getEmail(), equalTo(multiCartBuyer.getEmail())
                    );
                    assertThat(
                            deviceInfoRequest.getMuid(), equalTo(multiCartBuyer.getMuid())
                    );
                    assertThat(
                            deviceInfoRequest.getPhone(), equalTo(multiCartBuyer.getPhone())
                    );

                });
    }

    @Test
    public void testPromoKeysFromLoyaltyAreSaved() throws Exception {
        checkouterProperties.setPromoKeyName("bf2020_promo_key");
        checkouterProperties.setUsePromoKeysFromLoyaltyEmitResponse(true);
        var parameters = defaultBlueOrderParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        Order cart = multiOrder.getCarts().get(0);
        var promoKeyName = checkouterProperties.getPromoKeyName();
        assertThat(cart.getProperty(promoKeyName), is("promoKey"));
    }

    @Test
    public void testPromoKeysFromLoyaltyAreNotSavedWhenTheFeatureIsOff() throws Exception {
        checkouterProperties.setPromoKeyName("bf2020_promo_key");
        checkouterProperties.setUsePromoKeysFromLoyaltyEmitResponse(false);
        var parameters = defaultBlueOrderParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var promoKeyName = checkouterProperties.getPromoKeyName();
        assertThat(multiOrder.getCarts(), hasSize(1));
        Order cart = multiOrder.getCarts().get(0);
        assertThat(cart.getProperty(promoKeyName), is(nullValue()));
    }

    @Test
    @DisplayName("Проверка что запрос в лоялти уходит с полем paymentSystemType, cashbackType")
    void shouldSendRequestWithPaymentSystemType() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        builtMultiCart.setSelectedCashbackOption(CashbackOption.EMIT);
        Order order = parameters.getOrder();
        order.setPaymentSystem("MASTERCARD");
        parameters.setMockLoyalty(true);
        MultiCart cart = orderCreateHelper.cart(parameters);
        List<ServeEvent> calcAndSpendEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl())
                        || LoyaltyConfigurer.URI_SPEND_V3.equals(event.getRequest().getUrl()))
                .collect(Collectors.toList());
        assertThat(calcAndSpendEvents, not(empty()));
        for (ServeEvent requestEvent : calcAndSpendEvents) {
            String requestBody = requestEvent.getRequest().getBodyAsString();
            MultiCartWithBundlesDiscountRequest request = GSON.fromJson(
                    requestBody,
                    MultiCartWithBundlesDiscountRequest.class
            );
            assertThat(request.getCashbackOptionType(), is(CashbackType.EMIT));
            assertThat(requestBody, containsString("\"paymentSystemType\":\"mastercard\""));
        }
    }

    @Test
    @DisplayName("Проверка корректности запроса в лоялти после добавления новых полей - CashbackPromoRequest имеет " +
            "поле номинала, полученного от репорта, путем домножения поля share * 100, трешхолды, приоритет, " +
            "bucketName, specs, payByYaPlus, cmsDescriptionSemanticId полученные от " +
            "репорта")
    void shouldSendRequestWithNewFieldsToLoyalty() {
        final String promoKey = "j728vjnfPYaBILuXk_4zFQ";
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));

        final OfferPromo promo = new OfferPromo();
        promo.setPromoMd5(promoKey);
        promo.setPromoType("blue-cashback");
        BigDecimal share = new BigDecimal("123");
        promo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey(promoKey)
                        .promoType("blue-cashback")
                        .partnerId(1L)
                        .maxOfferCashbackThresholds(Set.of(PromoThreshold.builder()
                                        .code("1")
                                        .value(BigDecimal.ONE)
                                        .build(),
                                PromoThreshold.builder()
                                        .code("2")
                                        .value(BigDecimal.TEN)
                                        .build()))
                        .minOrderTotalThresholds(Set.of(PromoThreshold.builder()
                                .code("3")
                                .value(BigDecimal.ZERO)
                                .build()))
                        .marketTariffsVersionId(10L)
                        .share(share)
                        .promoBucketName("bucketName")
                        .priority(1)
                        .cmsDescriptionSemanticId("partner-default-cashback")
                        .build()
        );

        Specs expectedMedicalSpecs = new Specs(
                SPEC_VALUES.stream()
                        .map(specValue -> new InternalSpec(SPEC_TYPE, specValue))
                        .collect(Collectors.toSet())
        );
        Integer expectedPayByYaPlusPrice = 100;

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(foundOfferBuilder -> foundOfferBuilder
                        .promos(List.of(promo))
                        .specs(
                                ru.yandex.market.common.report.model.specs.Specs.fromSpecValues(SPEC_VALUES))
                        .payByYaPlus(PayByYaPlus.of(expectedPayByYaPlusPrice))
                        .build())
                .collect(Collectors.toList()));

        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        ServeEvent cashbackOptionsEvent = serveEvents.get(1);
        CashbackOptionsRequest cashbackOptionsRequest = GSON.fromJson(
                cashbackOptionsEvent.getRequest().getBodyAsString(),
                CashbackOptionsRequest.class
        );
        assertThat(cashbackOptionsRequest.getOrders().get(0).getItems().get(0).getPayByYaPlus(),
                is(expectedPayByYaPlusPrice));
        final List<OrderWithBundlesRequest> orders = discountRequest.getOrders();
        BundledOrderItemRequest bundledOrderItemRequest = orders.get(0).getItems().get(0);
        final CashbackPromoRequest promoRequest = bundledOrderItemRequest.getCashbackPromoRequests().get(0);
        assertThat(bundledOrderItemRequest.getPayByYaPlus(), is(expectedPayByYaPlusPrice));
        assertThat(bundledOrderItemRequest.getSpecs(), is(expectedMedicalSpecs));
        assertThat(promoRequest.getNominal(), comparesEqualTo(share.multiply(Utils.HUNDRED)));
        assertThat(promoRequest.getThresholds(), hasItems("1", "2", "3"));
        assertThat(promoRequest.getPriority(), is(1));
        assertThat(promoRequest.getPromoBucketName(), is("bucketName"));
        assertThat(promoRequest.getCmsSemanticId(), is("partner-default-cashback"));
    }

    @Test
    void shouldSendIsExpressDelivery() {
        Parameters parameters = blueNonFulfilmentOrderWithExpressDelivery();
        orderCreateHelper.cart(parameters);
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        ServeEvent cashbackOptionsEvent = serveEvents.stream()
                .filter(event -> event.getRequest().getUrl().startsWith(LoyaltyConfigurer.URI_CASHBACK_OPTIONS))
                .findFirst()
                .get();
        CashbackOptionsRequest cashbackOptionsRequest = GSON.fromJson(
                cashbackOptionsEvent.getRequest().getBodyAsString(),
                CashbackOptionsRequest.class
        );
        Assertions.assertTrue(cashbackOptionsRequest.getOrders().get(0).getItems().get(0).getIsExpressDelivery());
    }

    @Test
    void shouldSendOnlyBlueCashbackTypePromos() {
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();
        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));

        final OfferPromo blueCashbakPromo = new OfferPromo();
        blueCashbakPromo.setPromoMd5("j728vjnfPYaBILuXk_4zFQ");
        blueCashbakPromo.setPromoType("blue-cashback");
        blueCashbakPromo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey("j728vjnfPYaBILuXk_4zFQ")
                        .promoType("blue-cashback")
                        .partnerId(1L)
                        .maxOfferCashbackThresholds(Set.of(PromoThreshold.builder()
                                        .code("1")
                                        .value(BigDecimal.ONE)
                                        .build(),
                                PromoThreshold.builder()
                                        .code("2")
                                        .value(BigDecimal.TEN)
                                        .build()))
                        .minOrderTotalThresholds(Set.of(PromoThreshold.builder()
                                .code("3")
                                .value(BigDecimal.ZERO)
                                .build()))
                        .marketTariffsVersionId(10L)
                        .share(BigDecimal.TEN)
                        .promoBucketName("bucketName")
                        .priority(1)
                        .build()
        );

        final OfferPromo spreadDiscountReceiptPromo = new OfferPromo();
        spreadDiscountReceiptPromo.setPromoMd5("j728vjnfPYaBILuXk_4zFQ1");
        spreadDiscountReceiptPromo.setPromoType("spread-discount-receipt");
        spreadDiscountReceiptPromo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey("j728vjnfPYaBILuXk_4zFQ1")
                        .promoType("spread-discount-receipt")
                        .partnerId(1L)
                        .maxOfferCashbackThresholds(Set.of(PromoThreshold.builder()
                                        .code("1")
                                        .value(BigDecimal.ONE)
                                        .build(),
                                PromoThreshold.builder()
                                        .code("2")
                                        .value(BigDecimal.TEN)
                                        .build()))
                        .minOrderTotalThresholds(Set.of(PromoThreshold.builder()
                                .code("3")
                                .value(BigDecimal.ZERO)
                                .build()))
                        .marketTariffsVersionId(11L)
                        .share(BigDecimal.ONE)
                        .promoBucketName("bucketName2")
                        .priority(1)
                        .build()
        );
        List<OfferPromo> blueCashbackOfferPromos = List.of(blueCashbakPromo);
        List<OfferPromo> spreadDiscountReceiptPromos = List.of(spreadDiscountReceiptPromo);

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(foundOfferBuilder -> foundOfferBuilder
                        .promos(ListUtils.union(blueCashbackOfferPromos, spreadDiscountReceiptPromos))
                        .specs(
                                ru.yandex.market.common.report.model.specs.Specs.fromSpecValues(SPEC_VALUES))
                        .payByYaPlus(PayByYaPlus.of(100))
                        .build())
                .collect(Collectors.toList()));
        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );
        final List<OrderWithBundlesRequest> orders = discountRequest.getOrders();
        BundledOrderItemRequest bundledOrderItemRequest = orders.get(0).getItems().get(0);
        final List<CashbackPromoRequest> promoRequests = bundledOrderItemRequest.getCashbackPromoRequests();
        assertThat(promoRequests, hasSize(blueCashbackOfferPromos.size()));
    }

    @Test
    void shouldSendBnpl() throws Exception {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
        boolean bnplSelected = true;
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(bnplSelected);
        MultiCart cart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(cart, parameters);
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        List<ServeEvent> calcEvents = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl()))
                .collect(Collectors.toList());
        List<ServeEvent> optionEvents = serveEvents.stream()
                .filter(event -> LoyaltyConfigurer.URI_CASHBACK_OPTIONS.equals(event.getRequest().getUrl()))
                .collect(Collectors.toList());
        for (ServeEvent calcEvent : calcEvents) {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    calcEvent.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            assertThat(discountRequest.getBnplSelected(), equalTo(bnplSelected));
        }
        for (ServeEvent optionEvent : optionEvents) {
            CashbackOptionsRequest cashbackOptionsRequest = GSON.fromJson(
                    optionEvent.getRequest().getBodyAsString(),
                    CashbackOptionsRequest.class
            );
            assertThat(cashbackOptionsRequest.getBnplSelected(), equalTo(bnplSelected));
        }
    }

    @Test
    void shouldSendUsageClientDeviceType() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPlatform(Platform.YANDEX_GO_ANDROID);

        MultiCart cart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(cart, parameters);

        List<ServeEvent> optionEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> event.getRequest().getUrl() != null)
                .filter(event -> event.getRequest().getUrl().startsWith(LoyaltyConfigurer.URI_CASHBACK_OPTIONS))
                .collect(Collectors.toList());
        assertThat(optionEvents, hasSize(2));
        for (ServeEvent optionEvent : optionEvents) {
            CashbackOptionsRequest cashbackOptionsRequest = GSON.fromJson(
                    optionEvent.getRequest().getBodyAsString(),
                    CashbackOptionsRequest.class
            );
            assertThat(cashbackOptionsRequest.getClientDeviceType(), equalTo(UsageClientDeviceType.MARKET_GO));
        }
    }

    @Test
    void shouldSendAllowedPaymentTypesToCalc() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(cart, parameters);
        List<ServeEvent> calcEvents =
                loyaltyConfigurer.servedEvents().stream().filter(event ->
                        LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl())).collect(Collectors.toList());
        for (ServeEvent calcEvent : calcEvents) {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    calcEvent.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            Set<PaymentMethod> paymentOptions = cart.getPaymentOptions();
            List<BundledOrderItemRequest> items = discountRequest.getOrders().get(0).getItems();
            for (BundledOrderItemRequest item : items) {
                Set<PaymentType> allowedPaymentTypes = item.getAllowedPaymentTypes();
                assertThat(paymentOptions, hasSize(allowedPaymentTypes.size()));
                for (PaymentMethod paymentOption : paymentOptions) {
                    assertThat(
                            allowedPaymentTypes,
                            hasItem(LoyaltyUtils.PaymentTypeConverter.toLoyaltyPaymentType(paymentOption))
                    );
                }
            }
        }
    }

    @Test
    public void shouldSendAllowedPaymentTypesToCashbackOptions() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(cart, parameters);
        List<ServeEvent> cashbackOptionsEvents =
                loyaltyConfigurer.servedEvents().stream().filter(event ->
                        event.getRequest().getUrl().startsWith(LoyaltyConfigurer.URI_CASHBACK_OPTIONS)
                ).collect(Collectors.toList());
        assertFalse(cashbackOptionsEvents.isEmpty());
        for (ServeEvent cashbackOptionEvent : cashbackOptionsEvents) {
            CashbackOptionsRequest cashbackOptionsRequest = GSON.fromJson(
                    cashbackOptionEvent.getRequest().getBodyAsString(),
                    CashbackOptionsRequest.class
            );
            Set<PaymentMethod> paymentOptions = cart.getPaymentOptions();
            List<ItemCashbackRequest> items = cashbackOptionsRequest.getOrders().get(0).getItems();
            for (ItemCashbackRequest item : items) {
                Set<PaymentType> allowedPaymentTypes = item.getAllowedPaymentTypes();
                assertThat(paymentOptions, hasSize(allowedPaymentTypes.size()));
                for (PaymentMethod paymentOption : paymentOptions) {
                    assertThat(
                            allowedPaymentTypes,
                            hasItem(LoyaltyUtils.PaymentTypeConverter.toLoyaltyPaymentType(paymentOption))
                    );
                }
            }
        }
    }

    @Test
    public void shouldSendRequestWithCreditPaymentTypeWhenTinkoffCreditSelected() throws Exception {
        loyaltyConfigurer.resetAll();
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        MultiCart cart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(cart, parameters);
        List<ServeEvent> calcEvents =
                loyaltyConfigurer.servedEvents().stream().filter(event ->
                        LoyaltyConfigurer.URI_CALC_V3.equals(event.getRequest().getUrl())).collect(Collectors.toList());

        assertThat(calcEvents, hasSize(2));
        for (ServeEvent calcEvent : calcEvents) {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    calcEvent.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            assertThat(discountRequest.getOrders().get(0).getPaymentType(), CoreMatchers.is(PaymentType.CREDIT));
        }
    }

    @Test
    public void testCashbackDetails() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        List<String> detailsUiPromoFlag = List.of("detailsUiPromoFlag1", "detailsUiPromoFlag2");
        String detailsGroupKey = "detailsGroupKey";
        String detailsGroupName = "detailsGroupName";
        BigDecimal detailsGroupAmount = BigDecimal.valueOf(100);
        List<String> detailsGroupPromoKeys = List.of("1", "2");
        List<String> detailsGroupUiPromoFlag = List.of("detailsGroupUiPromoFlag3", "detailsGroupUiPromoFlag4");
        String cmsSemanticId = "default-cashback";
        String cashbackOptionsDetailsSuperGroupKey = "cashbackOptionsDetailsSuperGroupKey";
        String cashbackOptionsDetailsSuperGroupName = "cashbackOptionsDetailsSuperGroupName";
        List<String> cashbackOptionsDetailsSuperGroupKeys = List.of("groupKey1", "groupKey2");
        List<String> cashbackOptionsDetailsSuperGroupUiFlags = List.of("cashbackOptionsDetailsSuperGroupUiFlags");
        List<String> uiOrderPromoFlags = List.of("ui1", "ui2", "ui3");
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                        new CashbackOptions(
                                "1",
                                1,
                                Map.of("1", BigDecimal.TEN),
                                List.of(CashbackPromoResponse.builder()
                                        .setAmount(BigDecimal.TEN)
                                        .setPromoKey("1")
                                        .setUiPromoFlags(List.of("1"))
                                        .build()),
                                BigDecimal.ONE,
                                CashbackPermision.ALLOWED,
                                null,
                                uiOrderPromoFlags,
                                null,
                                new CashbackOptionsDetails(
                                        detailsUiPromoFlag,
                                        List.of(
                                                new CashbackOptionsDetailsGroup(
                                                        detailsGroupKey,
                                                        detailsGroupName,
                                                        detailsGroupAmount,
                                                        detailsGroupPromoKeys,
                                                        detailsGroupUiPromoFlag,
                                                        cmsSemanticId
                                                )
                                        ),
                                        List.of(new CashbackOptionsDetailsSuperGroup(
                                                cashbackOptionsDetailsSuperGroupKey,
                                                cashbackOptionsDetailsSuperGroupName,
                                                cashbackOptionsDetailsSuperGroupKeys,
                                                cashbackOptionsDetailsSuperGroupUiFlags
                                        )),
                                        null
                                )
                        ),
                        new CashbackOptions(
                                null, null, null, null, BigDecimal.ONE,
                                CashbackPermision.ALLOWED, null, null, null,
                                new CashbackOptionsDetails(Collections.emptyList(), Collections.emptyList(),
                                        Collections.emptyList(), Map.of())
                        ),
                        CashbackType.EMIT
                )
        );
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(CashbackOption.EMIT);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getCarts().get(0);
        Order savedOrder = orderService.getOrder(order.getId());
        assertThat(savedOrder.getProperty(ORDER_UI_PROMO_FLAGS), equalTo(uiOrderPromoFlags));
//        Детализации по спенду не должно быть
        assertThat(cart.getCashback().getSpend().getDetails(), nullValue());
//        Детализация по эмиту
        ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptionsDetails emitDetails =
                cart.getCashback().getEmit().getDetails();
        assertThat(emitDetails.getUiPromoFlags(), CoreMatchers.is(detailsUiPromoFlag));
        assertThat(emitDetails.getGroups(), hasSize(1));
        ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptionsDetailsSuperGroup superGroup =
                emitDetails.getSuperGroups().get(0);
        assertThat(superGroup.getKey(), CoreMatchers.is(cashbackOptionsDetailsSuperGroupKey));
        assertThat(superGroup.getName(), CoreMatchers.is(cashbackOptionsDetailsSuperGroupName));
        assertThat(superGroup.getGroupsKeys(), CoreMatchers.is(cashbackOptionsDetailsSuperGroupKeys));
        assertThat(superGroup.getUiPromoFlags(), CoreMatchers.is(cashbackOptionsDetailsSuperGroupUiFlags));
        ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptionsDetailsGroup emitGroupDetails =
                emitDetails.getGroups().get(0);
        assertThat(emitGroupDetails.getKey(), CoreMatchers.is(detailsGroupKey));
        assertThat(emitGroupDetails.getName(), CoreMatchers.is(detailsGroupName));
        assertThat(emitGroupDetails.getAmount(), CoreMatchers.is(detailsGroupAmount));
        assertThat(emitGroupDetails.getPromoKeys(), CoreMatchers.is(detailsGroupPromoKeys));
        assertThat(emitGroupDetails.getCmsSemanticId(), CoreMatchers.is(cmsSemanticId));
    }

    @Test
    void shouldSendPromocodePromoKeysToLoyalty() {
        final String promoKey = "j728vjnfPYaBILuXk_4zFQ";
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));

        final OfferPromo offerPromo = new OfferPromo();
        offerPromo.setPromoMd5(promoKey);
        offerPromo.setPromoStock(BigDecimal.ONE);
        offerPromo.setPromoType("promo-code");
        offerPromo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey(promoKey)
                        .promoType("promo-code")
                        .build()
        );

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(foundOfferBuilder -> foundOfferBuilder
                        .promos(List.of(offerPromo))
                        .build())
                .collect(Collectors.toList()));

        parameters.setMockLoyalty(true);
        final MultiCart cart = orderCreateHelper.cart(parameters);

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        final String shopSku = cart.getCarts().get(0).getItems().stream().findFirst().get().getShopSku();
        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));
        OrderWithBundlesRequest orderWithDeliveriesRequest = discountRequestOrders.get(0);
        assertThat(orderWithDeliveriesRequest.getItems(), hasItems(
                allOf(
                        hasProperty("shopSku", equalTo(shopSku)),
                        hasProperty("promoKeys", contains(promoKey))

                )
        ));
    }

    @Test
    void shouldSendEdaFlagToLoyalty() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getReportParameters().setIsEda(true);

        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));

        Map<OrderWithBundleAdditionalFlags, String> additionalFlags = discountRequestOrders.get(0).getAdditionalFlags();
        assertThat(additionalFlags, hasEntry(OrderWithBundleAdditionalFlags.DELIVERED_BY_EATS, "true"));
    }

    @Test
    void shouldSendEmptyFlagsForRegularOrderToLoyalty() {
        Parameters parameters = defaultBlueOrderParameters();

        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        List<OrderWithBundlesRequest> discountRequestOrders = discountRequest.getOrders();
        assertThat(discountRequestOrders, hasSize(1));

        Map<OrderWithBundleAdditionalFlags, String> additionalFlags = discountRequestOrders.get(0).getAdditionalFlags();
        assertThat(additionalFlags, nullValue());
    }

    @Test
    public void shouldApplySupplierMultiCartPromoWhenSupplierDiscountExist() {
        Parameters parameters = createMultiOrderParameters();
        enableUnifiedTariffs(parameters);

        parameters.setCheckCartErrors(false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getCarts(), hasSize(3));
        Order cart1 = multiCart.getCarts().get(0);
        Delivery cart1Delivery = cart1.getDeliveryOptions().get(0);
        Order cart2 = multiCart.getCarts().get(1);
        Delivery cart2Delivery = cart2.getDeliveryOptions().get(0);
        Order cart3 = multiCart.getCarts().get(2);
        Delivery cart3Delivery = cart3.getDeliveryOptions().get(0);

        assertThat(cart1.getTotal(), equalTo(cart1.getItemsTotal()));
        assertThat(cart1.getBuyerTotal(), equalTo(cart1.getItemsTotal()));
        assertThat(cart1.getPromoPrices().getSubsidyTotal(), nullValue());
        assertThat(cart1.getPromoPrices().getBuyerTotalDiscount(), equalTo(BigDecimal.ZERO));
        assertThat(cart1.getPromoPrices().getBuyerTotalBeforeDiscount(), equalTo(cart1.getItemsTotal()));
        assertThat(cart1.getPromos(), hasSize(0));
        assertThat(cart1Delivery.getPrice(), equalTo(BigDecimal.ZERO));
        assertThat(cart1Delivery.getPrices().getBuyerPriceBeforeDiscount(), equalTo(BigDecimal.ZERO));
        assertThat(cart1Delivery.getPrices().getBuyerDiscount(), nullValue());
        assertThat(cart1Delivery.getPrices().getBuyerSubsidy(), nullValue());
        assertThat(cart1Delivery.getPrices().getSubsidy(), nullValue());
        assertThat(cart1Delivery.getPromos(), hasSize(0));

        BigDecimal expectedCart2DeliveryPrice = BigDecimal.valueOf(25);
        BigDecimal expectedCart2Discount = BigDecimal.valueOf(24);
        BigDecimal expectedCart2TotalPrice = cart2.getItemsTotal().add(expectedCart2DeliveryPrice);
        assertThat(cart2.getTotal(), equalTo(expectedCart2TotalPrice));
        assertThat(cart2.getBuyerTotal(), equalTo(expectedCart2TotalPrice));
        assertThat(cart2.getPromoPrices().getBuyerTotalDiscount(), equalTo(expectedCart2Discount));
        assertThat(cart2.getPromoPrices().getBuyerTotalBeforeDiscount(),
                equalTo(cart2.getItemsTotal().add(BigDecimal.valueOf(49))));
        assertThat(cart2.getPromos(), hasSize(1));
        ru.yandex.market.checkout.checkouter.order.promo.PromoType multicartDiscount =
                ru.yandex.market.checkout.checkouter.order.promo.PromoType.MULTICART_DISCOUNT;
        assertThat(cart2.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(multicartDiscount))),
                hasProperty("deliveryDiscount", is(expectedCart2Discount)))));
        assertThat(cart2Delivery.getPrice(), equalTo(expectedCart2DeliveryPrice));
        assertThat(cart2Delivery.getPrices().getBuyerPriceBeforeDiscount(), equalTo(BigDecimal.valueOf(49)));
        assertThat(cart2Delivery.getPrices().getBuyerDiscount(), equalTo(expectedCart2Discount));
        assertThat(cart2Delivery.getPrices().getBuyerSubsidy(), equalTo(expectedCart2Discount));
        assertThat(cart2Delivery.getPrices().getSubsidy(), equalTo(expectedCart2Discount));
        assertThat(cart2Delivery.getPromos(), hasSize(1));
        assertThat(cart2Delivery.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(multicartDiscount))),
                hasProperty("buyerDiscount", is(expectedCart2Discount)),
                hasProperty("buyerSubsidy", is(expectedCart2Discount)),
                hasProperty("subsidy", is(expectedCart2Discount)))));

        BigDecimal expectedCart3DeliveryPrice = BigDecimal.valueOf(24);
        BigDecimal expectedCart3Discount = BigDecimal.valueOf(75);
        BigDecimal expectedCart3TotalPrice = cart3.getItemsTotal().add(expectedCart3DeliveryPrice);
        assertThat(cart3.getTotal(), equalTo(expectedCart3TotalPrice));
        assertThat(cart3.getBuyerTotal(), equalTo(expectedCart3TotalPrice));
        assertThat(cart3.getPromoPrices().getBuyerTotalDiscount(), equalTo(expectedCart3Discount));
        assertThat(cart3.getPromoPrices().getBuyerTotalBeforeDiscount(),
                equalTo(cart3.getItemsTotal().add(BigDecimal.valueOf(99))));
        assertThat(cart3.getPromos(), hasSize(2));
        ru.yandex.market.checkout.checkouter.order.promo.PromoType supplierMulticartDiscount =
                ru.yandex.market.checkout.checkouter.order.promo.PromoType.SUPPLIER_MULTICART_DISCOUNT;
        assertThat(cart3.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(multicartDiscount))),
                hasProperty("deliveryDiscount", is(BigDecimal.valueOf(25))))));
        assertThat(cart3.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(supplierMulticartDiscount))),
                hasProperty("deliveryDiscount", is(BigDecimal.valueOf(50))))));
        assertThat(cart3Delivery.getPrice(), equalTo(expectedCart3DeliveryPrice));
        assertThat(cart3Delivery.getPrices().getBuyerPriceBeforeDiscount(), equalTo(BigDecimal.valueOf(99)));
        assertThat(cart3Delivery.getPrices().getBuyerDiscount(), equalTo(expectedCart3Discount));
        assertThat(cart3Delivery.getPrices().getBuyerSubsidy(), equalTo(expectedCart3Discount));
        assertThat(cart3Delivery.getPrices().getSubsidy(), equalTo(expectedCart3Discount));
        assertThat(cart3Delivery.getPromos(), hasSize(2));
        assertThat(cart3Delivery.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(multicartDiscount))),
                hasProperty("buyerDiscount", is(BigDecimal.valueOf(25))),
                hasProperty("buyerSubsidy", is(BigDecimal.valueOf(25))),
                hasProperty("subsidy", is(BigDecimal.valueOf(25))))));
        assertThat(cart3Delivery.getPromos(), hasItem(allOf(
                hasProperty("promoDefinition",
                        hasProperty("type", is(supplierMulticartDiscount))),
                hasProperty("buyerDiscount", is(BigDecimal.valueOf(50))),
                hasProperty("buyerSubsidy", is(BigDecimal.valueOf(50))),
                hasProperty("subsidy", is(BigDecimal.valueOf(50))))));
    }

    @Test
    public void shouldSendSpreadCountSpecificationIfExist() {
        checkouterProperties.setEnableSpreadDiscountPromoSupport(true);
        final String promoKey = "j728vjnfPYaBILuXk_4zFQ";
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));

        final OfferPromo offerPromo = new OfferPromo();
        offerPromo.setPromoMd5(promoKey);
        offerPromo.setPromoType("spread-discount-count");
        offerPromo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey(promoKey)
                        .promoType("spread-discount-count")
                        .bounds(Set.of(
                                PromoBound.builder()
                                        .countBound(2)
                                        .countPercentDiscount(BigDecimal.valueOf(10))
                                        .countAbsoluteDiscount(BigDecimal.valueOf(20))
                                        .build(),
                                PromoBound.builder()
                                        .countBound(5)
                                        .countPercentDiscount(BigDecimal.valueOf(20))
                                        .countAbsoluteDiscount(BigDecimal.valueOf(40))
                                        .build()
                        ))
                        .build()
        );

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(foundOfferBuilder -> foundOfferBuilder
                        .promos(List.of(offerPromo))
                        .build())
                .collect(Collectors.toList()));

        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);

        // проверяем что отправили в лоялти
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        final List<OrderWithBundlesRequest> orders = discountRequest.getOrders();
        final List<BundledOrderItemRequest> items = orders.get(0).getItems();
        assertThat(items, hasItems(
                allOf(
                        hasProperty("spreadDiscountSpecification", allOf(
                                hasProperty("spreadDiscountCount", allOf(
                                        hasProperty("promoKey", equalTo(promoKey)),
                                        hasProperty("countBounds", hasItems(
                                                allOf(
                                                        hasProperty("bound",
                                                                CoreMatchers.equalTo(2)),
                                                        hasProperty("percentDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(10))),
                                                        hasProperty("absoluteDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(20)))
                                                ),
                                                allOf(
                                                        hasProperty("bound",
                                                                CoreMatchers.equalTo(5)),
                                                        hasProperty("percentDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(20))),
                                                        hasProperty("absoluteDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(40)))
                                                )
                                        ))
                                ))
                        ))
                )
        ));
    }

    @Test
    public void shouldSendSpreadReceiptSpecificationIfExist() {
        checkouterProperties.setEnableSpreadDiscountPromoSupport(true);
        final String promoKey = "j728vjnfPYaBILuXk_4zFQ";
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));

        final OfferPromo offerPromo = new OfferPromo();
        offerPromo.setPromoMd5(promoKey);
        offerPromo.setPromoType("spread-discount-receipt");
        offerPromo.setPromoDetails(
                PromoDetails.builder()
                        .promoKey(promoKey)
                        .promoType("spread-discount-receipt")
                        .bounds(Set.of(
                                PromoBound.builder()
                                        .receiptBound(BigDecimal.valueOf(1000))
                                        .receiptPercentDiscount(BigDecimal.valueOf(10))
                                        .receiptAbsoluteDiscount(BigDecimal.valueOf(20))
                                        .build(),
                                PromoBound.builder()
                                        .receiptBound(BigDecimal.valueOf(2000))
                                        .receiptPercentDiscount(BigDecimal.valueOf(20))
                                        .receiptAbsoluteDiscount(BigDecimal.valueOf(40))
                                        .build()
                        ))
                        .build()
        );

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(foundOfferBuilder -> foundOfferBuilder
                        .promos(List.of(offerPromo))
                        .build())
                .collect(Collectors.toList()));

        parameters.setMockLoyalty(true);
        orderCreateHelper.cart(parameters);

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));

        ServeEvent calcEvent = serveEvents.get(0);
        MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                calcEvent.getRequest().getBodyAsString(),
                MultiCartWithBundlesDiscountRequest.class
        );

        final List<OrderWithBundlesRequest> orders = discountRequest.getOrders();
        final List<BundledOrderItemRequest> items = orders.get(0).getItems();
        assertThat(items, hasItems(
                allOf(
                        hasProperty("spreadDiscountSpecification", allOf(
                                hasProperty("spreadDiscountReceipt", allOf(
                                        hasProperty("promoKey", equalTo(promoKey)),
                                        hasProperty("receiptBounds", hasItems(
                                                allOf(
                                                        hasProperty("bound",
                                                                CoreMatchers.equalTo(BigDecimal.valueOf(1000))),
                                                        hasProperty("percentDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(10))),
                                                        hasProperty("absoluteDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(20)))
                                                ),
                                                allOf(
                                                        hasProperty("bound",
                                                                CoreMatchers.equalTo(BigDecimal.valueOf(2000))),
                                                        hasProperty("percentDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(20))),
                                                        hasProperty("absoluteDiscount",
                                                                comparesEqualTo(BigDecimal.valueOf(40)))
                                                )
                                        ))
                                ))
                        ))
                )
        ));
    }

    @Test
    public void personalInformationSendsToLoyaltyTest() {
        final Buyer buyer = BuyerProvider.getDefaultBuyer(534534534825L);

        final Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getBuyer().getEmail(), is(notNullValue()));

        List<ServeEvent> calcs = loyaltyConfigurer.servedEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .getUrl().equals("/discount/calc/v3"))
                .collect(Collectors.toList());

        assertThat(calcs.size(), Matchers.is(not(0)));

        calcs.forEach(it -> {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    it.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class);

            final OperationContextDto operationContext = discountRequest.getOperationContext();

            assertThat(operationContext, allOf(
                    hasProperty("personalEmailId", equalTo(BuyerProvider.PERSONAL_EMAIL_ID)),
                    hasProperty("personalFullNameId", equalTo(BuyerProvider.PERSONAL_FULL_NAME_ID)),
                    hasProperty("personalPhoneId", equalTo(BuyerProvider.PERSONAL_PHONE_ID))));
        });
    }

    @Test
    public void b2bFlagSendsToLoyaltyTest() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();

        orderCreateHelper.createOrder(parameters);

        List<ServeEvent> calcs = loyaltyConfigurer.servedEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .getUrl().equals("/discount/calc/v3"))
                .collect(Collectors.toList());

        assertThat(calcs.size(), Matchers.is(not(0)));

        calcs.forEach(it -> {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    it.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class);

            assertTrue(discountRequest.getOperationContext().getIsB2B());
        });
    }

    @Test
    public void businessBalanceIdSendsToLoyaltyTest() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();

        orderCreateHelper.createOrder(parameters);

        List<ServeEvent> calcs = loyaltyConfigurer.servedEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .getUrl().equals("/discount/calc/v3"))
                .collect(Collectors.toList());

        assertThat(calcs.size(), Matchers.is(not(0)));

        calcs.forEach(it -> {
            MultiCartWithBundlesDiscountRequest discountRequest = GSON.fromJson(
                    it.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class);

            assertEquals(
                    B2bCustomersTestProvider.BUSINESS_BALANCE_ID,
                    discountRequest.getOperationContext().getBusinessBalanceId());
        });
    }

    private Parameters createMultiOrderParameters() {
        Parameters parameters = createOrderParameters(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "offer 1");
        Parameters orderParameters2 = createOrderParameters(
                BigDecimal.valueOf(49),
                BigDecimal.ZERO,
                BigDecimal.valueOf(49),
                "offer 2");
        parameters.addOrder(orderParameters2);
        Parameters orderParameters3 = createOrderParameters(
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(49),
                "offer 3");
        parameters.addOrder(orderParameters3);
        parameters.setCheckCartErrors(false);
        parameters.setMockLoyalty(false);

        DiscountResponseBuilder responseBuilder = DiscountResponseBuilder.create();
        responseBuilder.withOrder(createDeliveryPromoResponse(parameters.getOrders().get(0), BigDecimal.ZERO));
        responseBuilder.withOrder(createDeliveryPromoResponse(parameters.getOrders().get(1), BigDecimal.valueOf(24)));
        responseBuilder.withOrder(createDeliveryPromoResponse(parameters.getOrders().get(2), BigDecimal.valueOf(25)));

        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer ->
                        loyaltyConfigurer.mockResponse(
                                responseBuilder,
                                HttpStatus.OK.value()
                        )
        );
        return parameters;
    }

    private OrderResponseBuilder createDeliveryPromoResponse(Order order, BigDecimal discount) {
        List<DeliveryPromoResponse> promoResponses = new ArrayList<>();
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            promoResponses.add(new DeliveryPromoResponse(
                    discount,
                    PromoType.MULTICART_DISCOUNT,
                    null, null, null, null));
        }
        DeliveryResponse deliveryResponse = new DeliveryResponse("0", promoResponses);
        DeliveryResponse deliveryResponse2 = new DeliveryResponse("1", promoResponses);
        return OrderResponseBuilder.create()
                .withCartId(order.getLabel())
                .withDelivery(deliveryResponse)
                .withDelivery(deliveryResponse2);
    }

    private Parameters createOrderParameters(
            BigDecimal supplierPrice,
            BigDecimal supplierDiscount,
            BigDecimal price,
            String offer) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().setItems(Collections.singletonList(OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer(offer)
                .marketSku(null)
                .build()));
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(parameters.getReportParameters()
                .getActualDelivery()
                .getResults());
        actualDeliveryResult.getDelivery()
                .forEach(deliveryResult -> {
                    deliveryResult.setSupplierPrice(supplierPrice);
                    deliveryResult.setSupplierDiscount(supplierDiscount);
                    deliveryResult.setPrice(price);
                });
        actualDeliveryResult.getPickup()
                .forEach(deliveryResult -> {
                    deliveryResult.setSupplierPrice(supplierPrice);
                    deliveryResult.setSupplierDiscount(supplierDiscount);
                    deliveryResult.setPrice(price);
                });
        actualDeliveryResult.getPost()
                .forEach(deliveryResult -> {
                    deliveryResult.setSupplierPrice(supplierPrice);
                    deliveryResult.setSupplierDiscount(supplierDiscount);
                    deliveryResult.setPrice(price);
                });
        return parameters;
    }

    private DiscountResponseBuilder buildDiscountResponse(
            Order order,
            OrderItem item,
            List<UserCoinResponse> allCoins,
            List<Long> unusedCoins,
            List<ru.yandex.market.loyalty.api.model.coin.CoinError> errors
    ) {
        List<IdObject> unusedCoinIds = unusedCoins.stream().map(IdObject::new).collect(Collectors.toList());
        CouponError couponError = new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.BUDGET_EXCEEDED.name(),
                "кончились деньги", RANDOM_STRING));

        return DiscountResponseBuilder.create()
                .withOrder(OrderResponseBuilder.create()
                        .withItem(OrderItemResponseBuilder.create()
                                .offer(item.getFeedId(), item.getOfferId())
                                .price(item.getPrice())
                                .quantity(BigDecimal.valueOf(item.getCount()))
                                .promo(new ItemPromoResponse(
                                        BigDecimal.TEN,
                                        PromoType.SMART_SHOPPING,
                                        null,
                                        "promoKey",
                                        null,
                                        null,
                                        null,
                                        new IdObject(0L),
                                        null,
                                        null,
                                        null,
                                        null
                                )))
                        .withCartId(order.getLabel() == null ?
                                LocalCartLabelerService.createLabelForCounter(new AtomicLong()) : order.getLabel()
                        ))
                .withCoins(allCoins)
                .withUnusedCoins(unusedCoinIds)
                .withCoinErrors(errors)
                .withCouponError(couponError);
    }

    private DiscountResponseBuilder buildDiscountResponseWithRegionMismatchError(Parameters parameters) {
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        return DiscountResponseBuilder.create()
                .withOrder(OrderResponseBuilder.create()
                        .withItem(OrderItemResponseBuilder.create()
                                .offer(item.getFeedId(), item.getOfferId())
                                .price(item.getPrice())
                                .quantity(BigDecimal.valueOf(item.getCount())))
                        .withCartId(parameters.getOrder().getLabel() == null ?
                                LocalCartLabelerService.createLabelForCounter(new AtomicLong()) :
                                parameters.getOrder().getLabel()))
                .withCoinErrors(List.of(
                        buildCoinError(
                                parameters.getBuiltMultiCart().getCoinIdsToUse().get(0),
                                MarketLoyaltyErrorCode.REGION_MISMATCH_ERROR,
                                ""
                        ))
                );
    }

    private void enableUnifiedTariffs(Parameters parameters) {
        checkouterProperties.setEnableUnifiedTariffs(true);
        String experiment = MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE;
        CheckoutContextHolder.setExperiments(getExperiments().with(Set.of(experiment)));
        parameters.setExperiments(getExperiments().with(Set.of(experiment)));
    }

}
