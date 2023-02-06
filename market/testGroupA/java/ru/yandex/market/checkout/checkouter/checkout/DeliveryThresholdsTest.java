package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_SHOW_DELIVERY_THRESHOLD_FOR_EXPRESS_KEY;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.ALREADY_ZERO_PRICE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.LARGE_SIZED_CART;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.YA_PLUS_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.NO_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;

/**
 * @author gelvy
 * Created on: 10.06.2021
 **/
public class DeliveryThresholdsTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    private void mockStockStorage(String offerId, int count, long shopId, int warehouseId) {
        List<SSItem> ssItems = Collections.singletonList(SSItem.of(offerId, shopId, warehouseId));
        List<SSItemAmount> ssItemAmount = ssItems.stream().map(ssItem -> SSItemAmount.of(ssItem, count))
                .collect(Collectors.toList());
        stockStorageConfigurer.resetMappings();
        stockStorageConfigurer.mockGetAvailableCount(ssItems, false, ssItemAmount);
        stockStorageConfigurer.mockOkForFreeze(ssItems);
    }

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_USE_SS_BEFORE_ACTUAL_DELIVERY, false);
        stockStorageConfigurer.resetMappings();
    }


    @Test
    public void shouldSetNoFreeDeliveryForLargeSizedCart() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = BigDecimal.valueOf(699);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);
        actualDelivery.setBetterWithPlus(true);
        actualDelivery.getResults().get(0).setLargeSize(true);

        params.getLoyaltyParameters().setFreeDeliveryReason(THRESHOLD_FREE_DELIVERY);
        params.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        params.getLoyaltyParameters().setDeliveryDiscountMap(new HashMap<>());
        params.getLoyaltyParameters().getDeliveryDiscountMap().put(THRESHOLD_FREE_DELIVERY,
                new PriceLeftForFreeDeliveryResponseV3(BigDecimal.ZERO, BigDecimal.ZERO, WILL_BE_FREE_WITH_MORE_ITEMS));

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
        assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(multiCart.getDeliveryDiscountMap(), hasKey(LARGE_SIZED_CART));

        PriceLeftForFreeDeliveryResponseV3 value =
                multiCart.getDeliveryDiscountMap().get(LARGE_SIZED_CART);
        assertThat(value, notNullValue());
        assertThat(value.getPriceLeftForFreeDelivery(), nullValue());
        assertThat(value.getThreshold(), nullValue());
        assertThat(value.getStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    public void shouldNotChangeLoyaltyThresholdsWhenAlreadyFree() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = BigDecimal.valueOf(699);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);
        actualDelivery.setBetterWithPlus(true);

        params.getLoyaltyParameters().setFreeDeliveryReason(COIN_FREE_DELIVERY);
        params.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);
        params.getLoyaltyParameters().setDeliveryDiscountMap(Map.of(COIN_FREE_DELIVERY,
                new PriceLeftForFreeDeliveryResponseV3(BigDecimal.ZERO, BigDecimal.ZERO, ALREADY_FREE)));

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
        assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(COIN_FREE_DELIVERY));
        assertThat(multiCart.getDeliveryDiscountMap(), not(hasKey(THRESHOLD_FREE_DELIVERY)));
        assertThat(multiCart.getDeliveryDiscountMap(), not(hasKey(YA_PLUS_FREE_DELIVERY)));

        PriceLeftForFreeDeliveryResponseV3 value =
                multiCart.getDeliveryDiscountMap().get(COIN_FREE_DELIVERY);
        assertThat(value, notNullValue());
        assertThat(value.getPriceLeftForFreeDelivery(), equalTo(BigDecimal.ZERO));
        assertThat(value.getThreshold(), equalTo(BigDecimal.ZERO));
        assertThat(value.getStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    public void shouldReplaceThresholdsForRegularUserBelowThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = BigDecimal.valueOf(699);
        var cheaperDeliveryRemaining = cheaperDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);
        actualDelivery.setBetterWithPlus(true);

        MultiCart multiCart = orderCreateHelper.cart(params);

        if (Boolean.TRUE.equals(checkouterProperties.getEnabledPlusPerkForThresholdCalculation())) {
            assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
            assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
            assertThat(yaPlusValue, nullValue());

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, nullValue());
        } else {
            assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(cheaperDeliveryThreshold));
            assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
            assertThat(yaPlusValue, notNullValue());
            assertThat(yaPlusValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
            assertThat(yaPlusValue.getThreshold(), equalTo(cheaperDeliveryThreshold));
            assertThat(yaPlusValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, notNullValue());
            assertThat(thresholdValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
            assertThat(thresholdValue.getThreshold(), equalTo(cheaperDeliveryThreshold));
            assertThat(thresholdValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        }
    }

    @Test
    public void shouldReplaceThresholdsForRegularUserAboveThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = params.getOrder().getTotal();
        var cheaperDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);
        actualDelivery.setBetterWithPlus(true);

        MultiCart multiCart = orderCreateHelper.cart(params);

        if (Boolean.TRUE.equals(checkouterProperties.getEnabledPlusPerkForThresholdCalculation())) {
            assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
            assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, nullValue());
        } else {
            assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(cheaperDeliveryThreshold));
            assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
            assertThat(multiCart.getFreeDeliveryStatus(),
                    equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, notNullValue());
            assertThat(thresholdValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
            assertThat(thresholdValue.getThreshold(), equalTo(cheaperDeliveryThreshold));
            assertThat(thresholdValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION));
        }
    }

    @Test
    public void shouldReplaceThresholdsForRegularUserBelowThresholdWithSSCount() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_USE_SS_BEFORE_ACTUAL_DELIVERY, true);
        var params = WhiteParametersProvider.defaultWhiteParameters();
        OrderItem item = params.getOrder().getItems().iterator().next();
        mockStockStorage(item.getOfferId(), 999, item.getSupplierId(), item.getWarehouseId());
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = BigDecimal.valueOf(699);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        MultiCart multiCart = orderCreateHelper.cart(params);

        if (Boolean.TRUE.equals(checkouterProperties.getEnabledPlusPerkForThresholdCalculation())) {
            assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
            assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);

            assertThat(yaPlusValue, nullValue());
        } else {
            assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
            assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
            assertThat(yaPlusValue, notNullValue());
            assertThat(yaPlusValue.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
            assertThat(yaPlusValue.getThreshold(), equalTo(freeDeliveryThreshold));
            assertThat(yaPlusValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        }
    }


    @Test
    public void shouldReplaceThresholdsForPlusUserBelowThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = BigDecimal.valueOf(699);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        MultiCart multiCart = orderCreateHelper.cart(params);

        if (Boolean.TRUE.equals(checkouterProperties.getEnabledPlusPerkForThresholdCalculation())) {
            assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
            assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);

            assertThat(yaPlusValue, nullValue());
        } else {
            assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
            assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY));

            PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
            assertThat(yaPlusValue, notNullValue());
            assertThat(yaPlusValue.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
            assertThat(yaPlusValue.getThreshold(), equalTo(freeDeliveryThreshold));
            assertThat(yaPlusValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        }
    }

    @Test
    public void shouldReplaceThresholdsForPlusUserAboveThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setYandexPlus(true);
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = params.getOrder().getTotal();
        var freeDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY));

        PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
        assertThat(yaPlusValue, notNullValue());
        assertThat(yaPlusValue.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(yaPlusValue.getThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(yaPlusValue.getStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
    }

    @Test
    public void shouldReplaceThresholdsForExpressBelowThreshold() {
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = BigDecimal.valueOf(699);
        var cheaperDeliveryRemaining = cheaperDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(cheaperDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));

        PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
        assertThat(thresholdValue, notNullValue());
        assertThat(thresholdValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
        assertThat(thresholdValue.getThreshold(), equalTo(cheaperDeliveryThreshold));
        assertThat(thresholdValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void shouldReplaceThresholdsForExpressAboveThreshold() {
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = params.getOrder().getTotal();
        var cheaperDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);

        params.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        params.getLoyaltyParameters().setFreeDeliveryReason(YA_PLUS_FREE_DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(cheaperDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));

        PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
        assertThat(thresholdValue, notNullValue());
        assertThat(thresholdValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining));
        assertThat(thresholdValue.getThreshold(), equalTo(cheaperDeliveryThreshold));
        assertThat(thresholdValue.getStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
    }

    @Test
    public void shouldIgnoreExpressWhenThereAreOtherCarts() {
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = BigDecimal.valueOf(699);
        var cheaperDeliveryRemaining = cheaperDeliveryThreshold.subtract(params.getOrder().getTotal());

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);

        var params2 = BlueParametersProvider.defaultBlueOrderParameters();

        var cheaperDeliveryThreshold2 = params2.getOrder().getTotal();
        var cheaperDeliveryRemaining2 = BigDecimal.ZERO;

        var actualDelivery2 = params2.getReportParameters().getActualDelivery();
        actualDelivery2.setCheaperDeliveryThreshold(cheaperDeliveryThreshold2);
        actualDelivery2.setCheaperDeliveryRemainder(cheaperDeliveryRemaining2);
        actualDelivery2.setBetterWithPlus(true);

        params.addOrder(params2);

        MultiCart multiCart = orderCreateHelper.cart(params);

        if (Boolean.TRUE.equals(checkouterProperties.getEnabledPlusPerkForThresholdCalculation())) {
            assertThat(multiCart.getFreeDeliveryThreshold(), nullValue());
            assertThat(multiCart.getPriceLeftForFreeDelivery(), nullValue());
            assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, nullValue());
        } else {
            assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(cheaperDeliveryThreshold2));
            assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining2));
            assertThat(multiCart.getFreeDeliveryStatus(),
                    equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION));
            assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));

            PriceLeftForFreeDeliveryResponseV3 thresholdValue =
                    multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
            assertThat(thresholdValue, notNullValue());
            assertThat(thresholdValue.getPriceLeftForFreeDelivery(), equalTo(cheaperDeliveryRemaining2));
            assertThat(thresholdValue.getThreshold(), equalTo(cheaperDeliveryThreshold2));
            assertThat(thresholdValue.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION));
        }
    }

    @Test
    public void shouldRemoveThresholdWhenOnlyMultipleExpressCarts() {
        var cheaperDeliveryThreshold = BigDecimal.valueOf(699);

        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");
        params.getReportParameters().setIsExpress(true);
        ActualDelivery actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryThreshold.subtract(params.getOrder().getTotal()));
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);

        var params2 = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        params2.getReportParameters().setIsExpress(true);
        ActualDelivery actualDelivery2 = params.getReportParameters().getActualDelivery();
        actualDelivery2.setCheaperDeliveryRemainder(cheaperDeliveryThreshold.subtract(params2.getOrder().getTotal()));
        actualDelivery2.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);

        params.addOrder(params2);

        params.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        params.getLoyaltyParameters().setFreeDeliveryReason(THRESHOLD_FREE_DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(0));
    }

    @Test
    public void shouldRemoveThresholdsForEda() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");
        params.getReportParameters().setIsEda(true);

        params.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        params.getLoyaltyParameters().setFreeDeliveryReason(THRESHOLD_FREE_DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(0));
    }

    @Test
    public void shouldHideThresholdsForBlueExpressWithoutExperiment() {
        var cheaperDeliveryThreshold = BigDecimal.valueOf(699);
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "0");
        params.getReportParameters().setIsExpress(true);
        ActualDelivery actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryThreshold.subtract(params.getOrder().getTotal()));
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertEquals(multiCart.getFreeDeliveryReason(), FreeDeliveryReason.EXCLUDED_ITEMS);
        assertEquals(multiCart.getFreeDeliveryStatus(), FreeDeliveryStatus.NO_FREE_DELIVERY);
    }

    @Test
    public void shouldHideThresholdsWhenNoDataFromReport() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");
        ActualDelivery actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryRemainder(null);
        actualDelivery.setFreeDeliveryThreshold(null);
        actualDelivery.setCheaperDeliveryRemainder(null);
        actualDelivery.setCheaperDeliveryThreshold(null);

        params.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        params.getLoyaltyParameters().setFreeDeliveryReason(THRESHOLD_FREE_DELIVERY);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertEquals(multiCart.getFreeDeliveryReason(), FreeDeliveryReason.EXCLUDED_ITEMS);
        assertEquals(multiCart.getFreeDeliveryStatus(), FreeDeliveryStatus.NO_FREE_DELIVERY);
    }

    @Test
    public void shouldReplaceAlreadyZeroPriceForPlusUserAboveThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setYandexPlus(true);
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = params.getOrder().getTotal();
        var freeDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        params.getLoyaltyParameters().setFreeDeliveryReason(ALREADY_ZERO_PRICE);
        params.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY));

        PriceLeftForFreeDeliveryResponseV3 yaPlusValue =
                multiCart.getDeliveryDiscountMap().get(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY);
        assertThat(yaPlusValue, notNullValue());
        assertThat(yaPlusValue.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(yaPlusValue.getThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(yaPlusValue.getStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
    }

    @Test
    public void shouldNotReplaceAlreadyZeroPriceForRegularUserAboveThreshold() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var cheaperDeliveryThreshold = params.getOrder().getTotal();
        var cheaperDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(cheaperDeliveryThreshold);
        actualDelivery.setCheaperDeliveryRemainder(cheaperDeliveryRemaining);
        actualDelivery.setBetterWithPlus(true);

        params.getLoyaltyParameters().setFreeDeliveryReason(ALREADY_ZERO_PRICE);
        params.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(ALREADY_ZERO_PRICE));
    }

    @Test
    public void shouldSendNoFreeDeliveryForRegularUserWithFreeDelivery() {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setYandexPlus(false);
        enableUnifiedTariffs(params);
        addMarketCombineNewSortExperiment(params, "1");

        var freeDeliveryThreshold = params.getOrder().getTotal();
        var freeDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);
        actualDelivery.setBetterWithPlus(false);

        params.getLoyaltyParameters().setFreeDeliveryReason(ALREADY_ZERO_PRICE);
        params.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);

        MultiCart multiCart = orderCreateHelper.cart(params);

        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));
    }

    private void addMarketCombineNewSortExperiment(Parameters parameters, String marketCombineNewSortValue) {
        parameters.addExperiment(MARKET_SHOW_DELIVERY_THRESHOLD_FOR_EXPRESS_KEY, marketCombineNewSortValue);
    }

    private void enableUnifiedTariffs(Parameters parameters) {
        checkouterProperties.setEnableUnifiedTariffs(true);
        parameters.addExperiment(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE);
    }
}
