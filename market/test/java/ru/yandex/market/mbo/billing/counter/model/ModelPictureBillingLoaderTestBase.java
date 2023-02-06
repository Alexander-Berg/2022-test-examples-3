package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Test;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class ModelPictureBillingLoaderTestBase extends BillingLoaderTestBase {

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1),
        createAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, URL1)
    );

    private final List<AuditAction> auditActionsUpdate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, URL1, URL2),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, URL1, URL2)
    );

    private final List<AuditAction> auditActionsDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, URL1, null),
        createAction(2L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE, URL1, null)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, URL1, URL2),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, URL2, null),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1)
    );

    private final List<AuditAction> auditActionsSameDayDifferentUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, 1L, URL1, URL2),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, 1L, URL2, null),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, 1L, null, URL1)
    );

    private final List<AuditAction> auditActionsCreateThenUpdateWithoutValueChange = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, URL1, URL1)
    );

    private final List<AuditAction> auditActionsOtherParamOrValue = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, PARAM1, VALUE1, null, URL1),
        createAction(1L, AuditAction.ActionType.UPDATE, PARAM1, VALUE2, URL1, URL2),
        createAction(1L, AuditAction.ActionType.DELETE, PARAM2, VALUE1, URL1, null)
    );

    protected final List<AuditAction> auditActionsBulkCopy = Arrays.asList(
        createBulkAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_COPY,
            PARAM1, VALUE1, null, URL1),
        createBulkAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_COPY,
            PARAM1, VALUE2, null, URL2)
    );

    private final List<AuditAction> previousActionsCreate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1));

    private final List<AuditAction> previousActionsUpdate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, URL1, URL2));

    private final List<AuditAction> previousActionsDelete = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.DELETE,
            AuditAction.BillingMode.BILLING_MODE_FILL, URL1, null));

    private final List<AuditAction> auditActionsCreateYang = Collections.singletonList(
        actionWithSource(createAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, URL1), AuditAction.Source.YANG_TASK));

    protected AbstractModelPictureBillingLoader loader;

    protected abstract PaidAction getAddPaidAction();
    protected abstract PaidAction getDeletePaidAction();
    protected abstract PaidAction getCopyPaidAction();

    @Test
    public void testAddBillingNoHistory() {
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testUpdateBillingNoHistory() {
        auditService.writeActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsUpdate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testDeleteBillingNoHistory() {
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), getDeletePaidAction())
        );
    }


    @Test
    public void testCreateFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testCreateFoundUpdatedAction() {
        auditService.writeActions(previousActionsUpdate);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testCreateFoundRemovedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testUpdateFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsUpdate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testUpdateFoundUpdatedAction() {
        auditService.writeActions(previousActionsUpdate);
        auditService.writeActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsUpdate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testUpdateFoundRemovedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsUpdate.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testDeleteFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), getDeletePaidAction())
        );
    }

    @Test
    public void testDeleteFoundUpdatedAction() {
        auditService.writeActions(previousActionsUpdate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), getDeletePaidAction())
        );
    }

    @Test
    public void testDeleteFoundRemovedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), getDeletePaidAction())
        );
    }

    @Test
    public void testCreateDeleteSameDay() {
        auditService.writeActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDay.get(3), getAddPaidAction())
        );
    }

    @Test
    public void testCreateDeleteOtherUserSameDay() {
        auditService.writeActions(auditActionsSameDayDifferentUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayDifferentUser.get(3), getAddPaidAction())
        );
    }

    @Test
    public void testBulkCopy() {
        auditService.writeActions(auditActionsBulkCopy);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsBulkCopy.get(0), getCopyPaidAction())
        );
    }

    @Test
    public void testDifferentParamAndValueDoNotInteract() {
        auditService.writeActions(auditActionsOtherParamOrValue);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsOtherParamOrValue.get(0), getAddPaidAction()),
            createBillingAction(auditActionsOtherParamOrValue.get(1), getAddPaidAction()),
            createBillingAction(auditActionsOtherParamOrValue.get(2), getDeletePaidAction())
        );
    }

    @Test
    public void testCreateThenUpdateWithoutValueChange() {
        auditService.writeActions(auditActionsCreateThenUpdateWithoutValueChange);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreateThenUpdateWithoutValueChange.get(0), getAddPaidAction())
        );
    }

    @Test
    public void testYangTaskIgnored() {
        auditService.writeActions(auditActionsCreateYang);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }
}
