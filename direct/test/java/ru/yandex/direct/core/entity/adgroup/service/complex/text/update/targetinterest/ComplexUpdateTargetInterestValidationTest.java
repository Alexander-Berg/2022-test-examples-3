package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.targetinterest;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionAlreadyExists;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateTargetInterestValidationTest extends ComplexUpdateTargetInterestTestBase {

    @Test
    public void adGroupWithDuplicatedAddedRetargeting() {
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo1, randomRetConditionInfo);

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        TargetInterest addedRetargeting = randomPriceRetargeting(randomRetConditionInfo);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(asList(addedRetargeting, updatedRetargeting));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path path = path(index(0), field(TARGET_INTERESTS.name()), index(0));
        Defect error = retargetingConditionAlreadyExists();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path, error)));
    }

    @Test
    public void adGroupWithDuplicatedAddedAndDeletedRetargeting() {
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo1, randomRetConditionInfo);

        TargetInterest addedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo)
                .withId(null);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
        assertThat(targetInterests.get(0).getPriceContext().compareTo(addedRetargeting.getPriceContext()), is(0));
    }

    @Test
    public void adGroupWithAddedRetargetingsDuplicatedWithRetargetingInAnotherAdGroup() {
        createSecondAdGroup();
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo2, randomRetConditionInfo);

        TargetInterest addedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo)
                .withId(null);
        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting));
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(singletonList(updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<TargetInterest> targetInterests1 = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests1, hasSize(1));
        List<TargetInterest> targetInterests2 = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests2, hasSize(1));
    }

    @Test
    public void addDuplicatedRetargetingsToDifferentAdGroups() {
        createSecondAdGroup();
        RetConditionInfo randomRetConditionInfo = createRandomRetCondition();

        TargetInterest addedRetargeting1 = randomPriceRetargeting(randomRetConditionInfo);
        TargetInterest addedRetargeting2 = randomPriceRetargeting(randomRetConditionInfo);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting1));
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(singletonList(addedRetargeting2));

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<TargetInterest> targetInterests1 = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests1, hasSize(1));
        List<TargetInterest> targetInterests2 = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests2, hasSize(1));
    }
}
