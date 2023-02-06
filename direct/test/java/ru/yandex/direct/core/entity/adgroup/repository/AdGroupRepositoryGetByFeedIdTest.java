package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupRepositoryGetByFeedIdTest {
    @Autowired
    public Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;
    private ClientInfo clientInfo;
    private FeedInfo feedForText;
    private FeedInfo feedForDynamic;
    private FeedInfo feedForSmart;
    private CampaignInfo dynamicCampaign;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
        feedForText = steps.feedSteps().createDefaultFeed(clientInfo);
        feedForDynamic = steps.feedSteps().createDefaultFeed(clientInfo);
        feedForSmart = steps.feedSteps().createDefaultFeed(clientInfo);
        dynamicCampaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
    }

    @Test
    public void getAdGroupIdsByFeedId_eachGroupTypeHasOwnFeed() {
        var dynamicGroup = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo,
                activeDynamicFeedAdGroup(dynamicCampaign.getCampaignId(), feedForDynamic.getFeedId()));
        var smartGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedForSmart.getFeedId());
        var actual
                = adGroupRepository.getAdGroupIdsByFeedId(clientInfo.getShard(), List.of(feedForText.getFeedId(),
                feedForDynamic.getFeedId(), feedForSmart.getFeedId()));
        assertThat(actual).containsOnly(
                entry(feedForDynamic.getFeedId(), List.of(dynamicGroup.getAdGroupId())),
                entry(feedForSmart.getFeedId(), List.of(smartGroup.getAdGroupId())));
    }

    @Test
    public void getAdGroupIdsByFeedId_allGroupTypesHasOneFeed() {
        var dynamicGroup = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo,
                activeDynamicFeedAdGroup(dynamicCampaign.getCampaignId(), feedForText.getFeedId()));
        var smartGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedForText.getFeedId());
        var actual
                = adGroupRepository.getAdGroupIdsByFeedId(clientInfo.getShard(), List.of(feedForText.getFeedId()));
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(actual).containsOnlyKeys(feedForText.getFeedId());
        assertions.assertThat(actual.get(feedForText.getFeedId()))
                .containsExactlyInAnyOrder(dynamicGroup.getAdGroupId(), smartGroup.getAdGroupId());
        assertions.assertAll();
    }

}
