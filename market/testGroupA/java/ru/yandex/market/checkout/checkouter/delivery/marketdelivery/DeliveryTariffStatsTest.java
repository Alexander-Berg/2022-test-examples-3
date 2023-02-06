package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.kgt.DeliveryTariffStats;
import ru.yandex.market.checkout.checkouter.delivery.kgt.KgtTariffType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.common.report.model.KgtInfo;
import ru.yandex.market.common.report.model.TariffInfo;
import ru.yandex.market.common.report.model.TariffStats;
import ru.yandex.market.common.report.model.tariffFactors.KgtFactor;
import ru.yandex.market.common.report.model.tariffFactors.TariffType;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.test.providers.DeliveryRouteProvider.fromActualDelivery;

public class DeliveryTariffStatsTest extends AbstractWebTestBase {

    @Test
    public void orderPropertyWithTariffStatsInCartTest() {
        TariffStats tariffStats = createTariffStats();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().getActualDelivery().getResults().get(0).setTariffStats(tariffStats);

        MultiCart cart = orderCreateHelper.cart(parameters);
        var deliveryTariffStats =
                cart.getCarts().get(0).getProperty(OrderPropertyType.DELIVERY_TARIFF_STATS);

        checkKgtInfoAssertions(deliveryTariffStats);
    }

    @Test
    public void orderPropertyWithTariffStatsInCheckoutTest() throws Exception {
        TariffStats tariffStats = createTariffStats();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().getActualDelivery().getResults().get(0).setTariffStats(tariffStats);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts().get(0).getProperty(OrderPropertyType.DELIVERY_TARIFF_STATS));

        // зануляем, чтобы исключить сайд эффекта при чекауте
        cart.getCarts().get(0).setProperty(OrderPropertyType.DELIVERY_TARIFF_STATS, null);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);

        checkDeliveryTariffStatsAssertions(checkout);
    }

    @Test
    public void orderPropertyWithTariffStatsFromDeliveryRouteInCheckoutTest() throws Exception {
        // Добиваемся похода через комбинатор и необходимость брать tarifStats из delivery_route
        checkouterFeatureWriter.writeValue(
                ComplexFeatureType.COMBINATOR_FLOW,
                new SwitchWithWhitelist<>(true, singleton(Constants.COMBINATOR_EXPERIMENT)));

        var tariffStats = createTariffStats();
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        var actualDelivery = parameters.getReportParameters().getActualDelivery();
        var deliveryRoute = fromActualDelivery(actualDelivery, DeliveryType.DELIVERY);

        parameters.getReportParameters().setDeliveryRoute(deliveryRoute);
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);

        parameters.getReportParameters().getActualDelivery().getResults()
                .get(0).setTariffStats(tariffStats);
        parameters.getReportParameters().getDeliveryRoute().getResults()
                .get(0).setTariffStats(tariffStats);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts().get(0).getProperty(OrderPropertyType.DELIVERY_TARIFF_STATS));

        // зануляем, чтобы исключить сайд эффекта при чекауте
        cart.getCarts().get(0).setProperty(OrderPropertyType.DELIVERY_TARIFF_STATS, null);

         MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);

        checkDeliveryTariffStatsAssertions(checkout);
    }

    private void checkDeliveryTariffStatsAssertions(MultiOrder multiOrder) {
        DeliveryTariffStats deliveryTariffStats =
                multiOrder.getOrders().get(0).getProperty(OrderPropertyType.DELIVERY_TARIFF_STATS);

        assertNotNull(deliveryTariffStats);

        assertNotNull(deliveryTariffStats.getKgtInfo());
        assertNotNull(deliveryTariffStats.getTariffInfo());

        // проверяем, что в БД информация остается
        Order order = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        assertNotNull(order.getProperty(OrderPropertyType.DELIVERY_TARIFF_STATS));
    }

    @Nonnull
    private static TariffStats createTariffStats() {
        KgtInfo kgtInfo = new KgtInfo();
        TariffInfo tariffInfo = new TariffInfo();
        kgtInfo.setText("Kgt text");

        KgtFactor kgtFactor1 = new KgtFactor(TariffType.WEIGHT);
        KgtFactor.OrderValue orderValue = new KgtFactor.OrderValue();
        orderValue.setValue(BigDecimal.TEN);
        orderValue.setUnit("kg");
        kgtFactor1.setOrderValue(orderValue);

        KgtFactor kgtFactor2 = new KgtFactor(TariffType.VOLUME);
        KgtFactor.OrderValue orderValue2 = new KgtFactor.OrderValue();
        orderValue2.setValue(BigDecimal.ONE);
        orderValue2.setUnit("m3");
        kgtFactor2.setOrderValue(orderValue2);

        KgtFactor kgtFactor3 = new KgtFactor(TariffType.MAX_ITEM_DIM);
        KgtFactor.OrderValue orderValue3 = new KgtFactor.OrderValue();
        orderValue3.setValue(BigDecimal.ZERO);
        orderValue3.setUnit("cm");
        kgtFactor3.setOrderValue(orderValue3);

        kgtInfo.setFactors(Arrays.asList(kgtFactor1, kgtFactor2));
        tariffInfo.setFactors(Collections.singletonList(kgtFactor3));

        TariffStats tariffStats = new TariffStats();
        tariffStats.setKgtInfo(kgtInfo);
        tariffStats.setTariffInfo(tariffInfo);
        return tariffStats;
    }

    private void checkKgtInfoAssertions(DeliveryTariffStats deliveryTariffStats) {
        assertNotNull(deliveryTariffStats);

        // -- проверяем kgtInfo
        var resKgtInfo = deliveryTariffStats.getKgtInfo();
        assertEquals(2, resKgtInfo.getFactors().size());

        assertEquals(KgtTariffType.WEIGHT, resKgtInfo.getFactors().get(0).getType());
        assertEquals(KgtTariffType.VOLUME, resKgtInfo.getFactors().get(1).getType());

        assertNotNull(resKgtInfo.getFactors().get(0).getOrderValue());
        assertNotNull(resKgtInfo.getFactors().get(1).getOrderValue());

        assertEquals(0, BigDecimal.TEN.compareTo(resKgtInfo.getFactors().get(0).getOrderValue().getValue()));
        assertEquals(0, BigDecimal.ONE.compareTo(resKgtInfo.getFactors().get(1).getOrderValue().getValue()));

        assertEquals("kg", resKgtInfo.getFactors().get(0).getOrderValue().getUnit());
        assertEquals("m3", resKgtInfo.getFactors().get(1).getOrderValue().getUnit());

        // -- проверяем tariffInfo
        var resTariffInfo = deliveryTariffStats.getTariffInfo();
        assertEquals(1, resTariffInfo.getFactors().size());

        assertEquals(KgtTariffType.MAX_ITEM_DIM, resTariffInfo.getFactors().get(0).getType());
    }

}
