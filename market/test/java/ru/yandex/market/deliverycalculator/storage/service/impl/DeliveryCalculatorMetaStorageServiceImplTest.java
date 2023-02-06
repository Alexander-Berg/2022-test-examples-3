package ru.yandex.market.deliverycalculator.storage.service.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierBucketEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierRegionalDataEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasDeliveryOptionGroupEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPickupBucketEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPickupGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasPickupRegionalDataEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryOption;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasCourierBucketRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasCourierGenerationRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasDeliveryOptionGroupRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasPickupBucketRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasPickupGenerationRepository;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet
class DeliveryCalculatorMetaStorageServiceImplTest extends FunctionalTest {

    @Autowired
    private DeliveryCalculatorMetaStorageService service;
    @Autowired
    private GenerationRepository generationRepository;
    @Autowired
    private DaasCourierGenerationRepository daasCourierGenerationRepository;
    @Autowired
    private DaasCourierBucketRepository daasCourierBucketRepository;
    @Autowired
    private DaasPickupGenerationRepository daasPickupGenerationRepository;
    @Autowired
    private DaasPickupBucketRepository daasPickupBucketRepository;
    @Autowired
    private DaasDeliveryOptionGroupRepository daasDeliveryOptionGroupRepository;

    @BeforeEach
    void setUp() {
    }

    @DbUnitDataSet
    @Test
    // TODO: сейчас тест падает при запуске тестов во всем модуле, он клэшится с другими тестами
    // Скорее всего проблема связана с идентификаторами хибернейтовских сущностей, для их генерации используются сиквенсы
    // с шагом 100 на базе данных, перед каждым тестом сиквенсы перезапускаются, и в какой-то момент времени в
    // хиьернейтовскую сессию мы пытаемся вставить новый объект с идентификатором, который уже был использован до этого
    @Disabled
    void it_must_delete_all_daas_courier_generations_when_number_of_buckets_and_option_groups_is_35k() {
        // Given
        final int nBucketsAndOptionGroups = 35_000;
        final Generation generation = new Generation(1, 1);
        generation.setTime(Instant.now());
        final DaasCourierGeneration daasGeneration = new DaasCourierGeneration();
        daasGeneration.setTariffId(1);
        for (int i = 0; i < nBucketsAndOptionGroups; ++i) {
            createCourierTestBucket(daasGeneration);
        }
        generation.getDaasCourierGenerations().add(daasGeneration);
        daasGeneration.setGeneration(generation);

        generationRepository.save(generation);

        assertThat(daasCourierGenerationRepository.findAll()).hasSize(1);
        assertThat(daasCourierBucketRepository.findAll()).hasSize(nBucketsAndOptionGroups);
        assertThat(daasDeliveryOptionGroupRepository.findAll()).hasSize(nBucketsAndOptionGroups);

        // When
        final int actual = service.deleteDaasCourierGenerations(Collections.singleton(1L), 1);

        // Then
        assertEquals(1, actual);
        assertThat(daasCourierGenerationRepository.findAll()).isEmpty();
        assertThat(daasCourierBucketRepository.findAll()).isEmpty();
        assertThat(daasDeliveryOptionGroupRepository.findAll()).isEmpty();
    }

    @DbUnitDataSet(before = "deleteDaasCourierGenerations.before.csv", after = "deleteAllDaasCourierGenerations.after.csv")
    @Test
    void it_must_delete_all_daas_courier_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(1000L, 2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasCourierGenerations(tariffIds, generationId);

        // Then
        assertEquals(2, actual);
    }

