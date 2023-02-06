package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_RETARGETINGS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.maxCollectionSizeAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddRetargetingsTest extends ComplexTextAddTestBase {

    @Test
    public void oneAdGroupWithRetargetings() {
        ComplexTextAdGroup adGroup = adGroupWithRetargetings(singletonList(randomPriceRetargeting()));
        addAndCheckComplexAdGroups(singletonList(adGroup));
    }

    @Test
    public void oneAdGroupWithoutRetargetingsAndOneWith() {
        ComplexTextAdGroup emptyComplexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroup adGroupWithRetargetings = adGroupWithRetargetings(singletonList(randomPriceRetargeting()));
        addAndCheckComplexAdGroups(asList(emptyComplexAdGroup, adGroupWithRetargetings));
    }

    /**
     * Проверяем, что режим {@code autoPrices} корректно прокидывается до
     * {@link ru.yandex.direct.core.entity.retargeting.service.AddRetargetingsOperation}
     */
    @Test
    public void adGroupWithRetargetingWithAutoPrices() {
        TargetInterest retargeting = randomPriceRetargeting()
                .withPriceContext(null);
        ComplexTextAdGroup adGroup = adGroupWithRetargetings(singletonList(retargeting));
        addWithAutoPricesAndCheckComplexAdGroups(singletonList(adGroup));

        List<TargetInterest> retargetings = retargetingService
                .getTargetInterestsWithInterestByAdGroupIds(
                        singletonList(adGroup.getAdGroup().getId()),
                        campaign.getClientId(),
                        campaign.getShard()
                );
        assertThat("у ретаргетинга выставилась автоматическая ставка",
                retargetings.get(0).getPriceContext(),
                is(FIXED_AUTO_PRICE)
        );
    }

    @Test
    public void invalidInterestId() {
        ComplexTextAdGroup adGroup =
                adGroupWithRetargetings(singletonList(randomPriceRetargeting().withRetargetingConditionId(-1L)));
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("ошибка валидации: невалидный retargetingConditionId", result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field(TARGET_INTERESTS.name()),
                                index(0), field(TargetInterest.RETARGETING_CONDITION_ID.name())),
                                CommonDefects.validId())));
    }

    @Test
    public void maxRetargetingsInAdGroup() {
        List<TargetInterest> targetInterests = new ArrayList<>();
        for (int i = 0; i < MAX_RETARGETINGS_IN_ADGROUP; ++i) {
            RetConditionInfo retConditionInfo =
                    steps.retConditionSteps().createDefaultRetCondition(campaign.getClientInfo());
            targetInterests.add(
                    ComplexTextAdGroupTestData.randomPriceRetargeting(retConditionInfo.getRetConditionId()));
        }
        ComplexTextAdGroup adGroup = adGroupWithRetargetings(targetInterests);
        addAndCheckComplexAdGroups(singletonList(adGroup));
    }

    @Test
    public void tooManyRetargetingsInAdGroup() {
        List<TargetInterest> targetInterests = new ArrayList<>();
        for (int i = 0; i < MAX_RETARGETINGS_IN_ADGROUP + 1; ++i) {
            RetConditionInfo retConditionInfo =
                    steps.retConditionSteps().createDefaultRetCondition(campaign.getClientInfo());
            targetInterests.add(
                    ComplexTextAdGroupTestData.randomPriceRetargeting(retConditionInfo.getRetConditionId()));
        }
        ComplexTextAdGroup adGroup = adGroupWithRetargetings(targetInterests);
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("превышен лимит ретаргетингов на группу", result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field(TARGET_INTERESTS.name()),
                                index(0)), maxCollectionSizeAdGroup(MAX_RETARGETINGS_IN_ADGROUP))));
    }

    @Test
    public void oneAdGroupWithValidAndOneWithInvalidRetargeting() {
        ComplexTextAdGroup validAdGroup = adGroupWithRetargetings(singletonList(randomPriceRetargeting()));
        ComplexTextAdGroup invalidAdGroup = adGroupWithRetargetings(
                asList(randomPriceRetargeting(), randomPriceRetargeting().withRetargetingConditionId(-1L)));
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(validAdGroup, invalidAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("ошибка валидации: невалидный retargetingConditionId во второй группе",
                result.getValidationResult(), hasDefectDefinitionWith(validationError(
                        path(index(1), field(TARGET_INTERESTS.name()), index(1),
                                field(TargetInterest.RETARGETING_CONDITION_ID.name())), CommonDefects.validId())));
    }

    protected ComplexTextAdGroup adGroupWithRetargetings(List<TargetInterest> targetInterests) {
        return emptyAdGroupWithModelForAdd(campaignId, targetInterests, TARGET_INTERESTS);
    }

    protected TargetInterest randomPriceRetargeting() {
        return ComplexTextAdGroupTestData.randomPriceRetargeting(retConditionId);
    }
}
