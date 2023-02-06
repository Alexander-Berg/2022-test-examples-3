package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Статус модерации ГО с креативом в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class StatusModerateForCreativeImageBannersTest {

    private static final String CLIENT = "at-direct-creative-construct1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private CreativeBannerRule bannerRule;
    private Long campaignId;
    private Long bannerId;

    public StatusModerateForCreativeImageBannersTest(CampaignTypeEnum campaignType) {
        bannerRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }

    @Parameterized.Parameters(name = "Статус модерации ГО с креативом. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
        bannerId = bannerRule.getBannerId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    @Test
    @Description("Креатив * баннер No")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9277")
    public void statusModerateNoForBanner() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.No);

        checkBannerStatusModerate(StatusModerate.NO.toString());
    }

    @Test
    @Description("Креатив No баннер *")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9273")
    public void statusModerateNoForCreative() {
        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                bannerRule.getBannerId(),
                BannersPerformanceStatusmoderate.No);

        checkBannerStatusModerate(StatusModerate.NO.toString());
    }

    @Test
    @Description("Креатив * баннер RSS")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9278")
    public void statusModerateReadyForBanner() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.Ready);

        checkBannerStatusModerate(StatusModerate.READY.toString());
    }

    @Test
    @Description("Креатив RSS баннер *")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9274")
    public void statusModerateReadyForCreative() {
        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                bannerRule.getBannerId(),
                BannersPerformanceStatusmoderate.Ready);

        checkBannerStatusModerate(StatusModerate.READY.toString());
    }

    @Test
    @Description("Креатив New баннер New")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9275")
    public void statusModerateNewForBannerAndCreative() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.New);

        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                bannerRule.getBannerId(),
                BannersPerformanceStatusmoderate.New);

        checkBannerStatusModerate(StatusModerate.NEW.toString());
    }

    @Test
    @Description("Креатив Yes баннер Yes")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9276")
    public void statusModerateReadyForBannerAndYesForCreative() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.Yes);

        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                bannerRule.getBannerId(),
                BannersPerformanceStatusmoderate.Yes);

        checkBannerStatusModerate(StatusModerate.YES.toString());
    }

    private void checkBannerStatusModerate(String expStatus) {
        Banner banner = cmdRule.cmdSteps().groupsSteps().getBanner(CLIENT, campaignId, bannerId);
        assertThat(
                "статус модерации баннера соответствует ожиданиям",
                banner.getStatusModerate(),
                beanDiffer(expStatus)
        );
    }

}
