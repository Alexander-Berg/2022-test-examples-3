package ru.yandex.common.util;

import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.Test;

import ru.yandex.common.util.functional.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.common.util.StringUtils.RUS_TO_ENTRANSLIT;
import static ru.yandex.common.util.StringUtils.between;
import static ru.yandex.common.util.StringUtils.indexOfAny;
import static ru.yandex.common.util.StringUtils.isOfDigits;
import static ru.yandex.common.util.StringUtils.limit;
import static ru.yandex.common.util.StringUtils.longestWordInChars;
import static ru.yandex.common.util.StringUtils.longestWordInCodepoints;
import static ru.yandex.common.util.StringUtils.normalizeWhitespace;
import static ru.yandex.common.util.StringUtils.percentOfCaps;
import static ru.yandex.common.util.StringUtils.replace;
import static ru.yandex.common.util.StringUtils.replaceFirst;
import static ru.yandex.common.util.StringUtils.split;
import static ru.yandex.common.util.StringUtils.splitOnPunctuation;
import static ru.yandex.common.util.StringUtils.translit;
import static ru.yandex.common.util.StringUtils.trim;
import static ru.yandex.common.util.StringUtils.upFirstLetter;
import static ru.yandex.common.util.collections.CollectionFactory.array;

/**
 * Created on 20:33:24 04.02.2008
 *
 * @author jkff
 */
public class StringUtilsTest {
    @Test
    public void testSplitOnPunctuation() {
        assertThat(splitOnPunctuation(" - Hello, my dearest beautiful o'World!!!"))
                .containsExactly("Hello", "my", "dearest", "beautiful", "o", "World");
        assertThat(splitOnPunctuation("Hello")).containsExactly("Hello");
    }

    @Test
    public void testReplaceFirst() {
        assertThat(replaceFirst("aaaabbb", "aaa", "77")).isEqualTo("77abbb");
        assertThat(replaceFirst("123", "", "a")).isEqualTo("a123");
        assertThat(replaceFirst("123", "3", "a")).isEqualTo("12a");
        assertThat(replaceFirst("123", "3", "")).isEqualTo("12");
        assertThat(replaceFirst("http://?123123123", "?123", "www.ya.ru")).isEqualTo("http://www.ya.ru123123");
        assertThat(replaceFirst("123", "123", "")).isEmpty();
        assertThat(replaceFirst("", "123", "123")).isEmpty();
        assertThat(replaceFirst("123", "123", "123")).isEqualTo("123");
    }

    @Test
    public void testSplit() {
        String[] separators = {" ", "-", ":::"};

        // simple
        assertThat(split(null)).isEmpty();
        assertThat(split("")).isEmpty();
        assertThat(split("   \n\t  ")).isEmpty();
        assertThat(split(" a   b\tc\nd")).containsExactly("a", "b", "c", "d");

        // by separator
        assertThat(split(null, separators[0])).isEqualTo(Collections.singletonList(null));
        assertThat(split("", separators[0])).containsExactly("");
        assertThat(split(" \n\t ", separators[0])).containsExactly("", "\n\t", "");
        assertThat(split(" a b-c ", separators[1])).containsExactly(" a b", "c ");
        assertThat(split(" a b--c:::d", separators[2])).containsExactly(" a b--c", "d");
        assertThat(split("Nothing", separators[0])).containsExactly("Nothing");

        // by separator with limit
        assertThat(split(" Hello  my dearest  beautiful world ", ' ', 7))
                .containsExactly("", "Hello", "", "my", "dearest", "", "beautiful world ");

        // multiple
        assertThat(split(null, separators)).isEqualTo(Collections.singletonList(null));
        assertThat(split("", separators)).containsExactly("");
        assertThat(split("", new String[]{})).containsExactly("");
        assertThat(split(" a b-c ", separators)).containsExactly("a", "b", "c");
        assertThat(split(" a b--c:::d", separators)).containsExactly("a", "b", "c", "d");
        assertThat(split("Nothing", separators)).containsExactly("Nothing");
    }


    @Test
    public void testTrim() {
        // Let's make things more complex by using a unicode whitespace symbol and unicode text
        assertThat(trim("Превед мир")).isEqualTo("Превед мир");
        assertThat(trim("Превед\u202F\u202Fмир")).isEqualTo("Превед\u202F\u202Fмир");
        assertThat(trim("\u202FПревед мир\u202F")).isEqualTo("Превед мир");
        assertThat(trim("\u202F\u202F\u202F\u202FПревед мир\u202F\u202F\u202F")).isEqualTo("Превед мир");
        assertThat(trim("\u202F\u202FПревед мир")).isEqualTo("Превед мир");
        assertThat(trim("Превед мир\u202F\u202F\u202F")).isEqualTo("Превед мир");
        assertThat(trim("\u202F\u202F\u202F")).isEmpty();
        assertThat(trim("\u202F")).isEmpty();
        assertThat(trim("")).isEmpty();
        assertThat(trim("аb")).isEqualTo("аb");
        assertThat(trim("\u202Fаb")).isEqualTo("аb");
        assertThat(trim("\u202Fаb\u202F")).isEqualTo("аb");
        assertThat(trim("аb\u202F")).isEqualTo("аb");
        assertThat(trim("\u202F\u202F\u202Fаb\u202F\u202F")).isEqualTo("аb");
        assertThat(trim("а")).isEqualTo("а");
        assertThat(trim("\u202Fа")).isEqualTo("а");
        assertThat(trim("\u202Fа\u202F")).isEqualTo("а");
        assertThat(trim("а\u202F")).isEqualTo("а");
        assertThat(trim("\u202F\u202F\u202Fа\u202F\u202F")).isEqualTo("а");
    }

