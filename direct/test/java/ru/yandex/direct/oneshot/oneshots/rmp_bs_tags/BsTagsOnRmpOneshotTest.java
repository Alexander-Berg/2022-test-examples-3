package ru.yandex.direct.oneshot.oneshots.rmp_bs_tags;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;

@OneshotTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class BsTagsOnRmpOneshotTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BsTagsOnRmpOneshot oneshot;
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Test
    public void testValidate_BadCampType() {
        var campInfo = steps.campaignSteps().createActiveTextCampaign();
        var inputData = new InputData();
        inputData.setCampaignId(campInfo.getCampaignId());
        inputData.setTag("hellowo");

        var vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).isTrue();
    }

    @Test
    public void testValidate_GoodCampType() {
        var campInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        var inputData = new InputData();
        inputData.setCampaignId(campInfo.getCampaignId());
        inputData.setTag("helloworlde");

        var vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void testExecute_OverwriteTags() {
        var campInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        var adGroup1Info = steps.adGroupSteps().createAdGroup(
                new AdGroupInfo()
                        .withAdGroup(
                                activeMobileAppAdGroup(null)
                                        .withTargetTags(List.of("test target", "test tag"))
                                        .withPageGroupTags(List.of("test page group", "test tags"))
                                        .withStatusBsSynced(StatusBsSynced.YES)
                        )
                        .withCampaignInfo(campInfo)
        );
        var adGroup1Id = adGroup1Info.getAdGroupId();
        var adGroup2Info = steps.adGroupSteps().createActiveMobileContentAdGroup(campInfo);
        var adGroup2Id = adGroup2Info.getAdGroupId();

        var inputData = new InputData();
        inputData.setTag("new test tag");
        inputData.setCampaignId(campInfo.getCampaignId());
        oneshot.execute(inputData, null);

        var adGroups = adGroupRepository.getAdGroups(
                campInfo.getShard(),
                List.of(adGroup1Info.getAdGroupId(), adGroup2Info.getAdGroupId())
        );
        var actualTargetTags = StreamEx.of(adGroups).mapToEntry(AdGroup::getId, AdGroup::getTargetTags).toMap();
        var actualPageGroupTags = StreamEx.of(adGroups).mapToEntry(AdGroup::getId, AdGroup::getPageGroupTags).toMap();
        assertThat(actualTargetTags).containsOnly(
                entry(adGroup1Id, singletonList("new test tag")),
                entry(adGroup2Id, singletonList("new test tag"))
        );
        assertThat(actualPageGroupTags).containsOnly(
                entry(adGroup1Id, singletonList("new test tag")),
                entry(adGroup2Id, singletonList("new test tag"))
        );
        var actualStatusesBsSynced = StreamEx.of(adGroups)
                .mapToEntry(AdGroup::getId, AdGroup::getStatusBsSynced).toMap();
        assertThat(actualStatusesBsSynced).containsOnly(
                entry(adGroup1Id, StatusBsSynced.NO),
                entry(adGroup2Id, StatusBsSynced.NO)
        );
    }

    @Test
    public void testExecute_RemoveTags() {
        var campInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        var adGroup1Info = steps.adGroupSteps().createAdGroup(
                new AdGroupInfo()
                        .withAdGroup(
                                activeMobileAppAdGroup(null)
                                        .withTargetTags(List.of("test target", "test tag"))
                                        .withPageGroupTags(List.of("test page group", "test tags"))
                        )
                        .withCampaignInfo(campInfo)
        );
        var adGroup1Id = adGroup1Info.getAdGroupId();
        var adGroup2Info = steps.adGroupSteps().createActiveMobileContentAdGroup(campInfo);
        var adGroup2Id = adGroup2Info.getAdGroupId();

        var inputData = new InputData();
        inputData.setTag(null);
        inputData.setCampaignId(campInfo.getCampaignId());
        oneshot.execute(inputData, null);

        var adGroups = adGroupRepository.getAdGroups(
                campInfo.getShard(),
                List.of(adGroup1Info.getAdGroupId(), adGroup2Info.getAdGroupId())
        );
        var actualTargetTags = StreamEx.of(adGroups).mapToEntry(AdGroup::getId, AdGroup::getTargetTags).toMap();
        var actualPageGroupTags = StreamEx.of(adGroups).mapToEntry(AdGroup::getId, AdGroup::getPageGroupTags).toMap();
        assertThat(actualTargetTags).as("actual targetTags").containsOnly(
                entry(adGroup1Id, emptyList()),
                entry(adGroup2Id, emptyList())
        );
        assertThat(actualPageGroupTags).as("actual pageGroupTags").containsOnly(
                entry(adGroup1Id, emptyList()),
                entry(adGroup2Id, emptyList())
        );
    }
}
