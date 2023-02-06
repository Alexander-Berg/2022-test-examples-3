package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Негативные сценарии изменения графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(CmdTag.UPLOAD_IMAGE)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class NegativeChangeCreativeImageBannerTest extends NegativeCreativeImageBannerTestBase {

    public NegativeChangeCreativeImageBannerTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        campaignRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    }

    @Override
    protected GroupsParameters getGroupParameters() {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_GROUP_TEMPLATE.get(campaignType), Group.class)
                .withBanners(Collections.singletonList(newBanner));
        group.setAdGroupID(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .getPhrasesByCid(campaignId).get(0)
                .getPid().toString());
        group.getBanners().get(0).setBid(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .getPhrasesByCid(campaignId).get(0).getBid());
        return GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9263")
    public void imageBannerWithoutCreativeId() {
        super.imageBannerWithoutCreativeId();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9264")
    public void imageBannerWithInvalidCreativeId() {
        super.imageBannerWithInvalidCreativeId();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9265")
    public void imageBannerWithoutCreative() {
        super.imageBannerWithoutCreative();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9262")
    public void imageBannerWithAnotherLoginImageCreative() {
        super.imageBannerWithAnotherLoginImageCreative();
    }
}
