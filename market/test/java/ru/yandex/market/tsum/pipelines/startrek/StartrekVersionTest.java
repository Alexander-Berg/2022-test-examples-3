package ru.yandex.market.tsum.pipelines.startrek;

import java.util.Calendar;

import org.junit.Test;

import ru.yandex.market.tsum.pipelines.startrek.config.StartrekVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Sergey Filippov <rolenof@yandex-team.ru>
 */
public class StartrekVersionTest {
    @Test
    public void parse() {
        StartrekVersion v1 = StartrekVersion.getVersion("2019.01");
        assertEquals(2019, v1.getYear());
        assertEquals(1, v1.getMajor());
        assertFalse(v1.getMinor().isPresent());

        StartrekVersion v2 = StartrekVersion.getVersion("2019.02.3");
        assertEquals(2019, v2.getYear());
        assertEquals(2, v2.getMajor());
        assertEquals(3, v2.getMinor().getAsInt());

        StartrekVersion v3 = StartrekVersion.getVersion("2019.3");
        assertEquals(2019, v3.getYear());
        assertEquals(3, v3.getMajor());
        assertFalse(v3.getMinor().isPresent());

        StartrekVersion v4 = StartrekVersion.getVersion("2019");
        assertEquals(2019, v4.getYear());
        assertEquals(0, v4.getMajor());
        assertFalse(v3.getMinor().isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectParse() {
        StartrekVersion.getVersion("02");
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectParse2() {
        StartrekVersion.getVersion("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectParse3() {
        StartrekVersion.getVersion("");
    }

    @Test
    public void getNextMajor() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        StartrekVersion version = StartrekVersion.getVersion(currentYear + ".01");
        StartrekVersion nextMajorVersion = version.getNextMajorVersion();

        assertEquals(currentYear, nextMajorVersion.getYear());
        assertEquals(2, nextMajorVersion.getMajor());
        assertFalse(nextMajorVersion.getMinor().isPresent());
    }

    @Test
    public void getNextMinor() {
        StartrekVersion version = StartrekVersion.getVersion("2019.01");
        StartrekVersion nextMinorVersion = version.getNextMinorVersion();

        assertEquals(2019, nextMinorVersion.getYear());
        assertEquals(1, nextMinorVersion.getMajor());
        assertEquals(1, nextMinorVersion.getMinor().getAsInt());
    }

    @Test
    public void string() {
        StartrekVersion version = StartrekVersion.getVersion("2019.01");
        assertEquals("2019.01", version.getFormatted());

        StartrekVersion nextMinorVersion = version.getNextMinorVersion();
        assertEquals("2019.01.01", nextMinorVersion.getFormatted());
    }

    @Test
    public void compare() {
        StartrekVersion v1 = StartrekVersion.getVersion("2019.01");
        StartrekVersion v2 = StartrekVersion.getVersion("2019.02");
        StartrekVersion v3 = StartrekVersion.getVersion("2019.02.01");
        StartrekVersion v4 = StartrekVersion.getVersion("2018.02.01");
        StartrekVersion v5 = StartrekVersion.getVersion("2018.00");

        assertEquals(0, v1.compareTo(v1));
        assertEquals(-1, v1.compareTo(v2));
        assertEquals(-1, v1.compareTo(v3));
        assertEquals(1, v1.compareTo(v4));
        assertEquals(1, v1.compareTo(v5));

        assertEquals(1, v2.compareTo(v1));
        assertEquals(0, v2.compareTo(v2));
        assertEquals(-1, v2.compareTo(v3));
        assertEquals(1, v2.compareTo(v4));
        assertEquals(1, v2.compareTo(v5));

        assertEquals(1, v3.compareTo(v1));
        assertEquals(1, v3.compareTo(v2));
        assertEquals(0, v3.compareTo(v3));
        assertEquals(1, v3.compareTo(v4));
        assertEquals(1, v2.compareTo(v5));


        assertEquals(-1, v4.compareTo(v1));
        assertEquals(-1, v4.compareTo(v2));
        assertEquals(-1, v4.compareTo(v3));
        assertEquals(0, v4.compareTo(v4));
        assertEquals(1, v4.compareTo(v5));

        assertEquals(-1, v5.compareTo(v1));
        assertEquals(-1, v5.compareTo(v2));
        assertEquals(-1, v5.compareTo(v3));
        assertEquals(-1, v5.compareTo(v4));
        assertEquals(0, v5.compareTo(v5));
    }

    @Test
    public void newVersion() {
        final StartrekVersion version = StartrekVersion.getVersion();
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), version.getYear());
        assertEquals(0, version.getMajor());
        assertFalse(version.getMinor().isPresent());
    }
}