    @Test
    public void testNormalizeWhitespace() {
        assertThat(normalizeWhitespace("Превед\u202F\u202Fмир")).isEqualTo("Превед мир");
        assertThat(normalizeWhitespace("\u202FПревед\u202Fмир\u202F")).isEqualTo("Превед мир");
        assertThat(normalizeWhitespace("\u202F\u202F\u202F\u202FПревед\u202Fмир\u202F\u202F\u202F")).isEqualTo("Превед мир");
        assertThat(normalizeWhitespace("\u202F\u202FПревед\u202F\u202F\u202Fмир")).isEqualTo("Превед мир");
        assertThat(normalizeWhitespace("Превед мир\u202F\u202F\u202F")).isEqualTo("Превед мир");
        assertThat(normalizeWhitespace("\u202F\u202F\u202F")).isEmpty();
        assertThat(normalizeWhitespace("\u202F")).isEmpty();
        assertThat(normalizeWhitespace("")).isEmpty();
        assertThat(normalizeWhitespace("аb")).isEqualTo("аb");
        assertThat(normalizeWhitespace("\u202Fаb")).isEqualTo("аb");
        assertThat(normalizeWhitespace("\u202Fаb\u202F")).isEqualTo("аb");
        assertThat(normalizeWhitespace("аb\u202F")).isEqualTo("аb");
        assertThat(normalizeWhitespace("\u202F\u202F\u202Fаb\u202F\u202F")).isEqualTo("аb");
        assertThat(normalizeWhitespace("а")).isEqualTo("а");
        assertThat(normalizeWhitespace("\u202Fа")).isEqualTo("а");
        assertThat(normalizeWhitespace("\u202Fа\u202F")).isEqualTo("а");
        assertThat(normalizeWhitespace("а\u202F")).isEqualTo("а");
        assertThat(normalizeWhitespace("\u202F\u202F\u202Fа\u202F\u202F")).isEqualTo("а");
    }

    @Test
    public void testBetween() {
        assertThat(between("Haystack with a {<[NEEDLE]>} in the MEEDLE", "{<[", "]>}")).isEqualTo("NEEDLE");
        assertThat(between("Haystack with a {<[NEEDLE]>}", "{<[", "]>}")).isEqualTo("NEEDLE");
        assertThat(between("{<[NEEDLE]>} in the MEEDLE", "{<[", "]>}")).isEqualTo("NEEDLE");
        assertThat(between("{<[NEEDLE in the MEEDLE", "{<[", "]>}")).isEqualTo("NEEDLE in the MEEDLE");
        assertThat(between("Haystack with a NEEDLE]>}in the MEEDLE]>}", "{<[", "]>}")).isEmpty();
        assertThat(between("Haystack with a NEEDLE in the MEEDLE]>}", "{<[", "]>}")).isEmpty();
    }

    @Test
    public void testIndexOfAny() {
        ioaAssert("John Smith Married Mary Jane in March", array("Smith", "Mar"), "John ", "Smith");
        ioaAssert("John Smith Married Mary Jane in March", array("Mar"), "John Smith ", "Mar");
        ioaAssert("John Smith Married Mary Jane in March", array("Mary"), "John Smith Married ", "Mary");
        ioaAssert("John Smith Married Mary Jane in March", array("Mary", "Married", "March"), "John Smith ", "Married");
        ioaAssert("John Smith Married Mary Jane in March", array("March", "Married", "Mary"), "John Smith ", "Married");
        ioaAssert("John Smith Married Mary Jane in March", array("Feb"), null, null);
        ioaAssert("John Smith Married Mary Jane in March", array("Marchians are attacking!"), null, null);
        ioaAssert("John Smith Married Mary Jane in March", array("Marchians are attacking!", "Smith"), "John ", "Smith");
    }

