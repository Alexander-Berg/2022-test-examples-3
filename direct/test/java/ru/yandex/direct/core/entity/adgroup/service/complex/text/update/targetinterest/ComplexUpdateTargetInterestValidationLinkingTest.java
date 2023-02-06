package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.targetinterest;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.Retargeting.PRICE_CONTEXT;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateTargetInterestValidationLinkingTest extends ComplexUpdateTargetInterestTestBase {

    @Test
    public void errorInUpdatedRetargetingsIsIgnored() {
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo1, randomRetConditionInfo);

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> existingRetargetings = findTargetInterestsInAdGroup(adGroupInfo1);
        TestUtils.assumeThat("обновляемый ретаргетинг не удален", existingRetargetings, hasSize(1));

        AdGroup adGroup = getAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("группа должна быть обновлена",
                adGroup.getName(), equalTo(complexAdGroup.getAdGroup().getName()));
    }

    @Test
    public void adGroupWithInvalidAddedRetargeting() {
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();
        TargetInterest addedRetargeting = randomPriceRetargeting(randomRetConditionInfo)
                .withPriceContext(BigDecimal.valueOf(-1));
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path path = path(index(0), field(TARGET_INTERESTS.name()), index(0), field(PRICE_CONTEXT.name()));
        Defect error = invalidValueNotLessThan(Money.valueOf(CURRENCY.getMinPrice(), CURRENCY.getCode()));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path, error)));
    }

    @Test
    public void adGroupWithValidAddedRetargetingAndAdGroupWithValidAndInvalidAddedRetargetings() {
        createSecondAdGroup();
        RetConditionInfo randomRetConditionInfo1 = createRandomRetCondition();
        RetConditionInfo randomRetConditionInfo2 = createRandomRetCondition();
        RetConditionInfo randomRetConditionInfo3 = createRandomRetCondition();

        TargetInterest addedRetargeting1 = randomPriceRetargeting(randomRetConditionInfo1);
        TargetInterest addedRetargeting2 = randomPriceRetargeting(randomRetConditionInfo2);
        TargetInterest addedRetargeting3 = randomPriceRetargeting(randomRetConditionInfo3)
                .withPriceContext(BigDecimal.valueOf(-1));
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting1));
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(asList(addedRetargeting2, addedRetargeting3));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(complexAdGroup1, complexAdGroup2));

        Path path = path(index(1), field(TARGET_INTERESTS.name()), index(1), field(PRICE_CONTEXT.name()));
        Defect error = invalidValueNotLessThan(Money.valueOf(CURRENCY.getMinPrice(), CURRENCY.getCode()));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path, error)));
    }

    @Test
    public void adGroupWithUpdatedRetargetingAndAdGroupWithValidUpdatedAndInvalidAddedRetargetings() {
        createSecondAdGroup();
        RetConditionInfo randomRetConditionInfo1 = createRandomRetCondition();
        RetConditionInfo randomRetConditionInfo2 = createRandomRetCondition();
        RetConditionInfo randomRetConditionInfo3 = createRandomRetCondition();
        RetargetingInfo retargetingInfo1 = createRetargeting(adGroupInfo1, randomRetConditionInfo1);
        RetargetingInfo retargetingInfo2 = createRetargeting(adGroupInfo2, randomRetConditionInfo2);

        TargetInterest updatedRetargeting1 = getRetargetingWithUpdatedPrice(retargetingInfo1);
        TargetInterest updatedRetargeting2 = getRetargetingWithUpdatedPrice(retargetingInfo2);
        TargetInterest addedRetargeting3 = randomPriceRetargeting(randomRetConditionInfo3)
                .withPriceContext(BigDecimal.valueOf(-1));

        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(updatedRetargeting1));
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(asList(updatedRetargeting2, addedRetargeting3));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(complexAdGroup1, complexAdGroup2));

        Path path = path(index(1), field(TARGET_INTERESTS.name()), index(1), field(PRICE_CONTEXT.name()));
        Defect error = invalidValueNotLessThan(Money.valueOf(CURRENCY.getMinPrice(), CURRENCY.getCode()));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path, error)));
    }
}
