package ru.yandex.market.deliverycalculator.storage.model.metastorage.daas;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaasDeliveryServiceGroupEntityTest {

    private DaasDeliveryServiceGroupEntity entity;

    @BeforeEach
    void setUp() {
        entity = new DaasDeliveryServiceGroupEntity();
    }

    @Test
    void it_must_get_the_same_services_as_were_set() {
        // Given
        final Set<DeliveryService> expected = createTestServices();

        // When
        entity.setServices(expected);
        final Set<DeliveryService> actual = entity.getServices();

        // Then
        assertEquals(expected, actual);
    }

    private Set<DeliveryService> createTestServices() {
        return Sets.newHashSet(DeliveryService.builder()
                        .withCode("INSURANCE")
                        .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                        .withPriceCalculationParameter(100000.0)
                        .withMinPrice(50000)
                        .withMaxPrice(100000)
                        .withEnabledByDefault(false)
                        .build(),
                DeliveryService.builder()
                        .withCode("CASH_SERVICE")
                        .withPriceCalculationRule(DeliveryServicePriceSchemaType.PERCENT_DELIVERY)
                        .withPriceCalculationParameter(3.0)
                        .withMinPrice(30000)
                        .withMaxPrice(400000)
                        .withEnabledByDefault(true)
                        .build()
        );
    }
}