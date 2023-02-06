package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsAddOperationDynSmartTest extends AdGroupsAddOperationTestBase {
    // TODO: когда-нибудь это должно стать одним тестом..
    @Test
    public void performance_prepareAndApply_success() {
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
    public void dynamicFeed_prepareAndApply_success() {
        //Создаём группу
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        CampaignInfo dynamicCampaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        Long campaignId = dynamicCampaign.getCampaignId();
        DynamicFeedAdGroup adGroup = TestGroups.clientDynamicFeedAdGroup(campaignId, feedInfo.getFeedId());

        //Ожидаемое состояние полей специфичных для данного типа группы
        DynamicFeedAdGroup expected = new DynamicFeedAdGroup()
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
    public void dynamicText_prepareAndApply_success() {
        //Создаём группу
        CampaignInfo dynamicCampaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        var domainInfo = steps.domainSteps().createDomain(clientInfo.getShard(), "yandex.ru", true);
        Long campaignId = dynamicCampaign.getCampaignId();
        DynamicTextAdGroup adGroup = TestGroups.clientDynamicTextAdGroup(campaignId, domainInfo.getDomainId(),
                domainInfo.getDomain().getDomain());

        //Ожидаемое состояние полей специфичных для данного типа группы
        DynamicTextAdGroup expected = new DynamicTextAdGroup()
                .withType(adGroup.getType())
                .withCampaignId(adGroup.getCampaignId())
                .withMainDomainId(domainInfo.getDomainId())
                .withDomainUrl(domainInfo.getDomain().getDomain())
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
}
