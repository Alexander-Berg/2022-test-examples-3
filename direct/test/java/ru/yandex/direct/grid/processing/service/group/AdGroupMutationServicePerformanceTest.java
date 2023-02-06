package ru.yandex.direct.grid.processing.service.group;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddSmartAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddSmartAdGroupItem;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupMutationServicePerformanceTest {
    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AdGroupMutationService adGroupMutationService;

    @Test
    public void addPerformanceAdGroups_success_whenCampaignGeoIsSet() {
        //Создаём исходные данные, в т.ч. компанию с заданным гео
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Set<Integer> campaignGeo = StreamEx.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID)
                .map(Long::intValue)
                .toSet();
        Campaign campaign = TestCampaigns.activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long uid = clientInfo.getUid();
        User user = userRepository.fetchByUids(clientInfo.getShard(), singletonList(uid)).get(0);
        GdAddSmartAdGroupItem newItem = new GdAddSmartAdGroupItem()
                .withName("Test AdGroup Name")
                .withCampaignId(campaignInfo.getCampaignId())
                .withFeedId(feedInfo.getFeedId());
        GdAddSmartAdGroup newGdAddPerformanceAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(newItem));

        //Ожидаемые результаты
        PerformanceAdGroup expectedPerformanceAdGroup = new PerformanceAdGroup()
                .withName(newItem.getName())
                .withCampaignId(newItem.getCampaignId())
                .withFeedId(newItem.getFeedId());
        Integer[] expectedRegionIds = StreamEx.of(campaignGeo)
                .toArray(Integer.class);

        //Выполняем запрос
        TestAuthHelper.setDirectAuthentication(user);
        GridGraphQLContext context = buildContext(user);
        GdAddAdGroupPayload gdAddAdGroupPayload =
                adGroupMutationService.addPerformanceAdGroups(user.getClientId(), uid, newGdAddPerformanceAdGroup);
        checkState(isNotEmpty(gdAddAdGroupPayload.getAddedAdGroupItems()));

        //Сверяем ожидания и реальность
        Long adGroupId = gdAddAdGroupPayload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
        Integer[] actualRegionIds = StreamEx.of(actualAdGroup.getGeo())
                .map(Long::intValue)
                .toArray(Integer.class);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualAdGroup)
                    .is(matchedBy(beanDiffer(expectedPerformanceAdGroup)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualRegionIds).containsExactlyInAnyOrder(expectedRegionIds);
        });
    }
}
