package ru.yandex.market.api.controller.jackson;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.server.version.Version;

/**
 * Created by yntv on 10/7/16.
 */
public class VersionedPredicateTest {

    @Test
    public void fixedVersion() {
        Assert.assertTrue(Version.test(Version.V1_0_0, "1.0.0"));
        Assert.assertTrue(Version.test(Version.V2_0_0, "1.0.0"));
    }

    @Test
    public void insideLowAndHiBorders() {
        Assert.assertTrue(Version.test(Version.V2_0_1, "1.0.0-2.0.2"));
        Assert.assertTrue(Version.test(Version.V2_0_1, "2.0.1-2.0.2"));
        Assert.assertFalse(Version.test(Version.V2_0_1, "1.0.0-2.0.1"));
    }

    @Test
    public void outsideInterval() {
        Assert.assertFalse(Version.test(Version.V2_0_2, "1.0.0-2.0.0"));
        Assert.assertFalse(Version.test(Version.V1_0_0, "2.0.0-2.0.2"));
    }

    @Test
    public void undefinedHiBorder() {
        Assert.assertTrue(Version.test(Version.V2_0_2, "1.0.0-*"));
        Assert.assertTrue(Version.test(Version.V2_0_2, "2.0.2-*"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void emptyLowBorder() {
        Version notUsedVersion = Version.V2_0_2;
        Assert.assertTrue(Version.test(notUsedVersion, "-2.0.0"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void emptyHiBorder() {
        Version notUsedVersion = Version.V1_0_0;
        Assert.assertTrue(Version.test(notUsedVersion, "2.0.0-"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void undefinedLowBorder() {
        Assert.assertTrue(Version.test(Version.V2_0_1, "*-2.0.2"));
    }
}
