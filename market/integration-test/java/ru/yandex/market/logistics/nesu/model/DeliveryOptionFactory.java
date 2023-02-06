package ru.yandex.market.logistics.nesu.model;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOptionService;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryOptionServicePriceRule;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryServiceCode;

@UtilityClass
public class DeliveryOptionFactory {

    @Nonnull
    public DeliveryOptionService.DeliveryOptionServiceBuilder deliveryServiceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.DELIVERY)
            .priceCalculationRule(DeliveryOptionServicePriceRule.FIX)
            .priceCalculationParameter(100500)
            .minPrice(100500)
            .maxPrice(100500)
            .enabledByDefault(true);
    }

    @Nonnull
    public DeliveryOptionService.DeliveryOptionServiceBuilder insuranceServiceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.INSURANCE)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_COST)
            .priceCalculationParameter(0.006)
            .minPrice(0)
            .maxPrice(3000_00)
            .enabledByDefault(true);
    }

    @Nonnull
    public DeliveryOptionService.DeliveryOptionServiceBuilder cashServiceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.CASH_SERVICE)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_CASH)
            .priceCalculationParameter(0.017)
            .minPrice(0)
            .maxPrice(Long.MAX_VALUE)
            .enabledByDefault(true);
    }

    @Nonnull
    public DeliveryOptionService.DeliveryOptionServiceBuilder returnServiceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.RETURN)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_DELIVERY)
            .priceCalculationParameter(0.75)
            .minPrice(0)
            .maxPrice(Long.MAX_VALUE)
            .enabledByDefault(false);
    }
}
