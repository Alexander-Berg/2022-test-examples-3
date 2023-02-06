package ru.yandex.market.deliverycalculator.storage.repository.daas;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasDeliveryServiceGroupEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryService;

import static org.junit.Assert.assertEquals;

/**
 * Тест для {@link DaasServiceGroupRepository}.
 */
class DaasServiceGroupRepositoryTest extends FunctionalTest {

    @Autowired
    private DaasServiceGroupRepository tested;

    /**
     * Тест для {@link DaasServiceGroupRepository#save(Object).
     */
    @DbUnitDataSet(after = "database/daasDeliveryServiceGroups.after.csv")
    @Test
    void testStore() {
        DaasDeliveryServiceGroupEntity serviceGroup = new DaasDeliveryServiceGroupEntity();
        serviceGroup.setId(1L);
        serviceGroup.setServices(createTestServices());

        tested.save(serviceGroup);
    }

    /**
     * Тест для {@link DaasServiceGroupRepository#findById(Object)}.
     */
    @DbUnitDataSet(before = "database/daasDeliveryServiceGroups.after.csv")
    @Test
    void testRead() {
        DaasDeliveryServiceGroupEntity serviceGroup = new DaasDeliveryServiceGroupEntity();
        serviceGroup.setId(1L);
        serviceGroup.setServices(createTestServices());

        assertEquals(serviceGroup, tested.findById(1L).orElseThrow());
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


