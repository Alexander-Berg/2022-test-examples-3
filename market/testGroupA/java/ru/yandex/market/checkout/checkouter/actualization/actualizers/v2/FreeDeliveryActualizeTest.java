package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class FreeDeliveryActualizeTest extends AbstractWebTestBase {

    private static final long PUID = 2_190_550_858_753_437_200L;

    @BeforeEach
    void configure() {
        checkouterProperties.setEnableUnifiedTariffs(true);
    }

    @Test
    void shouldApplyFreeDeliveryOnYaPlus() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(PUID);
        params.setYandexPlus(true);
        params.setExperiments(Experiments.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryRemainder(BigDecimal.ZERO);
        actualDelivery.setFreeDeliveryThreshold(BigDecimal.ZERO);

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart, notNullValue());
        assertThat(cart.getDeliveryDiscountMap(), not(anEmptyMap()));
        assertThat(cart.getDeliveryDiscountMap(), hasKey(FreeDeliveryReason.YA_PLUS_FREE_DELIVERY));
    }

    @Test
    void shouldApplyFreeDeliveryOnThreshold() {
        checkouterProperties.setEnabledPlusPerkForThresholdCalculation(false);

        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(PUID);
        params.setExperiments(Experiments.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setCheaperDeliveryThreshold(BigDecimal.ZERO);
        actualDelivery.setCheaperDeliveryRemainder(BigDecimal.ZERO);
        actualDelivery.setBetterWithPlus(true);

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart, notNullValue());
        assertThat(cart.getDeliveryDiscountMap(), not(anEmptyMap()));
        assertThat(cart.getDeliveryDiscountMap(), hasKey(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
    }

    @Test
    void shouldNotApplyFreeDeliveryOnLargeSize() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(PUID);
        params.setExperiments(Experiments.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.getResults().get(0).setLargeSize(true);

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart, notNullValue());
        assertThat(cart.getDeliveryDiscountMap(), not(anEmptyMap()));
        assertThat(cart.getDeliveryDiscountMap(), hasKey(FreeDeliveryReason.LARGE_SIZED_CART));
    }

    @Test
    void shouldApplyFreeDeliveryOnExpress() {
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        params.setUid(PUID);
        params.setYandexPlus(true);

        var actualDelivery = params.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryRemainder(BigDecimal.ZERO);
        actualDelivery.setFreeDeliveryThreshold(BigDecimal.ZERO);
        actualDelivery.getResults().get(0).setAvailableDeliveryMethods(List.of(
                AvailableDeliveryType.EXPRESS.getCode()
        ));

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart, notNullValue());
        assertThat(cart.getDeliveryDiscountMap(), not(anEmptyMap()));
        assertThat(cart.getDeliveryDiscountMap(), hasKey(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
    }
}
