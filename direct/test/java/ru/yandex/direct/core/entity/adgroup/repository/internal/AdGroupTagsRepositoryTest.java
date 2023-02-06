package ru.yandex.direct.core.entity.adgroup.repository.internal;

import java.util.List;
import java.util.Map;

import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupTagsRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AdGroupTagsRepository adGroupTagsRepository;

    @Autowired
    private Steps steps;

    private int shard;
    private Configuration config;
    private ClientId clientId;
    private CampaignInfo defaultCampaign;
    private List<Long> campaignTags;

    @Before
    public void before() {
        defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        shard = defaultCampaign.getShard();
        config = dslContextProvider.ppc(shard).configuration();
        clientId = defaultCampaign.getClientId();
        campaignTags = steps.tagCampaignSteps().createDefaultTags(shard, clientId, defaultCampaign.getCampaignId(), 2);
    }

    @Test
    public void getAdGroupTags_Successful() {
        Long adGroupId = createAdGroupWithTags();
        List<Long> actual = adGroupTagsRepository.getAdGroupsTags(config, singletonList(adGroupId)).get(adGroupId);
        assertThat(actual, containsInAnyOrder(campaignTags.toArray()));
    }

    @Test
    public void addAdGroupTags_Successful() {
        AdGroupInfo defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(defaultCampaign);
        Map<Long, List<Long>> adGroupTags = singletonMap(defaultAdGroup.getAdGroupId(), campaignTags);
        adGroupTagsRepository.addAdGroupTags(config, adGroupTags);

        List<Long> actual = adGroupTagsRepository.getAdGroupsTags(config, singletonList(defaultAdGroup.getAdGroupId()))
                .get(defaultAdGroup.getAdGroupId());
        assertThat(actual, containsInAnyOrder(campaignTags.toArray()));
    }

    @Test
    public void deleteAdGroupTags_Successful() {
        Long adGroupId = createAdGroupWithTags();
        Map<Long, List<Long>> deletedAdGroupTags = singletonMap(adGroupId, singletonList(campaignTags.get(0)));

        adGroupTagsRepository.deleteFromAdGroup(config, deletedAdGroupTags);

        List<Long> expected = campaignTags.subList(1, campaignTags.size());
        List<Long> actual = adGroupTagsRepository.getAdGroupsTags(config, singletonList(adGroupId)).get(adGroupId);
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    private Long createAdGroupWithTags() {
        return steps.adGroupSteps().createAdGroup(activeTextAdGroup().withTags(campaignTags), defaultCampaign)
                .getAdGroupId();
    }
}
