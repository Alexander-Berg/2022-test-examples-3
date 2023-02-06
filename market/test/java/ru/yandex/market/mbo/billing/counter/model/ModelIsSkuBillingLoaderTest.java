package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.utils.XslNames;

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
public class ModelIsSkuBillingLoaderTest extends BillingLoaderTestBase {
    private static final Long IS_SKU_PARAM_ID = 321L;
    private static final String TRUE = Boolean.TRUE.toString();
    private static final String FALSE = Boolean.FALSE.toString();

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, TRUE),
        action(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, FALSE)
    );

    private final List<AuditAction> auditActionsUpdate = Arrays.asList(
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            TRUE, FALSE),
        action(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            TRUE, null),
        action(3L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            FALSE, TRUE),
        action(4L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            FALSE, null),
        action(5L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            null, FALSE),
        action(6L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, TRUE)
    );

    private final List<AuditAction> auditActionsDelete = Arrays.asList(
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, TRUE, null),
        action(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, FALSE, null)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, FALSE),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, FALSE, TRUE),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, TRUE, null),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, TRUE)
    );

    private final List<AuditAction> auditActionsSameDayOtherUser = Arrays.asList(
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            null, FALSE),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            3L, FALSE, TRUE),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            3L, TRUE, null),
        action(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM,
            3L, null, TRUE)
    );

    private final List<AuditAction> previousActionsCreateTrue = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, TRUE));

    private final List<AuditAction> previousActionsCreateFalse = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, FALSE));

    private final List<AuditAction> previousActionsUpdateTrue = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, TRUE));

    private final List<AuditAction> previousActionsUpdateFalse = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, TRUE, FALSE));

    private final List<AuditAction> previousActionsDeleteTrue = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, TRUE, null));

    private final List<AuditAction> previousActionsDeleteFalse = Collections.singletonList(
        previousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, FALSE, null));

    private ModelIsSkuBillingLoader loader;

    @Before
    public void setUp() {
        super.setUp();

        loader = new ModelIsSkuBillingLoader();
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_PARAM;
    }

    @Test
    public void testFillParamNoHistory() {
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testUpdateParamNoHistory() {
        auditService.writeActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsUpdate.get(2), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testDeleteParamNoHistory() {
        auditService.writeActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }

    @Test
    public void testCreateParamFoundCreatedTrueAction() {
        auditService.writeActions(previousActionsCreateTrue);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testCreateParamFoundCreatedFalseAction() {
        auditService.writeActions(previousActionsCreateFalse);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testCreateParamFoundUpdateTrueAction() {
        auditService.writeActions(previousActionsUpdateTrue);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testCreateParamFoundUpdatedFalseAction() {
        auditService.writeActions(previousActionsUpdateFalse);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testCreateParamFoundDeletedTrueAction() {
        auditService.writeActions(previousActionsDeleteTrue);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testCreateParamFoundDeletedFalseAction() {
        auditService.writeActions(previousActionsDeleteFalse);
        auditService.writeActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testUpdateParamSameUserSameDay() {
        auditService.writeActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDay.get(3), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    @Test
    public void testUpdateParamOtherUserSameDay() {
        auditService.writeActions(auditActionsSameDayOtherUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayOtherUser.get(3), PaidAction.MARK_MODEL_AS_SKU)
        );
    }

    private AuditAction action(Long entityId, AuditAction.ActionType type,
                               AuditAction.BillingMode billingMode,
                               String oldValue, String newValue) {
        return action(entityId, type, billingMode, DEFAULT_USER_ID, oldValue, newValue);
    }

    private AuditAction action(Long entityId, AuditAction.ActionType type,
                               AuditAction.BillingMode billingMode, Long userId,
                               String oldValue, String newValue) {
        return createAction(entityId, type, billingMode, userId, IS_SKU_PARAM_ID, XslNames.IS_SKU,
            oldValue, newValue);
    }

    private AuditAction previousAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode,
                                       String oldValue, String newValue) {
        return createPreviousAction(entityId, type, billingMode, IS_SKU_PARAM_ID, XslNames.IS_SKU,
            oldValue, newValue);
    }
}