    @Test
    public void testLongestWord() {
        assertThat(longestWordInCodepoints(null)).isNull();
        assertThat(longestWordInCodepoints("")).isEmpty();
        assertThat(longestWordInCodepoints("aaaa")).isEqualTo("aaaa");

        // \uD835\uDC00 is codepoint 1D400 "MATHEMATICAL BOLD CAPITAL A"

        assertThat(longestWordInCodepoints("\ud835\udc00 a")).isEqualTo("\ud835\udc00");
        assertThat(longestWordInCodepoints("\ud835\udc00a a")).isEqualTo("\ud835\udc00a");
        assertThat(longestWordInCodepoints("a\ud835\udc00 a")).isEqualTo("a\ud835\udc00");
        /*
         * Check this test for the broken pairs.
         */
        assertThat(longestWordInCodepoints("\ud835\ud835 a")).isEqualTo("\ud835\ud835");
        assertThat(longestWordInCodepoints("\ud835\udc00")).isEqualTo("\ud835\udc00");


        assertThat(longestWordInCodepoints("\ud835\udc00\ud835\udc00 aaa")).isEqualTo(
                "\ud835\udc00\ud835\udc00");
        assertThat(longestWordInChars("\ud835\udc00\ud835\udc00 aaa")).isEqualTo("aaa");

        assertThat(longestWordInCodepoints("bb aaaa dsd sd sds")).isEqualTo("aaaa");
        assertThat(longestWordInCodepoints("aaaa bb aaa dsd sd sds")).isEqualTo("aaaa");
        assertThat(longestWordInCodepoints("aaaa bb aaa       dsd sd sds")).isEqualTo("aaaa");
        assertThat(longestWordInCodepoints("bb aaa dsd sd sds aaaa")).isEqualTo("aaaa");
    }

    @Test
    public void testTranslit() {
        assertThat(translit(RUS_TO_ENTRANSLIT, "вода")).isEqualTo("voda");
        assertThat(translit(RUS_TO_ENTRANSLIT, "voda")).isEqualTo("voda");
        assertThat(translit(RUS_TO_ENTRANSLIT, "щи")).isEqualTo("schi");
        assertThat(translit(RUS_TO_ENTRANSLIT, null)).isNull();
        assertThat(translit(RUS_TO_ENTRANSLIT, "    ")).isEqualTo("    ");
    }

    private void ioaAssert(String haystack, String[] needles, String expectedSkippedPrefix, String expectedNeedle) {
        String[] holder = new String[1];
        int res = indexOfAny(haystack, needles, holder);
        assertThat(expectedSkippedPrefix == null
                ? -1
                : expectedSkippedPrefix.length()).isEqualTo(res);
        assertThat(holder[0]).isEqualTo(expectedNeedle);
    }

    @Test
    public void testReplace() {
        assertThat(replace(null, 'a', 'b', -1)).isNull();
        assertThat(replace("", 'a', 'b', -1)).isEmpty();
        assertThat(replace("any", 'a', 'b', 0)).isEqualTo("any");
        assertThat(replace("abaa", 'a', 'z', 0)).isEqualTo("abaa");
        assertThat(replace("abaa", 'a', 'z', 1)).isEqualTo("zbaa");
        assertThat(replace("abaa", 'a', 'z', 2)).isEqualTo("zbza");
        assertThat(replace("abaa", 'a', 'z', -1)).isEqualTo("zbzz");
    }

    @Test
    public void testLimit() {
        assertThat(limit("12345", 4)).isEqualTo("1234");
        assertThat(limit("12345", 5)).isEqualTo("12345");
        assertThat(limit("12345", 6)).isEqualTo("12345");
        assertThat(limit("12345", 10)).isEqualTo("12345");
        assertThat(limit("", 42)).isEmpty();
        assertThat(limit(null, 42)).isNull();
    }

    @Test
    public void testPercentOfCaps() {
        assertThat(percentOfCaps("ПРИВЕТ!111")).isEqualTo(100);
        assertThat(percentOfCaps("как дела? вот недавно приобрел авто...")).isZero();
    }

    @Test
    public void testUpFirstLetter() {
        assertThat(upFirstLetter("россия")).isEqualTo("Россия");
        assertThat(upFirstLetter("Москва")).isEqualTo("Москва");
        assertThat(upFirstLetter("м")).isEqualTo("М");
        assertThat(upFirstLetter("")).isEmpty();
    }

    @Test
    public void testIsOfDigists() {
        assertThat(isOfDigits("123")).isTrue();
        assertThat(isOfDigits("123g")).isFalse();
        assertThat(isOfDigits("")).isFalse();
        assertThat(isOfDigits(null)).isFalse();
        assertThat(isOfDigits("asbv")).isFalse();
    }

    @Test
    public void testReplaceWithFunction() {
        assertThat(replace("aeeec", Pattern.compile("e"), new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "b";
            }
        })).isEqualTo("abbbc");

        assertThat(replace("a123123123c", Pattern.compile("123"), new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "b";
            }
        })).isEqualTo("abbbc");

        assertThat(replace("abbbc", Pattern.compile("b"), new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "b";
            }
        })).isEqualTo("abbbc");

        assertThat(replace("bbb", Pattern.compile("b"), new Function<String, String>() {
            int count = 1;

            @Override
            public String apply(String s) {
                count = count * 10;
                return String.valueOf(count);
            }
        })).isEqualTo("101001000");

        assertThat(replace("bb-bb", Pattern.compile("b"), new Function<String, String>() {
            int count = 10000;

            @Override
            public String apply(String s) {
                count = count / 10;
                return String.valueOf(count);
            }
        })).isEqualTo("1000100-101");

        assertThat(replace("bb-bb", Pattern.compile("b\\-b"), new Function<String, String>() {
            int count = 10000;

            @Override
            public String apply(String s) {
                count = count / 10;
                return String.valueOf(count);
            }
        })).isEqualTo("b1000b");


    }

}
