package ru.yandex.common.util.http.robots_txt;

import junit.framework.TestCase;

import ru.yandex.common.util.functional.Filter;

/**
 * @autor traff
 */
public class RobotsTxtParsingTest extends TestCase {
    public void testYandexSection() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow:/\n" +
                "User-agent:Yandex\n" +
                "Disallow:/notForYandex\n");
        assertTrue(rules.fits("www.example.com/test"));
        assertFalse(rules.fits("www.example.com/notForYandex"));
    }

    public void testUpperCaseAgent() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-Agent:*\n" +
                "Disallow:/\n" +
                "User-Agent:Yandex\n" +
                "Disallow:/notForYandex\n");
        assertTrue(rules.fits("www.example.com/test"));
        assertFalse(rules.fits("www.example.com/notForYandex"));
    }


    public void testAllSection() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow:/\n");
        assertFalse(rules.fits("www.example.com/test"));
        assertFalse(rules.fits("www.example.com/notForYandex"));
    }


    public void testAllowYandex() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow:\n" +
                "User-agent:Yandex\n" +
                "Disallow:/\n" +
                "Allow:/test");
        assertTrue(rules.fits("www.example.com/test"));
        assertFalse(rules.fits("www.example.com/notForYandex"));
    }

    public void testWildcards() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow: /*st\n");
        assertFalse(rules.fits("www.example.com/test"));
        assertTrue(rules.fits("www.example.com/notForYandex"));
    }

    public void testWildcards2() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow: *?\n" +
                "Disallow: +?\n" +
                "Disallow: *.\n"
        );
        assertTrue(rules.fits("www.example.com/"));
    }

    public void testWildcards3() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow: /some$\n"
        );
        assertFalse(rules.fits("www.example.com/some$abc"));
    }

    public void testComments() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "#Rules for all agents\n" +
                "Disallow:/\n");
        assertFalse(rules.fits("www.example.com/test # comment on the same line"));
        assertFalse(rules.fits("www.example.com/notForYandex"));
    }

    public void testAllowAll() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow:/\n" +
                "Allow:/test\n");
        assertTrue(rules.fits("www.example.com/test"));
        assertFalse(rules.fits("www.example.com/notForAll"));
    }


    public void testEmptyDisallow() {
        Filter<String> rules = RobotsTxtRulesRepository.parse(
            "User-agent:*\n" +
                "Disallow:\n");
        assertTrue(rules.fits("www.example.com/test"));
        assertTrue(rules.fits("www.example.com/notForYandex"));
    }
}
