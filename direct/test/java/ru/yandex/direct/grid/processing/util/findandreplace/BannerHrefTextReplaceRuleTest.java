package ru.yandex.direct.grid.processing.util.findandreplace;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextHrefPart;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextInstruction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class BannerHrefTextReplaceRuleTest {

    @Parameterized.Parameter
    public Set<GdFindAndReplaceHrefTextHrefPart> hrefParts;

    @Parameterized.Parameter(1)
    public String search;

    @Parameterized.Parameter(2)
    public String replace;

    @Parameterized.Parameter(3)
    public String input;

    @Parameterized.Parameter(4)
    public String expected;

    @Parameterized.Parameters(name = "{0}; {1} -> {2}; {3} -> {4}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // I. Find and replace substring

                //replace only in domain
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                ".net", ".com", null, null},
                //replace only in domain
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        ".net", ".com", "http://domain.net/abc/def", "http://domain.com/abc/def"},
                //replace only in path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "/abc", "/aaa", "http://domain.net/abc/def", "http://domain.net/aaa/def"},
                //replace in domain and path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "domain", "newnew", "http://domain.net/abc/domain1", "http://newnew.net/abc/newnew1"},
                //replace in domain and path, href contains parameters
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "domain", "newnew", "http://domain.net/abc/domain1?domain=123",
                        "http://newnew.net/abc/newnew1?domain=123"},
                //replace in domain and path, href contains fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "domain", "newnew", "http://domain.net/abc/domain1#domain",
                        "http://newnew.net/abc/newnew1#domain"},
                //replace in domain and path, href contains parameters and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "domain", "newnew", "http://domain.net/abc/domain1?domain=123#domain",
                        "http://newnew.net/abc/newnew1?domain=123#domain"},
                //intersecting replace in path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "aa", "b", "http://domain.net/aaa", "http://domain.net/ba"},
                //replace with protocol
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "http://domain.net/aaa", "https://new.ru/bbb", "http://domain.net/aaa?param1=value",
                        "https://new.ru/bbb?param1=value"},
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "http", "https", "http://domain.net/aaa?param1=value",
                        "https://domain.net/aaa?param1=value"},

                //replace only in query
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "abc=123", "def=456", "http://domain.net?abc=123", "http://domain.net?def=456"},
                //replace only in query for empty href
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "abc=123", "def=456", null, null},
                //replace only in fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "#fr1", "#fr2", "http://domain.net#fr1", "http://domain.net#fr2"},
                //replace in query and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "par", "query", "http://domain.net?par1=abc#par", "http://domain.net?query1=abc#query"},
                //replace in query and fragment, href contains path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "par", "query", "http://domain.net/path/to?par1=abc#par",
                        "http://domain.net/path/to?query1=abc#query"},
                //fragment before query, replace in query
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "par", "query", "http://domain.net/path/to#par?par1=abc",
                        "http://domain.net/path/to#query?query1=abc"},
                //no query and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "path", "123", "http://domain.net/path/to", "http://domain.net/path/to"},

                //replace between path and param
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "to?abc", "to?a", "http://domain.net/path/to?abc=123&abcde=456#fr",
                        "http://domain.net/path/to?a=123&abcde=456#fr"},
                //replace in domain and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "domain", "another", "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "http://another.net/path/to?abc=123&abcde=456#another3"},
                //replace in domain and fragment for empty url
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "domain", "another", null, null},
                //unsuccessful replace in all modes
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "smt", "111", "http://domain.net/abc/def", "http://domain.net/abc/def"},
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "smt", "222", "http://domain.net?abc=123", "http://domain.net?abc=123"},
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "aaa", "bbb", "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "http://domain.net/path/to?abc=123&abcde=456#domain3"},
                //change protocol
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "http", "https", "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "https://domain.net/path/to?abc=123&abcde=456#domain3"},
                //change protocol, domain, path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "http://domain.net/path/to", "https://new.ru/another/path",
                        "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "https://new.ru/another/path?abc=123&abcde=456#domain3"},
                //change full href
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "https://new.ru/another/path#newfragment",
                        "http://domain.net/path/to?abc=123&abcde=456#domain3",
                        "https://new.ru/another/path#newfragment"},

                // II. Replace string totally

                // replace with star regexp
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        "*", "http://ya.ru", "http://domain.net/abc/def", "http://ya.ru"},

                // replace when search is null

                // replace with domain
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        null, "http://ya.ru", "http://domain.net/abc/def", "http://ya.ru"},
                // replace with path, href contains params
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        null, "http://ya.ru/abc", "http://domain.net/abc/domain1?domain=123",
                        "http://ya.ru/abc?domain=123"},
                // replace with path, href contains fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        null, "http://ya.ru/abc", "http://domain.net/abc/domain1#domain",
                        "http://ya.ru/abc#domain"},
                // replace with path, href contains fragment and params
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH),
                        null, "http://ya.ru/abc", "http://domain.net/abc/domain1?domain=123#domain",
                        "http://ya.ru/abc?domain=123#domain"},

                //replace only in query
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "?def=456", "http://domain.net?abc=123", "http://domain.net?def=456"},
                //replace only in fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "#fr2", "http://domain.net#fr1", "http://domain.net#fr2"},
                //replace in query and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "?par2=cba#foo", "http://domain.net/abc?par1=abc#par",
                        "http://domain.net/abc?par2=cba#foo"},
                //replace in query and fragment, href contains path
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "?foo=bar", "http://domain.net/path/to?par1=abc#par",
                        "http://domain.net/path/to?foo=bar"},
                //fragment before query, replace in query
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "#param", "http://domain.net/path/to#par?par1=abc",
                        "http://domain.net/path/to#param"},
                //no query and fragment
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "?123", "http://domain.net/path/to", "http://domain.net/path/to?123"},

                //replace all
                {ImmutableSet.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                        GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT),
                        null, "http://ya.ru?foo=bar", "http://domain.net/path/to?abc=123&abcde=456#fr",
                        "http://ya.ru?foo=bar"},
        });
    }

    @Test
    public void testReplacing() {
        GdFindAndReplaceHrefTextInstruction instruction = new GdFindAndReplaceHrefTextInstruction()
                .withHrefParts(hrefParts)
                .withSearch(search)
                .withReplace(replace);
        String result = new BannerHrefTextReplaceRule(instruction).apply(input);
        assertThat(result, is(expected));
    }
}
