package ru.yandex.direct.core.entity.adgroup.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFailed;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationTest extends AdGroupsUpdateOperationTestBase {

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroup1);
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedPreValidation_ResultHasItemError() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithInvalidMinusKeywords(adGroup1);
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, false);
    }

    @Test
    // очень важный тест: элементы, имеющие ошибки предварительной валидации, не должны проходить дальнейшую валидацию
    public void prepareAndApply_OneItemWithFailedPreValidationAndValidation_OnlyPreValidated() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithInvalidMinusKeywords(adGroup1);
        modelChanges.process(null, AdGroup.NAME);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.PARTIAL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный",
                result.isSuccessful(), is(true));
        assertThat("результат обновления элемента должен содержать только ошибку предварительной валидации",
                result.getResult().get(0).getErrors(), hasSize(1));
    }

    @Test
    public void prepareAndApply_OneItemWithFailedValidation_ResultHasItemError() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithInvalidName(adGroup1);
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, false);
    }

    private void updateAndAssertResult(Applicability applicability, ModelChanges<AdGroup> modelChanges,
                                       boolean itemResult) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(itemResult));
    }

    // ошибка операции при обновлении групп

    @Test
    public void prepareAndApply_TooManyItems_ResultIsFailed() {
        List<ModelChanges<AdGroup>> modelChangesList = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            modelChangesList.add(modelChangesWithValidName(adGroup1));
        }
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isFailed());
    }

    // возвращаемый результат при обновлении двух групп

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(adGroup1),
                modelChangesWithValidMinusKeywords(adGroup2),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                new ModelChanges<>(-1L, AdGroup.class),
                modelChangesWithValidMinusKeywords(adGroup2),
                false, true);
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                new ModelChanges<>(-1L, AdGroup.class),
                modelChangesWithInvalidMinusKeywords(adGroup2),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(adGroup1),
                modelChangesWithInvalidName(adGroup2),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithInvalidName(adGroup1),
                modelChangesWithInvalidName(adGroup2),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(adGroup1),
                modelChangesWithValidMinusKeywords(adGroup2.getId()),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                new ModelChanges<>(-1L, AdGroup.class),
                modelChangesWithValidMinusKeywords(adGroup2),
                false, true);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                new ModelChanges<>(-1L, AdGroup.class),
                modelChangesWithInvalidName(adGroup2),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(adGroup1),
                modelChangesWithInvalidName(adGroup2),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithInvalidName(adGroup1),
                modelChangesWithInvalidName(adGroup2),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItem_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                new ModelChanges<>(-1L, AdGroup.class),
                modelChangesWithValidMinusKeywords(adGroup2),
                false, true);
    }

    private void checkUpdateResultOfTwoItems(Applicability applicability,
                                             ModelChanges<AdGroup> modelChanges1, ModelChanges<AdGroup> modelChanges2,
                                             boolean modelChanges1Valid, boolean modelChanges2Valid) {
        List<ModelChanges<AdGroup>> modelChangesList = asList(modelChanges1, modelChanges2);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(applicability, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(modelChanges1Valid, modelChanges2Valid));
    }

    private ModelChanges<AdGroup> modelChangesWithInvalidName(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process("", AdGroup.NAME);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithValidMinusKeywords(AdGroup adGroup) {
        return modelChangesWithValidMinusKeywords(adGroup.getId());
    }
}
