package ru.yandex.mail.diffusion.testing;

import io.github.benas.randombeans.randomizers.AbstractRandomizer;

import java.util.OptionalInt;

public class OptionalIntRandomizer extends AbstractRandomizer<OptionalInt> {
    @Override
    public OptionalInt getRandomValue() {
        if (random.nextBoolean()) {
            return OptionalInt.of(random.nextInt());
        } else {
            return OptionalInt.empty();
        }
    }
}
