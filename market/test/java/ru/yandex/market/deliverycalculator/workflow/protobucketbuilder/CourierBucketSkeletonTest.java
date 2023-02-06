package ru.yandex.market.deliverycalculator.workflow.protobucketbuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.collection.MapRandomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;

import static io.github.benas.randombeans.FieldDefinitionBuilder.field;

class CourierBucketSkeletonTest {

    /**
     * Тест проверяет, что все поля объекта класса CourierBucketSkeleton и его рекурсивного обхода (за исключением
     * обнуленного deliveryCost и некоторых рабочих полей из класса протобуфа) копируются при вызове
     * CourierBucketSkeleton.getCopyWithZeroDeliveryCosts().
     * Нужен, чтобы при расширении модели не забывали поддерживать копирование соответствующих полей в указанном методе.
     */
    @Test
    void getCopyWithZeroDeliveryCostsTest() throws IllegalAccessException {
        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(field().named("regionalDeliveryDataByTargetRegionId").ofType(Map.class)
                                .inClass(CourierBucketSkeleton.class).get(),
                        new MapRandomizer<>(new IntegerRangeRandomizer(1000, 9999),
                                ProtoBucketBuilderTestUtils.getDeliveryRegionalDataSkeletonRandomizer(), 5))
                .overrideDefaultInitialization(true)
                .build();

        CourierBucketSkeleton courierBucketSkeleton = enhancedRandom.nextObject(CourierBucketSkeleton.class);
        CourierBucketSkeleton courierBucketSkeletonCopy = courierBucketSkeleton.getCopyWithZeroDeliveryCosts();

        // memoizedHashCode пересчитывается при вызове hashCode(), поэтому сбрасываем его в дефолтное значение,
        // чтобы оно не учитывалось в сравнении
        Set<DeliveryCalcProtos.DeliveryOption> options = new HashSet<>();
        Set<DeliveryCalcProtos.DeliveryService> services = new HashSet<>();

        courierBucketSkeleton.getRegionalDeliveryDataByTargetRegionId().forEach((regionId, regionalDeliveryDataSkeleton) -> {
            DeliveryOptionGroupSkeleton optionsSkeleton = regionalDeliveryDataSkeleton.getOptionGroup();
            DeliveryServiceGroupSkeleton servicesSkeleton = regionalDeliveryDataSkeleton.getServiceGroup();

            if (optionsSkeleton != null && CollectionUtils.isNotEmpty(optionsSkeleton.getOptions())) {
                options.addAll(optionsSkeleton.getOptions());
            }

            if (servicesSkeleton != null && CollectionUtils.isNotEmpty(servicesSkeleton.getServices())) {
                services.addAll(servicesSkeleton.getServices());
            }
        });

        ProtoBucketBuilderTestUtils.cleanUpMemoizedHashCodeFields(options);
        ProtoBucketBuilderTestUtils.cleanUpMemoizedHashCodeFields(services);
        ReflectionAssert.assertLenientEquals(courierBucketSkeleton, courierBucketSkeletonCopy);
    }
}
