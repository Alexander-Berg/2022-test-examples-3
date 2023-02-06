package ru.yandex.autotests.market.billing.backend.balance;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignBalance;
import ru.yandex.autotests.market.billing.backend.steps.BalanceNotificationSteps;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 10/16/15
 */
@Aqua.Test(title = "Тесты нотификации от Баланса. Негативные кейсы")
@Feature("balance")
@Description("Подробное описание теста https://st.yandex-team.ru/AUTOTESTMARKET-1079 \n" +
        "Проверяем, как изменился баланс в биллинге(market_billing.campaign_balance)\n" +
        " после отсылки  туда нового  баланса но тестовому заказу, а так же регистрацию платежа (market_billing.campaign_payments)\n" +
        " Для тестирования выбираем заказ с нулевым балансом")
@RunWith(Parameterized.class)
public class BalanceNotificationNegativeTest {

    @Parameterized.Parameters(name = "{index}. {7}")
    public static Collection<Object[]> testData() {
        billingDaoSteps = BillingDaoSteps.getInstance();
//        CampaignBalance balance = billingDaoSteps.getRandomCampaignBalanceWith(0);
        CampaignBalance balance = billingDaoSteps.getCampaignBalance(2002109L);
        String campaignId = balance.getCampaignId().toString();
        String serialNum = String.valueOf(balance.getLastPaymentSerialNum().longValue());

        return new ArrayList<Object[]>() {{
            add(new Object[]{12, campaignId, serialNum, 1, "signalDesc", "consumeSum", "5.", "идентификатор сервиса не маркетный"});
//            add(new Object[]{11, String.valueOf(Long.MAX_VALUE), serialNum, 1, "signalDesc", "consumeSum", "5.", "не существующая компания"});
            add(new Object[]{11, campaignId, serialNum, 1, "signalDesc", "consumeSum", "-0.01", "неверная сумма платежа"});
            add(new Object[]{11, null, serialNum, 1, "signalDesc", "consumeSum", "1000000000001d", "компания не определена (null)"});
            add(new Object[]{11, campaignId, serialNum, 1, "signalDesc", "consumeSum", null, "сумма платежа не определена (null)"});
            add(new Object[]{11, campaignId, serialNum, 1, "signalDesc", "consumeSum", "A1", "не числовая сумма платежа"});
            add(new Object[]{11, campaignId, serialNum, 1, "signalDesc", "consumeSum", "", "пустая сумма платежа"});
            add(new Object[]{11, campaignId, "A1", 1, "signalDesc", "consumeSum", "0.", "не числовой серийный номер"});

        }};
    }

    @Parameterized.Parameter(0)
    public int serviceId;
    @Parameterized.Parameter(1)
    public String serviceOrderId;
    @Parameterized.Parameter(2)
    public String serialNumStr;
    @Parameterized.Parameter(3)
    public int signal;
    @Parameterized.Parameter(4)
    public String signalDesc;
    @Parameterized.Parameter(5)
    public String consumeSum;
    @Parameterized.Parameter(6)
    public String consumeQty;
    @Parameterized.Parameter(7)
    public String testCaseDescription;

    static BillingDaoSteps billingDaoSteps;
    static BalanceNotificationSteps balanceNotificationSteps;


    @BeforeClass
    public static void setUp() throws Exception {
        balanceNotificationSteps = BalanceNotificationSteps.getInstance();
    }

    @Test
    public void testBalanceNotifyOrder() throws Exception {
        balanceNotificationSteps.throwableExecuteBalanceNotifyOrder(serviceId, serviceOrderId, serialNumStr,
                signal, signalDesc, consumeSum, consumeQty);
    }
}