    @DbUnitDataSet(before = "deleteDaasCourierGenerations.before.csv", after = "deletePartDaasCourierGenerations.after.csv")
    @Test
    void it_must_delete_one_daas_courier_generation() {
        // Given
        final List<Long> tariffIds = Collections.singletonList(2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasCourierGenerations(tariffIds, generationId);

        // Then
        assertEquals(1, actual);
    }

    @DbUnitDataSet(before = "deleteDaasCourierGenerations.before.csv", after = "deleteDaasCourierGenerations.before.csv")
    @Test
    void it_must_not_delete_daas_courier_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(3000L, 4000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasCourierGenerations(tariffIds, generationId);

        // Then
        assertEquals(0, actual);
    }

    @DbUnitDataSet
    @Test
    // TODO: сейчас тест падает при запуске тестов во всем модуле, он клэшится с другими тестами
    // Скорее всего проблема связана с идентификаторами хибернейтовских сущностей, для их генерации используются сиквенсы
    // с шагом 100 на базе данных, перед каждым тестом сиквенсы перезапускаются, и в какой-то момент времени в
    // хиьернейтовскую сессию мы пытаемся вставить новый объект с идентификатором, который уже был использован до этого
    @Disabled
    void it_must_delete_all_daas_pickup_generations_when_number_of_buckets_and_option_groups_is_35k() {
        // Given
        final int nBucketsAndOptionGroups = 35_000;
        final Generation generation = new Generation(2, 2);
        generation.setTime(Instant.now());
        final DaasPickupGeneration daasGeneration = new DaasPickupGeneration();
        daasGeneration.setTariffId(2);
        for (int i = 0; i < nBucketsAndOptionGroups; ++i) {
            createPickupTestBucket(daasGeneration);
        }
        generation.getDaasPickupGenerations().add(daasGeneration);
        daasGeneration.setGeneration(generation);

        generationRepository.save(generation);

        assertThat(daasPickupGenerationRepository.findAll()).hasSize(1);
        assertThat(daasPickupBucketRepository.findAll()).hasSize(nBucketsAndOptionGroups);
        assertThat(daasDeliveryOptionGroupRepository.findAll()).hasSize(nBucketsAndOptionGroups);

        // When
        final int actual = service.deleteDaasPickupGenerations(Collections.singleton(2L), 2);

        // Then
        assertEquals(1, actual);
        assertThat(daasPickupGenerationRepository.findAll()).isEmpty();
        assertThat(daasPickupBucketRepository.findAll()).isEmpty();
        assertThat(daasDeliveryOptionGroupRepository.findAll()).isEmpty();
    }

    @DbUnitDataSet(before = "deleteDaasPickupGenerations.before.csv", after = "deleteAllDaasPickupGenerations.after.csv")
    @Test
    void it_must_delete_all_daas_pickup_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(1000L, 2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPickupGenerations(tariffIds, generationId);

        // Then
        assertEquals(2, actual);
    }

    @DbUnitDataSet(before = "deleteDaasPickupGenerations.before.csv", after = "deletePartDaasPickupGenerations.after.csv")
    @Test
    void it_must_delete_one_daas_pickup_generation() {
        // Given
        final List<Long> tariffIds = Collections.singletonList(2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPickupGenerations(tariffIds, generationId);

        // Then
        assertEquals(1, actual);
    }

    @DbUnitDataSet(before = "deleteDaasPickupGenerations.before.csv", after = "deleteDaasPickupGenerations.before.csv")
    @Test
    void it_must_not_delete_daas_pickup_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(3000L, 4000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPickupGenerations(tariffIds, generationId);

        // Then
        assertEquals(0, actual);
    }

    @DbUnitDataSet(before = "deleteDaasPostGenerations.before.csv", after = "deleteAllDaasPostGenerations.after.csv")
    @Test
    void it_must_delete_all_daas_post_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(1000L, 2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPostGenerations(tariffIds, generationId);

        // Then
        assertEquals(2, actual);
    }

    @DbUnitDataSet(before = "deleteDaasPostGenerations.before.csv", after = "deletePartDaasPostGenerations.after.csv")
    @Test
    void it_must_delete_one_daas_post_generation() {
        // Given
        final List<Long> tariffIds = Collections.singletonList(2000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPostGenerations(tariffIds, generationId);

        // Then
        assertEquals(1, actual);
    }

    @DbUnitDataSet(before = "deleteDaasPostGenerations.before.csv", after = "deleteDaasPostGenerations.before.csv")
    @Test
    void it_must_not_delete_daas_post_generations() {
        // Given
        final List<Long> tariffIds = Arrays.asList(3000L, 4000L);
        final int generationId = 1;

        // When
        final int actual = service.deleteDaasPostGenerations(tariffIds, generationId);

        // Then
        assertEquals(0, actual);
    }

    private void createCourierTestBucket(DaasCourierGeneration generation) {
        DeliveryOption deliveryOptionForRegion = new DeliveryOption();
        deliveryOptionForRegion.setCost(2500);
        deliveryOptionForRegion.setMinDaysCount(1);
        deliveryOptionForRegion.setMaxDaysCount(2);
        deliveryOptionForRegion.setOrderBefore(4);
        deliveryOptionForRegion.setShopDeliveryCost(20L);

        DaasCourierRegionalDataEntity regionalDataEntity = new DaasCourierRegionalDataEntity();
        regionalDataEntity.setRegionId(1);
        regionalDataEntity.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity.getOptionsGroup().setOptions(Collections.singletonList(deliveryOptionForRegion));

        DaasCourierBucketEntity bucketEntity = new DaasCourierBucketEntity();
        generation.addBucket(bucketEntity);
        bucketEntity.setExternalId(2L);
        bucketEntity.setRegionalDeliveryData(Collections.singletonList(regionalDataEntity));
        regionalDataEntity.setBucket(bucketEntity);

    }

    private void createPickupTestBucket(DaasPickupGeneration generation) {
        DeliveryOption deliveryOptionForRegion = new DeliveryOption();
        deliveryOptionForRegion.setCost(2500);
        deliveryOptionForRegion.setMinDaysCount(1);
        deliveryOptionForRegion.setMaxDaysCount(2);
        deliveryOptionForRegion.setOrderBefore(4);
        deliveryOptionForRegion.setShopDeliveryCost(20L);

        DaasPickupRegionalDataEntity regionalDataEntity = new DaasPickupRegionalDataEntity();
        regionalDataEntity.setRegionId(1);
        regionalDataEntity.setOptionsGroup(new DaasDeliveryOptionGroupEntity());
        regionalDataEntity.getOptionsGroup().setOptions(Collections.singletonList(deliveryOptionForRegion));

        DaasPickupBucketEntity bucketEntity = new DaasPickupBucketEntity();
        generation.addBucket(bucketEntity);
        bucketEntity.setExternalId(2L);
        bucketEntity.setRegionalDeliveryData(Collections.singletonList(regionalDataEntity));
        regionalDataEntity.setBucket(bucketEntity);
        regionalDataEntity.setPickupPoints("{}");

    }
}
