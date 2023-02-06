package ru.yandex.market.tsum.pipelines.wms.jobs;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.tsum.clients.juggler.v2.JugglerApiV2Client;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.wms.resources.JugglerState;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsDowntimeConfig;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class WmsSetDowntimeJugglerJobTest {

    private final TestJobContext jobContext = new TestJobContext();
    private final JugglerApiV2Client jugglerApiV2Client = createMockClient();
    private final WmsSetDowntimeJugglerJob downtimeJugglerJob = createMockJob();

    private JugglerApiV2Client createMockClient() {
        JugglerApiV2Client mock = mock(JugglerApiV2Client.class);
        when(mock.setDowntimes(eq("wms-test"), eq(""), eq("wms-namespace"),
            anyList(), eq("Test description"), eq(1))).thenReturn("");
        when(mock.getDowntimeIds(eq(""), eq("wms-test"), eq(""), eq("wms-namespace"),
            anyList())).thenReturn(List.of("TEST_DOWNTIME_ID"));
        when(mock.removeDowntimesByIds(List.of("TEST_DOWNTIME_ID"))).thenReturn(Collections.emptyList());
        return mock;
    }

    private WmsSetDowntimeJugglerJob createMockJob() {
        WmsSetDowntimeJugglerJob job = new WmsSetDowntimeJugglerJob(jugglerApiV2Client);
        job.setDowntimeConfig(enabled());
        job.setStartReleaseInfo(new ReleaseInfo(
            new FixVersion(2021, "0000"),
            "TEST123"
        ));
        return job;
    }

    private WmsDowntimeConfig enabled() {
        return new WmsDowntimeConfig("wms-test", "wms-namespace", Collections.emptyList(),
            1, "Test description", JugglerState.ENABLED);
    }

    private WmsDowntimeConfig disabled() {
        return new WmsDowntimeConfig("wms-test", "wms-namespace", Collections.emptyList(),
            1, "Test description", JugglerState.DISABLED);
    }

    @Test
    public void shouldSetDowntimeWhenEnabled() throws Exception {
        downtimeJugglerJob.setDowntimeConfig(enabled());
        downtimeJugglerJob.execute(jobContext);

        verify(jugglerApiV2Client, only()).setDowntimes(eq("wms-test"), anyString(), eq("wms-namespace"),
            anyList(), anyString(), eq(1L));
    }

    @Test
    public void shouldUnsetDowntimeWhenDisabled() throws Exception {
        downtimeJugglerJob.setDowntimeConfig(disabled());
        downtimeJugglerJob.execute(jobContext);

        verify(jugglerApiV2Client, times(1)).getDowntimeIds(anyString(), eq("wms-test"),
            anyString(), eq("wms-namespace"), anyList());
        verify(jugglerApiV2Client, times(1)).removeDowntimesByIds(anyList());
        verify(jugglerApiV2Client, never()).setDowntimes(anyString(), anyString(), anyString(),
            anyList(), anyString(), anyLong());
    }

}
