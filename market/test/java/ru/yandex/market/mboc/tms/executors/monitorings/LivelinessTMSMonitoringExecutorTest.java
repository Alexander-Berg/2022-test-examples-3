package ru.yandex.market.mboc.tms.executors.monitorings;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


public class LivelinessTMSMonitoringExecutorTest {

    private JdbcTemplate jdbcTemplate;

    private ComplexMonitoring complexMonitoring;

    private LivelinessTMSMonitoringExecutor executor;

    @Before
    public void before() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        complexMonitoring = new ComplexMonitoring();
        executor = new LivelinessTMSMonitoringExecutor(jdbcTemplate, complexMonitoring);
    }

    @Test
    public void test_01() {
        when(jdbcTemplate.query(anyString(), any(LivelinessTMSInfoMapper.class))).thenReturn(new ArrayList<>());
        executor.execute();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
        assertTrue(complexMonitoring.getResult().getMessage().contains("no tms job has taken a lock"));
    }

    @Test
    public void test_02() {
        ArrayList<LivelinessTMSInfo> example = new ArrayList<>();
        example.add(new LivelinessTMSInfo()
            .setApplicationName("Default Application Name")
            .setLockType("tuple")
            .setQuery("SELECT * FROM abo.ba WHERE abo=\"ba\"")
            .setTxStart(6.66d));
        when(jdbcTemplate.query(anyString(), any(LivelinessTMSInfoMapper.class))).thenReturn(example);
        executor.execute();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
    }

    @Test
    public void test_03() {
        ArrayList<LivelinessTMSInfo> example = new ArrayList<>();
        example.add(new LivelinessTMSInfo()
            .setApplicationName("Default Application Name")
            .setLockType("tuple")
            .setQuery("SELECT * FROM abo.ba WHERE abo=\"ba\"")
            .setTxStart(666d));
        when(jdbcTemplate.query(anyString(), any(LivelinessTMSInfoMapper.class))).thenReturn(example);
        executor.execute();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult().getStatus());
        assertTrue(complexMonitoring.getResult().getMessage().contains("TMS job is stuck"));
    }


}
