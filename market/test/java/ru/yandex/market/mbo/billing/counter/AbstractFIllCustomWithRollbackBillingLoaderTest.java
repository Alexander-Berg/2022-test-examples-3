package ru.yandex.market.mbo.billing.counter;

import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.billing.counter.base.AbstractBillingLoader;
import ru.yandex.market.mbo.db.billing.dao.PaidEntry;
import ru.yandex.market.mbo.db.billing.dao.PaidEntryDao;
import ru.yandex.market.mbo.billing.counter.base.PaidEntryQueryParams;
import ru.yandex.market.mbo.billing.counter.model.BillingLoaderTestBase;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 23.10.2018
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public abstract class AbstractFIllCustomWithRollbackBillingLoaderTest extends BillingLoaderTestBase {
    protected static final BigDecimal PRICE = new BigDecimal(2);

    protected final List<AuditAction> auditActionsCreate = Arrays.asList(
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1),
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE3),
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        action(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE3)
    );

    protected final List<AuditAction> auditActionsDelete = Arrays.asList(
        action(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        action(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, null),
        action(2L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE, VALUE2, null)
    );

    protected final List<AuditAction> auditActionsDeleteCreate = Arrays.asList(
        action(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE2),
        action(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        action(3L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        action(3L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1)
    );

    protected final List<AuditAction> auditPreviousActionsCreate = Arrays.asList(
        previousAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1),
        previousAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        previousAction(3L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1)
    );

    protected final List<AuditAction> auditOldActionsCreate = Arrays.asList(
        oldAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1),
        oldAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        oldAction(3L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1)
    );

    protected final List<AuditAction> auditActionsDeleteAnotherUser = Arrays.asList(
        actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, null),
        actionOfAnotherUser(2L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE, VALUE2, null)
    );

    protected final List<AuditAction> auditActionsDeleteCreateAnotherUser = Arrays.asList(
        actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        actionOfAnotherUser(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE2),
        actionOfAnotherUser(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        actionOfAnotherUser(3L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        actionOfAnotherUser(3L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1)
    );

    protected final List<AuditAction> auditActionsDeleteAnotherUserCreateSameUser = Arrays.asList(
        actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE2),
        action(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2),
        actionOfAnotherUser(3L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null),
        action(3L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1)
    );

    protected final List<AuditAction> auditActionsWithAnotherPropertyName = Arrays.asList(
        action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, getParameterId(), PARAM1_NAME, null, VALUE3),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, getParameterId(), PARAM2_NAME, null, VALUE2),
        createAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, PARAM3, PARAM3_NAME, null, VALUE3)
    );

    protected final List<AuditAction> auditActionsCreateYang = Arrays.asList(
        actionWithSource(action(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1), AuditAction.Source.YANG_TASK),
        actionWithSource(action(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE3), AuditAction.Source.YANG_TASK),
        actionWithSource(action(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE2), AuditAction.Source.YANG_TASK),
        actionWithSource(action(2L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE3), AuditAction.Source.YANG_TASK)
    );

    @Mock
    protected PaidEntryDao paidEntryDao;

    public abstract PaidAction getAddAction();
    public abstract PaidAction getDeleteAction();
    public abstract PaidAction getRollbackAction();
    public abstract AbstractBillingLoader getLoader();
    public abstract Long getParameterId();
    public abstract String getAuditPropertyName();

    @Test
    public void testAddValueNoHistory() {
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        // Only actions with BILLING_MODE_FILL_CUSTOM are billed
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), getAddAction())
        );
    }

    @Test
    public void testDeleteValueSameUserRollback() {
        // Value was added, and then deleted on the next day - rollback expected
        auditService.writeActions(auditPreviousActionsCreate);
        auditService.writeActions(auditActionsDelete);

        when(paidEntryDao.getPaidEntry(any(PaidEntryQueryParams.class)))
            .thenReturn(Optional.of(new PaidEntry(2D, 1L)));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        // Rollback is billed with negative price
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionWithSpecialPrice(auditActionsDelete.get(0), getRollbackAction(), PRICE.negate())
        );
    }

    @Test
    public void testDeleteValueSameUserNoRollback() {
        // Value was added, and then deleted in more than 2 weeks - delete expected
        auditService.writeActions(auditOldActionsCreate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), getDeleteAction())
        );
    }

    @Test
    public void testDeleteAddValueSameUserRollback() {
        // Value was added, and then deleted and added on the next day - rollback and add expected
        // If the new value is the same as the old one, 2nd addition is billed with zero
        auditService.writeActions(auditPreviousActionsCreate);
        auditService.writeActions(auditActionsDeleteCreate);

        when(paidEntryDao.getPaidEntry(any(PaidEntryQueryParams.class)))
            .thenReturn(Optional.of(new PaidEntry(2D, 1L)));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionWithSpecialPrice(auditActionsDeleteCreate.get(0), getRollbackAction(), PRICE.negate()),
            createBillingAction(auditActionsDeleteCreate.get(1), getAddAction())
            // Last two actions in auditActionsDeleteCreate are mutually squashed and so nothing recorded
        );
    }

    @Test
    public void testUpdateValueSameUserNoRollback() {
        // Value was added, and then deleted and added in more than 2 weeks - delete and add expected
        // If the new value is the same as the old one, 2nd addition is billed with zero
        auditService.writeActions(auditOldActionsCreate);
        auditService.writeActions(auditActionsDeleteCreate);

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDeleteCreate.get(0), getDeleteAction()),
            createBillingAction(auditActionsDeleteCreate.get(1), getAddAction())
            // Last two actions in auditActionsDeleteCreate are mutually squashed and so nothing recorded.
        );
    }

    @Test
    public void testDeleteValueAnotherUserRollback() {
        // Value was added by user 1 and then deleted on the next day by user 2
        // User 1 gets rollback, user 2 gets addition
        auditService.writeActions(auditPreviousActionsCreate);
        auditService.writeActions(auditActionsDeleteAnotherUser);

        when(paidEntryDao.getPaidEntry(any(PaidEntryQueryParams.class)))
            .thenReturn(Optional.of(new PaidEntry(2D, 1L)));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionOfAnotherUser(auditActionsDeleteAnotherUser.get(0),
                getRollbackAction(), PRICE.negate(), DEFAULT_USER_ID),
            createBillingAction(auditActionsDeleteAnotherUser.get(0), getDeleteAction())
        );
    }

    @Test
    public void testDeleteAddValueAnotherUserRollback() {
        // Value was added by user 1 and then deleted and added on the next day by user 2
        // User 1 gets rollback, user 2 gets addition
        // If the new value is the same as the old one, 2nd addition is billed with zero
        auditService.writeActions(auditPreviousActionsCreate);
        auditService.writeActions(auditActionsDeleteCreateAnotherUser);

        when(paidEntryDao.getPaidEntry(any(PaidEntryQueryParams.class)))
            .thenReturn(Optional.of(new PaidEntry(2D, 1L)));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionOfAnotherUser(auditActionsDeleteCreateAnotherUser.get(0),
                getRollbackAction(), PRICE.negate(), DEFAULT_USER_ID),
            createBillingAction(auditActionsDeleteCreateAnotherUser.get(0), getDeleteAction()),
            createBillingAction(auditActionsDeleteCreateAnotherUser.get(1), getAddAction())
            // Last two actions in auditActionsDeleteCreate are mutually squashed and so nothing recorded.
            // Previous action not rolled back either.
        );
    }

    @Test
    public void testDeleteValueAnotherUserAddValueSameUser() {
        // Value was added by user 1, then deleted on the next day by user 2, then added back by user 1
        // User 1 gets rollback, user 2 gets addition
        // If the new value is the same as the old one, 2nd addition is billed with zero
        auditService.writeActions(auditPreviousActionsCreate);
        auditService.writeActions(auditActionsDeleteAnotherUserCreateSameUser);

        when(paidEntryDao.getPaidEntry(any(PaidEntryQueryParams.class)))
            .thenReturn(Optional.of(new PaidEntry(2D, 1L)));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionOfAnotherUser(auditActionsDeleteAnotherUserCreateSameUser.get(0),
                getRollbackAction(), PRICE.negate(), DEFAULT_USER_ID),
            createBillingAction(auditActionsDeleteAnotherUserCreateSameUser.get(0), getDeleteAction()),
            createBillingAction(auditActionsDeleteAnotherUserCreateSameUser.get(1), getAddAction())
            // Last two actions in auditActionsDeleteCreate are mutually squashed and so nothing recorded.
            // Previous action not rolled back either. No matter which user.
        );
    }

    @Test
    public void testFilterWrongPropertyName() {
        auditService.writeActions(auditActionsWithAnotherPropertyName);

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsWithAnotherPropertyName.get(0), getAddAction())
        );
    }

    @Test
    public void testYangTaskIgnored() {
        auditService.writeActions(auditActionsCreateYang);

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }

    protected AuditAction action(Long entityId, AuditAction.ActionType type,
                               AuditAction.BillingMode billingMode,
                               String oldValue, String newValue) {
        return createAction(entityId, type, billingMode, getParameterId(), getAuditPropertyName(), oldValue, newValue);
    }

    protected AuditAction previousAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode,
                                       String oldValue, String newValue) {
        return createPreviousAction(entityId, type, billingMode, getParameterId(), getAuditPropertyName(),
            oldValue, newValue);
    }

    protected AuditAction actionOfAnotherUser(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode,
                                       String oldValue, String newValue) {
        return createActionOfAnotherUser(entityId, type, billingMode, getParameterId(), getAuditPropertyName(),
            oldValue, newValue);
    }

    protected AuditAction oldAction(Long entityId, AuditAction.ActionType type,
                                            AuditAction.BillingMode billingMode,
                                            String oldValue, String newValue) {
        return createOldAction(entityId, type, billingMode, getParameterId(), getAuditPropertyName(),
            oldValue, newValue);
    }
}
