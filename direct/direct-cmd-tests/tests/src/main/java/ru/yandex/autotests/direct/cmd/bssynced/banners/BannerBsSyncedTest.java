package ru.yandex.autotests.direct.cmd.bssynced.banners;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка статуса bsSynced для новых баннеров")
@Features(TestFeatures.BANNERS)
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(CampTypeTag.MCBANNER)
@RunWith(Parameterized.class)
public class BannerBsSyncedTest {

    protected static final String CLIENT = "at-direct-bssync-banners1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;

    public BannerBsSyncedTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "bsSynced баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DMO},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.MCBANNER}
        });
    }

    @Test
    @Description("Для нового баннера statusBsSynced No")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9287")
    public void statusForNewBannerIsNo() {
        BsSyncedHelper.checkGroupBsSynced(CLIENT, bannersRule.getGroupId(), PhrasesStatusbssynced.No);
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }

}
