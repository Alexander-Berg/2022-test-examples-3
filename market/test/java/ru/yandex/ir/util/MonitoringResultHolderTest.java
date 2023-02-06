package ru.yandex.ir.util;

import org.junit.Test;
import ru.yandex.market.http.MonitoringResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MonitoringResultHolderTest {
    @Test
    public void initialState() {
        assertEquals(
            "ok",
            new MonitoringResultHolder(MonitoringResult.OK).getMergedMonitoringResult().getMessage()
        );
        assertEquals(
            "initial unknown state",
            new MonitoringResultHolder().getMergedMonitoringResult().getMessage()
        );
        assertEquals(
            MonitoringResult.Status.WARNING,
            new MonitoringResultHolder().getMergedMonitoringResult().getStatus()
        );
    }

    @Test
    public void test() {
        MonitoringResultHolder holder = new MonitoringResultHolder(MonitoringResult.OK);

        holder.setMonitoringResult("okThenErr", new MonitoringResult(MonitoringResult.Status.OK, "i am ok"));
        String message1 = holder.getMergedMonitoringResult().getMessage();
        assertTrue("i am ok".equals(message1));

        MonitoringResult iAmWarn = new MonitoringResult(MonitoringResult.Status.WARNING, "i am warn");
        holder.setMonitoringResult("warnName", iAmWarn);
        assertEquals("warnName: i am warn", holder.getMergedMonitoringResult().getMessage());
        assertEquals(iAmWarn.getStatus(), holder.getMergedMonitoringResult().getStatus());

        MonitoringResult iAmErr = new MonitoringResult(MonitoringResult.Status.ERROR, "i am err");
        holder.setMonitoringResult("okThenErr", iAmErr);
        assertTrue(
            "warnName: i am warn; okThenErr: i am err".equals(holder.getMergedMonitoringResult().getMessage()) ||
                "okThenErr: i am err; warnName: i am warn".equals(holder.getMergedMonitoringResult().getMessage())
        );
        assertEquals(iAmErr.getStatus(), holder.getMergedMonitoringResult().getStatus());

        holder.setMonitoringResult("warnName", MonitoringResult.OK);
        holder.setMonitoringResult("okThenErr", MonitoringResult.OK);
        assertEquals(MonitoringResult.Status.OK, holder.getMergedMonitoringResult().getStatus());
        assertEquals("ok", holder.getMergedMonitoringResult().getMessage());
    }
}
