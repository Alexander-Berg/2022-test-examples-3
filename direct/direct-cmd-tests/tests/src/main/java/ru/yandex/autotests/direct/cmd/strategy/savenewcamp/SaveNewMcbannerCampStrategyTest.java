package ru.yandex.autotests.direct.cmd.strategy.savenewcamp;

import java.util.Collection;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.strategy.testdata.StrategyTestData;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.direct.utils.strategy.data.StrategyGroup;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сохранения стратегий для контроллера saveNewCamp (ГО на Поиске)")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.MCBANNER)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveNewMcbannerCampStrategyTest extends SaveNewCampStrategyTestBase {

    public SaveNewMcbannerCampStrategyTest(Strategies strategy) {
        super(strategy);
    }

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return StrategyTestData.getTextCampStrategiesList(StrategyGroup.ROI, StrategyGroup.CPA);
    }

    @Override
    protected CampaignRule getCampaignRule() {
        return new TextBannersRule().withUlogin(CLIENT);
    }

    @Test
    @Override
    @TestCaseId("10974")
    public void checkCampaignStrategyBlock() {
        super.checkCampaignStrategyBlock();
    }
}
