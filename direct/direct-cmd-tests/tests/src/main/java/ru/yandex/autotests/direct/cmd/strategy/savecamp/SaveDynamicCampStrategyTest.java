package ru.yandex.autotests.direct.cmd.strategy.savecamp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.strategy.testdata.StrategyTestData;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

@Aqua.Test
@Description("Проверка сохранения стратегий для контроллера saveCamp (ДТО кампания)")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveDynamicCampStrategyTest extends SaveCampStrategyTestBase {

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return StrategyTestData.getDynamicCampStrategiesList();
    }

    @Override
    protected CampaignRule getCampaignRule() {
        return new DynamicBannersRule().withUlogin(CLIENT);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10021")
    public void checkCampaignStrategyBlock() {
        super.checkCampaignStrategyBlock();
    }
}
