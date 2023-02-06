package ru.yandex.market.mboc.common.dict;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WarehouseServiceAuditRecorderTest {
    private WarehouseServiceAuditRecorder auditRecorder;
    private AuditWriterMock auditWriterMock;

    @Before
    public void setUp() throws Exception {
        auditWriterMock = new AuditWriterMock();
        auditRecorder = new WarehouseServiceAuditRecorder(auditWriterMock);
        ReflectionTestUtils.setField(auditRecorder, "environment", "unit-test");
    }

    @Test
    public void writeChangesToAuditDeleteEvent() {
        String userName = "TestUser";
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setId(1L)
            .setTitle("Title1");

        WarehouseService warehouseServiceBefore = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(true)
            .childSskus(Set.of("a", "b"))
            .build();
        WarehouseService warehouseServiceAfter = null;

        var warehouseServiceAuditItem = new WarehouseServiceAuditRecorder.WarehouseServiceAuditItem(
            userName,
            offer1,
            warehouseServiceBefore,
            warehouseServiceAfter
        );
        auditRecorder.writeChangesToAudit(List.of(warehouseServiceAuditItem));

        var actions = auditWriterMock.getActions(offer1.getId());

        var actionsByPropName = actions.stream()
            .collect(Collectors.groupingBy(MboAudit.MboAction.Builder::getPropertyName));
        var needSortActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.NEED_SORT_FIELD, List.of());
        var childSskusActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.CHILD_SSKUS_FIELD, List.of());

        assertThat(actions)
            .map(MboAudit.MboAction.Builder::getActionType)
            .allMatch(MboAudit.ActionType.DELETE::equals);
        assertThat(needSortActions)
            .hasSize(1);
        assertThat(childSskusActions)
            .hasSize(1);
        var needSortAction = needSortActions.get(0);
        var childSskuAction = childSskusActions.get(0);

        assertFalse(needSortAction.hasNewValue());
        assertEquals(Boolean.valueOf(needSortAction.getOldValue()), warehouseServiceBefore.isNeedSort());
        assertFalse(childSskuAction.hasNewValue());
        assertThat(warehouseServiceBefore.getChildSskus())
            .allMatch(ssku -> childSskuAction.getOldValue().contains(ssku));
    }

    @Test
    public void writeChangesToAuditUpdateEvent() {
        String userName = "TestUser";
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setId(1L)
            .setTitle("Title1");

        var warehouseServiceBefore = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(false)
            .childSskus(Set.of())
            .build();
        var warehouseServiceAfter = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(true)
            .childSskus(Set.of("a", "b"))
            .build();

        var warehouseServiceAuditItem = new WarehouseServiceAuditRecorder.WarehouseServiceAuditItem(
            userName,
            offer1,
            warehouseServiceBefore,
            warehouseServiceAfter
        );
        auditRecorder.writeChangesToAudit(List.of(warehouseServiceAuditItem));

        var actions = auditWriterMock.getActions(offer1.getId());

        var actionsByPropName = actions.stream()
            .collect(Collectors.groupingBy(MboAudit.MboAction.Builder::getPropertyName));
        var needSortActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.NEED_SORT_FIELD, List.of());
        var childSskusActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.CHILD_SSKUS_FIELD, List.of());

        assertThat(actions)
            .map(MboAudit.MboAction.Builder::getActionType)
            .allMatch(MboAudit.ActionType.UPDATE::equals);
        assertThat(needSortActions)
            .hasSize(1);
        assertThat(childSskusActions)
            .hasSize(1);
        var needSortAction = needSortActions.get(0);
        var childSskuAction = childSskusActions.get(0);

        assertEquals(Boolean.valueOf(needSortAction.getOldValue()), warehouseServiceBefore.isNeedSort());
        assertEquals(Boolean.valueOf(needSortAction.getNewValue()), warehouseServiceAfter.isNeedSort());
        assertThat(warehouseServiceBefore.getChildSskus())
            .allMatch(ssku -> childSskuAction.getOldValue().contains(ssku));
        assertThat(warehouseServiceAfter.getChildSskus())
            .allMatch(ssku -> childSskuAction.getNewValue().contains(ssku));
    }

    @Test
    public void writeChangesToAuditCreateEvent() {
        String userName = "TestUser";
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setId(1L)
            .setTitle("Title1");

        WarehouseService warehouseServiceBefore = null;
        var warehouseServiceAfter = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(true)
            .childSskus(Set.of("a", "b"))
            .build();

        var warehouseServiceAuditItem = new WarehouseServiceAuditRecorder.WarehouseServiceAuditItem(
            userName,
            offer1,
            warehouseServiceBefore,
            warehouseServiceAfter
        );
        auditRecorder.writeChangesToAudit(List.of(warehouseServiceAuditItem));

        var actions = auditWriterMock.getActions(offer1.getId());

        var actionsByPropName = actions.stream()
            .collect(Collectors.groupingBy(MboAudit.MboAction.Builder::getPropertyName));
        var needSortActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.NEED_SORT_FIELD, List.of());
        var childSskusActions = actionsByPropName
            .getOrDefault(WarehouseServiceAuditRecorder.CHILD_SSKUS_FIELD, List.of());

        assertThat(actions)
            .map(MboAudit.MboAction.Builder::getActionType)
            .allMatch(MboAudit.ActionType.CREATE::equals);
        assertThat(needSortActions)
            .hasSize(1);
        assertThat(childSskusActions)
            .hasSize(1);
        var needSortAction = needSortActions.get(0);
        var childSskuAction = childSskusActions.get(0);

        assertFalse(needSortAction.hasOldValue());
        assertEquals(Boolean.valueOf(needSortAction.getNewValue()), warehouseServiceAfter.isNeedSort());
        assertFalse(childSskuAction.hasOldValue());
        assertThat(warehouseServiceAfter.getChildSskus())
            .allMatch(ssku -> childSskuAction.getNewValue().contains(ssku));
    }

    @Test
    public void writeChangesToAuditShouldNotWriteActionsWithoutChanges() {
        String userName = "TestUser";
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setId(1L)
            .setTitle("Title1");

        WarehouseService warehouseServiceBefore = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(true)
            .childSskus(Set.of("a", "b"))
            .build();
        var warehouseServiceAfter = WarehouseService.builder()
            .supplierId(offer1.getBusinessId())
            .shopSku(offer1.getShopSku())
            .needSort(true)
            .childSskus(Set.of("b", "a"))
            .build();

        var warehouseServiceAuditItem = new WarehouseServiceAuditRecorder.WarehouseServiceAuditItem(
            userName,
            offer1,
            warehouseServiceBefore,
            warehouseServiceAfter
        );
        auditRecorder.writeChangesToAudit(List.of(warehouseServiceAuditItem));

        var actions = auditWriterMock.getActions(offer1.getId());

        assertThat(actions)
            .isEmpty();
    }
}
