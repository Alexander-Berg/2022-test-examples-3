package ru.yandex.common.util.profile.statdump;

import junit.framework.TestCase;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Date: Feb 17, 2009
 * Time: 1:01:44 PM
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class ProfilerTest extends TestCase {

    public void testAddResult() throws Throwable {

        final NumberFormat formatter = new DecimalFormat("#0.00");
        StatDumpProfiler profiler = new StatDumpProfiler();
        profiler.addResult("test","test1", System.currentTimeMillis(), 33);
        profiler.addResult("test","test1", System.currentTimeMillis(), 34);
        profiler.addResult("test","test2", System.currentTimeMillis(), 33);

        assertEquals( formatter.format(3.00), profiler.getKey2formattedStat().get("test.count"));
        assertEquals(formatter.format(33.33), profiler.getKey2formattedStat().get("test.avg"));
        assertEquals(formatter.format(34.00), profiler.getKey2formattedStat().get("test.max"));

        assertEquals(formatter.format(2.00), profiler.getKey2formattedStat().get("test.test1.count"));
        assertEquals(formatter.format(33.50), profiler.getKey2formattedStat().get("test.test1.avg"));
        assertEquals(formatter.format(34.00), profiler.getKey2formattedStat().get("test.test1.max"));

    }
}
