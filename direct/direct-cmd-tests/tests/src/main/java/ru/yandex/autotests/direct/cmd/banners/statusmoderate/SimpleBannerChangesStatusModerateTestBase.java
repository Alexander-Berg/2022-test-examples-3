package ru.yandex.autotests.direct.cmd.banners.statusmoderate;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class SimpleBannerChangesStatusModerateTestBase {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long campaignId;
    private Banner expectedBanner;

    public CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "statusModerate баннера после незначительных изменений. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    public SimpleBannerChangesStatusModerateTestBase(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withUlogin(CLIENT);
        expectedBanner = bannersRule.getBanner();
        bannersRule.overrideBannerTemplate(new Banner()
                .withTitle(expectedBanner.getTitle().toLowerCase().replace(" ", ","))
                .withBody(expectedBanner.getBody().toLowerCase().replace(" ", ",")));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    protected abstract String getStatusModerate();
    protected abstract String getExpectedStatusModerate();

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());

        cmdRule.apiSteps().bannersFakeSteps().setStatusModerate(bannersRule.getBannerId(), getStatusModerate());
    }

    @Description("Добавление пробелов после запятой в заголовке баннера")
    public void addSpaceAfterCommaBannerTitle() {
        Group saveGroup = getGroup();
        saveGroup.getBanners().get(0).setTitle(expectedBanner.getTitle().replace(",", ", "));
        sendAndCheck(saveGroup);
    }

    @Description("Добавление пробелов после запятой в тексте баннера")
    public void addSpaceAfterCommaBannerBody() {
        Group saveGroup = getGroup();
        saveGroup.getBanners().get(0).setBody(expectedBanner.getBody().replace(",", ", "));
        sendAndCheck(saveGroup);
    }

    private void sendAndCheck(Group group) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("Статус модерации не изменился", actualGroup.getBanners().get(0).getStatusModerate(),
                equalTo(getExpectedStatusModerate()));
    }

    private Group getGroup() {
        Group group = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, campaignType);
        return group;
    }
}
