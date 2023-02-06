package ru.yandex.market.deliverycalculator.searchengine.service.converter;

import java.util.Set;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.controller.model.DeliveryOptionDto;
import ru.yandex.market.deliverycalculator.searchengine.controller.model.DeliveryServiceDto;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryService;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DeliveryOptionData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryOptionConverterTest extends FunctionalTest {

    @Autowired
    private DeliveryOptionConverter tested;

    /**
     * Тест для {@link DeliveryOptionConverter#convertDeliveryOption(DeliveryOptionData)}.
     */
    @Test
    void testConvert() {
        DeliveryOptionData deliveryOptionData = createTestOptionData();

        DeliveryOptionDto actualResult = tested.convertDeliveryOption(deliveryOptionData);

        assertNotNull(actualResult);
        assertEquals(deliveryOptionData.getTariffId(), actualResult.getTariffId());
        assertEquals(deliveryOptionData.getDeliveryServiceId(), actualResult.getDeliveryServiceId());
        assertEquals(deliveryOptionData.getTariffType(), actualResult.getTariffType());
        assertEquals(deliveryOptionData.getPickupPoints(), actualResult.getPickupPoints());
        assertEquals(deliveryOptionData.getPickupPoints(), actualResult.getPickupPoints());
        assertEquals(deliveryOptionData.getMinDays(), actualResult.getMinDays());
        assertEquals(deliveryOptionData.getMaxDays(), actualResult.getMaxDays());
        assertEquals(deliveryOptionData.getCost(), actualResult.getCost());
        assertEquals(deliveryOptionData.getServiceCustomerPay(), actualResult.getServiceCustomerPay());
        assertServices(actualResult.getServices());
    }

    private void assertServices(Set<DeliveryServiceDto> services) {
        assertNotNull(services);
        assertEquals(4, services.size());
        assertTrue(services.contains(createExpectedInsuranceService()));
        assertTrue(services.contains(createExpectedUnknownService()));
        assertTrue(services.contains(createExpectedSortService()));
        assertTrue(services.contains(createExpectedCashService()));
    }

    private DeliveryServiceDto createExpectedUnknownService() {
        return DeliveryServiceDto.builder()
                .withCode("BLA")
                .withPriceCalculationParameter(10.0)
                .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                .withEnabledByDefault(false)
                .withMinPrice(2000L)
                .withMaxPrice(22000L)
                .withPaidByCustomer(null)
                .build();
    }

    private DeliveryServiceDto createExpectedInsuranceService() {
        return DeliveryServiceDto.builder()
                .withCode("INSURANCE")
                .withPriceCalculationParameter(20.0)
                .withPriceCalculationRule(DeliveryServicePriceSchemaType.PERCENT_CASH)
                .withEnabledByDefault(true)
                .withMinPrice(5000L)
                .withMaxPrice(10000L)
                .withPaidByCustomer(true)
                .build();
    }

    private DeliveryServiceDto createExpectedSortService() {
        return DeliveryServiceDto.builder()
                .withCode("SORT")
                .withPriceCalculationParameter(0)
                .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                .withEnabledByDefault(false)
                .withMinPrice(0L)
                .withMaxPrice(0L)
                .withPaidByCustomer(true)
                .build();
    }

    private DeliveryServiceDto createExpectedCashService() {
        return DeliveryServiceDto.builder()
                .withCode("CASH_SERVICE")
                .withPriceCalculationParameter(0)
                .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                .withEnabledByDefault(false)
                .withMinPrice(0L)
                .withMaxPrice(0L)
                .withPaidByCustomer(true)
                .build();
    }

    @NotNull
    private DeliveryOptionData createTestOptionData() {
        return DeliveryOptionData.builder()
                .withDeliveryServiceId(1L)
                .withCost(1500L)
                .withMinDays(2)
                .withMaxDays(5)
                .withTariffId(1396L)
                .withTariffType(YaDeliveryTariffType.COURIER)
                .withVolumeWeightCoefficient(25.0)
                .withPickupPoints(Sets.newHashSet(1L, 2L))
                .withServicesPayedByCustomer(Sets.newHashSet(
                        DeliveryServiceCode.SORT,
                        DeliveryServiceCode.INSURANCE,
                        DeliveryServiceCode.CASH_SERVICE))
                .withServices(Sets.newHashSet(DeliveryService.builder()
                                .withCode("INSURANCE")
                                .withPriceCalculationParameter(20.0)
                                .withPriceCalculationRule(DeliveryServicePriceSchemaType.PERCENT_CASH)
                                .withEnabledByDefault(true)
                                .withMinPrice(5000L)
                                .withMaxPrice(10000L)
                                .build(),
                        DeliveryService.builder()
                                .withCode("BLA")
                                .withPriceCalculationParameter(10.0)
                                .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                                .withEnabledByDefault(false)
                                .withMinPrice(2000L)
                                .withMaxPrice(22000L)
                                .build(),
                        null))
                .build();
    }
}
