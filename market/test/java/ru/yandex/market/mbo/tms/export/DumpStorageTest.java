package ru.yandex.market.mbo.tms.export;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.dumpstorage.DumpStorageInfoService;
import ru.yandex.market.mbo.synchronizer.export.storage.DumpStorageService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 24.06.2019
 */
public class DumpStorageTest {

    private DumpStorageService dumpStorageService;
    private DumpStorageInfoService infoService;
    private ReplicationService replicationService;

    @Before
    public void setUp() {
        infoService = mock(DumpStorageInfoService.class);
        replicationService = mock(ReplicationService.class);
        dumpStorageService = new DumpStorageService(null, infoService,
            null, replicationService, null);
    }

    @Test
    public void testSwitchToFrozen() {
        when(infoService.getFrozenSession(anyString())).thenReturn("1122_33412");
        when(infoService.getRecentSessionId(anyString())).thenReturn("1522_33412");
        assertEquals(dumpStorageService.getGoodSessionToSwitch("qqq"), "1122_33412");
    }

    @Test
    public void testSwitchToRecent() {
        when(infoService.getFrozenSession(anyString())).thenReturn("");
        when(infoService.getRecentSessionId(anyString())).thenReturn("1522_33412");
        assertEquals(dumpStorageService.getGoodSessionToSwitch("qqq"), "1522_33412");
    }

}
