package ru.yandex.autotests.market.billing.backend.balance;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.util.concurrent.TimeUnit;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 10/16/15
 */
@Aqua.Test(title = "Тесты нотификации от Баланса. Позитивные кейсы")
@Feature("balance")
@Description("Подробное описание теста https://st.yandex-team.ru/AUTOTESTMARKET-1079 \n" +
        "Проверяем, как изменился баланс в биллинге(market_billing.campaign_balance)\n" +
        " после отсылки  туда нового  баланса но тестовому заказу, а так же регистрацию платежа (market_billing.campaign_payments)\n" +
        " Для тестирования выбираеи заказ с нулевым балансом")
@RunWith(Parameterized.class)
public class BalanceNotificationPositiveTest {

    @Parameterized.Parameters(name = "{index}. {4}")
    public static Collection<Object[]> testData() {
        billingDaoSteps = BillingDaoSteps.getInstance();
        CampaignBalance balance = billingDaoSteps.getCampaignBalance(2002109L);
        Long campaignId = balance.getCampaignId();
        Double serialNum = balance.getLastPaymentSerialNum();

        return new ArrayList<Object[]>() {{
            //todo:Добавить описание кейсов
            add(new Object[]{11, campaignId, serialNum + 1d, "5.", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 2d, "5.", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 3d, "5.", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 3d, "5.", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 4d, "0.", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 5d, "0.00999999", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 6d, "0.99999999", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 7d, "0.001", "описание кейса"});
            add(new Object[]{11, campaignId, serialNum + 8d, "1000000000000.", "описание кейса"});
        }};
    }

    @Parameterized.Parameter(0)
    public int serviceId;
    @Parameterized.Parameter(1)
    public Long serviceOrderId;
    @Parameterized.Parameter(2)
    public Double serialNumDouble;
    @Parameterized.Parameter(3)
    public String consumeQty;
    @Parameterized.Parameter(4)
    public String testCaseDescription;

    static BillingDaoSteps billingDaoSteps;
    static BalanceNotificationSteps balanceNotificationSteps;


    @BeforeClass
    public static void setUp() throws Exception {
        balanceNotificationSteps = BalanceNotificationSteps.getInstance();
    }

    @Test
    public void testBalanceNotifyOrder() throws Exception {
        TimeUnit.SECONDS.sleep(1); //задержка для того чтобы время платежей отличалось гарантированно на секунду.
        CampaignBalance cbBefore = billingDaoSteps.getCampaignBalance(serviceOrderId);
        DateTime paymentTime = new DateTime(DateTimeZone.forID("Europe/Moscow")).withMillisOfSecond(0); // aqua воркеры могут работать в другой таймзоне, но база всегда по Мск
        balanceNotificationSteps.executeBalanceNotifyOrder(
                serviceId, serviceOrderId.toString(), serialNumDouble, consumeQty);

        CampaignBalance cbAfter = billingDaoSteps.getCampaignBalance(serviceOrderId);
        balanceNotificationSteps.checkPaymentRegistration(cbBefore, cbAfter, serviceOrderId, serialNumDouble, consumeQty, paymentTime);
    }
}
