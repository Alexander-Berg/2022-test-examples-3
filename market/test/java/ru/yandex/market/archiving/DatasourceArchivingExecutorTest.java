package ru.yandex.market.archiving;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.archiving.step.DatasourceArchivingStep;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DatasourceArchivingExecutor}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchivingExecutorTest extends FunctionalTest {
    @Autowired
    private DatasourceArchivingExecutor datasourceArchivingExecutor;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    Clock clock;

    @BeforeEach
    void init() {
        environmentService.addValue("datasource.archiving.limit", "10");
        environmentService.addValue("datasource.archiving.switch", "1");
    }

    @Test
    @DisplayName("Обход всех шагов, продвижение магазинов по фазам")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingExecutor/steps.before.csv",
            after = "csv/datasourceArchivingExecutor/steps.after.csv"
    )
    void testSteps() {
        // given
        when(clock.instant()).thenReturn(SingleFileCsvProducer.Functions.sysdate().toInstant());

        // when
        datasourceArchivingExecutor.doJob(null);

        // then
        verify(partnerNotificationClient, times(3)).sendNotification(any());
    }

    @Test
    @DisplayName("Удаление из очереди, если прошел модерацию")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingExecutor/del_from_queue.moderation.before.csv",
            after = "csv/datasourceArchivingExecutor/del_from_queue.moderation.after.csv"
    )
    void testDelFromQueueCuzModeration() {
        datasourceArchivingExecutor.doJob(null);
    }

    @Test
    @DisplayName("Удаление из очереди, если принудительно отключили удаление")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingExecutor/del_from_queue.disabled.before.csv",
            after = "csv/datasourceArchivingExecutor/del_from_queue.disabled.after.csv"
    )
    void testDelFromQueueCuzArchivingDisabled() {
        datasourceArchivingExecutor.doJob(null);
    }

    @Test
    @DisplayName("Выключатель: выключен")
    void testSwitch() {
        DatasourceArchivingStep stepMock = Mockito.mock(DatasourceArchivingStep.class);
        environmentService.setValue("datasource.archiving.switch", "0");
        DatasourceArchivingExecutor executor = new DatasourceArchivingExecutor(environmentService, List.of(stepMock));
        executor.doJob(null);
        verifyNoInteractions(stepMock);
    }

    @Test
    @DisplayName("Архивация магазина с FORCE_ARCHIVE")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingExecutor/force_archive.before.csv",
            after = "csv/datasourceArchivingExecutor/force_archive.after.csv"
    )
    void testForceArchive() {
        datasourceArchivingExecutor.doJob(null);
    }
}
