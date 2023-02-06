package ru.yandex.autotests.market.billing.backend.completeDailyExecutor;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 06/04/15
 */

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.ConsoleConnector;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleResource;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignBillingDay;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignInfo;
import ru.yandex.autotests.market.billing.backend.steps.CompleteDailyExecutorSteps;
import ru.yandex.qatools.allure.annotations.Description;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assume.assumeThat;

@Aqua.Test(title = "Проверка ежедневного обиливания")
@Feature("Tms")
@Description("Тест берёт кампании, отматывает закрытые дни, запускает tms зачачу completeDailyExecutor " +
        "и сверяет правильность расчётов баланса, расходов на клики, сервисные платежи, открутки, компенсации")
@RunWith(Parameterized.class)
public class DailyBillingTest {

    @ClassRule
    public static MarketBillingConsoleResource consoleResource = new MarketBillingConsoleResource(ConsoleConnector.BILLING);
    private static CompleteDailyExecutorSteps completeDailyExecutorSteps = new CompleteDailyExecutorSteps();

    @Parameterized.Parameter
    public Long campaignIdFrom;

    @Parameterized.Parameter(1)
    public Long campaignIdTill;

    @Parameterized.Parameter(2)
    public LocalDate day;

    @Parameterized.Parameter(3)
    public Integer fid;

    @Parameterized.Parameter(4)
    public Integer period;

    @Parameterized.Parameter(5)
    public Integer billingType;

    @SuppressWarnings("unused")
    @Parameterized.Parameter(6)
    public String parameterDescription;

    @Parameterized.Parameters(name = "{6}")
    public static Collection<Object[]> testData() {
        LocalDate now = LocalDate.now();
        return new ArrayList<Object[]>() {{
            add(new Object[]{null, null, now, null, null, CampaignInfo.BILLING_TYPE_FREE,
                    "Случайные кампании для тарифа FREE"});
            add(new Object[]{null, null, now, 1, 0, null,
                    "Случайные кампании с формулой 1 и периодом 0."});
            add(new Object[]{null, null, now, 3, 0, null,
                    "Случайные кампании с формулой 3 и периодом 0"});
            add(new Object[]{21171435L, 21171435L, now, null, null, null,
                    "Тестовая кампания 21171435 (zubikova2)"});
            add(new Object[]{21193300L, 21193300L, now, null, null, null,
                    "Тестовая кампания 21193300 (forautotests)"});
            add(new Object[]{21194186L, 21194186L, now, null, null, null,
                    "Тестовая кампания 21194186 (forautotests-2.yandex.ru)"});
        }};
    }

    @Test
    public void completeDailyTest() throws IOException {
        List<CampaignBillingDay> campaigns = completeDailyExecutorSteps.getCampaignBillingDays(
                campaignIdFrom, campaignIdTill, day, fid, period, billingType);

        assumeThat(campaigns, describedAs("Кампании с заданным условием есть", hasSize(greaterThan(0))));

        for (CampaignBillingDay campaign : campaigns) {
            // why, java, why?
            Duration between = Duration.between(campaign.getLastClosedDay().atStartOfDay(),
                    campaign.getLastClosed().atStartOfDay());

            // skip rollback for negative periods
            between = between.isNegative() ? Duration.ZERO : between.plusDays(1L);

            completeDailyExecutorSteps.callRollBackDayCompletion(campaign.getCampaignId(), between);
            completeDailyExecutorSteps.getCampaignBillingDayPrevoius(campaign.getCampaignId(), campaign.getLastClosedDay());
        }

        consoleResource.getConsole().runCompleteDailyExecutor();

        for (CampaignBillingDay campaign : campaigns) {
            completeDailyExecutorSteps.checkCampaignBillingDays(campaign.getCampaignId());
        }
    }
}
