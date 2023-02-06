package ru.yandex.market.mbo.synchronizer.export.storage;


import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.export.dumpstorage.DumpStorageInfoService;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;

/**
 * @author apluhin
 * @created 9/23/21
 */
public class DumpStorageServiceReplicationTest {

    private static final String DUMP_NAME = "stuff";
    private static final String SESSION_ID = "20190722_1301";

    private DumpStorageService dumpStorageService;
    private DumpStorageService storageService;
    private DumpStorageInfoService dumpStorageInfoService;
    private ReplicationService replicationService;
    private Path localFolder = new File("test").toPath();

    @Before
    public void setUp() throws Exception {
        storageService = Mockito.mock(DumpStorageService.class);
        dumpStorageInfoService = Mockito.mock(DumpStorageInfoService.class);
        replicationService = Mockito.mock(ReplicationService.class);
        dumpStorageService = new DumpStorageService(
            storageService,
            dumpStorageInfoService,
            null,
            replicationService,
            null
        );
    }

    @Test
    public void testIgnoreFailedReplicate() {
        Mockito.when(replicationService.copySession(Mockito.eq(SESSION_ID))).thenReturn(false);
        dumpStorageService.saveSession(
            "stuff",
            SESSION_ID,
            localFolder,
            true,
            Collections.emptySet()
        );
        Mockito.verify(
            dumpStorageInfoService, Mockito.times(1)
        ).updateSessionInfo(
            Mockito.eq(DUMP_NAME),
            Mockito.eq(SESSION_ID),
            Mockito.eq(MboDumpSessionStatus.OK),
            Mockito.eq(false)
        );
    }

    @Test(expected = Exception.class)
    public void testFailedWithS3DumpFailed() {
        Mockito.when(replicationService.copySession(Mockito.eq(SESSION_ID))).thenReturn(false);
        Mockito.doThrow(RuntimeException.class).when(storageService)
            .saveSession(Mockito.eq(DUMP_NAME), Mockito.eq(SESSION_ID), Mockito.eq(localFolder), Mockito.eq(true),
                Mockito.anySet()
            );
        dumpStorageService.saveSession(
            "stuff",
            SESSION_ID,
            localFolder,
            true,
            Collections.emptySet()
        );
        Mockito.verify(
            dumpStorageInfoService, Mockito.times(1)
        ).updateSessionInfo(
            Mockito.eq(DUMP_NAME),
            Mockito.eq(SESSION_ID),
            Mockito.eq(MboDumpSessionStatus.OK),
            Mockito.eq(false)
        );
    }

    @Test
    public void testSuccessReplicateAndDump() {
        Mockito.when(replicationService.copySession(Mockito.eq(SESSION_ID))).thenReturn(true);
        dumpStorageService.saveSession(
            "stuff",
            SESSION_ID,
            localFolder,
            true,
            Collections.emptySet()
        );
        Mockito.verify(
            dumpStorageInfoService, Mockito.times(1)
        ).updateSessionInfo(
            Mockito.eq(DUMP_NAME),
            Mockito.eq(SESSION_ID),
            Mockito.eq(MboDumpSessionStatus.OK),
            Mockito.eq(true)
        );
    }
}
