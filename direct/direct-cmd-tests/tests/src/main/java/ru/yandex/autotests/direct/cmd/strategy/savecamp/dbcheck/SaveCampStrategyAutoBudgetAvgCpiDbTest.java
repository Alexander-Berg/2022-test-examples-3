package ru.yandex.autotests.direct.cmd.strategy.savecamp.dbcheck;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
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
import ru.yandex.qatools.allure.annotations.TestCaseId;

@Aqua.Test
@Description("Проверка сохранения стратегии autobudget_avg_cpi в бд")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.STRATEGY)
@Tag(CampTypeTag.MOBILE)
public class SaveCampStrategyAutoBudgetAvgCpiDbTest extends SaveCampStrategyDbTestBase {

    private static final Double AVG_CPI = 3.0d;

    @Override
    protected CampaignRule getCampaignRule() {
        return new MobileBannersRule().withUlogin(getLogin());
    }

    @Override
    protected CampaignStrategy getCampaignStrategy() {
        CampaignStrategy campaignStrategy =
                CmdStrategyBeans.getStrategyBean(Strategies.AVERAGE_CPI_OPTIMIZATION_DEFAULT);
        campaignStrategy.getSearch()
                .withAvgCpi(AVG_CPI)
                .withBid(BID.toString())
                .withSum(SUM.toString());
        return campaignStrategy;
    }

    @Override
    protected CampaignsRecord getExpectedCampaignDb() {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpi)
                .withAvgCpi(AVG_CPI)
                .withBid(BID)
                .withSum(SUM)
                .withPayForConversion(0)
                .withVersion(1);
        return new CampaignsRecord()
                .setStrategyName(CampaignsStrategyName.autobudget_avg_cpi)
                .setStrategyData(strategyData.toJsonElement());
    }

    @Test
    @Override
    @TestCaseId("10997")
    public void checkCampaignStrategyDb() {
        super.checkCampaignStrategyDb();
    }
}
