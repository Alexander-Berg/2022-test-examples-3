package ru.yandex.market.mbo.tms.yt.modelstorage.replication;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.export.dumpstorage.YtSessionService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;

/**
 * @author apluhin
 * @created 9/22/21
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ReplicateYtHistoricDumpsExecutorTest {

    private ReplicateYtHistoricDumpsExecutor replicateYtHistoricDumpsExecutor;
    private ReplicationService replicationService;
    private Yt sourceYt;
    private Yt destinationYt;
    private String exportPath = "//home/market/stub/mbo/export";
    private List<String> sessions = Arrays.asList(
        "20210910_1308",
        "20210910_1607",
        "20210913_1325",
        "20210913_2154",
        "20210914_1514",
        "20210914_1931",
        "20210915_1453",
        "20210915_1538",
        "20210915_1800",
        "20210915_2107"
    );

    private YtSessionService src;
    private YtSessionService dst;


    @Before
    public void setUp() throws Exception {
        replicationService = Mockito.mock(ReplicationService.class);
        sourceYt = Mockito.mock(Yt.class);
        destinationYt = Mockito.mock(Yt.class);
        replicateYtHistoricDumpsExecutor = new ReplicateYtHistoricDumpsExecutor(
            replicationService,
            sourceYt,
            destinationYt,
            exportPath,
            5
        );
        src = Mockito.mock(YtSessionService.class);
        dst = Mockito.mock(YtSessionService.class);
        ReflectionTestUtils.setField(replicateYtHistoricDumpsExecutor, "sourceSessionService", src);
        ReflectionTestUtils.setField(replicateYtHistoricDumpsExecutor, "destinationSessionService", dst);
    }

    @Test
    public void testReplicatePartOfDumps() throws Exception {
        Mockito.when(src.getLastSuccessYtSessions(Mockito.eq(10L))).thenReturn(sessions);
        Mockito.when(dst.getSuccessReplicatedYtSessions()).thenReturn(sessions.subList(0, 7));
        Mockito.when(src.listFiles(Mockito.any())).thenAnswer(invocation ->
            generateTestFiles(((YPath) invocation.getArgument(0)).name())
        );
        Mockito.when(replicationService.copySession(
            Mockito.anyString()
        )).thenReturn(true);

        replicateYtHistoricDumpsExecutor.doRealJob(null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(replicationService, Mockito.times(3))
            .copySession(captor.capture());


        List<String> workSessions = Arrays.asList(
            "20210915_1538",
            "20210915_1800",
            "20210915_2107"
        );

        Assertions.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(workSessions);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(dst, Mockito.times(3))
            .markSessionAsSuccessfulReplicated(stringArgumentCaptor.capture());
    }

    @Test
    public void testSkipReplicateWithSyncedState() throws Exception {
        Mockito.when(src.getLastSuccessYtSessions(Mockito.eq(10L))).thenReturn(sessions);
        Mockito.when(dst.getSuccessReplicatedYtSessions()).thenReturn(sessions);
        Mockito.when(src.listFiles(Mockito.any())).thenAnswer(invocation ->
            generateTestFiles(((YPath) invocation.getArgument(0)).name())
        );

        replicateYtHistoricDumpsExecutor.doRealJob(null);


    }

    @Test
    public void testIgnoreFailedTransfer() throws Exception {
        Mockito.when(src.getLastSuccessYtSessions(Mockito.eq(10L))).thenReturn(sessions);
        Mockito.when(dst.getSuccessReplicatedYtSessions()).thenReturn(sessions.subList(0, 7));
        Mockito.when(src.listFiles(Mockito.any())).thenAnswer(invocation ->
            generateTestFiles(((YPath) invocation.getArgument(0)).name())
        );
        Mockito.when(replicationService.copySession(
            Mockito.anyString()
        )).thenReturn(false);

        replicateYtHistoricDumpsExecutor.doRealJob(null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(replicationService, Mockito.times(3))
            .copySession(captor.capture());

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(dst, Mockito.times(0))
            .markSessionAsSuccessfulReplicated(stringArgumentCaptor.capture());
    }

    private List<String> generateTestFiles(String sessionId) {
        return Arrays.asList(
            generateTestFilePath("model", sessionId),
            generateTestFilePath("sku", sessionId),
            generateTestFilePath("all_models", sessionId)
        );
    }

    private String generateTestFilePath(String fileName, String sessionId) {
        return String.format("%s/%s/%s", exportPath, sessionId, fileName);
    }


}
