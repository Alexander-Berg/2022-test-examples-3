package ru.yandex.market.deliverycalculator.workflow.service;

import java.util.HashSet;
import java.util.Map;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.MapUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasDeliveryServiceGroupEntity;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link DeliveryServicesService}.
 */
class DeliveryServicesServiceTest extends FunctionalTest {

    @Autowired
    private DeliveryServicesService tested;

    @DbUnitDataSet(before = "database/storeDaasDeliveryServices.before.csv",
            after = "database/storeDaasDeliveryServices.after.csv")
    @Test
    void testStoreServiceGroups() {
        DeliveryCalcProtos.DeliveryServicesGroup serviceGroup1 = DeliveryCalcProtos.DeliveryServicesGroup.newBuilder()
                .setDeliveryServiceGroupId(1L)
                .addDeliveryServices(DeliveryCalcProtos.DeliveryService.newBuilder()
                        .setMinValue(1L)
                        .setMaxValue(2L)
                        .setEnabledByDefault(true)
                        .setPriceCalculationRule(DeliveryCalcProtos.DeliveryServicePriceCalculationRule.PERCENT_DELIVERY)
                        .setPriceCalculationParameter(25.2)
                        .setCode("Very Important")
                        .build())
                .addDeliveryServices(DeliveryCalcProtos.DeliveryService.newBuilder()
                        .setMinValue(2L)
                        .setMaxValue(3L)
                        .setEnabledByDefault(false)
                        .setPriceCalculationRule(DeliveryCalcProtos.DeliveryServicePriceCalculationRule.FIX)
                        .setPriceCalculationParameter(122334455)
                        .setCode("Very Important 2")
                        .build())
                .build();

        DeliveryCalcProtos.DeliveryServicesGroup serviceGroup2 = DeliveryCalcProtos.DeliveryServicesGroup.newBuilder()
                .setDeliveryServiceGroupId(2L)
                .addDeliveryServices(DeliveryCalcProtos.DeliveryService.newBuilder()
                        .setMinValue(4L)
                        .setMaxValue(5L)
                        .setEnabledByDefault(true)
                        .setPriceCalculationRule(DeliveryCalcProtos.DeliveryServicePriceCalculationRule.PERCENT_COST)
                        .setPriceCalculationParameter(2.2)
                        .setCode("Very Important 3")
                        .build())
                .build();

        DeliveryCalcProtos.DeliveryServicesGroup serviceGroup3 = DeliveryCalcProtos.DeliveryServicesGroup.newBuilder()
                .setDeliveryServiceGroupId(3L)
                .addDeliveryServices(DeliveryCalcProtos.DeliveryService.newBuilder()
                        .setMinValue(5L)
                        .setMaxValue(6L)
                        .setEnabledByDefault(true)
                        .setPriceCalculationRule(DeliveryCalcProtos.DeliveryServicePriceCalculationRule.PERCENT_CASH)
                        .setPriceCalculationParameter(1.2)
                        .setCode("Very Important 4")
                        .build())
                .build();

        Map<Long, DaasDeliveryServiceGroupEntity> services = tested.store(Sets.newHashSet(serviceGroup1,
                serviceGroup2, serviceGroup3));

        assertNotNull(services);
        assertEquals(3, services.size());
    }

    @DbUnitDataSet
    @Test
    void testStoreServiceGroups_storingEmptyList() {
        assertTrue(MapUtils.isEmpty(tested.store(new HashSet<>())));
        assertTrue(MapUtils.isEmpty(tested.store(null)));
    }

    @Test
    @DbUnitDataSet(before = "database/deleteDeliveryDataServices.before.csv",
            after = "database/deleteDeliveryDataServices.after.csv")
    void testDeleteUnusedServices() {
        tested.deleteUnusedServiceGroups();
    }

}
