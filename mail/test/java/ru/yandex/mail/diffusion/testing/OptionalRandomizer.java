package ru.yandex.mail.diffusion.testing;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.AbstractRandomizer;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class OptionalRandomizer extends AbstractRandomizer<Optional<?>> {
    private final EnhancedRandom enhancedRandom;
    private final Class<?> valueType;

    @Override
    public Optional<?> getRandomValue() {
        if (random.nextBoolean()) {
            return Optional.of(enhancedRandom.nextObject(valueType));
        } else {
            return Optional.empty();
        }
    }
}
