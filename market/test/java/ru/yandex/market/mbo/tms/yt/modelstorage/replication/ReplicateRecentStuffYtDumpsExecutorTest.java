package ru.yandex.market.mbo.tms.yt.modelstorage.replication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.export.dumpstorage.YtSessionService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;

/**
 * @author apluhin
 * @created 9/21/21
 */
public class ReplicateRecentStuffYtDumpsExecutorTest {

    private ReplicateYtDumpsExecutor replicateYtDumpsExecutor;
    private ReplicationService replicationService;
    private Yt sourceYt;
    private Yt destinationYt;
    private String exportPath = "//home/market/stub/mbo/export";
    private String sessionId = "20210911_1657";
    private YtSessionService src;
    private YtSessionService dst;

    @Before
    public void setUp() throws Exception {
        replicationService = Mockito.mock(ReplicationService.class);
        sourceYt = Mockito.mock(Yt.class);
        destinationYt = Mockito.mock(Yt.class);
        replicateYtDumpsExecutor = new ReplicateYtDumpsExecutor(
            replicationService,
            sourceYt,
            destinationYt,
            exportPath
        );
        src = Mockito.mock(YtSessionService.class);
        dst = Mockito.mock(YtSessionService.class);
        ReflectionTestUtils.setField(replicateYtDumpsExecutor, "sourceSessionService", src);
        ReflectionTestUtils.setField(replicateYtDumpsExecutor, "destinationSessionService", dst);
    }

    @Test
    public void testReplicateCompletedSession() throws Exception {
        List<String> testFiles = generateTestFiles();
        Mockito.when(src.getRecentYtSession()).thenReturn(sessionId);
        Mockito.when(src.listFiles(Mockito.eq(
            YPath.simple(exportPath + "/" + sessionId)
        ))).thenReturn(testFiles);
        Mockito.when(replicationService.copySession(
            Mockito.anyString()
        )).thenReturn(true);

        replicateYtDumpsExecutor.doRealJob(null);

        Mockito.verify(replicationService, Mockito.times(1)).copySession(
            Mockito.eq(sessionId)
        );

        Mockito.verify(dst, Mockito.times(1))
            .markSessionAsSuccessfulReplicated(Mockito.eq(sessionId));
        Mockito.verify(dst, Mockito.times(1))
            .createRecentLink(Mockito.eq(sessionId));
        Mockito.verify(dst, Mockito.times(1))
            .markSessionAsSuccessfulReplicated(Mockito.eq(sessionId));
    }

    @Test
    public void testReplicateCompletedSessionWithFailedTransfer() throws Exception {
        List<String> testFiles = generateTestFiles();
        Mockito.when(src.getRecentYtSession()).thenReturn(sessionId);
        Mockito.when(src.listFiles(Mockito.eq(
            YPath.simple(exportPath + "/" + sessionId)
        ))).thenReturn(testFiles);
        Mockito.when(replicationService.copySession(
            Mockito.anyString()
        )).thenReturn(false);

        replicateYtDumpsExecutor.doRealJob(null);

        Mockito.verify(replicationService, Mockito.times(1)).copySession(
            Mockito.eq(sessionId)
        );

        Mockito.verify(dst, Mockito.times(0))
            .markSessionAsSuccessfulReplicated(Mockito.eq(sessionId));
        Mockito.verify(dst, Mockito.times(0))
            .createRecentLink(Mockito.eq(sessionId));
        Mockito.verify(dst, Mockito.times(0))
            .markSessionAsSuccessfulReplicated(Mockito.eq(sessionId));
    }

    @Test
    public void testSkipReplicatedRecentDump() throws Exception {
        Mockito.when(src.getRecentYtSession()).thenReturn(sessionId);
        Mockito.when(dst.getOptionalRecentYtSession()).thenReturn(Optional.of(sessionId));
        replicateYtDumpsExecutor.doRealJob(null);
        Mockito.verify(replicationService, Mockito.times(0)).copySession(Mockito.anyString());
        Mockito.verify(dst, Mockito.times(0))
            .createRecentLink(Mockito.anyString());
    }

    private List<String> generateTestFiles() {
        return Arrays.asList(
            generateTestFilePath("model"),
            generateTestFilePath("sku"),
            generateTestFilePath("all_models")
        );
    }

    private String generateTestFilePath(String fileName) {
        return String.format("%s/%s/%s", exportPath, sessionId, fileName);
    }

}
