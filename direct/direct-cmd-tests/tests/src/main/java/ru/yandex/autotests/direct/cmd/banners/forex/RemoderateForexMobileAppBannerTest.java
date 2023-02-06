package ru.yandex.autotests.direct.cmd.banners.forex;


import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * https://st.yandex-team.ru/DIRECT-50356
 */
@Aqua.Test
@Description("Переотправка мобильных баннеров на модерацию с флагом forex")
@Stories(TestFeatures.Banners.MANAGE_VCARDS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.FOREX)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.MOBILE)
public class RemoderateForexMobileAppBannerTest extends ForexDynamicBannerBaseTest {

    public BannersRule getBannerRule() {
        return new MobileBannersRule();
    }

    @Test
    @Description("Меняем таргетинг кампании на Россию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9172")
    public void changeCampaignTargetingToRussia() {
        super.changeCampaignTargetingToRussia();
    }


    @Test
    @Description("Таргетинг кампании начинает включать в себя Россию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9173")
    public void includeRussiaToCampaignTargeting() {
        super.includeRussiaToCampaignTargeting();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя Россию и другой регион")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9174")
    public void includeRussiaAndUkraineToCampaignTargeting() {
        super.includeRussiaAndUkraineToCampaignTargeting();
    }

    @Test
    @Description("Меняем таргетинг кампании на регион России")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9175")
    public void changeCampaignTargetingToRegionOfRussia() {
        super.changeCampaignTargetingToRegionOfRussia();
    }

    @Test
    @Description("Меняем таргетинг кампании на Крым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9176")
    public void changeCampaignTargetingToCrimea() {
        super.changeCampaignTargetingToCrimea();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя один регион России")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9177")
    public void includeRegionOfRussiaToCampaignTargeting() {
        super.includeRegionOfRussiaToCampaignTargeting();
    }

    @Test
    @Description("Таргетинг кампании начинает включать в себя Крым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9178")
    public void includeCrimeaToCampaignTargeting() {
        super.includeCrimeaToCampaignTargeting();
    }

}
