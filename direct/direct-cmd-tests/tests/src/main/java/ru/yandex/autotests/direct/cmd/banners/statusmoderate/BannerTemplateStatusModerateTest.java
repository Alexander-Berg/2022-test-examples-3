package ru.yandex.autotests.direct.cmd.banners.statusmoderate;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Статус модерации шаблонного баннера после изменении фразы")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag("TESTIRT-9435")
@RunWith(Parameterized.class)
public class BannerTemplateStatusModerateTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String TITLE_TEMPLATE = "#Заголовок#";
    private static final String BODY_TEMPLATE = "#Текст#";
    private static final String PHRASE = "Другая фраза";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long campaignId;
    private Group expectedGroup;

    public CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "statusModerate шаблонного баннера после изменения фразы. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    public BannerTemplateStatusModerateTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideBannerTemplate(new Banner()
                        .withTitle(TITLE_TEMPLATE)
                        .withBody(BODY_TEMPLATE))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        expectedGroup = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, campaignType);
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
    }

    @Test
    @Description("statusModerate баннера при изменении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10739")
    public void changeTitleCase() {
        expectedGroup.getPhrases().get(0).withPhrase(PHRASE);
        sendAndCheck();
    }

    private void sendAndCheck() {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, expectedGroup));
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("Статус модерации сбросился", actualGroup.getBanners().get(0).getStatusModerate(),
                equalTo(StatusModerate.READY.toString()));
    }
}
