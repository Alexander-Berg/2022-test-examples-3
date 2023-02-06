package ru.yandex.market.deliverycalculator.storage.repository.daas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasDeliveryOptionGroupEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPostBucketEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPostGenerationEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPostRegionalDataEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryOption;

import static java.util.Arrays.asList;

/**
 * Тест для {@link DaasPostBucketRepository}.
 */
public class DaasPostBucketRepositoryTest extends FunctionalTest {

    @Autowired
    private DaasPostBucketRepository tested;
    @Autowired
    private DaasPostGenerationRepository generationRepository;

    /**
     * Тест для {@link DaasPostBucketRepository#save(Object).
     */
    @DbUnitDataSet(before = "database/daasPostBucketsRepositoryStoreTest.before.csv",
            after = "database/daasPostBucketsRepositoryStoreTest.after.csv")
    @Test
    void testSave() {
        DaasPostGenerationEntity generation = generationRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Incorrect test data"));
        DaasPostBucketEntity testBucket = createTestBucket(generation);

        tested.save(testBucket);

        System.out.println("X");
    }

    private DaasPostBucketEntity createTestBucket(DaasPostGenerationEntity generation) {
        DeliveryOption deliveryOption1ForRegion1 = new DeliveryOption();
        deliveryOption1ForRegion1.setCost(2500);
        deliveryOption1ForRegion1.setMinDaysCount(1);
        deliveryOption1ForRegion1.setMaxDaysCount(2);
        deliveryOption1ForRegion1.setOrderBefore(4);
        deliveryOption1ForRegion1.setShopDeliveryCost(20L);

        DeliveryOption deliveryOption2ForRegion1 = new DeliveryOption();
        deliveryOption2ForRegion1.setCost(2501);
        deliveryOption2ForRegion1.setMinDaysCount(2);
        deliveryOption2ForRegion1.setMaxDaysCount(3);
        deliveryOption2ForRegion1.setOrderBefore(4);
        deliveryOption2ForRegion1.setShopDeliveryCost(21L);

        DeliveryOption deliveryOption1ForRegion2 = new DeliveryOption();
        deliveryOption1ForRegion2.setCost(2520);
        deliveryOption1ForRegion2.setMinDaysCount(21);
        deliveryOption1ForRegion2.setMaxDaysCount(22);
        deliveryOption1ForRegion2.setOrderBefore(24);
        deliveryOption1ForRegion2.setShopDeliveryCost(220L);

        DaasPostRegionalDataEntity regionalDataEntity1 = new DaasPostRegionalDataEntity();
        regionalDataEntity1.setRegionId(1);
        regionalDataEntity1.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity1.getOptionsGroup().setOptions(asList(deliveryOption1ForRegion1, deliveryOption2ForRegion1));
        regionalDataEntity1.setPickupPoints("{\"outletGroups\": [{\"outletIds\": [106], \"dimensions\": {\"dimSum\": 186.0, \"dimensions\": [56.0, 58.0, 72.0]}}]}");

        DaasPostRegionalDataEntity regionalDataEntity2 = new DaasPostRegionalDataEntity();
        regionalDataEntity2.setRegionId(2);
        regionalDataEntity2.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity2.getOptionsGroup().setOptions(asList(deliveryOption1ForRegion2));
        regionalDataEntity2.setPickupPoints("{\"outletGroups\": [{\"outletIds\": [106], \"dimensions\": {\"dimSum\": 186.0, \"dimensions\": [56.0, 58.0, 72.0]}}]}");

        DaasPostBucketEntity bucketEntity = new DaasPostBucketEntity();
        bucketEntity.setGeneration(generation);
        bucketEntity.setExternalId(2L);
        bucketEntity.setRegionalDeliveryData(asList(regionalDataEntity1, regionalDataEntity2));
        bucketEntity.setTariffId(1L);
        regionalDataEntity1.setBucket(bucketEntity);
        regionalDataEntity2.setBucket(bucketEntity);

        return bucketEntity;
    }
}
