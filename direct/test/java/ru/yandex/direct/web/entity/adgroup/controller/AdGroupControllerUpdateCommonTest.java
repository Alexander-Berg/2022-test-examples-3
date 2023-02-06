package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateCommonTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_EmptyAdGroup_AdGroupIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null);
        updateAndCheckResult(singletonList(requestAdGroup));

        AdGroup actualAdGroup = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat("группа не обновлена", actualAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithPageGroupTags_AdGroupUpdated() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withPageGroupTags(pageGroupTags);
        updateAndCheckResult(singletonList(requestAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getPageGroupTags(), containsInAnyOrder(pageGroupTags.toArray()));
    }

    @Test
    public void update_AdGroupWithTargetTags_AdGroupUpdated() {
        List<String> targetTags = asList("target_tag1", "target_tag2");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withTargetTags(targetTags);
        updateAndCheckResult(singletonList(requestAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getTargetTags(), containsInAnyOrder(targetTags.toArray()));
    }
}
