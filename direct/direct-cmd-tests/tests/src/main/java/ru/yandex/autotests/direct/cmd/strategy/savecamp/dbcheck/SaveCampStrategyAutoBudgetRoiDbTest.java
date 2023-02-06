package ru.yandex.autotests.direct.cmd.strategy.savecamp.dbcheck;

import org.junit.Test;

import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.db.beans.campaign.StrategyData;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStrategyName;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сохранения стратегии autobudget_roi в бд")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.STRATEGY)
@Tag(CampTypeTag.TEXT)
public class SaveCampStrategyAutoBudgetRoiDbTest extends SaveCampStrategyDbTestBase {

    private static final Integer RESERVE_RETURN = 50;
    private static final Double PROFITABILITY = 30d;
    private static final Double ROI_COEF = 3.55d;

    @Override
    protected CampaignRule getCampaignRule() {
        return new TextBannersRule().withUlogin(getLogin());
    }

    @Override
    protected CampaignStrategy getCampaignStrategy() {
        CampaignStrategy campaignStrategy =
                CmdStrategyBeans.getStrategyBean(Strategies.ROI_OPTIMIZATION_DEFAULT);
        campaignStrategy.getSearch()
                .withBid(BID.toString())
                .withSum(SUM.toString())
                .withGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId().toString())
                .withReserveReturn(RESERVE_RETURN)
                .withRoiCoef(ROI_COEF)
                .withProfitability(PROFITABILITY);
        return campaignStrategy;
    }

    @Override
    protected CampaignsRecord getExpectedCampaignDb() {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_roi)
                .withBid(BID)
                .withSum(SUM)
                .withGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId().toString())
                .withReserveReturn(RESERVE_RETURN)
                .withRoiCoef(ROI_COEF)
                .withProfitability(PROFITABILITY)
                .withVersion(1);
        return new CampaignsRecord()
                .setStrategyName(CampaignsStrategyName.autobudget_roi)
                .setStrategyData(strategyData.toJsonElement());
    }

    @Test
    @Override
    @TestCaseId("10999")
    public void checkCampaignStrategyDb() {
        super.checkCampaignStrategyDb();
    }
}
