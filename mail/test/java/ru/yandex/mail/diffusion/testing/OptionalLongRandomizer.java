package ru.yandex.mail.diffusion.testing;

import io.github.benas.randombeans.randomizers.AbstractRandomizer;

import java.util.OptionalLong;

public class OptionalLongRandomizer extends AbstractRandomizer<OptionalLong> {
    @Override
    public OptionalLong getRandomValue() {
        if (random.nextBoolean()) {
            return OptionalLong.of(random.nextLong());
        } else {
            return OptionalLong.empty();
        }
    }
}
