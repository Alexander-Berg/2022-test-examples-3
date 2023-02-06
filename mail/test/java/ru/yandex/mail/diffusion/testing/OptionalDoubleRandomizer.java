package ru.yandex.mail.diffusion.testing;

import io.github.benas.randombeans.randomizers.AbstractRandomizer;

import java.util.OptionalDouble;

public class OptionalDoubleRandomizer extends AbstractRandomizer<OptionalDouble> {
    @Override
    public OptionalDouble getRandomValue() {
        if (random.nextBoolean()) {
            return OptionalDouble.of(random.nextDouble());
        } else {
            return OptionalDouble.empty();
        }
    }
}
