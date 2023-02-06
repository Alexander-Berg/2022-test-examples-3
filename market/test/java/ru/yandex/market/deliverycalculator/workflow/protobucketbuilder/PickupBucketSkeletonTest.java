package ru.yandex.market.deliverycalculator.workflow.protobucketbuilder;

import java.util.Map;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.collection.MapRandomizer;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import static io.github.benas.randombeans.FieldDefinitionBuilder.field;
import static java.util.stream.Collectors.toSet;

class PickupBucketSkeletonTest {

    /**
     * Тест проверяет, что все поля объекта класса PickupBucketSkeleton и его рекурсивного обхода (за исключением
     * обнуленного deliveryCost и некоторых рабочих полей из класса протобуфа) копируются при вызове
     * PickupBucketSkeleton.getCopyWithZeroDeliveryCosts().
     * Нужен, чтобы при расширении модели не забывали поддерживать копирование соответствующих полей в указанном методе.
     */
    @Test
    void getCopyWithZeroDeliveryCostsTest() throws IllegalAccessException {
        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(field().named("outlet2OptionGroup").ofType(Map.class).inClass(PickupBucketSkeleton.class).get(),
                        new MapRandomizer<>(
                                ProtoBucketBuilderTestUtils.getOutletInfoRandomizer(),
                                ProtoBucketBuilderTestUtils.getDeliveryOptionGroupSkeletonRandomizer(),
                                5
                        )
                )
                .overrideDefaultInitialization(true)
                .build();

        PickupBucketSkeleton pickupBucketSkeleton = enhancedRandom.nextObject(PickupBucketSkeleton.class);
        PickupBucketSkeleton pickupBucketSkeletonCopy = pickupBucketSkeleton.getCopyWithZeroDeliveryCosts();

        // memoizedHashCode пересчитывается при вызове hashCode(), поэтому сбрасываем его в дефолтное значение,
        // чтобы оно не учитывалось в сравнении
        ProtoBucketBuilderTestUtils.cleanUpMemoizedHashCodeFields(pickupBucketSkeleton.getOutletId2OptionGroupMap()
                .values().stream()
                .flatMap(skeleton -> skeleton.getOptions().stream())
                .collect(toSet()));

        ReflectionAssert.assertLenientEquals(pickupBucketSkeleton, pickupBucketSkeletonCopy);
    }

}
