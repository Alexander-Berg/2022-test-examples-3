package ru.yandex.antifraud;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.lua_context_manager.FileListsProvider;
import ru.yandex.antifraud.lua_context_manager.TimeRange;
import ru.yandex.test.util.TestBase;

public class InListTunerTest extends TestBase {
    public InListTunerTest() {
        super(false, 0L);
    }
    @Test
    public void testGetNameOnly() {
        Assert.assertEquals("1234", FileListsProvider.getFileNameOnly("1234"));
        Assert.assertEquals("1234", FileListsProvider.getFileNameOnly("1234.test"));
        Assert.assertEquals("1234.test", FileListsProvider.getFileNameOnly("1234.test.txt"));
    }

    @Test
    public void testReadList() throws Exception {
        final Instant now = Instant.parse("2020-11-17T21:03:55.00Z");
        final TimeRange after = new TimeRange(now, now.plus(1, ChronoUnit.HOURS));
        final TimeRange before = new TimeRange(now.minus(1, ChronoUnit.HOURS), now);
        final TimeRange forever = new TimeRange(now, Instant.MAX);

        final String data = "value\tcomment\tstatus\tcreatedat\texpireat\n" +
                "8.8.8.8\tban google forever\t\t1605647035000\t+1000000000-12-31T23:59:59.999999999Z\n" +
                "1.2.3.4\tsome comment\t\t\t1605647035000\t1605650635000\n" +
                "2.3.4.5\tsome по русски\t\t2020-11-17T20:03:55Z\t1605647035000\n";

        final Map<String, TimeRange> res = FileListsProvider.readList(new BufferedReader(new StringReader(data)));
        Assert.assertEquals(3, res.size());
        Assert.assertEquals(after, res.get("1.2.3.4"));
        Assert.assertEquals(before, res.get("2.3.4.5"));
        Assert.assertEquals(forever, res.get("8.8.8.8"));
    }
}
