package ru.yandex.market.api.util;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.ApiMatchers;

/**
 * @author dimkarp93
 */
public class ApiStringsTest {
    @Test
    public void getBytesNullSafe() {
        Assert.assertNull(ApiStrings.getBytes(null));
    }

    @Test
    public void getBytesEmpty() {
        Assert.assertEquals(
            0,
            ApiStrings.getBytes("").length
        );
    }

    @Test
    public void getBytesCorrect() throws Exception {
        String input = "abc";

        Assert.assertTrue(
            Arrays.equals(
                input.getBytes("UTF-8"),
                ApiStrings.getBytes(input)
            )
        );
    }

    @Test
    public void removeIncorrectHtmlTag() {
        Assert.assertEquals(
            "Hello",
            ApiStrings.removeHtmlTags("Hello </b> </body>")
        );
    }

    @Test
    public void removeAllTags() {
        Assert.assertEquals(
            "Hello world!",
            ApiStrings.removeHtmlTags("<form><table><tr>Hello world!</tr></table></form>")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void containsNTimesTimesIncorrect() {
        ApiStrings.containsStringNTimes("abc", "def", -1);
    }

    @Test
    public void containsNTimesStringIncorrect() {
        Assert.assertFalse(ApiStrings.containsStringNTimes(null, "def", 1));
        Assert.assertFalse(ApiStrings.containsStringNTimes("", "def", 1));
    }

    @Test
    public void containsNTimesSubstringIncorrect() {
        Assert.assertFalse(ApiStrings.containsStringNTimes("abc", null, 1));
        Assert.assertFalse(ApiStrings.containsStringNTimes("abc", "", 1));
    }

    @Test
    public void containsNTimesNoFound() {
        Assert.assertTrue(ApiStrings.containsStringNTimes("abc", "abd", 0));
        Assert.assertFalse(ApiStrings.containsStringNTimes("abc", "abc", 0));
        Assert.assertFalse(ApiStrings.containsStringNTimes("xabcd", "abc", 0));
    }

    @Test
    public void containsNTimesOneTime() {
        Assert.assertTrue(ApiStrings.containsStringNTimes("abc", "abc", 1));
        Assert.assertTrue(ApiStrings.containsStringNTimes("xabcd", "abc", 1));
        Assert.assertTrue(ApiStrings.containsStringNTimes("babab", "bab", 1));
    }

    @Test
    public void containsNTimesManyTime() {
        Assert.assertTrue(ApiStrings.containsStringNTimes("bbb", "b", 3));
        Assert.assertTrue(ApiStrings.containsStringNTimes("tesxxtes", "tes", 2));
    }

    @Test
    public void kvParserTest() {
        Assert.assertTrue(ApiStrings.kvParse(null).isEmpty());
        Assert.assertTrue(ApiStrings.kvParse("").isEmpty());

        Assert.assertThat(ApiStrings.kvParse("a=1").entrySet(), Matchers.contains(ApiMatchers.entry("a", "1")));
        Assert.assertThat(ApiStrings.kvParse("a=1;").entrySet(), Matchers.contains(ApiMatchers.entry("a", "1")));
        Assert.assertThat(ApiStrings.kvParse("a=").entrySet(), Matchers.contains(ApiMatchers.entry("a", "")));
        Assert.assertThat(ApiStrings.kvParse("a=;").entrySet(), Matchers.contains(ApiMatchers.entry("a", "")));
        Assert.assertThat(ApiStrings.kvParse("a").entrySet(), Matchers.contains(ApiMatchers.entry("a", "")));
        Assert.assertThat(ApiStrings.kvParse("a;").entrySet(), Matchers.contains(ApiMatchers.entry("a", "")));

        Assert.assertThat(ApiStrings.kvParse("a=1;b=2").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", "1"), ApiMatchers.entry("b", "2")));
        Assert.assertThat(ApiStrings.kvParse("a=1;b=2").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", "1"), ApiMatchers.entry("b", "2")));
        Assert.assertThat(ApiStrings.kvParse("a=1;b").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", "1"), ApiMatchers.entry("b", "")));
        Assert.assertThat(ApiStrings.kvParse("a=;b=2").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", ""), ApiMatchers.entry("b", "2")));
        Assert.assertThat(ApiStrings.kvParse("a=;b=2").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", ""), ApiMatchers.entry("b", "2")));
        Assert.assertThat(ApiStrings.kvParse("a;b=").entrySet(),
                Matchers.containsInAnyOrder(ApiMatchers.entry("a", ""), ApiMatchers.entry("b", "")));
    }
}
