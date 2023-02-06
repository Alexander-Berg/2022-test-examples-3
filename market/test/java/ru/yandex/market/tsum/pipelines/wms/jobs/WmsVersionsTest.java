package ru.yandex.market.tsum.pipelines.wms.jobs;

import junit.framework.TestCase;
import org.joda.time.LocalDate;

import ru.yandex.market.tsum.pipelines.wms.jobs.WmsVersions.WmsVersion;

public class WmsVersionsTest extends TestCase {

    public void testNextRegularVersionNameResetCounterOnNextMonth() {
        assertEquals("2021.5.1",
            WmsVersion.fromVersionName("2021.4.1").nextRegularVersion(LocalDate.parse("2021-05-10")).toString());
    }

    public void testNextRegularVersionNameIncreaseCounterOnSameMonth() {
        assertEquals("2021.4.2",
            WmsVersion.fromVersionName("2021.4.1").nextRegularVersion(LocalDate.parse("2021-04-10")).toString());
    }

    public void testNextHotfixVersionNameAddSuffixIfNotHotfix() {
        assertEquals("2021.4.1.hotfix1",
            WmsVersion.fromVersionName("2021.4.1").nextHotfixVersion().toString());
    }

    public void testNextHotfixVersionNameIncSuffixIfHotfix() {
        assertEquals("2021.4.1.hotfix2",
            WmsVersion.fromVersionName("2021.4.1.hotfix1").nextHotfixVersion().toString());
    }

    public void testNextHotfixVersionNameHotfixBranchRelevance() {
        assertTrue(versionBelongsToHotfixSequence("2021.4.1", "2021.4.1.hotfix1"));
        assertTrue(versionBelongsToHotfixSequence("2021.4.1", "2021.4.1.hotfix2"));
        assertTrue(versionBelongsToHotfixSequence("2021.4.1.hotfix1", "2021.4.1.hotfix2"));
        assertTrue(versionBelongsToHotfixSequence("2021.4.1", "2021.4.1"));

        assertFalse(versionBelongsToHotfixSequence("2021.4.1", "2021.4.2"));
        assertFalse(versionBelongsToHotfixSequence("2021.4.1", "2021.4.10.hotfix1"));
        assertFalse(versionBelongsToHotfixSequence("2021.4.10", "2021.4.1.hotfix1"));
        assertFalse(versionBelongsToHotfixSequence("2021.4.1.hotfix1", "2021.4.10.hotfix2"));
        assertFalse(versionBelongsToHotfixSequence("2021.4.10.hotfix1", "2021.4.1.hotfix2"));
    }

    private static boolean versionBelongsToHotfixSequence(String baseVersionName, String testedVersionName) {
        WmsVersion baseVersion = WmsVersion.fromVersionName(baseVersionName);
        WmsVersion testedVersion = WmsVersion.fromVersionName(testedVersionName);
        return testedVersion.belongsToHotfixSequence(baseVersion);
    }
}
