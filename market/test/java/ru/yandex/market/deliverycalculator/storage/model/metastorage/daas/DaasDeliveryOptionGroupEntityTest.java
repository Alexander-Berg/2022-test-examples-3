package ru.yandex.market.deliverycalculator.storage.model.metastorage.daas;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryOption;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasDeliveryOptionGroupRepository;

class DaasDeliveryOptionGroupEntityTest extends FunctionalTest {

    @Autowired
    private DaasDeliveryOptionGroupRepository repository;

    @DbUnitDataSet(after = "database/saveDaasDeliveryOptionGroup2.after.csv")
    @Test
    void test() {
        repository.save(createTestEntity());
    }

    private DaasDeliveryOptionGroupEntity createTestEntity() {
        final DaasDeliveryOptionGroupEntity entity = new DaasDeliveryOptionGroupEntity();

        final DeliveryOption deliveryOption = new DeliveryOption();
        deliveryOption.setCost(1);
        deliveryOption.setMinDaysCount(2);
        deliveryOption.setMaxDaysCount(3);
        deliveryOption.setOrderBefore(4);

        final DeliveryOption deliveryOption2 = new DeliveryOption();
        deliveryOption2.setCost(2);
        deliveryOption2.setMinDaysCount(3);
        deliveryOption2.setMaxDaysCount(4);
        deliveryOption2.setOrderBefore(5);
        deliveryOption2.setShopDeliveryCost(1L);

        entity.setOptions(Arrays.asList(deliveryOption, deliveryOption2));

        return entity;
    }
}