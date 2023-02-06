package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.billing.counter.model.ModelPictureBillingLoader.UNPAID_THRESHOLD_DAYS;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelPictureBillingLoaderTest extends ModelPictureBillingLoaderTestBase {

    private final List<AuditAction> auditActionsCreateMoveDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM1, PARAM1_NAME, null, URL1),
        createBulkAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM2, PARAM2_NAME, null, URL2),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_MOVE,
            PARAM2, PARAM2_NAME, URL2, URL1),
        createBulkAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_MOVE,
            PARAM3, PARAM3_NAME, null, URL2),
        createBulkAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE,
            PARAM1, PARAM1_NAME, URL1, null),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_MOVE,
            PARAM3, PARAM3_NAME, URL2, URL1),
        createBulkAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE,
            PARAM2, PARAM2_NAME, URL1, null),
        createBulkAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_MOVE,
            PARAM4, PARAM4_NAME, null, URL2),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM3, PARAM3_NAME, URL1, null)
    );

    private final List<AuditAction> auditActionsUpdateUpdateNonCopy = Arrays.asList(
        createBulkAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM1, PARAM1_NAME, null, URL1),
        createBulkAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM1, PARAM1_NAME, URL1, URL2)
    );

    private final List<AuditAction> auditActionsUpdateUpdateAllCopy = Arrays.asList(
        createBulkAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_MOVE,
            PARAM1, PARAM1_NAME, null, URL1),
        createBulkAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_COPY,
            PARAM1, PARAM1_NAME, URL1, URL2)
    );

    // Время первого действия установится в далёкое прошлое
    private final List<AuditAction> auditActionsSameOldUpdate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM1, PARAM1_NAME, URL2, URL1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM1, PARAM1_NAME, URL1, URL2)
    );

    // Время первого действия установится в недалёкое прошлое
    private final List<AuditAction> auditActionsSameRecentUpdate = Arrays.asList(
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM2, PARAM2_NAME, URL1, URL2),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
            PARAM2, PARAM2_NAME, URL2, URL1)
    );

    @Before
    public void setUp() {
        super.setUp();

        loader = new ModelPictureBillingLoader();
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");

        addTimeOffsetsToOldActions();
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_PICTURE;
    }


    @Override
    protected PaidAction getAddPaidAction() {
        return PaidAction.ADD_MODEL_PICTURE;
    }

    @Override
    protected PaidAction getDeletePaidAction() {
        return PaidAction.DELETE_MODEL_PICTURE;
    }

    @Override
    protected PaidAction getCopyPaidAction() {
        return PaidAction.COPY_MODEL_PICTURE;
    }

    @Test
    public void testCreateMoveDelete() {
        auditService.writeActions(auditActionsCreateMoveDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreateMoveDelete.get(7), getAddPaidAction())
        );
    }

    @Test
    public void testBulkSquashSkippedOnNonCopyActions() {
        auditService.writeActions(auditActionsUpdateUpdateNonCopy);
        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsUpdateUpdateNonCopy.get(0), getAddPaidAction()),
            createBillingAction(auditActionsUpdateUpdateNonCopy.get(1), getAddPaidAction())
        );
    }

    @Test
    public void testBulkSquashAppliedOnCopyActions() {
        auditService.writeActions(auditActionsUpdateUpdateAllCopy);
        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsUpdateUpdateAllCopy.get(0), getCopyPaidAction())
        );
    }

    /**
     * Проверяем, что если для текущего действия нашлось действие над той же сущностью в 25-дневном интервале в прошлом,
     * то вот это текущее действие биллим с нулевой стоимостью.
     */
    @Test
    public void testOldActionsWithinThresholdCauseZeroPrice() {
        auditService.writeActions(auditActionsSameRecentUpdate);
        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsSameRecentUpdate.get(1), getAddPaidAction())
        );
    }

    /**
     * Проверяем, что если последнее (не считая актуального) действие над той же сущностью делалось сто лет назад, то
     * биллим актуальное действие как обычно с ценой.
     */
    @Test
    public void testOldActionsOutsideThresholdDontMatter() {
        auditService.writeActions(auditActionsSameOldUpdate);
        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameOldUpdate.get(1), getAddPaidAction())
        );
    }

    private void addTimeOffsetsToOldActions() {
        Calendar longTimeAgo = Calendar.getInstance();
        longTimeAgo.add(Calendar.DAY_OF_MONTH, -(UNPAID_THRESHOLD_DAYS + 3));
        auditActionsSameOldUpdate.get(0).setDate(longTimeAgo.getTime());
        Calendar notSoLongAgo = Calendar.getInstance();
        notSoLongAgo.add(Calendar.DAY_OF_MONTH, -(UNPAID_THRESHOLD_DAYS - 3));
        auditActionsSameRecentUpdate.get(0).setDate(notSoLongAgo.getTime());
    }
}
