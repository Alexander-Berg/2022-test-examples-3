package ru.yandex.autotests.direct.cmd.strategy.savecamp;

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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

@Aqua.Test
@Description("Проверка сохранения стратегий для контроллера saveCamp (текстовая кампания)")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveTextCampStrategyTest extends SaveCampStrategyTestBase {

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return StrategyTestData.getTextCampStrategiesList();
    }

    @Override
    protected CampaignRule getCampaignRule() {
        return new TextBannersRule().withUlogin(CLIENT);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10024")
    public void checkCampaignStrategyBlock() {
        super.checkCampaignStrategyBlock();
    }
}
