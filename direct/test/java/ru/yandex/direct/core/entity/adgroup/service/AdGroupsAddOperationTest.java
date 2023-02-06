package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationTest extends AdGroupsAddOperationTestBase {

    // возвращаемый результат при добавлении одной группы

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        addAndAssertResult(Applicability.PARTIAL, singletonList(clientTextAdGroup(campaignId)), true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedPreValidation_ResultHasItemError() {
        AdGroup adGroup = adGroupWithInvalidType();
        addAndAssertResult(Applicability.PARTIAL, singletonList(adGroup), false);
    }

    @Test
    // очень важный тест: элементы, имеющие ошибки предварительной валидации, не должны проходить дальнейшую валидацию
    public void prepareAndApply_OneItemWithFailedPreValidationAndValidation_OnlyPreValidated() {
        AdGroup adGroup = adGroupWithInvalidType();
        adGroup.withName(null);
        AdGroupsAddOperation addOperation = createAddOperation(Applicability.PARTIAL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный",
                result.isSuccessful(), is(true));
        assertThat("результат добавления элемента должен содержать только ошибку предварительной валидации",
                result.getResult().get(0).getErrors(), hasSize(1));
    }

    @Test
    public void prepareAndApply_OneItemWithFailedValidation_ResultHasItemError() {
        AdGroup adGroup = adGroupWithInvalidName();
        addAndAssertResult(Applicability.PARTIAL, singletonList(adGroup), false);
    }

    private void addAndAssertResult(Applicability applicability, List<AdGroup> models, boolean itemResult) {
        AdGroupsAddOperation addOperation = createAddOperation(applicability, models);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isSuccessful(itemResult));
    }

    // возвращаемый результат при добавлении двух групп

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsExpected() {
        checkAddResultOfTwoItems(Applicability.PARTIAL,
                clientTextAdGroup(campaignInfo.getCampaignId()),
                clientTextAdGroup(campaignInfo.getCampaignId()),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialYes_OneInvalidOnPreValidationAndOneInvalidOnValidation_ResultIsExpected() {
        checkAddResultOfTwoItems(Applicability.PARTIAL,
                adGroupWithInvalidType(),
                adGroupWithInvalidName(),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsExpected() {
        checkAddResultOfTwoItems(Applicability.FULL,
                clientTextAdGroup(campaignInfo.getCampaignId()),
                clientTextAdGroup(campaignInfo.getCampaignId()),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialNo_OneInvalidOnPreValidationAndOneInvalidOnValidation_ResultIsExpected() {
        checkAddResultOfTwoItems(Applicability.FULL,
                adGroupWithInvalidType(),
                adGroupWithInvalidName(),
                false, true);
    }

    private void checkAddResultOfTwoItems(Applicability applicability,
                                          AdGroup adGroup1, AdGroup adGroup2,
                                          boolean model1ResultValid, boolean model2ResultValid) {
        List<AdGroup> models = asList(adGroup1, adGroup2);

        AdGroupsAddOperation addOperation = createAddOperation(applicability, models);
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(model1ResultValid, model2ResultValid));
    }
}
