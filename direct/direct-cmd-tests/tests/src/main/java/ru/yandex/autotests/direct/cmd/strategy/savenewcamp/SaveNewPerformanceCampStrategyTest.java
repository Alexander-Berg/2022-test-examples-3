package ru.yandex.autotests.direct.cmd.strategy.savenewcamp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.strategy.testdata.StrategyTestData;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.direct.utils.strategy.data.StrategyGroup;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ru.yandex.autotests.direct.cmd.util.CommonUtils.convertEmptyToNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;

@Aqua.Test
@Description("Проверка сохранения стратегий для контроллера saveNewCamp (кампания динамических медийных объявлений)")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveNewPerformanceCampStrategyTest extends SaveNewCampStrategyTestBase {
    protected final static String SUPER = Logins.SUPER;

    public SaveNewPerformanceCampStrategyTest(Strategies strategy) {
        super(strategy);
    }

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return StrategyTestData.getPerformanceCampStrategiesList(StrategyGroup.ROI);
    }

    @Override
    protected CampaignRule getCampaignRule() {
        return new PerformanceBannersRule().withUlogin(CLIENT);
    }

    @Override
    protected String getLogin() {
        return SUPER;
    }

    @Override
    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest saveCampRequest = new SaveCampRequest();
        expectedCampaignStrategy = CmdStrategyBeans.getStrategyBean(strategy);
        if (!strategy.getStrategyGroups().contains(StrategyGroup.CPC)) {
            saveCampRequest.setMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString());
            expectedCampaignStrategy.getNet().setGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId().toString());
        }
        saveCampRequest.setJsonStrategy(expectedCampaignStrategy);
        return saveCampRequest;
    }

    @Override
    protected void checkSearchStrategy(EditCampResponse editCampResponse) {
        assertThat("Параметры стратегии совпадают с ожидаемыми",
                editCampResponse.getCampaign().getStrategy().getSearch(),
                beanDiffer(convertEmptyToNull(expectedCampaignStrategy.getSearch()))
                        .useCompareStrategy(allFieldsExcept(newPath("bid"), newPath("avgBid"), newPath("avgCpa"),
                                newPath("filterAvgBid"), newPath("sum"), newPath("goalId"), newPath("filterAvgCpa"))));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10027")
    public void checkCampaignStrategyBlock() {
        super.checkCampaignStrategyBlock();
    }
}
