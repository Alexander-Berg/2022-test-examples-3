package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelEnumValueAliasesBillingLoaderTest extends BillingLoaderTestBase {

    private static final String ALIAS = "alias";

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS),
        createAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, ALIAS)
    );

    private final List<AuditAction> auditActionsDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, ALIAS, null),
        createAction(2L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE, ALIAS, null)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, ALIAS, null),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS)
    );

    private final List<AuditAction> auditActionsSameDayDifferentUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, ALIAS, null),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS)
    );

    private final List<AuditAction> auditActionsOtherParam = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, PARAM1, VALUE1, null, ALIAS),
        createAction(1L, AuditAction.ActionType.CREATE, PARAM2, VALUE1, ALIAS, null)
    );

    private final List<AuditAction> previousActionsCreate = Arrays.asList(
            createPreviousAction(
                1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, ALIAS));

    private final List<AuditAction> previousActionsDelete = Arrays.asList(
            createPreviousAction(
                1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, ALIAS, null));

    private ModelEnumValueAliasBillingLoader loader;

    @Before
    public void setUp() {
        super.setUp();

        loader = new ModelEnumValueAliasBillingLoader();
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS;
    }

    @Test
    public void testAddBillingNoHistory() {
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testDeleteBillingNoHistory() {
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testCreateFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testCreateFoundRemovedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testDeleteFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testDeleteFoundDeletedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testCreateDeleteSameDay() {
        auditService.writeActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDay.get(2), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testCreateDeleteOtherUserSameDay() {
        auditService.writeActions(auditActionsSameDayDifferentUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayDifferentUser.get(2), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

    @Test
    public void testDifferentParamAndValueDoNotInteract() {
        auditService.writeActions(auditActionsOtherParam);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsOtherParam.get(0), PaidAction.ADD_MODEL_VALUE_ALIAS),
            createBillingAction(auditActionsOtherParam.get(1), PaidAction.ADD_MODEL_VALUE_ALIAS)
        );
    }

}
