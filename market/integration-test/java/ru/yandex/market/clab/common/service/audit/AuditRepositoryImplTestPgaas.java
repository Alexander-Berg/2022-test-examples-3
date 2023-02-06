package ru.yandex.market.clab.common.service.audit;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.ActionType;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 21.10.2018
 */

public class AuditRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private AuditRepository auditRepository;

    private AuditAction filterAction1;

    private AuditAction filterAction2;

    private AuditAction filterAction3;

    List<AuditAction> filterActions;

    @Before
    public void before() {
        filterAction1 = RandomTestUtils.randomObject(AuditAction.class, "id")
            .setActionDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
            .setActionType(ActionType.CREATE)
            .setEntityType(EntityType.CART)
            .setEntityInternalId(1L)
            .setStaffLogin("user1");

        filterAction2 = RandomTestUtils.randomObject(AuditAction.class, "id")
            .setActionDate(filterAction1.getActionDate().plusSeconds(1))
            .setActionType(ActionType.UPDATE)
            .setEntityType(EntityType.GOOD)
            .setEntityInternalId(2L)
            .setStaffLogin("user1");

        filterAction3 = RandomTestUtils.randomObject(AuditAction.class, "id")
            .setActionDate(filterAction1.getActionDate().plusSeconds(2))
            .setActionType(ActionType.DELETE)
            .setEntityType(EntityType.MOVEMENT)
            .setEntityInternalId(3L)
            .setStaffLogin("user2");

        filterActions = ImmutableList.of(filterAction1, filterAction2, filterAction3);
    }

    @Test
    public void insertAndGet() {
        AuditAction auditAction = RandomTestUtils.randomObject(AuditAction.class, "id");
        auditRepository.writeAction(auditAction);

        List<AuditAction> saved = auditRepository.findActions(new AuditActionFilter());

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(auditAction);
    }

    @Test
    public void insertBatchAndGet() {
        AuditAction auditAction1 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction2 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction3 = RandomTestUtils.randomObject(AuditAction.class, "id");
        auditAction2.setActionDate(auditAction1.getActionDate().plusSeconds(1));
        auditAction3.setActionDate(auditAction1.getActionDate().plusSeconds(2));

        auditRepository.writeActions(ImmutableList.of(auditAction1, auditAction2, auditAction3));

        List<AuditAction> saved = auditRepository.findActions(new AuditActionFilter());

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(auditAction3, auditAction2, auditAction1);
    }

    @Test
    public void testSortByDateAndPaging() {
        AuditAction auditAction1 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction2 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction3 = RandomTestUtils.randomObject(AuditAction.class, "id");
        auditAction2.setActionDate(auditAction1.getActionDate().plusSeconds(1));
        auditAction3.setActionDate(auditAction1.getActionDate().plusSeconds(2));

        auditRepository.writeActions(ImmutableList.of(auditAction1, auditAction2, auditAction3));

        List<AuditAction> saved = auditRepository.findActions(
            new AuditActionFilter(),
            AuditSortBy.ACTION_DATE.asc(),
            PageFilter.page(1, 2));

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(auditAction3);
    }

    @Test
    public void testSortByStaffLoginAndPaging() {
        AuditAction auditAction1 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction2 = RandomTestUtils.randomObject(AuditAction.class, "id");
        AuditAction auditAction3 = RandomTestUtils.randomObject(AuditAction.class, "id");
        auditAction1.setStaffLogin("abc");
        auditAction2.setStaffLogin("cde");
        auditAction3.setStaffLogin("def");

        auditRepository.writeActions(ImmutableList.of(auditAction1, auditAction2, auditAction3));

        List<AuditAction> saved = auditRepository.findActions(
            new AuditActionFilter(),
            Sorting.of(AuditSortBy.STAFF_LOGIN, SortOrder.DESC),
            PageFilter.page(0, 2));

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(auditAction3, auditAction2);
    }

    @Test
    public void testFilterById() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> all = auditRepository.findActions(
            new AuditActionFilter()
        );

        List<AuditAction> toFilter = all.stream()
            .limit(2)
            .collect(Collectors.toList());
        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .addIds(toFilter.stream()
                    .map(AuditAction::getId)
                    .collect(Collectors.toList()))
        );

        assertThat(filtered).containsExactlyInAnyOrderElementsOf(toFilter);
    }

    @Test
    public void testFilterByDate() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .setStartDate(filterAction1.getActionDate())
                .setEndDate(filterAction2.getActionDate())
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction2);
    }

    @Test
    public void testFilterByEntityType() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .addEntityType(EntityType.GOOD)
                .addEntityType(EntityType.MOVEMENT)
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction2, filterAction3);
    }

    @Test
    public void testFilterByActionType() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .addActionType(ActionType.CREATE)
                .addActionType(ActionType.UPDATE)
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction1, filterAction2);
    }

    @Test
    public void testFilterByEntityInternalId() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .addEntityInternalId(1L)
                .addEntityInternalId(3L)
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction1, filterAction3);
    }

    @Test
    public void testFilterByStaffLogin() {
        auditRepository.writeActions(filterActions);

        List<AuditAction> filtered = auditRepository.findActions(
            new AuditActionFilter()
                .setStaffLogin("user1")
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction1, filterAction2);
    }

    private void assertIdsExistAndClean(List<AuditAction> actions) {
        assertThat(actions).noneMatch(a -> a.getId() == null);
        actions.forEach(a -> a.setId(null));
    }
}
