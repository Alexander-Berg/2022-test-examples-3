package ru.yandex.market.deliverycalculator.storage.repository.daas;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierBucketEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierRegionalDataEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasDeliveryOptionGroupEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryOption;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

/**
 * Тест для {@link DaasCourierBucketRepository}.
 */
class DaasCourierBucketRepositoryTest extends FunctionalTest {

    @Autowired
    private DaasCourierGenerationRepository generationRepository;
    @Autowired
    private DaasCourierBucketRepository tested;

    /**
     * Тест для {@link DaasCourierBucketRepository#save(Object).
     */
    @DbUnitDataSet(before = "database/daasCourierBucketsRepositoryStoreTest.before.csv",
            after = "database/daasCourierBucketsRepositoryStoreTest.after.csv")
    @Test
    void testSave() {
        DaasCourierGeneration daasCourierGeneration = generationRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Incorrect test data"));
        DaasCourierBucketEntity testBucket = createTestBucket(daasCourierGeneration);

        tested.save(testBucket);

        Optional<DaasCourierBucketEntity> actualBucket = tested.findAll().stream()
                .findFirst();
        assertTrue(actualBucket.isPresent());
    }

    private DaasCourierBucketEntity createTestBucket(DaasCourierGeneration generation) {
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

        DaasCourierRegionalDataEntity regionalDataEntity1 = new DaasCourierRegionalDataEntity();
        regionalDataEntity1.setRegionId(1);
        regionalDataEntity1.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity1.getOptionsGroup().setOptions(asList(deliveryOption1ForRegion1, deliveryOption2ForRegion1));

        DaasCourierRegionalDataEntity regionalDataEntity2 = new DaasCourierRegionalDataEntity();
        regionalDataEntity2.setRegionId(2);
        regionalDataEntity2.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity2.getOptionsGroup().setOptions(asList(deliveryOption1ForRegion2));

        DaasCourierBucketEntity bucketEntity = new DaasCourierBucketEntity();
        bucketEntity.setGeneration(generation);
        bucketEntity.setExternalId(2L);
        bucketEntity.setRegionalDeliveryData(asList(regionalDataEntity1, regionalDataEntity2));
        bucketEntity.setTariffId(1L);
        regionalDataEntity1.setBucket(bucketEntity);
        regionalDataEntity2.setBucket(bucketEntity);

        return bucketEntity;
    }
}
