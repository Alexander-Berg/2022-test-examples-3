package ru.yandex.market.mboc.common.masterdata.services.iris.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.EnhancedRandomParameters;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.registry.CustomRandomizerRegistry;

import ru.yandex.market.mbo.protoutils.ProtoReflectionUtil;

public class ProtobufRandomizerRegistry extends CustomRandomizerRegistry {

    private static final LoadingCache<Descriptors.Descriptor, Class<? extends Message>> PROTO_CLASS_CACHE =
        CacheBuilder.newBuilder()
            .build(CacheLoader.from(ProtoReflectionUtil::getMessageClass));
    private final EnhancedRandom random;
    private final List<RandomizerOverride> randomizerOverrides = new ArrayList<>();

    public ProtobufRandomizerRegistry(EnhancedRandom random) {
        this.random = random;
    }

    @Override
    public void init(EnhancedRandomParameters parameters) {
        registerRandomizer(ByteString.class, new ByteStringRandomizer(random));
        registerRandomizer(RepeatedFieldSize.class, () -> {
            Integer min = parameters.getCollectionSizeRange().getMin();
            Integer max = parameters.getCollectionSizeRange().getMax();
            int size = max > min ? min + random.nextInt(max - min) : min;
            return new RepeatedFieldSize(size);
        });

    }

    @Override
    public Randomizer<?> getRandomizer(Class<?> type) {
        Randomizer<?> cachedRandomizer = super.getRandomizer(type);
        if (cachedRandomizer != null) {
            return cachedRandomizer;
        }
        if (!Message.class.isAssignableFrom(type)) {
            return null;
        }
        //noinspection unchecked
        Class<? extends Message> messageClass = (Class<? extends Message>) type;
        Message instance = ProtoReflectionUtil.getDefaultInstance(messageClass);
        ProtobufRandomizer<Message> randomizer = new ProtobufRandomizer<>(this, random, instance);
        registerRandomizer(type, randomizer);
        return randomizer;
    }

    public Randomizer<?> getRandomizer(Descriptors.FieldDescriptor field) {
        for (RandomizerOverride override : randomizerOverrides) {
            Randomizer<?> randomizer = override.getRandomizer(field);
            if (randomizer != null) {
                return randomizer;
            }
        }
        return null;
    }

    public void registerProtoRandomizer(Predicate<Descriptors.FieldDescriptor> predicate, Randomizer<?> randomizer) {
        randomizerOverrides.add(new RandomizerOverride(predicate, randomizer));
    }

    public Randomizer<?> getRandomizer(final Descriptors.Descriptor messageType) {
        Class<? extends Message> messageClass = PROTO_CLASS_CACHE.getUnchecked(messageType);
        return getRandomizer(messageClass);
    }

    private static class RandomizerOverride {
        private final Predicate<Descriptors.FieldDescriptor> predicate;
        private final Randomizer<?> randomizer;

        RandomizerOverride(Predicate<Descriptors.FieldDescriptor> predicate, Randomizer<?> randomizer) {
            this.predicate = predicate;
            this.randomizer = randomizer;
        }

        public Randomizer<?> getRandomizer(Descriptors.FieldDescriptor field) {
            if (predicate.test(field)) {
                return randomizer;
            }
            return null;
        }
    }

    public static class RepeatedFieldSize {
        private final int size;

        public RepeatedFieldSize(int collectionSize) {
            this.size = collectionSize;
        }

        public Integer getSize() {
            return size;
        }
    }

}
