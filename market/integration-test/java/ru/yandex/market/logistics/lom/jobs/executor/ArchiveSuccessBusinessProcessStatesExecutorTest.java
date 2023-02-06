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
@DisplayName("Увоз бизнес-процессов в успешных терминальных статусах")
@DatabaseSetup("/jobs/executor/businessProcessStateArchiver/before/businessProcessStates.xml")
@SuppressWarnings({"ConstantConditions", "SpringJavaInjectionPointsAutowiringInspection"})
class ArchiveSuccessBusinessProcessStatesExecutorTest extends AbstractArchiveBusinessProcessStatesExecutorTest {

    @Autowired
    private ArchiveSuccessBusinessProcessStatesExecutor successBusinessProcessStatesExecutor;

    @Test
    @Override
    @JpaQueriesCount(27)
    @DisplayName("Размер батча и число батчей не указаны в internal_variable")
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/before/business_process_states_updated.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/after/5_success_archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noInternalVariablesSet() {
        successBusinessProcessStatesExecutor.doJob(null);

        verifyWriteRows(buildMapNode(1L, 1607050800000L));
        verifyWriteRows(buildMapNode(2L, 1607049635000L));
        verifyWriteRows(buildMapNode(3L, 1607046035000L));
        verifyWriteRows(buildMapNode(5L, 1607053200000L));
        verifyWriteRows(buildMapNode(6L, 1607050800000L));

        verify(hahnYt, times(5)).tables();

        softly.assertThat(successBusinessProcessStatesExecutor.saveToYtOnly()).isFalse();
        assertYdbContainsBusinessProcessWithEntities(
            List.of(
                ydbProcess(1L, Instant.parse("2020-12-04T03:00:00Z")),
                ydbProcess(2L, Instant.parse("2020-12-04T02:40:35Z")),
                ydbProcess(3L, Instant.parse("2020-12-04T01:40:35Z")),
                ydbProcess(5L, Instant.parse("2020-12-04T03:40:00Z")),
                ydbProcess(6L, Instant.parse("2020-12-04T03:00:00Z"))
            ),
            NOW_TIME
        );
    }

    @Test
    @Override
    @JpaQueriesCount(11)
    @DisplayName("С заданными размером батча и числом процессов в батче")
    @DatabaseSetup(
        value = {
            "/jobs/executor/businessProcessStateArchiver/before/business_process_states_updated.xml",
            "/jobs/executor/businessProcessStateArchiver/before/batch_size_and_batch_count.xml",
        },
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/after/3_success_archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void withInternalVariables() {
        successBusinessProcessStatesExecutor.doJob(null);

        verifyWriteRows(
            buildMapNode(1L, 1607050800000L),
            buildMapNode(2L, 1607049635000L),
            buildMapNode(3L, 1607046035000L)
        );

        verify(hahnYt).tables();

        softly.assertThat(successBusinessProcessStatesExecutor.saveToYtOnly()).isFalse();
        assertYdbContainsBusinessProcessWithEntities(
            List.of(
                ydbProcess(1L, Instant.parse("2020-12-04T03:00:00Z")),
                ydbProcess(2L, Instant.parse("2020-12-04T02:40:35Z")),
                ydbProcess(3L, Instant.parse("2020-12-04T01:40:35Z"))
            ),
            NOW_TIME
        );
    }

    @Test
    @Override
    @JpaQueriesCount(3)
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/before/no_suitable_updated_processes.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Нет процессов для архивирования")
    void noProcessesToArchive() {
        successBusinessProcessStatesExecutor.doJob(null);

        softly.assertThat(successBusinessProcessStatesExecutor.saveToYtOnly()).isFalse();
        assertYdbNotContainsProcesses();
    }
}
