package ru.yandex.market.mbi.affiliate.promo.random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertTrue;

public class RandomStringGeneratorTest {

    @Test
    public void test() {
        var generator = new RandomStringGenerator();
        List<String> results = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            results.add(generator.nextString(8));
        }
        assertTrue(results.stream().allMatch(s -> s.length() == 8));
        assertTrue(results.stream().allMatch(RandomStringGeneratorTest::matchesAlphabet));
        assertThat(results.size()).isEqualTo(new HashSet<>(results).size());
    }

    private static boolean matchesAlphabet(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (RandomStringGenerator.ALPHABET.indexOf(s.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}