package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
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
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuBillingLoaderTest extends BillingLoaderTestBase {

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE)
    );

    private final List<AuditAction> auditActionsDelete = Collections.singletonList(
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL)
    );

    private final List<AuditAction> previousActionsCreate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL));

    private final List<AuditAction> previousActionsDelete = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL));

    private final List<AuditAction> auditActionsCreateYang = Collections.singletonList(
        actionWithSource(createAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL), AuditAction.Source.YANG_TASK));

    private SkuBillingLoader loader;

    @Before
    public void setUp() {
        super.setUp();

        loader = new SkuBillingLoader();
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_SKU;
    }

    @Test
    public void testCreateSkuNoHistory() {
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.CREATE_SKU)
        );
    }

    @Test
    public void testDeleteSkuNoHistory() {
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_SKU)
        );
    }

    @Test
    public void testCreateFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.CREATE_SKU)
        );
    }

    @Test
    public void testCreateFoundRemovedAction() {
        auditService.writeActions(previousActionsDelete);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.CREATE_SKU)
        );
    }

    @Test
    public void testDeleteFoundCreatedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_SKU)
        );
    }

    @Test
    public void testDeleteFoundDeletedAction() {
        auditService.writeActions(previousActionsCreate);
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_SKU)
        );
    }

    @Test
    public void testCreateDeleteSameDay() {
        auditService.writeActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }

    @Test
    public void testYangTaskIgnored() {
        auditService.writeActions(auditActionsCreateYang);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }
}
