package ru.yandex.antifraud;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.lua_context_manager.UaTraitsTuner;
import ru.yandex.test.util.TestBase;


public class UaTraitsTest extends TestBase {
    public UaTraitsTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final UaTraitsTuner uaTraitsTuner = new UaTraitsTuner(resource("metrika/uatraits/data/browser" +
                ".xml"));
        final Map<String, String> parsed = uaTraitsTuner.detect("Mozilla/5.0 (X11; " +
                "Ubuntu; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0");
        Assert.assertEquals("Firefox", parsed.get("BrowserName"));
        Assert.assertEquals("Gecko", parsed.get("BrowserEngine"));
        Assert.assertEquals("true", parsed.get("x64"));
        Assert.assertEquals("Ubuntu", parsed.get("OSName"));
        Assert.assertEquals("false", parsed.get("isTouch"));
        Assert.assertEquals("false", parsed.get("isMobile"));
        Assert.assertEquals("89.0", parsed.get("BrowserVersion"));
        Assert.assertEquals("89.0", parsed.get("BrowserEngineVersion"));
        Assert.assertEquals("true", parsed.get("isBrowser"));
    }
}
