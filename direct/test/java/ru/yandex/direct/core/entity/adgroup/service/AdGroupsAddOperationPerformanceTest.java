package ru.yandex.direct.core.entity.adgroup.service;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.result.ResultState.BROKEN;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationPerformanceTest extends AdGroupsAddOperationTestBase {

    @Test
    public void prepareAndApply_success() {
        //Создаём группу
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        CampaignInfo performanceCampaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        Long campaignId = performanceCampaign.getCampaignId();
        PerformanceAdGroup adGroup = TestGroups.clientPerformanceAdGroup(campaignId, feedInfo.getFeedId());

        //Ожидаемое состояние полей специфичных для данного типа группы
        PerformanceAdGroup expected = new PerformanceAdGroup()
                .withType(adGroup.getType())
                .withCampaignId(adGroup.getCampaignId())
                .withFeedId(adGroup.getFeedId())
                .withStatusBLGenerated(adGroup.getStatusBLGenerated())
                .withFieldToUseAsName(adGroup.getFieldToUseAsName())
                .withFieldToUseAsBody(adGroup.getFieldToUseAsBody());

        //Выполняем операцию
        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        Long adGroupId = result.get(0).getResult();

        //Получаем реальное значение из базы
        AdGroup actual = adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);

        //Сверяем ожидания и реальность
        assertThat(actual)
                .is(matchedBy(beanDiffer(expected)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void prepareAndApply_faultValidationErrorForNotExistFeedId() {
        //Создаём группу с несуществующим feedId
        Long notExistFeedId = Integer.MAX_VALUE - 1L;
        CampaignInfo performanceCampaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        Long campaignId = performanceCampaign.getCampaignId();
        PerformanceAdGroup adGroup = TestGroups.clientPerformanceAdGroup(campaignId, notExistFeedId);

        //Выполняем операцию
        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> massResult = addOperation.prepareAndApply();
        Result<Long> result = massResult.get(0);

        //Проверяем результат
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getState()).isEqualTo(BROKEN);
            soft.assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(field(PerformanceAdGroup.FEED_ID.name())),
                            AdGroupDefects.feedNotExist(notExistFeedId)))));
        });
    }

    @Test
    public void prepareAndApply_ValidAdGroup_GroupCreatedAlreadyModerated() {
        //Создаём группу
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        CampaignInfo performanceCampaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        Long campaignId = performanceCampaign.getCampaignId();
        PerformanceAdGroup adGroup = TestGroups.clientPerformanceAdGroup(campaignId, feedInfo.getFeedId());

        AdGroupsAddOperation addOperation = createAddOperation(singletonList(adGroup), false);
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getErrorCount()).isZero();

        AdGroup actual = adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroup.getId())).get(0);

        PerformanceAdGroup expected = new PerformanceAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO);
        assertThat(actual)
                .is(matchedBy(beanDiffer(expected)
                        .useCompareStrategy(onlyExpectedFields())));
    }
}
