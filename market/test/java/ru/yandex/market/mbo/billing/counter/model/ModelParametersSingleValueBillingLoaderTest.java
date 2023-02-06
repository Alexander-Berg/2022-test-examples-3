package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;
import ru.yandex.market.mbo.gwt.exceptions.NoGuruCategoryException;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelParametersSingleValueBillingLoaderTest extends ModelParametersBillingLoaderTestBase {

    private final List<AuditAction> auditActionsCreate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, null, VALUE1)
    );

    private final List<AuditAction> auditActionsUpdate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, VALUE2),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, VALUE1, VALUE2),
        createAction(3L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_CHECK, VALUE1, VALUE1)
    );

    private final List<AuditAction> auditActionsUpdateAnotherUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE1, VALUE2),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, 3L, VALUE1, VALUE2)
    );

    private final List<AuditAction> auditActionsDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, VALUE1, null)
    );

    private final List<AuditAction> auditActionsDeleteAnotherUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE1, null),
        createAction(2L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_NONE, 3L, VALUE1, null)
    );

    private final List<AuditAction> auditActionsSameDay = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, VALUE2),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, null),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1)
    );

    private final List<AuditAction> auditActionsSameDayCreateUpdate = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, VALUE2)
    );

    private final List<AuditAction> auditActionsSameDayCreateDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null)
    );

    private final List<AuditAction> auditActionsSameDayUpdateDelete = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, VALUE2),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null)
    );

    private final List<AuditAction> auditActionsSameDayOtherUser = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE1, VALUE2),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, VALUE2, null),
        createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, null, VALUE1)
    );

    private final List<AuditAction> auditActionsOtherParam = Arrays.asList(
        createAction(1L, AuditAction.ActionType.UPDATE, PARAM1, PARAM1_NAME, null, VALUE1),
        createAction(1L, AuditAction.ActionType.UPDATE, PARAM1, PARAM1_NAME, VALUE1, VALUE2),
        createAction(1L, AuditAction.ActionType.UPDATE, PARAM2, PARAM2_NAME, VALUE2, null)
    );

    private final List<AuditAction> previousActionsCreate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1));

    private final List<AuditAction> previousActionsUpdate = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, VALUE2));

    private final List<AuditAction> previousActionsDelete = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE1, null));

    private final List<AuditAction> previousActionsDeleteAnotherValue = Collections.singletonList(
        createPreviousAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, null));

    private final List<AuditAction> oldActionsCreate = Collections.singletonList(
        createOldAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1));

    private final List<AuditAction> oldActionsCreateUpdate = Arrays.asList(
        createOldAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE2),
        createOldAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, VALUE2, VALUE1));

    private final List<AuditAction> auditActionsCreateYang = Collections.singletonList(
        actionWithSource(createAction(1L, AuditAction.ActionType.UPDATE,
            AuditAction.BillingMode.BILLING_MODE_FILL, null, VALUE1), AuditAction.Source.YANG_TASK));

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
    public void testUpdateParamNoHistory() {
        insertAuditActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsUpdate.get(0), PaidAction.FILL_MODEL_PARAMETER)
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
    public void testCreateParamFoundCreatedAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testCreateParamFoundUpdateAction() {
        insertAuditActions(previousActionsUpdate);
        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

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
    public void testUpdateParamFoundCreateAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // we bill only rollback as the same user changed the parameter
        assertThat(billingActions).containsExactlyInAnyOrder(
                createBillingActionWithSpecialPrice(auditActionsUpdate.get(0), PaidAction.ROLLBACK_MODEL_PARAMETER,
                        PRICE.negate())
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
    public void testDeleteParamFoundUpdateAction() {
        insertAuditActions(previousActionsUpdate);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

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

        // We bill repeated delete with zero even if previous deleted value (of the same param/user) is different
        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testUpdateParamAnotherUserFoundCreatedAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsUpdateAnotherUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // Paying for new user for correcting
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionOfAnotherUser(auditActionsUpdateAnotherUser.get(0),
                PaidAction.ROLLBACK_MODEL_PARAMETER, PRICE.negate(), DEFAULT_USER_ID),
            createBillingAction(auditActionsUpdateAnotherUser.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testDeleteParamAnotherUserFoundCreatedAction() {
        insertAuditActions(previousActionsCreate);
        insertAuditActions(auditActionsDeleteAnotherUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingActionOfAnotherUser(auditActionsDeleteAnotherUser.get(0),
                PaidAction.ROLLBACK_MODEL_PARAMETER, PRICE.negate(), DEFAULT_USER_ID),
            createBillingAction(auditActionsDeleteAnotherUser.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testUpdateParamSameUserSameDay() {
        insertAuditActions(auditActionsSameDay);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDay.get(3), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testUpdateParamSameUserSameDayCreateUpdate() {
        insertAuditActions(auditActionsSameDayCreateUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayCreateUpdate.get(1), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testUpdateParamSameUserSameDayCreateDelete() {
        insertAuditActions(auditActionsSameDayCreateDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).isEmpty();
    }

    @Test
    public void testUpdateParamSameUserSameDayUpdateDelete() {
        insertAuditActions(auditActionsSameDayUpdateDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayUpdateDelete.get(1), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testUpdateParamOtherUserSameDay() {
        insertAuditActions(auditActionsSameDayOtherUser);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsSameDayOtherUser.get(3), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testUpdateParamFoundOldCreateUpdateAction() {
        insertAuditActions(oldActionsCreateUpdate);
        insertAuditActions(auditActionsUpdate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // old actions (more than 14 days old) are not rolled back
        // action is billed with zero because the user has already added the same param value
        assertThat(billingActions).containsExactlyInAnyOrder(
            createRepeatedBillingAction(auditActionsUpdate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                    .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testDeleteParamFoundOldCreateAction() {
        insertAuditActions(oldActionsCreate);
        insertAuditActions(auditActionsDelete);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        // old actions (more than 14 days old) are not rolled back
        // action is billed with zero because the user has already added the param (21 days ago)
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsDelete.get(0), PaidAction.DELETE_MODEL_PARAMETER)
        );
    }

    @Test
    public void testDifferentParamAndValueDoNotInteract() {
        insertAuditActions(auditActionsOtherParam);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
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
    public void payIfSearchInfoDifficultyExists() {
        insertAuditActions(auditActionsCreate);
        Long categoryId = auditActionsCreate.get(0).getCategoryId();

        // assert search info is set
        Optional<BigDecimal> searchInfoDifficulty = guruCategoryService.getSearchInfoDifficulty(categoryId);
        assertThat(searchInfoDifficulty).isPresent();
        assertThat(searchInfoDifficulty.get()).isEqualTo(SEARCH_DIFFICULTY);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
                .setPriceMultiplicator(SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void tariffDoesNotChangeIfDifficultyMissed() {
        guruCategory.removeAttribute("search_info_difficulty");

        insertAuditActions(auditActionsCreate);
        Long categoryId = auditActionsCreate.get(0).getCategoryId();

        // assert search info is not set
        Optional<BigDecimal> searchInfoDifficulty = guruCategoryService.getSearchInfoDifficulty(categoryId);
        assertThat(searchInfoDifficulty).isEmpty();

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
        );
    }

    @Test
    public void payAsRegularTarifIfIfGuruCategoryMappingMissed() {
        when(categoryMappingService.getGuruCategoryByCategoryIdOrFail(Mockito.anyLong()))
            .thenThrow(NoGuruCategoryException.class);

        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
        );
    }

    @Test
    public void payAsRegularTarifIfTovarCategoryNotFound() {
        when(categoryMappingService.getGuruCategoryByCategoryIdOrFail(Mockito.anyLong()))
            .thenThrow(CategoryNotFoundException.class);

        insertAuditActions(auditActionsCreate);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsCreate.get(0), PaidAction.FILL_MODEL_PARAMETER)
        );
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
    public void testPayForSameUserCorrectionWithinDay() {
        List<AuditAction> actions = Arrays.asList(
                createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
                        null, VALUE1),
                createAction(1L, AuditAction.ActionType.UPDATE, AuditAction.BillingMode.BILLING_MODE_FILL,
                        VALUE1, VALUE2)
        );
        insertAuditActions(actions);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);
        assertThat(billingActions).containsExactlyInAnyOrder(
                createBillingAction(actions.get(1), PaidAction.FILL_MODEL_PARAMETER)
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
