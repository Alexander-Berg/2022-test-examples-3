package ru.yandex.market.clab.tms.kpi;

import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ru.yandex.market.clab.common.health.HealthLog;
import ru.yandex.market.clab.common.health.Stats;
import ru.yandex.market.clab.common.service.audit.AuditRepositoryStub;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.RequestedGoodWrapper;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepositoryStub;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.ActionType;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;

import java.time.LocalDateTime;
import java.util.Collections;

public class KpiServiceTestBase {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    AuditRepositoryStub auditRepository = new AuditRepositoryStub();

    GoodRepositoryStub goodRepository = new GoodRepositoryStub();

    RequestedGoodRepositoryStub requestedGoodRepositoryStub = new RequestedGoodRepositoryStub();

    @Mock
    HealthLog healthLog;

    @Captor
    protected ArgumentCaptor<Stats> statsCaptor;

    public void setUp() {
        Mockito.doNothing().when(healthLog).writeStats(statsCaptor.capture());
    }

    public void createAndSaveAuditAction(EntityType entityType,
                                         long entityInternalId,
                                         LocalDateTime dateTime,
                                         String propertyName,
                                         String oldValue,
                                         String newValue) {
        ActionType actionType = ActionType.UPDATE;
        if (oldValue == null) {
            actionType = ActionType.CREATE;
        }
        if (newValue == null) {
            actionType = ActionType.DELETE;
        }
        auditRepository.writeActions(Collections.singletonList(new AuditAction()
            .setEntityInternalId(entityInternalId)
            .setActionDate(dateTime)
            .setActionType(actionType)
            .setEntityType(entityType)
            .setPropertyName(propertyName)
            .setOldValue(oldValue)
            .setNewValue(newValue)));
    }

    public void createAndSaveRandomAction(EntityType entityType,
                                          long entityInternalId,
                                          LocalDateTime dateTime) {
        AuditAction auditAction = RandomTestUtils.randomObject(AuditAction.class)
            .setEntityType(entityType)
            .setEntityInternalId(entityInternalId)
            .setActionDate(dateTime);
        auditRepository.writeActions(Collections.singletonList(auditAction));
    }

    public void createAndSaveGoodStateAction(LocalDateTime dateTime,
                                             long goodId,
                                             GoodState oldValue,
                                             GoodState newValue) {
        createAndSaveAuditAction(EntityType.GOOD, goodId, dateTime, GoodWrapper.STATE,
            oldValue != null ? oldValue.name() : null,
            newValue != null ? newValue.name() : null);
    }

    public void createAndSaveRequestedGoodStateAction(LocalDateTime dateTime,
                                                      long goodId,
                                                      RequestedGoodState oldValue,
                                                      RequestedGoodState newValue) {
        createAndSaveAuditAction(EntityType.REQUESTED_GOOD, goodId, dateTime, RequestedGoodWrapper.STATE,
            oldValue != null ? oldValue.name() : null,
            newValue != null ? newValue.name() : null);
    }

    public Good createAndSaveGood(GoodState state) {
        Good good = RandomTestUtils.randomObject(Good.class, "id", "modifiedTs")
            .setCategoryId(2L)
            .setSupplierType(SupplierType.FIRST_PARTY)
            .setState(state);
        return goodRepository.save(good);
    }

    public RequestedGood createAndSaveRequestedGood(Long goodId, RequestedGoodState state) {
        RequestedGood requestedGood = RandomTestUtils.randomObject(RequestedGood.class, "id")
            .setGoodId(goodId)
            .setCategoryId(2L)
            .setState(state);
        return requestedGoodRepositoryStub.save(requestedGood);
    }
}
