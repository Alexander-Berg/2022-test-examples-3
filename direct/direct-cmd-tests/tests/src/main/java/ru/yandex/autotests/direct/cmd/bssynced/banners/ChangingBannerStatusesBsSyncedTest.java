package ru.yandex.autotests.direct.cmd.bssynced.banners;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.makeCampSynced;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced баннера при изменении статусов")
@Features(TestFeatures.BANNERS)
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@RunWith(Parameterized.class)
public class ChangingBannerStatusesBsSyncedTest {
    protected static final String CLIENT = "at-direct-bssync-banners1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;
    private Banner createdBanner;
    private Group createdGroup;

    public ChangingBannerStatusesBsSyncedTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сброс statusBsSynced баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DMO},
                {CampaignTypeEnum.DTO}
        });
    }

    @Before
    public void before() {
        createdGroup = bannersRule.getCurrentGroup();
        createdBanner = createdGroup.getBanners().get(0);
        makeCampSynced(cmdRule, bannersRule.getCampaignId());
    }

    @Test
    @Description("Сброс bsSynced у баннера при остановке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9288")
    public void stoppingBannerShouldResetStatusBsSynced() {
        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.NO);
        check();
    }

    @Test
    @Description("Сброс bsSynced у баннера при запуске")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9289")
    public void startingBannerShouldResetStatusBsSynced() {
        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.NO);
        makeCampSynced(cmdRule, bannersRule.getCampaignId());
        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.YES);
        check();
    }

    @Test
    @Description("Сброс bsSynced у баннера при архивации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9290")
    public void archivingBannerShouldResetStatusBsSynced() {
        cmdRule.cmdSteps().bannerSteps()
                .setBannersStatusShow(bannersRule.getGroupId(), singletonList(bannersRule.getBannerId()),
                        PerlBoolean.NO);
        cmdRule.cmdSteps().bannerSteps().archiveBanner(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(),
                bannersRule.getBannerId());
        check();
    }

    @Test
    @Description("Сброс bsSynced у баннера при разархивации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9291")
    public void unarchivingBannerShouldResetStatusBsSynced() {
        cmdRule.cmdSteps().bannerSteps()
                .setBannersStatusShow(bannersRule.getGroupId(), singletonList(bannersRule.getBannerId()),
                        PerlBoolean.NO);
        cmdRule.cmdSteps().bannerSteps().archiveBanner(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(),
                bannersRule.getBannerId());
        makeCampSynced(cmdRule, bannersRule.getCampaignId());
        cmdRule.cmdSteps().bannerSteps().unarchiveBanner(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(),
                bannersRule.getBannerId());
        check();
    }

    private void check() {
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }


}
