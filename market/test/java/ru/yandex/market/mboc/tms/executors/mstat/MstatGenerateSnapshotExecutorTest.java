package ru.yandex.market.mboc.tms.executors.mstat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.yt.util.tables.YtClientWrapper;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.services.mstat.yt.SnapShotSqlGenerator;
import ru.yandex.market.mboc.common.services.mstat.yt.StubLoader;
import ru.yandex.market.mboc.common.services.mstat_events.StepEventsSender;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.transfermanager.TransferManagerService;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.MoveNode;

import static ru.yandex.market.mboc.common.services.mstat.yt.StubLoader.DESTINATION_EXPORT_TABLE;
import static ru.yandex.market.mboc.common.services.mstat.yt.StubLoader.DESTINATION_EXPORT_TMP_TABLE;
import static ru.yandex.market.mboc.tms.executors.mstat.MstatGenerateSnapshotExecutor.LAST_SUCCESS_UPLOAD_DATE;

/**
 * @author apluhin
 * @created 3/1/21
 */
public class MstatGenerateSnapshotExecutorTest {

    MstatGenerateSnapshotExecutor executor;
    JdbcTemplate yqlHahnJdbcTemplate;
    List<SnapShotSqlGenerator> snapShotSqlGeneratorList;
    TransferManagerService transferManager;
    StorageKeyValueService keyValueService;


    YtClient hahnClient;

    @Before
    public void setUp() throws Exception {
        yqlHahnJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        snapShotSqlGeneratorList = Collections.singletonList(
            new StubLoader(null, null, null));
        transferManager = Mockito.mock(TransferManagerService.class);

        YtClientWrapper hahnClientWrapper = Mockito.mock(YtClientWrapper.class);
        hahnClient = Mockito.mock(YtClient.class);
        Mockito.when(hahnClientWrapper.getClient()).thenReturn(hahnClient);

        keyValueService = new StorageKeyValueServiceMock();

        executor = new MstatGenerateSnapshotExecutor(
            yqlHahnJdbcTemplate,
            snapShotSqlGeneratorList,
            transferManager,
            UnstableInit.simple(hahnClientWrapper),
            Mockito.mock(StepEventsSender.class),
            keyValueService
        );
    }

    @Test
    public void testCreateSnapShot() {
        Mockito.when(hahnClient.moveNode(Mockito.any())).thenReturn(CompletableFuture.completedFuture(GUID.create()));
        keyValueService.putValue(LAST_SUCCESS_UPLOAD_DATE, LocalDate.MIN);
        executor.execute();

        Mockito.verify(transferManager, Mockito.times(1)).transferFiles(
            Mockito.eq("hahn"),
            Mockito.eq("arnold"),
            Mockito.eq(Arrays.asList(DESTINATION_EXPORT_TABLE))
        );

        ArgumentCaptor<MoveNode> moveNodeArgumentCaptor = ArgumentCaptor.forClass(MoveNode.class);
        Mockito.verify(hahnClient, Mockito.times(1)).moveNode(moveNodeArgumentCaptor.capture());

        MoveNode moveNode = moveNodeArgumentCaptor.getValue();
        Assertions.assertThat(moveNode.getSource()).isEqualTo(YPath.simple(DESTINATION_EXPORT_TMP_TABLE));
        Assertions.assertThat(moveNode.getDestination()).isEqualTo(YPath.simple(DESTINATION_EXPORT_TABLE));

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(yqlHahnJdbcTemplate, Mockito.times(1))
            .update(stringArgumentCaptor.capture());
        Assertions.assertThat(stringArgumentCaptor.getValue())
            .isEqualTo("DROP TABLE `//test/tmp_table`; COMMIT; test sql");
    }

    @Test
    public void testSkipSuccessLoad() {
        keyValueService.putValue(LAST_SUCCESS_UPLOAD_DATE, LocalDate.now());
        executor.execute();
        Mockito.verify(hahnClient, Mockito.times(0)).moveNode(Mockito.any());
        Mockito.verify(yqlHahnJdbcTemplate, Mockito.times(0)).update(Mockito.anyString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyGenerator() {
        YtClientWrapper hahnClientWrapper = Mockito.mock(YtClientWrapper.class);
        executor = new MstatGenerateSnapshotExecutor(
            yqlHahnJdbcTemplate,
            Collections.emptyList(),
            transferManager,
            UnstableInit.simple(hahnClientWrapper),
            Mockito.mock(StepEventsSender.class),
            keyValueService
        );
    }
}
