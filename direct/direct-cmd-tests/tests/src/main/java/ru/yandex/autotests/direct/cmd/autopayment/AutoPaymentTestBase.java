package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;

public abstract class AutoPaymentTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected String payMethodId;
    protected Long walletId;
    protected MoneyCurrency moneyCurrency;
    protected Long campaignId;

    protected abstract String getClient();

    @Before
    public void before() {
        prepareData();
        cmdRule.getApiStepsRule().as(getClient());
        payMethodId = cmdRule.apiSteps().balanceSimpleSteps().listPaymentMethods()
                .getPaymentMethods().keySet()
                .stream().findFirst().orElse(null);
        if (payMethodId == null) {
            payMethodId = cmdRule.apiSteps().balanceSimpleSteps()
                    .bindCreditCard(new BindCreditCardRequest().defaultCreditCard()).getPaymentMethod();
        }
        cmdRule.getApiStepsRule().as(Logins.SUPER);
    }

    protected void prepareData() {

        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        walletId = (long) cmdRule.apiSteps().financeSteps().enableAndGetSharedAccount(getClient());
        AutoPaymentHelper.deleteAutopaymentDB(walletId, getClient());

        Long campaignId = cmdRule.apiSteps().campaignSteps().getCampaigns(getClient()).get(0).getId();
        cmdRule.cmdSteps().campaignSteps().remoderateCamp(campaignId, getClient());
        cmdRule.apiSteps().makeCampaignModerated(campaignId.intValue());

        moneyCurrency = MoneyCurrency.get(User.get(getClient()).getCurrency());
        cmdRule.cmdSteps().authSteps().authenticate(User.get(getClient()));
    }

}
