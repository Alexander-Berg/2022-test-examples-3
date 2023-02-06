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
@Description("Кейсы когда текстовые баннеры не должны отправляться на модерацию с флагом forex")
@Stories(TestFeatures.Banners.MANAGE_VCARDS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.FOREX)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class NotRemoderateForexTextBannerBaseTest extends ForexDynamicBannerNegativeBaseTest {

    public BannersRule getBannerRule() {
        return new TextBannersRule();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9160")
    public void changeCampaignTargetingEuropeToEurope() {
        super.changeCampaignTargetingEuropeToEurope();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9161")
    public void changeCampaignTargetingRegionOfRussiaToRussia() {
        super.changeCampaignTargetingRegionOfRussiaToRussia();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9162")
    public void changeCampaignTargetingCrimeaToRussia() {
        super.changeCampaignTargetingCrimeaToRussia();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9163")
    public void campTargetingToRussiaWithoutForexFlag() {
        super.campTargetingToRussiaWithoutForexFlag();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9164")
    public void campTargetingToRegionOfRussiaWithoutForexFlag() {
        super.campTargetingToRegionOfRussiaWithoutForexFlag();
    }

}
