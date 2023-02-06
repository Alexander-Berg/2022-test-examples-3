package ru.yandex.market.mbo.billing.counter;

import com.google.common.collect.ImmutableMap;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoBase;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.core.audit.OracleAuditService;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.history.ChangeType;
import ru.yandex.market.mbo.history.EntityType;
import ru.yandex.market.mbo.history.model.CommonKeys;
import ru.yandex.market.mbo.history.model.EntityHistoryEntry;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.utils.BatchUpdater;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class AbstractBillingLoaderTest {

    protected static final Calendar ACTIONS_DATE = Calendar.getInstance();
    protected static final Calendar FROM_DATE = Calendar.getInstance();
    protected static final Calendar TO_DATE = Calendar.getInstance();
    protected static final Pair<Calendar, Calendar> INTERVAL = new Pair<>(FROM_DATE, TO_DATE);
    protected static final BigDecimal PRICE = new BigDecimal(2);
    protected static final long DEFAULT_ACTION_ID = -1L;

    static {
        FROM_DATE.setTime(ACTIONS_DATE.getTime());
        FROM_DATE.add(Calendar.HOUR, -6);
        TO_DATE.setTime(ACTIONS_DATE.getTime());
        TO_DATE.add(Calendar.HOUR, 6);
    }

    @Mock
    protected TarifProvider tarifProvider;

    @Mock
    protected BillingOperations billingOperations;

    @Mock
    protected BatchUpdater<BatchUpdateData> operationsUpdater;

    @Mock
    protected OracleAuditService oracleAuditService;

    @Captor
    protected ArgumentCaptor<BatchUpdateData> batchUpdateCaptor;

    protected void setUp() {
        when(billingOperations.getOperationsUpdater()).thenReturn(operationsUpdater);
        when(tarifProvider.containsTarif(anyInt(), anyLong())).thenReturn(true);
        when(tarifProvider.getTarif(anyInt(), anyLong(), any(Calendar.class))).thenReturn(PRICE);
    }

    protected void mockProcessHistoryLog(List<EntityHistoryEntry> historyEntries) {
        doAnswer(i -> {
            EntityType type = i.getArgument(0);
            Consumer<EntityHistoryEntry> consumer = i.getArgument(4);
            historyEntries.stream()
                .filter(e -> e.getEntityType() == type)
                .forEach(consumer::accept);
            return null;
        }).when(oracleAuditService).processHistoryLog(any(EntityType.class), any(Date.class), any(Date.class),
            anyBoolean(), any(Consumer.class));
    }

    protected void mockProcessHistoryLogWithTable(List<EntityHistoryEntry> historyEntries) {
        doAnswer(i -> {
            EntityType type = i.getArgument(0);
            Consumer<EntityHistoryEntry> consumer = i.getArgument(5);
            historyEntries.stream()
                .filter(e -> e.getEntityType() == type)
                .forEach(consumer::accept);
            return null;
        }).when(oracleAuditService).processHistoryLog(any(EntityType.class), any(Date.class), any(Date.class),
            anyBoolean(), anyString(), any(Consumer.class));
    }

    protected void mockGetHistoryLog(List<EntityHistoryEntry> historyEntries, Set<String> matchAliases) {
        doAnswer(i -> {
            EntityType type = i.getArgument(0);
            if (i.getArguments().length == 11) {
                // Match query
                String alias = i.getArgument(8);
                if (matchAliases.contains(alias)) {
                    return historyEntries.stream()
                        .filter(e -> e.getEntityType() == type)
                        .collect(Collectors.toList());
                }
            }
            return Collections.emptyList();
        }).when(oracleAuditService).getHistoryLog(any(EntityType.class), any(Date.class), any(Date.class),
            anyBoolean(), anyString(), anyString(), any());
    }

    protected static BatchUpdateData createBilledAction(PaidAction paidAction) {
        return createBilledAction(paidAction, DEFAULT_ACTION_ID);
    }

    protected static BatchUpdateData createBilledEntityAction(PaidAction paidAction, long entityId,
                                                              AuditAction.EntityType entityType) {
        return createBilledEntityAction(paidAction, DEFAULT_ACTION_ID, 100L, 1L, entityId, entityType);
    }

    protected static BatchUpdateData createBilledAction(PaidAction paidAction, long sourceId) {
        return createBilledAction(paidAction, sourceId, 100L);
    }

    protected static BatchUpdateData createBilledAction(PaidAction paidAction, long sourceId, long categoryId) {
        return new BatchUpdateData(1L, categoryId, new Timestamp(ACTIONS_DATE.getTimeInMillis()),
            paidAction.getId(), PRICE, null, sourceId, DEFAULT_ACTION_ID, 1,
            BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE, BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID,
            null, null, sourceId, null);
    }

    protected static BatchUpdateData createBilledAction(PaidAction paidAction, long sourceId,
                                                        long categoryId, long uid) {
        return createBilledEntityAction(paidAction, sourceId, categoryId, uid, sourceId,
                AuditAction.EntityType.MODEL_GURU);
    }

    protected static BatchUpdateData createBilledEntityAction(PaidAction paidAction, long sourceId,
                                                            long categoryId, long uid, long entityId,
                                                              AuditAction.EntityType entityType) {
        return new BatchUpdateData(uid, categoryId, new Timestamp(ACTIONS_DATE.getTimeInMillis()),
                paidAction.getId(), PRICE, null, sourceId, DEFAULT_ACTION_ID, 1,
                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE, BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID,
                null, null, entityId, entityType);
    }

    protected static BatchUpdateData createBilledActionWithTime(PaidAction paidAction, long sourceId,
                                                        long categoryId, long uid, long millis) {
        return new BatchUpdateData(uid, categoryId, new Timestamp(millis),
            paidAction.getId(), PRICE, null, sourceId, DEFAULT_ACTION_ID, 1,
            BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE, BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID,
            null, null, null, null);
    }

    protected static EntityHistoryEntry createHistoryEntry(EntityType entityType, ChangeType changeType, long entityId,
                                                         Snapshot before, Snapshot after) {
        return new EntityHistoryEntry(1L, 1L, ACTIONS_DATE, changeType.ordinal(),
            DEFAULT_ACTION_ID, entityType, entityId, before, after, changeType.name(), "Entity", "User");
    }

    protected static Snapshot createSnapshot(String fieldName, String... values) {
        ImmutableMap<String, ? extends List<String>> attributes = ImmutableMap.of(
            fieldName, Arrays.asList(values),
            XslNames.NAME, Collections.singletonList("Name"),
            CommonKeys.GURU_CATEGORY_ID.name(), Collections.singletonList("100")
        );
        return Snapshot.fromAttributes(attributes);
    }

    protected static Snapshot createEmptySnapshot() {
        return Snapshot.fromAttributes(Collections.emptyMap());
    }
}
