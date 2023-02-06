package ru.yandex.autotests.direct.cmd.bssynced.href;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сброс статуса statusBsSynced группы и баннера при добавлении параметра для учета корректировок в URL")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class AddCoefGoalContextIdTest extends CoefGoalContextIdBaseTest {

    public AddCoefGoalContextIdTest(CampaignTypeEnum campaignType, String href) {
        this.campaignType = campaignType;
        this.href = href;
        bannerRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideBannerTemplate(new Banner().withHref(href))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }

    public String getNewHref() {
        return href + PARAM_WITH_COEF_GOAL_CONTEXT;
    }

    @Test
    @Description("Сброс bsSynced группы и баннера при добавлении параметра для учета корректировок в URL")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9340")
    public void checkBsSyncedAfterCoefGoalContextIdAdded() {
        super.test();
    }

    public void check() {
        BsSyncedHelper.checkGroupBsSynced(bannerRule.getCurrentGroup(), StatusBsSynced.NO);
        assertThat("статус bsSynced баннера сбросился",
                bannerRule.getCurrentGroup().getBanners().get(0).getStatusBsSynced(),
                equalTo(StatusBsSynced.NO.toString()));
    }
}
