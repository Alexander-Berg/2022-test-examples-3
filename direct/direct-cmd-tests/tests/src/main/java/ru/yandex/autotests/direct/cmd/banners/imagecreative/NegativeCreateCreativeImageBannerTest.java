package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Негативные сценарии сохранения графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class NegativeCreateCreativeImageBannerTest extends NegativeCreativeImageBannerTestBase {

    public NegativeCreateCreativeImageBannerTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        campaignRule = new CampaignRule().withMediaType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER).withRules(campaignRule);
    }

    @Before
    public void before() {
        super.before();
    }

    @Override
    protected GroupsParameters getGroupParameters() {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_GROUP_TEMPLATE.get(campaignType), Group.class)
                .withBanners(Collections.singletonList(newBanner));
        return GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9267")
    public void imageBannerWithoutCreativeId() {
        super.imageBannerWithoutCreativeId();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9268")
    public void imageBannerWithInvalidCreativeId() {
        super.imageBannerWithInvalidCreativeId();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9269")
    public void imageBannerWithoutCreative() {
        super.imageBannerWithoutCreative();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9266")
    public void imageBannerWithAnotherLoginImageCreative() {
        super.imageBannerWithAnotherLoginImageCreative();
    }
}
