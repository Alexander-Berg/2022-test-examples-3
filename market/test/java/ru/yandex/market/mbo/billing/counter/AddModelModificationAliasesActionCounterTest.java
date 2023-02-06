package ru.yandex.market.mbo.billing.counter;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.history.ChangeType;
import ru.yandex.market.mbo.history.EntityType;
import ru.yandex.market.mbo.history.model.EntityHistoryEntry;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.history.model.ValueType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Заметка для разработчика:
 * Это параметризованный тест. Каждый метод запускается два раза - для модели и для модификации. Соответственно,
 * тип сущности и ожидаемый результат передаются параметрами тестирующих методов EntityType modelType и
 * PaidAction desiredAction. Генерятся эти параметры методами {@link #modelOrModificationCreate()} и
 * {@link #modelOrModificationToTask()}.
 * @author anmalysh
 */
@RunWith(JUnitParamsRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class AddModelModificationAliasesActionCounterTest extends AbstractBillingLoaderTest {

    private static final List<EntityHistoryEntry> MATCHED = Collections.singletonList(
        createHistoryEntry(EntityType.LOG_OBJECT, ChangeType.UPDATED, DEFAULT_ACTION_ID, //entityId в логах = sourceId
            createEmptySnapshot(), createEmptySnapshot())
    );

    private AddModelModificationAliasesActionCounter counter;

    @Before
    public void setUp() {
        initMocks(this);
        counter = new AddModelModificationAliasesActionCounter();
        counter.setOracleAuditService(oracleAuditService);
        counter.setBillingOperations(billingOperations);
        super.setUp();
    }

    /**
     * Проверяем, что факт создания модели отловился по любому действию типа ADDED из старого аудита.
     */
    @Test
    @Parameters(method = "modelOrModificationCreate")
    public void testModelCreationBilledByAnyParam(EntityType modelType, PaidAction desiredAction) {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(modelType, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                createSnapshot(XslNames.VENDOR, "66606660666")),
            createHistoryEntry(modelType, ChangeType.ADDED, 2L,
                createEmptySnapshot(),
                createSnapshot(XslNames.URL, "www.pics.com/10-of-10-jackals.jpeg")),
            createHistoryEntry(modelType, ChangeType.UPDATED, 3L, // тип не ADDED, не побиллится
                createSnapshot(XslNames.VENDOR, "123456789"),
                createSnapshot(XslNames.VENDOR, "987654321"))
        );
        mockProcessHistoryLogWithTable(auditEntries);
        mockNothingMatchedInHistoryLog();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(desiredAction, 1L, AuditAction.EntityType.MODEL_GURU),
                createBilledEntityAction(desiredAction, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    /**
     * Проверим, что создание модели с алиасами, не влияющими на матчинг биллится как создание модели.
     */
    @Test
    @Parameters(method = "modelOrModificationCreate")
    public void testNotMatchedAliasesBilledAsCreateModel(EntityType modelType, PaidAction desiredAction) {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(modelType, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                createSnapshot(XslNames.ALIASES, "mighty", "powerful")),
            createHistoryEntry(modelType, ChangeType.ADDED, 2L,
                createEmptySnapshot(),
                createSnapshot(XslNames.ALIASES, "strong"))
        );
        mockProcessHistoryLogWithTable(auditEntries);
        mockNothingMatchedInHistoryLog();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(desiredAction, 1L, AuditAction.EntityType.MODEL_GURU),
                createBilledEntityAction(desiredAction, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    /**
     * Проверяем, что создание модели с алиасами, повлиявшими на матчинг биллится как таска.
     */
    @Test
    @Parameters(method = "modelOrModificationToTask")
    public void testMatchedAliasesBilledAsModelTask(EntityType modelType, PaidAction desiredAction) {
        List<EntityHistoryEntry> auditEntries = Arrays.asList(
            createHistoryEntry(modelType, ChangeType.ADDED, 1L,
                createEmptySnapshot(),
                createSnapshot(XslNames.ALIASES, "mighty", "powerful")),
            createHistoryEntry(modelType, ChangeType.ADDED, 2L,
                createEmptySnapshot(),
                createSnapshot(XslNames.ALIASES, "strong"))
        );
        mockProcessHistoryLogWithTable(auditEntries);
        mockGetHistoryLog(MATCHED, ImmutableSet.of("powerful", "strong"));

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(2)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(desiredAction, 1L, AuditAction.EntityType.MODEL_GURU),
                createBilledEntityAction(desiredAction, 2L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    /**
     * Усложняем. Теперь предположим, что из аудита одновременно пришла информация как об алиасах, так и о других
     * параметрах, заполненных в модели при создании. В итоге ожидаем обилливание как "Создание модели", ибо на
     * матчинг алиасы не повлияли.
     */
    @Test
    @Parameters(method = "modelOrModificationCreate")
    public void testNotMatchedAliasesAndParamsCombined(EntityType modelType, PaidAction desiredAction) {
        Snapshot thoroughSnapshot = createSnapshot(XslNames.ALIASES, "mighty", "powerful");
        thoroughSnapshot.put(XslNames.VENDOR, ValueType.STRING, "Славные хоббитцы инкорпорейтед");
        thoroughSnapshot.put(XslNames.URL, ValueType.STRING, "www.www.com");
        List<EntityHistoryEntry> auditEntries = Collections.singletonList(
            createHistoryEntry(modelType, ChangeType.ADDED, 1L, createEmptySnapshot(), thoroughSnapshot)
        );
        mockProcessHistoryLogWithTable(auditEntries);
        mockNothingMatchedInHistoryLog();

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(1)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
                createBilledEntityAction(desiredAction, 1L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    /**
     * Аналогично предыдущему, но теперь сделаем так, чтобы алиасы фигурировали в матчинге. А это значит, что модель
     * побиллится как таска, а остальные параметры не сыграют роли вообще.
     */
    @Test
    @Parameters(method = "modelOrModificationToTask")
    public void testMatchedAliasesAndParamsCombined(EntityType modelType, PaidAction desiredAction) {
        Snapshot thoroughSnapshot = createSnapshot(XslNames.ALIASES, "mighty", "powerful");
        thoroughSnapshot.put(XslNames.VENDOR, ValueType.STRING, "Славные хоббитцы инкорпорейтед");
        thoroughSnapshot.put(XslNames.URL, ValueType.STRING, "www.www.com");
        List<EntityHistoryEntry> auditEntries = Collections.singletonList(
            createHistoryEntry(modelType, ChangeType.ADDED, 1L, createEmptySnapshot(), thoroughSnapshot)
        );
        mockProcessHistoryLogWithTable(auditEntries);
        mockGetHistoryLog(MATCHED, ImmutableSet.of("powerful"));

        counter.doLoad(INTERVAL, tarifProvider);
        verify(operationsUpdater, times(1)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            createBilledEntityAction(desiredAction, 1L, AuditAction.EntityType.MODEL_GURU)
        );
    }

    private void mockNothingMatchedInHistoryLog() {
        doReturn(Collections.emptyList()).when(oracleAuditService).getHistoryLog(
                any(EntityType.class),
                any(Date.class),
                any(Date.class),
                anyBoolean(),
                anyString(),
                anyString(),
                any());
    }

    @SuppressWarnings("unused")
    private Object[] modelOrModificationCreate() {
        return new Object[] {
            new Object[] {
                EntityType.MODEL, PaidAction.ADD_MODEL
            },
            new Object[] {
                EntityType.MODIFICATION, PaidAction.ADD_MODIFICATION_CARD
            }
        };
    }

    @SuppressWarnings("unused")
    private Object[] modelOrModificationToTask() {
        return new Object[] {
            new Object[] {
                EntityType.MODEL, PaidAction.TO_TASK
            },
            new Object[] {
                EntityType.MODIFICATION, PaidAction.TO_TASK_MODIFICATION
            }
        };
    }
}
