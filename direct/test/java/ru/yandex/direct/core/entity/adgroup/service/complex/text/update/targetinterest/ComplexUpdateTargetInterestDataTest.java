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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateTargetInterestDataTest extends ComplexUpdateTargetInterestTestBase {

    @Test
    public void adGroupWithEmptyRetargetings() {
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(emptyList());
        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    // добавление

    @Test
    public void adGroupWithAddedRetargeting() {
        RetConditionInfo retConditionInfo = createRandomRetCondition();

        TargetInterest addedRetargeting = randomPriceRetargeting(retConditionInfo);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
        assertThat(targetInterests.get(0).getPriceContext().longValue(),
                equalTo(addedRetargeting.getPriceContext().longValue()));
    }

    @Test
    public void adGroupWithTwoAddedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();

        TargetInterest addedRetargeting1 = randomPriceRetargeting(retConditionInfo1);
        TargetInterest addedRetargeting2 = randomPriceRetargeting(retConditionInfo2);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(asList(addedRetargeting1, addedRetargeting2));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(2));

        List<Long> prices = mapList(targetInterests, ti -> ti.getPriceContext().longValue());
        assertThat(prices,
                containsInAnyOrder(
                        addedRetargeting1.getPriceContext().longValue(),
                        addedRetargeting2.getPriceContext().longValue()));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithTwoAddedRetargetings() {
        createSecondAdGroup();
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();

        TargetInterest addedRetargeting1 = randomPriceRetargeting(retConditionInfo1);
        TargetInterest addedRetargeting2 = randomPriceRetargeting(retConditionInfo2);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(asList(addedRetargeting1, addedRetargeting2));

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests, hasSize(2));

        List<Long> prices = mapList(targetInterests, ti -> ti.getPriceContext().longValue());
        assertThat(prices,
                containsInAnyOrder(
                        addedRetargeting1.getPriceContext().longValue(),
                        addedRetargeting2.getPriceContext().longValue()));
    }

    /**
     * Проверяем, что включение режима {@code autoPrices} корректно
     * прокидывается до {@link ru.yandex.direct.core.entity.retargeting.service.AddRetargetingsOperation}
     */
    @Test
    public void emptyAdGroupAddRetargetingWithAutoPrices() {
        assumeManualStrategyWithDifferentPlaces();
        RetConditionInfo retConditionInfo = createRandomRetCondition();

        TargetInterest retargeting = randomPriceRetargeting(retConditionInfo)
                .withPriceContext(null);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(retargeting));

        updateWithAutoPricesAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
        assertThat(targetInterests.get(0).getPriceContext(), equalTo(FIXED_AUTO_PRICE));
    }

    // обновление (на самом деле ничего не обновляет)

    @Test
    public void adGroupWithUpdatedRetargeting() {
        RetConditionInfo retConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo1, retConditionInfo);

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
    }

    @Test
    public void adGroupWithTwoUpdatedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        RetargetingInfo retargetingInfo1 = createRetargeting(adGroupInfo1, retConditionInfo1);
        RetargetingInfo retargetingInfo2 = createRetargeting(adGroupInfo1, retConditionInfo2);

        TargetInterest updatedRetargeting1 = getRetargetingWithUpdatedPrice(retargetingInfo1);
        TargetInterest updatedRetargeting2 = getRetargetingWithUpdatedPrice(retargetingInfo2);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(asList(updatedRetargeting1, updatedRetargeting2));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(2));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithUpdatedRetargeting() {
        createSecondAdGroup();
        RetConditionInfo retConditionInfo = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo2, retConditionInfo);

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withTargetInterests(singletonList(updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<TargetInterest> targetInterests1 = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests1, emptyIterable());
        List<TargetInterest> targetInterests2 = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests2, hasSize(1));
    }

    // удаление

    @Test
    public void adGroupWithDeletedRetargeting() {
        RetConditionInfo retConditionInfo = createRandomRetCondition();
        createRetargeting(adGroupInfo1, retConditionInfo);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, emptyIterable());
    }

    @Test
    public void adGroupWithDeletedRetargetingAndUntouchedAdGroup() {
        createSecondAdGroup();
        RetConditionInfo retConditionInfo = createRandomRetCondition();
        createRetargeting(adGroupInfo1, retConditionInfo);
        createRetargeting(adGroupInfo2, retConditionInfo);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests1 = findTargetInterestsInAdGroup(adGroupInfo1);
        assumeThat(targetInterests1, emptyIterable());

        List<TargetInterest> targetInterests2 = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests2, hasSize(1));
    }

    @Test
    public void adGroupWithTwoDeletedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        createRetargeting(adGroupInfo1, retConditionInfo1);
        createRetargeting(adGroupInfo1, retConditionInfo2);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, emptyIterable());
    }

    @Test
    public void emptyAdGroupAndAdGroupWithDeletedRetargeting() {
        createSecondAdGroup();
        RetConditionInfo retConditionInfo = createRandomRetCondition();
        createRetargeting(adGroupInfo2, retConditionInfo);

        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2);

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests, emptyIterable());
    }

    // добавление/обновление/удаление

    @Test
    public void adGroupWithAddedAndUpdatedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo1, retConditionInfo1);
        Long oldPrice = retargetingInfo.getRetargeting().getPriceContext().longValue();

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        TargetInterest addedRetargeting = randomPriceRetargeting(retConditionInfo2);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(asList(addedRetargeting, updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        List<Long> prices = mapList(targetInterests, ti -> ti.getPriceContext().longValue());
        assertThat(prices, containsInAnyOrder(oldPrice, addedRetargeting.getPriceContext().longValue()));
    }

    @Test
    public void adGroupWithAddedAndDeletedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        createRetargeting(adGroupInfo1, retConditionInfo1);

        TargetInterest addedRetargeting = randomPriceRetargeting(retConditionInfo2);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
        assertThat(targetInterests.get(0).getPriceContext().longValue(),
                equalTo(addedRetargeting.getPriceContext().longValue()));
    }

    @Test
    public void adGroupWithUpdatedAndDeletedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        RetargetingInfo retargetingInfo1 = createRetargeting(adGroupInfo1, retConditionInfo1);
        Long oldPrice = retargetingInfo1.getRetargeting().getPriceContext().longValue();
        createRetargeting(adGroupInfo1, retConditionInfo2);

        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests, hasSize(1));
        assertThat(targetInterests.get(0).getPriceContext().longValue(), equalTo(oldPrice));
    }

    @Test
    public void adGroupWithAddedAndUpdatedAndDeletedRetargetings() {
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        RetConditionInfo retConditionInfo3 = createRandomRetCondition();
        RetargetingInfo retargetingInfo1 = createRetargeting(adGroupInfo1, retConditionInfo1);
        Long oldPrice = retargetingInfo1.getRetargeting().getPriceContext().longValue();
        createRetargeting(adGroupInfo1, retConditionInfo2);

        TargetInterest addedRetargeting = randomPriceRetargeting(retConditionInfo3);
        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(asList(addedRetargeting, updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<TargetInterest> targetInterests = findTargetInterestsInAdGroup(adGroupInfo1);
        List<Long> prices = mapList(targetInterests, ti -> ti.getPriceContext().longValue());
        assertThat(prices, containsInAnyOrder(oldPrice, addedRetargeting.getPriceContext().longValue()));
    }

    @Test
    public void adGroupWithAddedRetargetingAndEmptyAdGroupAndAdGroupWithAddedAndUpdatedRetargetings() {
        createSecondAdGroup();
        createThirdAdGroup();
        RetConditionInfo retConditionInfo1 = createRandomRetCondition();
        RetConditionInfo retConditionInfo2 = createRandomRetCondition();
        RetConditionInfo retConditionInfo3 = createRandomRetCondition();
        RetargetingInfo retargetingInfo = createRetargeting(adGroupInfo3, retConditionInfo3);
        Long oldPrice = retargetingInfo.getRetargeting().getPriceContext().longValue();

        TargetInterest addedRetargeting1 = randomPriceRetargeting(retConditionInfo1);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withTargetInterests(singletonList(addedRetargeting1));

        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2);

        TargetInterest addedRetargeting2 = randomPriceRetargeting(retConditionInfo2);
        TargetInterest updatedRetargeting = getRetargetingWithUpdatedPrice(retargetingInfo);
        ComplexTextAdGroup complexAdGroup3 = createValidAdGroupForUpdate(adGroupInfo3)
                .withTargetInterests(asList(addedRetargeting2, updatedRetargeting));

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2, complexAdGroup3));

        List<TargetInterest> targetInterests1 = findTargetInterestsInAdGroup(adGroupInfo1);
        assertThat(targetInterests1, hasSize(1));
        assertThat(targetInterests1.get(0).getPriceContext().longValue(),
                equalTo(addedRetargeting1.getPriceContext().longValue()));

        List<TargetInterest> targetInterests2 = findTargetInterestsInAdGroup(adGroupInfo2);
        assertThat(targetInterests2, emptyIterable());

        List<TargetInterest> targetInterests3 = findTargetInterestsInAdGroup(adGroupInfo3);
        List<Long> prices = mapList(targetInterests3, ti -> ti.getPriceContext().longValue());
        assertThat(prices, containsInAnyOrder(oldPrice, addedRetargeting2.getPriceContext().longValue()));
    }
}
