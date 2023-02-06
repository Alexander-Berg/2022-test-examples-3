package ru.yandex.autotests.direct.cmd.banners.forex;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
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
@Description("Кейсы когда DTO баннеры не должны отправляться на модерацию с флагом forex")
@Stories(TestFeatures.Banners.MANAGE_VCARDS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.FOREX)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.DYNAMIC)
public class NotRemoderateForexDynamicBannerBaseTest extends ForexDynamicBannerNegativeBaseTest {

    public BannersRule getBannerRule() {
        return new DynamicBannersRule();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9150")
    public void changeCampaignTargetingEuropeToEurope() {
        super.changeCampaignTargetingEuropeToEurope();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9151")
    public void changeCampaignTargetingRegionOfRussiaToRussia() {
        super.changeCampaignTargetingRegionOfRussiaToRussia();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9152")
    public void changeCampaignTargetingCrimeaToRussia() {
        super.changeCampaignTargetingCrimeaToRussia();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9153")
    public void campTargetingToRussiaWithoutForexFlag() {
        super.campTargetingToRussiaWithoutForexFlag();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9154")
    public void campTargetingToRegionOfRussiaWithoutForexFlag() {
        super.campTargetingToRegionOfRussiaWithoutForexFlag();
    }

}
