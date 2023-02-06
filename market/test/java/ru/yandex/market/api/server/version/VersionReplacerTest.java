package ru.yandex.market.api.server.version;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.Context;

/**
 * @author dimkarp93
 */
public class VersionReplacerTest extends UnitTestBase {
    private final VersionReplacer replacer = new VersionReplacer();

    @Test
    public void replaceV2() {
        doPathTest("V2/beru", "{version}/beru", "V2", Version.V2_0_0);
        doMethodTest("V2/beru", "{version}/beru", "V2", VersionReplacer.Pattern.V2);
    }

    @Test
    public void noReplaceV2() {
        doPathTest("{version}/beru", "{version}/beru", "V2", Version.V3_0_0);
        doMethodTest("UNKNOWN_VERSION_STUB/beru", "{version}/beru", "V2", VersionReplacer.Pattern.V3);
    }

    @Test
    public void replaceV3() {
        doPathTest("V3/beru", "{version3}/beru", "V3", Version.V3_0_0);
        doMethodTest("V3/beru", "{version3}/beru", "V3", VersionReplacer.Pattern.V3);
    }

    @Test
    public void noReplaceV3() {
        doPathTest("{version3}/beru", "{version3}/beru", "V3", Version.V2_0_0);
        doMethodTest("UNKNOWN_VERSION_STUB/beru", "{version3}/beru", "V3", VersionReplacer.Pattern.V2);
    }

    public void doPathTest(String expected, String source, String filler, Version version) {
        Assert.assertEquals(expected, replacer.pathReplace(source, filler, makeContext(version)));
    }

    public void doMethodTest(String expected, String source, String filler, VersionReplacer.Pattern pattern) {
        Assert.assertEquals(expected, replacer.methodReplace(source, filler, pattern));

    }

    public static Context makeContext(Version version) {
        Context context = new Context("123");
        context.setVersion(version);
        return context;
    }
}
