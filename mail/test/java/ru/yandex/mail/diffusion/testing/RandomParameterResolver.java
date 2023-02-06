package ru.yandex.mail.diffusion.testing;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.registry.CustomRandomizerRegistry;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class RandomParameterResolver implements ParameterResolver {
    private class CustomRegistry extends CustomRandomizerRegistry {
        CustomRegistry() {
            registerRandomizer(OptionalInt.class, new OptionalIntRandomizer());
            registerRandomizer(OptionalLong.class, new OptionalLongRandomizer());
            registerRandomizer(OptionalDouble.class, new OptionalDoubleRandomizer());
        }

        @Override
        @SneakyThrows
        public Randomizer<?> getRandomizer(Field field) {
            if (field.getType() == Optional.class) {
                val type = (ParameterizedType) field.getGenericType();
                val valueType = Class.forName(type.getActualTypeArguments()[0].getTypeName());
                return new OptionalRandomizer(random, valueType);
            }
            return super.getRandomizer(field);
        }

        @Override
        public Randomizer<?> getRandomizer(Class<?> type) {
            if (type == Optional.class) {
                throw new IllegalStateException("Optional type is not supported after type erasure");
            }
            return super.getRandomizer(type);
        }
    }

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .objectPoolSize(1000)
        .charset(StandardCharsets.UTF_8)
        .stringLengthRange(0, 100)
        .collectionSizeRange(0, 100)
        .scanClasspathForConcreteTypes(true)
        .overrideDefaultInitialization(true)
        .registerRandomizerRegistry(new CustomRegistry())
        .build();

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(Random.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        return random.nextObject(parameterContext.getParameter().getType());
    }
}
