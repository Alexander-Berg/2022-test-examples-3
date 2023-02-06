package ru.yandex.market.tsum.utils;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 15.07.2019
 */
public class UnixGlobUtilsTest {

    @Test
    public void toRegexPatternTest() {
        // basic
        Assert.assertTrue(matchGlob("foo.html", "foo.html"));
        Assert.assertFalse(matchGlob("foo.html", "foo.htm"));
        Assert.assertFalse(matchGlob("foo.html", "bar.html"));

        // match zero or more characters
        Assert.assertTrue(matchGlob("foo.html", "f*"));
        Assert.assertTrue(matchGlob("foo.html", "*.html"));
        Assert.assertTrue(matchGlob("foo.html", "foo.html*"));
        Assert.assertTrue(matchGlob("foo.html", "*foo.html"));
        Assert.assertTrue(matchGlob("foo.html", "*foo.html*"));
        Assert.assertFalse(matchGlob("foo.html", "*.htm"));
        Assert.assertFalse(matchGlob("foo.html", "f.*"));

        // match one character
        Assert.assertTrue(matchGlob("foo.html", "?oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "??o.html"));
        Assert.assertTrue(matchGlob("foo.html", "???.html"));
        Assert.assertTrue(matchGlob("foo.html", "???.htm?"));
        Assert.assertFalse(matchGlob("foo.html", "foo.???"));

        // group of subpatterns
        Assert.assertTrue(matchGlob("foo.html", "foo{.html,.class}"));
        Assert.assertTrue(matchGlob("foo.html", "foo.{class,html}"));
        Assert.assertFalse(matchGlob("foo.html", "foo{.htm,.class}"));

        // bracket expressions
        Assert.assertTrue(matchGlob("foo.html", "[f]oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "[e-g]oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "[abcde-g]oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "[abcdefx-z]oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "[!a]oo.html"));
        Assert.assertTrue(matchGlob("foo.html", "[!a-e]oo.html"));
        Assert.assertTrue(matchGlob("foo-bar", "foo[-a-z]bar"));
        Assert.assertTrue(matchGlob("foo.html", "foo[!-]html"));

        // groups of subpattern with bracket expressions
        Assert.assertTrue(matchGlob("foo.html", "[f]oo.{[h]tml,class}"));
        Assert.assertTrue(matchGlob("foo.html", "foo.{[a-z]tml,class}"));
        Assert.assertTrue(matchGlob("foo.html", "foo.{[!a-e]tml,.class}"));

        // assume special characters are allowed in file names
        Assert.assertTrue(matchGlob("{foo}.html", "\\{foo*"));
        Assert.assertTrue(matchGlob("{foo}.html", "*\\}.html"));
        Assert.assertTrue(matchGlob("[foo].html", "\\[foo*"));
        Assert.assertTrue(matchGlob("[foo].html", "*\\].html"));

        // errors
        Assert.assertTrue(isWrongPattern("*[a--z]"));
        Assert.assertTrue(isWrongPattern("*[a--]"));
        Assert.assertTrue(isWrongPattern("*[a-z"));
        Assert.assertTrue(isWrongPattern("*{class,java"));
        Assert.assertTrue(isWrongPattern("*.{class,{.java}}"));
        Assert.assertTrue(isWrongPattern("*.html\\"));

        // unix specific
        Assert.assertTrue(matchGlob("/tmp/foo", "/tmp/*"));
        Assert.assertTrue(matchGlob("/tmp/foo/bar", "/tmp/**"));
        Assert.assertTrue(matchGlob("myfile?", "myfile\\?"));
        Assert.assertTrue(matchGlob("one\\two", "one\\\\two"));
        Assert.assertTrue(matchGlob("one*two", "one\\*two"));
    }

    private boolean matchGlob(String path, String globPatter) {
        String regexpPattern = UnixGlobUtils.toRegexPattern(globPatter);
        return Pattern.compile(regexpPattern).matcher(path).find();
    }

    private boolean isWrongPattern(String globPatter) {
        try {
            String regexpPattern = UnixGlobUtils.toRegexPattern(globPatter);
            Pattern.compile(regexpPattern);
            return false;
        } catch (Exception ignore) {
        }
        return true;
    }

}
