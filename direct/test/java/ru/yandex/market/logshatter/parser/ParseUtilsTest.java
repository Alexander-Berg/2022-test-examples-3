package ru.yandex.market.logshatter.parser;

import com.google.common.primitives.UnsignedLong;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author zoom
 */
public class ParseUtilsTest extends Assert {

    @Test
    public void shouldSplitPatternToLevelsCorrectlyWhenMethodIsNotDefined() {
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("root"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("/root"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("root/"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("/root/"));
    }

    @Test
    public void shouldSplitPatternToLevelsCorrectlyWhenMethodIsDefined() {
        assertArrayEquals(new String[]{"root", "GET"}, ParseUtils.splitPatternToLevels("GET:root"));
        assertArrayEquals(new String[]{"root", "POST"}, ParseUtils.splitPatternToLevels("POST:/root"));
        assertArrayEquals(new String[]{"root", "METHOD"}, ParseUtils.splitPatternToLevels("METHOD:root/"));
        assertArrayEquals(new String[]{"root", "HEAD"}, ParseUtils.splitPatternToLevels("HEAD:/root/"));
    }

    @Test
    public void testUrlParamStringExtraction() {
        assertEquals("module=Contacts&action=DetailView", ParseUtils.extractParamsSubstring("/index.php?module=Contacts&action=DetailView"));
        assertEquals("module=Contacts&action=DetailView", ParseUtils.extractParamsSubstring("/index.php#target?module=Contacts&action=DetailView"));
        assertEquals("module=Contacts", ParseUtils.extractParamsSubstring("/index.php?module=Contacts#target&action=DetailView"));
        assertEquals("module=Contacts&action=DetailView", ParseUtils.extractParamsSubstring("/index.php?module=Contacts&action=DetailView#target"));
    }

    @Test
    public void testSipHash() {
        Assert.assertEquals("2202906307356721367", ParseUtils.sipHash64("").toString());
        Assert.assertEquals("17179166182469397862", ParseUtils.sipHash64("yBIQkuh-QY_w1rH-YY5AtQ").toString());
        Assert.assertEquals("7469197971809657900", ParseUtils.sipHash64("zaOn1OchUnP7U9PF68rfMw").toString());
        Assert.assertEquals("2711373773322570989", ParseUtils.sipHash64("CnVJYkbescuVEx0iNRA8AA").toString());
        Assert.assertEquals("156869119347133483", ParseUtils.sipHash64("-1").toString());
    }

    @Test
    public void testStringParamExtraction() {
        assertEquals("2", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "id"));
        assertEquals("2", ParseUtils.extractStringParam("/index.php?id=2&campaign_id=1&_user_id=3&euid=4", "id"));
        assertEquals("3", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "_user_id"));
        assertEquals("4", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "euid"));

    }

    @Test
    public void testExtractPath() {
        assertEquals("", ParseUtils.cutQueryStringAndFragment(""));
        assertEquals("/", ParseUtils.cutQueryStringAndFragment("/"));
        assertEquals("/a", ParseUtils.cutQueryStringAndFragment("/a"));
        assertEquals("/a/b", ParseUtils.cutQueryStringAndFragment("/a/b"));
        assertEquals("/a/b", ParseUtils.cutQueryStringAndFragment("/a/b?c=d"));
    }

    @Test
    public void testIpv4ToLong() {
        assertEquals(0, ParseUtils.ipv4ToLong(""));
        assertEquals(0, ParseUtils.ipv4ToLong("12.12.12"));
        assertEquals(3232235777L, ParseUtils.ipv4ToLong("192.168.1.1"));
        assertEquals(4294967295L, ParseUtils.ipv4ToLong("255.255.255.255"));
        assertEquals(16843009L, ParseUtils.ipv4ToLong("1.1.1.1"));
        assertEquals(16777216L, ParseUtils.ipv4ToLong("1.0.0.0"));
        assertEquals(0, ParseUtils.ipv4ToLong("266.0.0.0"));
        assertEquals(0, ParseUtils.ipv4ToLong("2a0c:5247:e17e:3bde:f26b:5e88:468d:371a"));
    }

    @Test
    public void testParseLong() {
        assertEquals(Long.valueOf(0), ParseUtils.parseLong("a", 0L));
        assertEquals(Long.valueOf(11111111), ParseUtils.parseLong("11111111", 0L));
        assertEquals(Long.valueOf(0), ParseUtils.parseLong("9863946471548151861", 0L));
    }

    @Test
    public void testParseUnsignedLong() {
        assertEquals(UnsignedLong.valueOf(0), ParseUtils.parseUnsignedLong("a", UnsignedLong.valueOf(0)));
        assertEquals(UnsignedLong.valueOf(0), ParseUtils.parseUnsignedLong("a"));
        assertEquals(UnsignedLong.valueOf(11111111), ParseUtils.parseUnsignedLong("11111111", UnsignedLong.valueOf(0)));
        assertEquals(UnsignedLong.valueOf("9863946471548151861"), ParseUtils.parseUnsignedLong("9863946471548151861", UnsignedLong.valueOf(0)));
    }
}
