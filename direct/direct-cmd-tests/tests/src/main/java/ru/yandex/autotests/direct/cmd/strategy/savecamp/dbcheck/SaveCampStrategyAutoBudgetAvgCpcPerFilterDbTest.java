package ru.yandex.autotests.direct.cmd.strategy.savecamp.dbcheck;

import java.math.BigDecimal;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.db.beans.campaign.StrategyData;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStrategyName;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сохранения стратегии autobudget_avg_cpc_per_filter в бд")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.STRATEGY)
@Tag(CampTypeTag.PERFORMANCE)
public class SaveCampStrategyAutoBudgetAvgCpcPerFilterDbTest extends SaveCampStrategyDbTestBase {

    @Override
    protected CampaignRule getCampaignRule() {
        return new PerformanceBannersRule().withUlogin(getLogin());
    }

    @Override
    protected CampaignStrategy getCampaignStrategy() {
        CampaignStrategy campaignStrategy = CmdStrategyBeans.getStrategyBean(Strategies.CPC_OPTIMIZATION_FILTER);
        campaignStrategy.getNet()
                .withAvgBid(AVG_BID.toString())
                .withBid(BID.toString())
                .withSum(SUM.toString());
        return campaignStrategy;
    }

    @Override
    protected CampaignsRecord getExpectedCampaignDb() {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpc_per_filter)
                .withFilterAvgBid(BigDecimal.valueOf(AVG_BID))
                .withBid(BID)
                .withSum(SUM)
                .withVersion(1);
        return new CampaignsRecord()
                .setStrategyName(CampaignsStrategyName.autobudget_avg_cpc_per_filter)
                .setStrategyData(strategyData.toJsonElement());
    }

    @Test
    @Override
    @TestCaseId("10996")
    public void checkCampaignStrategyDb() {
        super.checkCampaignStrategyDb();
    }
}
