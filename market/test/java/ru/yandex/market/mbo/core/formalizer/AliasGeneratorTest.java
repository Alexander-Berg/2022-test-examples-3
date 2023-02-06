package ru.yandex.market.mbo.core.formalizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class AliasGeneratorTest {

    @Test
    public void testEmptyStringYieldsEmptySet() {
        Set<String> actual = AliasGenerator.generateMinimalistic("");
        Set<String> expected = expected();
        Assertions.assertThat(actual).isEqualTo(expected);

        actual = AliasGenerator.generateMinimalistic(null);
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testSpacesOnlyYieldsEmptySet() {
        Set<String> actual = AliasGenerator.generateMinimalistic("  \t \n  ");
        Set<String> expected = expected();
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testOneWordYieldsSingletonSet() {
        Set<String> actual = AliasGenerator.generateMinimalistic("meow");
        Set<String> expected = expected("meow");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testOneWordWithSpacesYieldsSingletonSet() {
        Set<String> actual = AliasGenerator.generateMinimalistic("   meow \t");
        Set<String> expected = expected("meow");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTwoWords() {
        Set<String> actual = AliasGenerator.generateMinimalistic("me ow");
        Set<String> expected = expected("meow", "me ow");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTwoWordsWithSpaces() {
        Set<String> actual = AliasGenerator.generateMinimalistic(" me  \t ow  ");
        Set<String> expected = expected("meow", "me ow");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testThreeWords() {
        Set<String> actual = AliasGenerator.generateMinimalistic("a b c");
        Set<String> expected = expected("abc", "a b c", "ab c", "a bc");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testThreeBigWords() {
        Set<String> actual = AliasGenerator.generateMinimalistic("aaa bb cccc");
        Set<String> expected = expected("aaabbcccc", "aaa bb cccc", "aaabb cccc", "aaa bbcccc");
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFourWords() {
        Set<String> actual = AliasGenerator.generateMinimalistic("a b c d");
        Set<String> expected = expected(
            "abcd",
            "a b c d",
            "ab c d",
            "a bc d",
            "a b cd",
            "abc d",
            "a bcd",
            "ab cd"
        );
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFiveWords() {
        Set<String> actual = AliasGenerator.generateMinimalistic("a b c d e");
        Set<String> expected = expected(
            "abcde",
            "a b c d e",
            "ab c d e",
            "a bc d e",
            "a b cd e",
            "a b c de",
            "a bcde",
            "ab cde",
            "abc de",
            "abcd e"
        );
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFiveWordsButWithCapitalDelimiterInSomePlaces() {
        Set<String> actual = AliasGenerator.generateMinimalistic("aa BbCcDd Ee");
        Set<String> expected = expected(
            "aaBbCcDdEe",
            "aa Bb Cc Dd Ee",
            "aaBb Cc Dd Ee",
            "aa BbCc Dd Ee",
            "aa Bb CcDd Ee",
            "aa Bb Cc DdEe",
            "aa BbCcDdEe",
            "aaBb CcDdEe",
            "aaBbCc DdEe",
            "aaBbCcDd Ee"
        );
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    private static Set<String> expected(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
