package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.AbstractBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.category.mappings.CategoryMappingService;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.history.ChangeType;
import ru.yandex.market.mbo.history.EntityType;
import ru.yandex.market.mbo.history.model.CommonKeys;
import ru.yandex.market.mbo.history.model.EntityHistoryEntry;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class NonTaskFillModelCounterTest extends AbstractBillingLoaderTest {

    private NonTaskFillModelCounter counter = new NonTaskFillModelCounter();

    @Mock
    private TaskTracker taskTracker;
    @Mock
    private CategoryMappingService categoryMappingService;
    @Mock
    private GuruVendorsReader vendorsReader;
    @Mock
    private ModelStorageService modelStorageService;

    @Before
    public void before() {
        super.setUp();
        counter.setBillingOperations(billingOperations);
        counter.setTaskTracker(taskTracker);
        counter.setOracleAuditService(oracleAuditService);
        counter.setCategoryMappingService(categoryMappingService);
        counter.setVendorsReader(vendorsReader);
        counter.setModelStorageService(modelStorageService);
        when(modelStorageService.searchById(anyLong())).thenReturn(new CommonModel());
    }

    @Test
    public void testActionsBilledOk() {
        // Имитируем записи из старого аудита: модели проставили подпись оператора.
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(false),
                operatorSigned(true))
        );
        mockProcessHistoryLog(auditEntries);
        // Пусть модели НЕ фигурируют в активных тасках в tt_*
        letTaskTrackerReturnNothing();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 1L, AuditAction.EntityType.MODEL_GURU),
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    @Test
    public void testNoOperatorSignNotBilled() {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(false)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(true),
                operatorSigned(false))
        );
        mockProcessHistoryLog(auditEntries);
        // Пусть модели НЕ фигурируют в активных тасках в tt_*
        letTaskTrackerReturnNothing();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    @Test
    public void testModelNeverBilledTwice() {
        // Имитируем записи из старого аудита: моделям проставили подпись, причём одной дважды.
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(false),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 1L, // = первая модель
                operatorSigned(false),
                operatorSigned(true))
        );
        mockProcessHistoryLog(auditEntries);
        // Пусть модели НЕ фигурируют в активных тасках в tt_*
        letTaskTrackerReturnNothing();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 1L, AuditAction.EntityType.MODEL_GURU),
                // повторное изменение игнорируется.
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    @Test
    public void testModelsInActiveTasksNotBilled() {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(false),
                operatorSigned(true))
        );
        mockProcessHistoryLog(auditEntries);
        // Пусть модель №1 теперь фигурирует в активных тасках в tt_*
        letTaskTrackerReturn(1L, Collections.singletonList(
                new Task(666L, 1L, 0, Status.TASK_OPENED)));
        letTaskTrackerReturn(2L, Collections.singletonList(
                new Task(667L, 2L, 0, Status.INITIAL)));

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(1)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                // модель, фигурирующая в тасках не биллится
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    @Test
    public void testModelsInInactiveTasksBilledOk() {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(false),
                operatorSigned(true))
        );
        mockProcessHistoryLog(auditEntries);
        // Пусть модель №1 теперь фигурирует в неактивных тасках в tt_*
        letTaskTrackerReturn(1L, Collections.singletonList(new Task(666L, 1L, 0, Status.INITIAL)));

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 1L, AuditAction.EntityType.MODEL_GURU),
                createBilledEntityAction(PaidAction.FILL_MODEL_CARD, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    @Test
    public void testModelsNotBilledInTasksAllStatuses() {
        // Тест аналогичен предыдущим двум, но теперь проверим сразу все статусы активных тасок.
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 2L,
                operatorSigned(false),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 3L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 4L,
                operatorSigned(false),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 5L,
                createEmptySnapshot(),
                operatorSigned(true)),
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.UPDATED, 6L,
                operatorSigned(false),
                operatorSigned(true))
        );
        mockProcessHistoryLog(auditEntries);
        letTaskTrackerReturn(1L, Collections.singletonList(new Task(666L, 1L, 0, Status.TASK_OPENED)));
        letTaskTrackerReturn(2L, Collections.singletonList(new Task(667L, 2L, 0, Status.TASK_IN_PROCESS)));
        letTaskTrackerReturn(3L, Collections.singletonList(new Task(668L, 3L, 0, Status.TASK_FINISHED)));
        letTaskTrackerReturn(4L, Collections.singletonList(new Task(669L, 4L, 0, Status.TASK_ACCEPTED)));
        letTaskTrackerReturn(5L, Collections.singletonList(new Task(660L, 5L, 0, Status.TASK_ACCEPTED_WITHOUT_CHECK)));
        letTaskTrackerReturn(6L, Collections.singletonList(new Task(661L, 6L, 0, Status.TASK_RETURNED)));

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    @Test
    public void testNoGuruCategoryNotBilled() {
        Snapshot emptySnapshot = createEmptySnapshot();
        Snapshot signedSnapshot = operatorSigned(true);
        emptySnapshot.remove(CommonKeys.GURU_CATEGORY_ID.name());
        signedSnapshot.remove(CommonKeys.GURU_CATEGORY_ID.name());
        List<EntityHistoryEntry> auditEntries = Collections.singletonList(
            createHistoryEntry(EntityType.MODEL_PARAMETERES, ChangeType.ADDED, 1L, emptySnapshot, signedSnapshot)
        );
        mockProcessHistoryLog(auditEntries);
        letTaskTrackerReturnNothing();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    private void letTaskTrackerReturn(long expectedContentId, List<Task> desiredTasks) {
        when(taskTracker.getTaskByContentId(expectedContentId, TaskType.FILL_MODEL.getId())).thenReturn(desiredTasks);
    }

    private void letTaskTrackerReturnNothing() {
        when(taskTracker.getTaskByContentId(anyLong(), anyInt()))
            .thenReturn(Collections.emptyList());
    }

    private static Snapshot operatorSigned(Boolean value) {
        return createSnapshot(XslNames.OPERATOR_SIGN, value.toString());
    }
}
