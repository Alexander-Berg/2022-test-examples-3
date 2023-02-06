package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.groups.CopyGroupStatusModerateBaseTest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Статус модерации динамической группы при копированиии")
@Stories(TestFeatures.Banners.COPY_GROUP)
@Features(TestFeatures.GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
public class CopyPerfomanceGroupModerationTest extends CopyGroupStatusModerateBaseTest {
    private BannersStatusmoderate bannerModerateStatus = BannersStatusmoderate.Yes;

    private BannersStatuspostmoderate bannerPostModerateStatus = BannersStatuspostmoderate.Yes;


    public CopyPerfomanceGroupModerationTest() {
        bannerRule = new PerformanceBannersRule().withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps().setBannerStatusModerate(
                bannerRule.getBannerId(), BannersStatusmoderate.valueOf(bannerModerateStatus.toString())
        );
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusPostModerate(
                bannerRule.getBannerId(), BannersStatuspostmoderate.valueOf(bannerPostModerateStatus.toString())
        );
    }

    @Override
    protected void copyGroup() {
        Group group = bannerRule.getGroup();
        group.setCampaignID(bannerRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> b.withCid(bannerRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannerRule.getCampaignId(), group);
        groupRequest.setIsGroupsCopyAction("1");
        groupRequest.setNewGroup("0");
        bannerRule.saveGroup(groupRequest);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9824")
    public void copyGroupTest() {
        super.copyGroupTest();
    }
}
