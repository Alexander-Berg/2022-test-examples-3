package ru.yandex.market.deliverycalculator.workflow.protobucketbuilder;

import java.util.Map;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.collection.MapRandomizer;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import static io.github.benas.randombeans.FieldDefinitionBuilder.field;
import static java.util.stream.Collectors.toSet;

class PostBucketSkeletonTest {

    /**
     * Тест проверяет, что все поля объекта класса PostBucketSkeleton и его рекурсивного обхода (за исключением
     * обнуленного deliveryCost и некоторых рабочих полей из класса протобуфа) копируются при вызове
     * PostBucketSkeleton.getCopyWithZeroDeliveryCosts().
     * Нужен, чтобы при расширении модели не забывали поддерживать копирование соответствующих полей в указанном методе.
     */
    @Test
    void getCopyWithZeroDeliveryCostsTest() throws IllegalAccessException {
        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(field().named("postOffice2OptionGroup").ofType(Map.class).inClass(PostBucketSkeleton.class).get(),
                        new MapRandomizer<>(
                                ProtoBucketBuilderTestUtils.getPostOfficeInfoRandomizer(),
                                ProtoBucketBuilderTestUtils.getDeliveryOptionGroupSkeletonRandomizer(), 5))
                .overrideDefaultInitialization(true)
                .build();

        PostBucketSkeleton postBucketSkeleton = enhancedRandom.nextObject(PostBucketSkeleton.class);
        PostBucketSkeleton postBucketSkeletonCopy = postBucketSkeleton.getCopyWithZeroDeliveryCosts();

        // memoizedHashCode пересчитывается при вызове hashCode(), поэтому сбрасываем его в дефолтное значение,
        // чтобы оно не учитывалось в сравнении
        ProtoBucketBuilderTestUtils.cleanUpMemoizedHashCodeFields(postBucketSkeleton.getPostOffice2OptionGroup()
                .values().stream()
                .flatMap(skeleton -> skeleton.getOptions().stream())
                .collect(toSet()));

        ReflectionAssert.assertLenientEquals(postBucketSkeleton, postBucketSkeletonCopy);
    }
}
