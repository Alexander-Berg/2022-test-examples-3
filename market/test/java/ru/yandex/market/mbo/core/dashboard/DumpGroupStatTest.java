package ru.yandex.market.mbo.core.dashboard;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData.Status;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("checkstyle:magicnumber")
public class DumpGroupStatTest {
    private Map<Type, DumpGroupStat.Stat> stat = DumpGroupStat.analyze(Arrays.asList(
            dump(1, Type.STUFF, Status.OK, 100, 0),
            build(dump(2, Type.STUFF, Status.FAILED, 110, 10), d -> d.setRestarted(true)),
            dump(3, Type.CLUSTERS, Status.FAILED, 100, 0),
            dump(4, Type.CLUSTERS, Status.FAILED, 100, 10),
            dump(5, Type.CLUSTERS, Status.OK, 100, 20),
            dump(6, Type.CMS, Status.OK, 100, 0),
            dump(7, Type.CMS, Status.OK_BUT_SOME_FAILED, 100, 10),
            dump(8, Type.FAST, Status.OK_BUT_SOME_FAILED, 100, 0)
    ));

    @Test
    public void testRestartsNotCounted() {
        assertEquals(0, stat.get(Type.STUFF).getConsecutiveFails().size());
    }

    @Test
    public void testFailsNotCountedInMedianTime() {
        assertEquals(100.0, stat.get(Type.STUFF).getBaseTime(), 0.0001);
    }

    @Test
    public void testConsecutiveFails() {
        assertEquals(0, stat.get(Type.CLUSTERS).getConsecutiveFails().size());
        assertEquals(1, stat.get(Type.CMS).getConsecutiveFails().size());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testLastSuccess() {
        assertEquals("session-6", stat.get(Type.CMS).getLastSuccess().getSessionName());
        assertNull(stat.get(Type.FAST).getLastSuccess());
    }

    @Test
    public void testStat() {
        assertEquals(2, stat.get(Type.CLUSTERS).getCountByStatus().get(Status.FAILED).intValue());
        assertEquals(1, stat.get(Type.CLUSTERS).getCountByStatus().get(Status.OK).intValue());
    }

    @Test
    public void testExportsCount() {
        assertEquals(3, stat.get(Type.CLUSTERS).getExportsCount());
    }

    private DumpGroupData dump(int n, Type type, Status status, int duration, long timeOffset) {
        return build(new DumpGroupData(), d -> {
            d.setType(type.getTmsExecutorName());
            d.setStatus(status);
            d.setDuration(duration);
            d.setSessionName("session-" + n);

            long finished = System.currentTimeMillis() + timeOffset;
            d.setCreatedTime(new Date(finished - duration));
            d.setFinishTime(new Date(finished));
        });
    }

    private <T> T build(T item, Consumer<T> builder) {
        builder.accept(item);
        return item;
    }
}
