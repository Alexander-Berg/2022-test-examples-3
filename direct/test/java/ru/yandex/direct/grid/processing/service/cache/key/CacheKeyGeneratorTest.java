package ru.yandex.direct.grid.processing.service.cache.key;

import java.util.HashSet;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.grid.processing.service.cache.util.CacheKeyGenerator;

public class CacheKeyGeneratorTest {
    public static final int KEY_SIZE = 10;
    public static final int TESTS_NUM = 1000;

    @Test
    public void correctAlphabetLengthAndUniqueness() {
        CacheKeyGenerator gen = new CacheKeyGenerator(KEY_SIZE);
        HashSet<String> set = new HashSet<>();
        SoftAssertions softly = new SoftAssertions();

        for (int i = 0; i < TESTS_NUM; i++) {
            String key = gen.generate();
            set.add(key);
            softly.assertThat(key).hasSize(KEY_SIZE);
            softly.assertThat(key.chars().allMatch(c -> CacheKeyGenerator.ALPHABET.indexOf(c) >= 0)).isTrue();
        }
        softly.assertThat(set).hasSize(TESTS_NUM);
        softly.assertAll();
    }
}
