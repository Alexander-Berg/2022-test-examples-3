package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Архивирование 'старых' бизнес-процессов")
@SuppressWarnings("ConstantConditions")
class ArchiveOldBusinessProcessStatesExecutorTest extends AbstractArchiveBusinessProcessStatesExecutorTest {

    @Autowired
    private ArchiveOldBusinessProcessStatesExecutor oldBusinessProcessStatesArchiverExecutor;

    @Test
    @Override
    @JpaQueriesCount(27)
    @DisplayName("Размер батча и число батчей не указаны в internal_variable")
    @DatabaseSetup("/jobs/executor/businessProcessStateArchiver/before/businessProcessStates.xml")
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/before/old_processes_ignore_save_to_yt_flag.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/after/5_archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noInternalVariablesSet() {
        oldBusinessProcessStatesArchiverExecutor.doJob(null);

        verifyWriteRows(buildMapNode(1L, 1600414656000L));
        verifyWriteRows(buildMapNode(2L, 1603006656000L));
        verifyWriteRows(buildMapNode(3L, 1603093056000L));
        verifyWriteRows(buildMapNode(5L, 1601192256000L));
        verifyWriteRows(buildMapNode(6L, 1602747456000L));

        verify(hahnYt, times(5)).tables();

        softly.assertThat(oldBusinessProcessStatesArchiverExecutor.saveToYtOnly()).isFalse();
        assertYdbContainsBusinessProcessWithEntities(
            List.of(
                ydbProcess(1L, Instant.parse("2020-09-18T07:37:36Z")),
                ydbProcess(2L, Instant.parse("2020-10-18T07:37:36Z")),
                ydbProcess(3L, Instant.parse("2020-10-19T07:37:36Z")),
                ydbProcess(5L, Instant.parse("2020-09-27T07:37:36Z")),
                ydbProcess(6L, Instant.parse("2020-10-15T07:37:36Z"))
            ),
            NOW_TIME
        );
    }

    @Test
    @Override
    @JpaQueriesCount(11)
    @DisplayName("С заданными размером батча и числом процессов в батче")
    @DatabaseSetup({
        "/jobs/executor/businessProcessStateArchiver/before/businessProcessStates.xml",
        "/jobs/executor/businessProcessStateArchiver/before/batch_size_and_batch_count.xml",
    })
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/before/old_processes_ignore_save_to_yt_flag.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/after/3_archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void withInternalVariables() {
        oldBusinessProcessStatesArchiverExecutor.doJob(null);

        verifyWriteRows(
            buildMapNode(1L, 1600414656000L),
            buildMapNode(2L, 1603006656000L),
            buildMapNode(3L, 1603093056000L)
        );

        verify(hahnYt).tables();

        softly.assertThat(oldBusinessProcessStatesArchiverExecutor.saveToYtOnly()).isFalse();
        assertYdbContainsBusinessProcessWithEntities(
            List.of(
                ydbProcess(1L, Instant.parse("2020-09-18T07:37:36Z")),
                ydbProcess(2L, Instant.parse("2020-10-18T07:37:36Z")),
                ydbProcess(3L, Instant.parse("2020-10-19T07:37:36Z"))
            ),
            NOW_TIME
        );
    }

    @Test
    @Override
    @JpaQueriesCount(3)
    @DisplayName("Нет процессов для архивирования")
    void noProcessesToArchive() {
        oldBusinessProcessStatesArchiverExecutor.doJob(null);

        noProcessesToArchiveLogged();

        softly.assertThat(oldBusinessProcessStatesArchiverExecutor.saveToYtOnly()).isFalse();
        assertYdbNotContainsProcesses();
    }
}
