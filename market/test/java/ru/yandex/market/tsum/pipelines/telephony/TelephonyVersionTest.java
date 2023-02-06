package ru.yandex.market.tsum.pipelines.telephony;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class TelephonyVersionTest {
    private static final String PREFIX = "prefix";
    private static final int YEAR = 2020;
    private static final Month MONTH = Month.APRIL;
    private static final int QUARTER = 2;
    private static final Clock CLOCK = Clock.fixed(
        LocalDateTime.of(YEAR, MONTH, 1, 0, 0).toInstant(ZoneOffset.UTC),
        ZoneId.of("UTC"));

    static {
        TelephonyVersion.setClock(CLOCK);
    }

    @Test
    public void initialRelease() {
        TelephonyVersion version = new TelephonyVersion(PREFIX);
        Assert.assertEquals(version.getPrefix(), PREFIX);
        Assert.assertEquals(version.getYear(), YEAR);
        Assert.assertEquals(version.getQuarter(), QUARTER);
        Assert.assertEquals(version.getNumber(), 1);
        Assert.assertEquals(version.getHotFix(), 0);
    }

    @Test
    public void nextReleaseInTheSameQuarter() {
        TelephonyVersion prevVersion = new TelephonyVersion(PREFIX);
        TelephonyVersion version = prevVersion.nextRelease();
        Assert.assertEquals(version.getPrefix(), PREFIX);
        Assert.assertEquals(version.getYear(), YEAR);
        Assert.assertEquals(version.getQuarter(), QUARTER);
        Assert.assertEquals(version.getNumber(), 2);
        Assert.assertEquals(version.getHotFix(), 0);
    }

    @Test
    public void nextReleaseInTheNextQuarter() {
        TelephonyVersion prevVersion = new TelephonyVersion(PREFIX, YEAR, QUARTER - 1, 10, 0);
        TelephonyVersion version = prevVersion.nextRelease();
        Assert.assertEquals(version.getPrefix(), PREFIX);
        Assert.assertEquals(version.getYear(), YEAR);
        Assert.assertEquals(version.getQuarter(), QUARTER);
        Assert.assertEquals(version.getNumber(), 1);
        Assert.assertEquals(version.getHotFix(), 0);
    }

    @Test
    public void firstFix() {
        TelephonyVersion prevVersion = new TelephonyVersion(PREFIX, YEAR, QUARTER, 10, 0);
        TelephonyVersion version = prevVersion.nextHotFix();
        Assert.assertEquals(version.getPrefix(), PREFIX);
        Assert.assertEquals(version.getYear(), YEAR);
        Assert.assertEquals(version.getQuarter(), QUARTER);
        Assert.assertEquals(version.getNumber(), 10);
        Assert.assertEquals(version.getHotFix(), 1);
    }

    @Test
    public void nextFix() {
        TelephonyVersion prevVersion = new TelephonyVersion(PREFIX, YEAR, QUARTER, 10, 8);
        TelephonyVersion version = prevVersion.nextHotFix();
        Assert.assertEquals(version.getPrefix(), PREFIX);
        Assert.assertEquals(version.getYear(), YEAR);
        Assert.assertEquals(version.getQuarter(), QUARTER);
        Assert.assertEquals(version.getNumber(), 10);
        Assert.assertEquals(version.getHotFix(), 9);
    }

    @Test
    public void parseStringVersion() {
        TelephonyVersion version = TelephonyVersion.valueOf("prefix2020.2.5");
        Assert.assertEquals(version.getPrefix(), "prefix");
        Assert.assertEquals(version.getYear(), 2020);
        Assert.assertEquals(version.getQuarter(), 2);
        Assert.assertEquals(version.getNumber(), 5);
        Assert.assertEquals(version.getHotFix(), 0);
    }

    @Test
    public void parseStringVersionWithHotFix() {
        TelephonyVersion version = TelephonyVersion.valueOf("prefix2020.2.5.fix8");
        Assert.assertEquals(version.getPrefix(), "prefix");
        Assert.assertEquals(version.getYear(), 2020);
        Assert.assertEquals(version.getQuarter(), 2);
        Assert.assertEquals(version.getNumber(), 5);
        Assert.assertEquals(version.getHotFix(), 8);
    }

    @Test
    public void testToString() {
        String release = "prefix2020.2.5";
        String hotFix = "prefix2020.2.5.fix8";
        Assert.equals(release, TelephonyVersion.valueOf(release).toString());
        Assert.equals(hotFix, TelephonyVersion.valueOf(hotFix).toString());
    }

    @Test
    public void testComparator() {
        TelephonyVersion version = TelephonyVersion.valueOf("prefix2020.2.5.fix8");
        TelephonyVersion theSameVersion = new TelephonyVersion("prefix", 2020, 2, 5, 8);

        TelephonyVersion differInPrefix = TelephonyVersion.valueOf("sss2020.2.5.fix8");
        TelephonyVersion differInYear = TelephonyVersion.valueOf("prefix2019.2.5.fix8");
        TelephonyVersion differInQuarter = TelephonyVersion.valueOf("prefix2020.1.5.fix8");
        TelephonyVersion differInNumber = TelephonyVersion.valueOf("prefix2020.2.4.fix8");
        TelephonyVersion differInHotFix = TelephonyVersion.valueOf("prefix2020.2.5.fix7");

        Assert.equals(version, theSameVersion);
        Assert.notEquals(version, differInPrefix);
        Assert.notEquals(version, differInYear);
        Assert.notEquals(version, differInQuarter);
        Assert.notEquals(version, differInNumber);
        Assert.notEquals(version, differInHotFix);
    }

    @Test
    public void testTheSameRelease() {
        TelephonyVersion release = TelephonyVersion.valueOf("prefix2020.2.5");
        TelephonyVersion theSameRelease = new TelephonyVersion("prefix", 2020, 2, 5, 0);
        TelephonyVersion hotFix = TelephonyVersion.valueOf("prefix2020.2.5.fix8");
        TelephonyVersion anotherReleaseByPrefix = TelephonyVersion.valueOf("sss2020.2.5");
        TelephonyVersion anotherReleaseByYear = TelephonyVersion.valueOf("prefix2019.2.5");
        TelephonyVersion anotherReleaseByQuarter = TelephonyVersion.valueOf("prefix2020.1.5");
        TelephonyVersion anotherReleaseByNumber = TelephonyVersion.valueOf("prefix2020.2.4");

        Assert.isTrue(release.isTheSameRelease(theSameRelease));
        Assert.isTrue(release.isTheSameRelease(hotFix));
        Assert.isFalse(release.isTheSameRelease(anotherReleaseByPrefix));
        Assert.isFalse(release.isTheSameRelease(anotherReleaseByYear));
        Assert.isFalse(release.isTheSameRelease(anotherReleaseByQuarter));
        Assert.isFalse(release.isTheSameRelease(anotherReleaseByNumber));
    }

    @Test
    public void testIsValid() {
        Assert.isTrue(TelephonyVersion.isValid("pfx2020.2.555.fix777"));

        Assert.isFalse(TelephonyVersion.isValid("1pfx2020.2.555.fix777"), "PrefixWithDigits");
        Assert.isFalse(TelephonyVersion.isValid("pfx200.2.555.fix777"), "ShortYear");
        Assert.isFalse(TelephonyVersion.isValid("pfx20210.2.555.fix777"), "LongYear");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020.0.555.fix777"), "Quarter out of range");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020.5.555.fix777"), "Quarter out of range");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020..555.fix777"), "No quarter");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020.2.0.fix777"), "Wrong number");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020.2.555.fixs777"), "Wrong fix string");
        Assert.isFalse(TelephonyVersion.isValid("pfx2020.2.555fix777"), "No fix separator");
    }
}
