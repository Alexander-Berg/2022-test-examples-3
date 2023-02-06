package ru.yandex.autotests.market.shop.backend.tms;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.common.wait.Waiter;
import ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsoleFactory;
import ru.yandex.autotests.market.shop.backend.core.dao.entities.autopayment.AutoPaymentRequest;
import ru.yandex.autotests.market.shop.backend.steps.AutoPaymentExecutorSteps;
import ru.yandex.qatools.allure.annotations.Description;

@Aqua.Test(title = "Тест авто пополнения")
@Feature("autoPaymentExecutor")
@Description("Тест проверяет, что авто пополнение корректно работает")
public class AutoPaymentTest {
    public static final int TIME_OUT_IN_SECONDS = 600;
    public static final int SLEEP_TIME_OUT = 30 * 1000;
    public static final String SUCCESS_RESPONSE_CODE = "success";
    public static final String FAIL_RESPONSE_CODE = "authorization_reject";
    public static AutoPaymentExecutorSteps apSteps = new AutoPaymentExecutorSteps();


    private static final long SHOULD_AUTO_PAY_CAMPAIGN_ID = 1001041124;
    private static final long FAIL_AUTO_PAY_CAMPAIGN_ID = 1001043942;

    @Test
    public void autoPaymentTest() {
        apSteps.clearAutoPaymentHistory(SHOULD_AUTO_PAY_CAMPAIGN_ID);
        final long balance = apSteps.getCampaignBalanceActual(SHOULD_AUTO_PAY_CAMPAIGN_ID);

        new Waiter().waitSomething(apSteps, TIME_OUT_IN_SECONDS, SLEEP_TIME_OUT, apSteps -> {
            MarketShopConsoleFactory.connectToShop().runAutoPaymentExecutor();
            final AutoPaymentRequest autoPaymentRequest = apSteps.getAutoPaymentRequest(SHOULD_AUTO_PAY_CAMPAIGN_ID);
            return SUCCESS_RESPONSE_CODE.equals(autoPaymentRequest.getResponseCode());
        });

        new Waiter().waitSomething(apSteps, TIME_OUT_IN_SECONDS, SLEEP_TIME_OUT, apSteps ->
                apSteps.getCampaignBalanceActual(SHOULD_AUTO_PAY_CAMPAIGN_ID) - balance == 1000);
    }


    @Test
    public void autoPaymentFailTest() {
        apSteps.clearAutoPaymentHistory(FAIL_AUTO_PAY_CAMPAIGN_ID);
        new Waiter().waitSomething(apSteps, TIME_OUT_IN_SECONDS, SLEEP_TIME_OUT, apSteps -> {
            MarketShopConsoleFactory.connectToShop().runAutoPaymentExecutor();
            final AutoPaymentRequest autoPaymentRequest = apSteps.getAutoPaymentRequest(FAIL_AUTO_PAY_CAMPAIGN_ID);
            return FAIL_RESPONSE_CODE.equals(autoPaymentRequest.getResponseCode());
        });

    }

}
