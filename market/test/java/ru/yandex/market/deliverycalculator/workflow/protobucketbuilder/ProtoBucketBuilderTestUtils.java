package ru.yandex.market.deliverycalculator.workflow.protobucketbuilder;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.UnknownFieldSet;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.collection.SetRandomizer;
import io.github.benas.randombeans.randomizers.misc.SkipRandomizer;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.workflow.mardopickup.OutletInfo;
import ru.yandex.market.deliverycalculator.workflow.mardopost.PostOfficeInfo;

import static io.github.benas.randombeans.FieldDefinitionBuilder.field;

class ProtoBucketBuilderTestUtils {

    static <T extends AbstractMessage> void cleanUpMemoizedHashCodeFields(Collection<T> cleanedSkeletons) throws IllegalAccessException {
        for (T cleanedSkeleton : cleanedSkeletons) {
            ProtoBucketBuilderTestUtils.setInternalField(cleanedSkeleton, "memoizedHashCode", 0);
        }
    }

    static Randomizer<DeliveryRegionalDataSkeleton> getDeliveryRegionalDataSkeletonRandomizer() {
        return new Randomizer<DeliveryRegionalDataSkeleton>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .randomize(field().named("optionGroup").ofType(DeliveryOptionGroupSkeleton.class).inClass(DeliveryRegionalDataSkeleton.class).get(),
                            getDeliveryOptionGroupSkeletonRandomizer())
                    .randomize(field().named("serviceGroup").ofType(DeliveryServiceGroupSkeleton.class).inClass(DeliveryRegionalDataSkeleton.class).get(),
                            getDeliveryServiceGroupSkeletonRandomizer())
                    .randomize(field().named("optionType").ofType(DeliveryCalcProtos.OptionType.class).inClass(DeliveryRegionalDataSkeleton.class).get(),
                            (Supplier<DeliveryCalcProtos.OptionType>) () -> DeliveryCalcProtos.OptionType.NORMAL_OPTION)
                    .overrideDefaultInitialization(true)
                    .build();

            @Override
            public DeliveryRegionalDataSkeleton getRandomValue() {
                return enhancedRandom.nextObject(DeliveryRegionalDataSkeleton.class);
            }
        };
    }

    static Randomizer<OutletInfo> getOutletInfoRandomizer() {
        return new Randomizer<OutletInfo>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().build();

            @Override
            public OutletInfo getRandomValue() {
                return enhancedRandom.nextObject(OutletInfo.class);
            }
        };
    }

    static Randomizer<PostOfficeInfo> getPostOfficeInfoRandomizer() {
        return new Randomizer<PostOfficeInfo>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().build();

            @Override
            public PostOfficeInfo getRandomValue() {
                return enhancedRandom.nextObject(PostOfficeInfo.class);
            }
        };
    }

    static Randomizer<DeliveryOptionGroupSkeleton> getDeliveryOptionGroupSkeletonRandomizer() {
        return new Randomizer<DeliveryOptionGroupSkeleton>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .randomize(field().named("options").ofType(Set.class).inClass(DeliveryOptionGroupSkeleton.class).get(),
                            new SetRandomizer<>(getDeliveryOptionRandomizer(), 1))
                    .overrideDefaultInitialization(true)
                    .build();

            @Override
            public DeliveryOptionGroupSkeleton getRandomValue() {
                return enhancedRandom.nextObject(DeliveryOptionGroupSkeleton.class);
            }
        };
    }

    private static Randomizer<DeliveryServiceGroupSkeleton> getDeliveryServiceGroupSkeletonRandomizer() {
        return new Randomizer<DeliveryServiceGroupSkeleton>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .randomize(field().named("services").ofType(Set.class).inClass(DeliveryServiceGroupSkeleton.class).get(),
                            new SetRandomizer<>(getDeliveryServiceRandomizer(), 1))
                    .overrideDefaultInitialization(true)
                    .build();

            @Override
            public DeliveryServiceGroupSkeleton getRandomValue() {
                return enhancedRandom.nextObject(DeliveryServiceGroupSkeleton.class);
            }
        };
    }

    private static Randomizer<DeliveryCalcProtos.DeliveryService> getDeliveryServiceRandomizer() {
        return new Randomizer<DeliveryCalcProtos.DeliveryService>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    // чтобы работали методы has*() в классе DeliveryCalcProtos.DeliveryService
                    .randomize(field().named("bitField0_").ofType(int.class).inClass(DeliveryCalcProtos.DeliveryService.class).get(), (Supplier<Integer>) () -> 63)
                    // чтобы при сравнении они были заполнены дефолтными значениями и не учитывались
                    .randomize(UnknownFieldSet.class, new SkipRandomizer())
                    .randomize(field().named("code_").ofType(Object.class).inClass(DeliveryCalcProtos.DeliveryService.class).get(), (Supplier<String>) () -> "INSURANCE")
                    .randomize(field().named("priceCalculationRule_").ofType(int.class).inClass(DeliveryCalcProtos.DeliveryService.class).get(), (Supplier<Integer>) DeliveryCalcProtos.DeliveryServicePriceCalculationRule.FIX::getNumber)
                    .randomize(field().named("memoizedIsInitialized").ofType(byte.class).inClass(DeliveryCalcProtos.DeliveryService.class).get(), (Supplier<Byte>) () -> (byte) 0)
                    .randomize(field().named("memoizedSize").ofType(int.class).inClass(AbstractMessage.class).get(), (Supplier<Integer>) () -> 0)
                    .randomize(field().named("memoizedHashCode").ofType(int.class).inClass(AbstractMessage.class).get(), (Supplier<Integer>) () -> 0)
                    .overrideDefaultInitialization(true)
                    .build();

            @Override
            public DeliveryCalcProtos.DeliveryService getRandomValue() {
                return enhancedRandom.nextObject(DeliveryCalcProtos.DeliveryService.class);
            }
        };
    }


    private static Randomizer<DeliveryCalcProtos.DeliveryOption> getDeliveryOptionRandomizer() {
        return new Randomizer<DeliveryCalcProtos.DeliveryOption>() {
            private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    // чтобы работали методы has*() в классе DeliveryCalcProtos.DeliveryOption
                    .randomize(field().named("bitField0_").ofType(int.class).inClass(DeliveryCalcProtos.DeliveryOption.class).get(), (Supplier<Integer>) () -> 15)
                    // чтобы при сравнении они были заполнены дефолтными значениями и не учитывались
                    .randomize(UnknownFieldSet.class, new SkipRandomizer())
                    .randomize(field().named("memoizedIsInitialized").ofType(byte.class).inClass(DeliveryCalcProtos.DeliveryOption.class).get(), (Supplier<Byte>) () -> (byte) 0)
                    .randomize(field().named("memoizedSize").ofType(int.class).inClass(AbstractMessage.class).get(), (Supplier<Integer>) () -> 0)
                    .randomize(field().named("memoizedHashCode").ofType(int.class).inClass(AbstractMessage.class).get(), (Supplier<Integer>) () -> 0)
                    .exclude(field().named("deliveryCost_").inClass(DeliveryCalcProtos.DeliveryOption.class).get())
                    .exclude(field().named("tariffId_").inClass(DeliveryCalcProtos.DeliveryOption.class).get())
                    .exclude(field().named("shopDeliveryCost_").inClass(DeliveryCalcProtos.DeliveryOption.class).get())
                    .overrideDefaultInitialization(true)
                    .build();

            @Override
            public DeliveryCalcProtos.DeliveryOption getRandomValue() {
                return enhancedRandom.nextObject(DeliveryCalcProtos.DeliveryOption.class);
            }
        };
    }

    private static void setInternalField(AbstractMessage target, String field, int value) throws IllegalAccessException {
        Class<?> clazz = target.getClass();
        Field f = null;
        while (f == null && clazz != Object.class) {
            f = getField(clazz, field);
            clazz = clazz.getSuperclass();
        }
        if (f == null) {
            return;
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field getField(Class<?> clazz, String field) {
        try {
            return clazz.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
