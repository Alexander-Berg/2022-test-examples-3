package ru.yandex.autotests.direct.cmd.banners.forex;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * https://st.yandex-team.ru/DIRECT-50356
 */
@Aqua.Test
@Description("Переотправка текстовых баннеров на модерацию с флагом forex")
@Stories(TestFeatures.Banners.MANAGE_VCARDS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.FOREX)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class RemoderateForexTextBannerTest extends ForexDynamicBannerBaseTest {

    public BannersRule getBannerRule() {
        return new TextBannersRule();
    }

    @Test
    @Description("Меняем таргетинг кампании на Россию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9179")
    public void changeCampaignTargetingToRussia() {
        super.changeCampaignTargetingToRussia();
    }


    @Test
    @Description("Таргетинг кампании начинает включать в себя Россию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9180")
    public void includeRussiaToCampaignTargeting() {
        super.includeRussiaToCampaignTargeting();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя Россию и другой регион")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9181")
    public void includeRussiaAndUkraineToCampaignTargeting() {
        super.includeRussiaAndUkraineToCampaignTargeting();
    }

    @Test
    @Description("Меняем таргетинг кампании на регион России")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9182")
    public void changeCampaignTargetingToRegionOfRussia() {
        super.changeCampaignTargetingToRegionOfRussia();
    }

    @Test
    @Description("Меняем таргетинг кампании на Крым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9183")
    public void changeCampaignTargetingToCrimea() {
        super.changeCampaignTargetingToCrimea();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя один регион России")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9184")
    public void includeRegionOfRussiaToCampaignTargeting() {
        super.includeRegionOfRussiaToCampaignTargeting();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя Крым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9185")
    public void includeCrimeaToCampaignTargeting() {
        super.includeCrimeaToCampaignTargeting();
    }

}
