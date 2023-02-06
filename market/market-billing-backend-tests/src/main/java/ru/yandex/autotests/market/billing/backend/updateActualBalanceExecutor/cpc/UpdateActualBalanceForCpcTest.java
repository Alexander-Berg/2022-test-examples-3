package ru.yandex.autotests.market.billing.backend.updateActualBalanceExecutor.cpc;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 22/04/15
 */

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignBalanceActual;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignBillingNow;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.ClicksGenerationSteps;
import ru.yandex.autotests.market.billing.backend.steps.UpdateActualBalanceExecutorSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.hazelcast.LockRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Aqua.Test(title = "Проверка обиливания кликов")
@Feature("Cpc")
@Description("Тест генерирует клики, ждёт их раскладки в биллинге, запускает tms задачу updateActualBalanceExecutor и " +
        "проверяет правильное обновление баланса компании во вьюхе market_billing.campaign_balance")
@RunWith(Parameterized.class)
public class UpdateActualBalanceForCpcTest {

    @ClassRule
    public static LockRule lockRule = new LockRule("Lock by campaign id 243245");

    @Parameterized.Parameters(name = "Герерируем {0} кликов с dayAgo={1}, pp={2}, shopId={3}, clickPrice={4}")
    public static Collection<Object[]> testData() {
        return new ArrayList<Object[]>() {{
            add(new Object[]{10, 0, 243245L, "37"});
        }};
    }

    @Parameterized.Parameter(0)
    public static int clicksCount;

    @Parameterized.Parameter(1)
    public static int daysAgo;

    @Parameterized.Parameter(2)
    public static Long shopId;

    @Parameterized.Parameter(3)
    public static String clickPrice;

    ClicksGenerationSteps generationSteps = ClicksGenerationSteps.getInstance();
    UpdateActualBalanceExecutorSteps uabeSteps = UpdateActualBalanceExecutorSteps.getInstance();
    BillingDaoSteps billingDaoSteps = BillingDaoSteps.getInstance();

    @Test
    public void updateActualBalanceTest() throws IOException {

        //запоминаем баланс компании до генерации кликов
        Long campaignId = billingDaoSteps.getCampaignInfo(shopId).getCampaignId();
        CampaignBalanceActual cbaBefore = billingDaoSteps.getCampaignBalanceActual(campaignId);

        //генерируем клики
        generationSteps.generateLbClicksAndWait(clicksCount, daysAgo, shopId, clickPrice);

        //обновляем актуальный баланс
        MarketBillingConsoleFactory.connectToBilling().updateActualBalance();

        //получаем агрегированную информацию о кликах, заказах и компенсациях на текущий момент
        CampaignBillingNow cbn = uabeSteps.getCampaignBillingNow(campaignId);

        //получаем актуальный баланс компании
        CampaignBalanceActual cbaAfter = billingDaoSteps.getCampaignBalanceActual(campaignId);

        //на основании начального баланса и агрегированной информации, вычилисляем ожидаемый баланс и сравниваем с актуальным
        uabeSteps.checkCampaignBalance(cbaBefore, cbaAfter, cbn);
    }

}
