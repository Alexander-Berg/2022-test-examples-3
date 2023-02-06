package ru.yandex.market.logistics.lom.jobs.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Джоба, выполняющая combine_chunks")
class CombineYtChunksExecutorTest extends AbstractContextualTest {

    private static final String EXPECTED_TABLE_PATH =
        "//home/market/testing/delivery/logistics_lom/business_process_state_archive";

    @Autowired
    private CombineYtChunksExecutor combineYtChunksExecutor;

    @Autowired
    protected Yt hahnYt;

    @Autowired
    private YtTables ytTables;

    @Autowired
    private YtOperations ytOperations;

    @Autowired
    private Operation ytOperation;

    @BeforeEach
    void setUp() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(hahnYt.operations()).thenReturn(ytOperations);

        YPath path = YPath.simple(EXPECTED_TABLE_PATH);
        when(ytOperations.mergeAndGetOp(
            MergeSpec.builder()
                .addInputTable(path)
                .setOutputTable(path)
                .setCombineChunks(true)
                .build()
        ))
            .thenReturn(ytOperation);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ytTables, hahnYt, ytOperation, ytOperations);
    }

    @Test
    @DisplayName("Вызов только combine_chunks")
    void combineChunks() {
        combineYtChunksExecutor.doJob(mock(JobExecutionContext.class));
        verifyCombineChunks();
    }

    private void verifyCombineChunks() {
        YPath path = YPath.simple(EXPECTED_TABLE_PATH);
        verify(ytOperations).mergeAndGetOp(safeRefEq(
            MergeSpec.builder()
                .addInputTable(path)
                .setOutputTable(path)
                .setCombineChunks(true)
                .build()
        ));
        verify(ytOperation).await();
        verify(hahnYt).operations();
    }
}
