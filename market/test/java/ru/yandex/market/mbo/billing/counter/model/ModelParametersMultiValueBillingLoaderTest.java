package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;

import java.math.BigDecimal;
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
public class ModelParametersMultiValueBillingLoaderTest extends ModelParametersBillingLoaderTestBase {

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(2L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE1)
    );

    private final List<AuditAction> auditActionsDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null),
        createAction(2L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_NONE, VALUE1, null)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE2),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE4),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE3, null)
    );

    private final List<AuditAction> auditActionsSameDayOtherUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, null, VALUE2),
        createAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, null, VALUE4),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE1, null),
        createAction(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE3, null)
    );

    private final List<AuditAction> auditActionsOtherParam = Arrays.asList(
        createAction(1L, AuditAction.ActionType.CREATE, PARAM1, PARAM1_NAME, null, VALUE1),
        createAction(1L, AuditAction.ActionType.CREATE, PARAM1, PARAM1_NAME, null, VALUE2),
        createAction(1L, AuditAction.ActionType.DELETE, PARAM2, PARAM2_NAME, VALUE2, null)
    );

    private final List<AuditAction> previousActionsCreate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1));

    private final List<AuditAction> previousActionsDelete = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.DELETE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null));

    private final List<AuditAction> previousActionsCreateAnotherValue = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE2));

    private final List<AuditAction> previousActionsDeleteAnotherValue = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.DELETE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, null));

    private final List<AuditAction> oldActionsCreate = Collections.singletonList(
        createOldAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1));

    private final List<AuditAction> oldActionsDelete = Collections.singletonList(
        createOldAction(1L, AuditAction.ActionType.DELETE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null));

    private final List<AuditAction> auditActionsCreateYang = Collections.singletonList(
        actionWithSource(createAction(1L, AuditAction.ActionType.CREATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1), AuditAction.Source.YANG_TASK)
    );

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_PARAM;
    }

    @Test
    public void testFillParamNoHistory() {
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testDeleteParamNoHistory() {
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testCreateParamFoundCreateAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testCreateParamFoundCreateActionAnotherValue() {
        insertAuditActions(previousActionsCreateAnotherValue);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // another value of the same param is not counted - do not bill with zero
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testCreateParamFoundDeleteAction() {
        insertAuditActions(previousActionsDelete);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testCreateParamFoundDeleteActionAnotherValue() {
        insertAuditActions(previousActionsDeleteAnotherValue);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // another value of the same param is not counted - do not bill with zero
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testDeleteParamFoundCreatedAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionWithSpecialPrice(auditActionsDelete.get(0), PaidAction.ROLLBACK_MODEL_PARAMETER,
                PRICE.negate())
        );
    }

    @Test
    public void testDeleteParamFoundCreatedActionAnotherValue() {
        insertAuditActions(previousActionsCreateAnotherValue);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // another value of the same param is not counted - do not perform rollback
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDeleteParamFoundDeleteAction() {
        insertAuditActions(previousActionsDelete);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDeleteParamFoundDeleteActionAnotherValue() {
        insertAuditActions(previousActionsDeleteAnotherValue);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // another value of the same param is not counted - do not bill with zero
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDeleteParamFoundOldCreateAction() {
        insertAuditActions(oldActionsCreate);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDeleteParamFoundOldDeleteAction() {
        insertAuditActions(oldActionsDelete);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testUpdateParamSameUserSameDay() {
        insertAuditActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDay.get(1), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsSameDay.get(2), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsSameDay.get(4), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testUpdateParamOtherUserSameDay() {
        insertAuditActions(auditActionsSameDayOtherUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayOtherUser.get(1), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsSameDayOtherUser.get(2), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsSameDayOtherUser.get(4), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDifferentParamAndValueDoNotInteract() {
        insertAuditActions(auditActionsOtherParam);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsOtherParam.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsOtherParam.get(1), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY),
            createBillingAction(auditActionsOtherParam.get(2), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testYangTaskIgnored() {
        insertAuditActions(auditActionsCreateYang);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertEquals(0, billingActions.size());
    }

    @Test
    public void payIfFillDifficultyExists() {
        insertAuditActions(auditActionsCreate);
        Long categoryId = auditActionsCreate.get(0).getCategoryId();
        Long parameterId = auditActionsCreate.get(0).getParameterId();

        BigDecimal fillDifficulty = new BigDecimal("1.5");
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(parameterId, "")
            .setCategoryHid(categoryId)
            .setName("My-parameter")
            .setFillDifficulty(fillDifficulty)
            .build());

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(fillDifficulty))
                .setParameterName("My-parameter")
        );
    }

    @Test
    public void skipFillDifficultyIfItIsSetOnServiceParams() {
        insertAuditActions(auditActionsCreate);
        Long categoryId = auditActionsCreate.get(0).getCategoryId();
        Long parameterId = auditActionsCreate.get(0).getParameterId();

        BigDecimal fillDifficulty = new BigDecimal("1.5");
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(parameterId, "")
            .setCategoryHid(categoryId)
            .setName("My-parameter")
            .setFillDifficulty(fillDifficulty)
            .setService(true)
            .build());

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void payIfFillDifficultyNoSet() {
        insertAuditActions(auditActionsCreate);
        Long categoryId = auditActionsCreate.get(0).getCategoryId();
        Long parameterId = auditActionsCreate.get(0).getParameterId();

        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(parameterId, "")
            .setCategoryHid(categoryId)
            .setName("My-parameter")
            .build());

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }
}
