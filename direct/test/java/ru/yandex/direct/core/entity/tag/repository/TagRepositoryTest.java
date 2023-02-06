package ru.yandex.direct.core.entity.tag.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.tag.model.CampaignTag;
import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private Steps steps;

    private int shard;
    private ClientId clientId;
    private CampaignInfo defaultCampaign;
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private Tag tag1;
    private Tag tag2;

    @Before
    public void before() {
        defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        shard = defaultCampaign.getShard();
        clientId = defaultCampaign.getClientId();

        adGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(defaultCampaign);
        adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(defaultCampaign);
        tag1 = steps.tagCampaignSteps().createDefaultTag(defaultCampaign);
        tag2 = steps.tagCampaignSteps().createDefaultTag(defaultCampaign);
        steps.tagCampaignSteps().addAdGroupTag(adGroupInfo1, tag1);
        steps.tagCampaignSteps().addAdGroupTag(adGroupInfo2, tag1);
    }

    @Test
    public void getAll_Successful() {
        Map<Long, List<Long>> actual =
                tagRepository.getCampaignsTagIds(shard, clientId, Set.of(tag1.getId(), tag2.getId()));
        assumeThat(actual.keySet(), hasSize(1));
        System.out.println(actual);
        assertThat(actual.get(defaultCampaign.getCampaignId()), containsInAnyOrder(tag1.getId(), tag2.getId()));
    }

    @Test
    public void getPartial_Successful() {
        Map<Long, List<Long>> actual =
                tagRepository.getCampaignsTagIds(shard, clientId, Set.of(tag1.getId()));
        assumeThat(actual.keySet(), hasSize(1));
        assertThat(actual.get(defaultCampaign.getCampaignId()), containsInAnyOrder(tag1.getId()));
    }

    @Test
    public void getCampaignTagsWithUseCount() {
        var tags = tagRepository.getCampaignTagsWithUseCount(shard, List.of(defaultCampaign.getCampaignId()));
        assertThat(tags, contains(beanDiffer(new CampaignTag()
                        .withId(tag1.getId())
                        .withUsesCount(2))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()),
                beanDiffer(new CampaignTag()
                        .withId(tag2.getId())
                        .withUsesCount(0))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        ));
    }
}
