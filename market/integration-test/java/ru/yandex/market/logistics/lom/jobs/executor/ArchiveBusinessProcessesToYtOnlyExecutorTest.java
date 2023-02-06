package ru.yandex.market.logistics.lom.jobs.executor;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Увоз бизнес-процессов только в YT")
@DatabaseSetup("/jobs/executor/businessProcessStateArchiver/before/businessProcessStates.xml")
@SuppressWarnings({"ConstantConditions", "SpringJavaInjectionPointsAutowiringInspection"})
class ArchiveBusinessProcessesToYtOnlyExecutorTest extends AbstractArchiveBusinessProcessStatesExecutorTest {

    @Autowired
    private ArchiveBusinessProcessesToYtOnlyExecutor archiveBusinessProcessesToYtOnlyExecutor;

    @Test
    @Override
    @JpaQueriesCount(26)
    @DisplayName("Размер батча и число батчей не указаны в internal_variable")
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/to_yt_only/before/6_processes_with_save_to_yt_flag.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/to_yt_only/after/6_archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noInternalVariablesSet() {
        archiveBusinessProcessesToYtOnlyExecutor.doJob(null);

        verifyWriteRows(
            buildMapNode(1L, 1600414656000L),
            buildMapNode(2L, 1603006656000L),
            buildMapNode(3L, 1603093056000L),
            buildMapNode(4L, 1608622656000L),
            buildMapNode(5L, 1601192256000L),
            buildMapNode(6L, 1602747456000L)
        );

        verify(hahnYt).tables();

        softly.assertThat(archiveBusinessProcessesToYtOnlyExecutor.saveToYtOnly()).isTrue();
        assertYdbNotContainsProcesses();
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=WARN\t"
                    + "format=plain\t"
                    + "payload=Already deleted ids [123456789]"
            );
    }

    @Test
    @DisplayName("Размер батча и чиcло процессов в батче строго определены - тест пустой")
    void withInternalVariables() {
    }

    @Test
    @Override
    @JpaQueriesCount(1)
    @DatabaseSetup(
        value = "/jobs/executor/businessProcessStateArchiver/to_yt_only/before/0_processes_save_to_yt_only.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/businessProcessStateArchiver/to_yt_only/after/businessProcessStates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Нет процессов для архивирования")
    void noProcessesToArchive() {
        archiveBusinessProcessesToYtOnlyExecutor.doJob(null);

        softly.assertThat(archiveBusinessProcessesToYtOnlyExecutor.saveToYtOnly()).isTrue();
        assertYdbNotContainsProcesses();
    }
}
