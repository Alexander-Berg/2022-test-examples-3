package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalFreeCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.internalAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationInternalTest extends AdGroupsAddOperationTestBase {

    @Override
    protected CampaignInfo createModeratedCampaign() {
        Campaign campaign = activeInternalFreeCampaign(clientId, operatorUid)
                .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.YES);
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(campaign), true);
    }

    @Test
    public void prepareAndApply_AddMinusKeywords_ValidationError() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId)
                .withMinusKeywords(singletonList("minusword"));

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(InternalAdGroup.MINUS_KEYWORDS)), AdGroupDefectIds.Gen.MINUS_KEYWORDS_NOT_ALLOWED)));
    }

    @Test
    public void prepareAndApply_AddNegativeLevel_ValidationError() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, -1L);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(InternalAdGroup.LEVEL)), new Defect<>(NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN,
                new NumberDefectParams().withMin(0L)))));
    }

    @Test
    public void prepareAndApply_AddNegativeRf_ValidationError() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, 4L, 0, -1);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(InternalAdGroup.RF)), new Defect<>(NumberDefectIds.MUST_BE_GREATER_THAN_MIN,
                new NumberDefectParams().withMin(0)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(InternalAdGroup.RF_RESET)), new Defect<>(NumberDefectIds.MUST_BE_GREATER_THAN_MIN,
                new NumberDefectParams().withMin(0)))));
    }

    @Test
    public void prepareAndApply_AddLargeRf_ValidationError() {
        Integer largeRfValue = 10000;
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, 3L, largeRfValue, largeRfValue);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_AddNullRfWithNonNullRfReset_ValidationError() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, 2L, null, 3);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_IncorrectStartAndFinishTimes_ValidationError() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, null, null, null,
                LocalDateTime.now().withNano(0), LocalDateTime.now().minusDays(1).withNano(0));

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(InternalAdGroup.FINISH_TIME)),
                AdGroupDefectIds.Gen.FINISH_TIME_SHOULD_BE_GREATER_THAN_START_TIME)));
    }

    @Test
    public void prepareAndApply_Valid() {
        InternalAdGroup adGroup = internalAdGroup(campaignId, 0L);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer((AdGroup) adGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ValidAdGroup_GroupCreatedAlreadyModerated() {
        InternalAdGroup adGroup = internalAdGroup(campaignId, 0L);

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, allOf(
                hasProperty("statusBsSynced", is(StatusBsSynced.NO)),
                hasProperty("statusModerate", is(StatusModerate.YES)),
                hasProperty("statusPostModerate", is(StatusPostModerate.YES))
        ));
    }

    @Test
    public void prepareAndApply_CorrectStartAndFinishTimes_Valid() {
        InternalAdGroup adGroup = activeInternalAdGroup(campaignId, null, null, null,
                LocalDateTime.now().withNano(0), LocalDateTime.now().plusDays(1).withNano(0));

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));
    }
}
