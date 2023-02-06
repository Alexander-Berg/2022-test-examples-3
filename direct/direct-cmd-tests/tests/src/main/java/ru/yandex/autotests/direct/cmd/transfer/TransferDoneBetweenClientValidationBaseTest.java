package ru.yandex.autotests.direct.cmd.transfer;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;

public abstract class TransferDoneBetweenClientValidationBaseTest {
    protected static final String CLIENT_LOGIN = "at-direct-transfer-rub";
    /*
        TODO: перейти на transfer-ag-cl-rub-4, когда он приедет в Баланс (телепортировав по необходимости)
     */
    protected static final String AGENCY_CLIENT = "transfer-ag-cl-rub-4";
    protected static final String ANOTHER_AGENCY_CLIENT = "transfer-ag-cl-2";
    protected Float sumToTransfer;
    protected Currency userCurrency;
    protected MoneyCurrency moneyCurrency;
    @Rule
    public DirectCmdRule cmdRule;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Before
    public void before() {
        cmdRule.getApiStepsRule().as(Logins.SUPER);
    }

    protected void createInvoice(String managerLogin, String login, Long cid) {
        userCurrency = User.get(login).getCurrency();
        moneyCurrency = MoneyCurrency.get(userCurrency);
        sumToTransfer = 3000f;
        cmdRule.getApiStepsRule().as(managerLogin);
        int invoiceID = cmdRule.apiSteps().createInvoice(cid.intValue(),
                sumToTransfer,
                userCurrency
        );
        cmdRule.apiSteps().balanceSteps().turnOnInvoice(invoiceID);
    }

    protected void addCampaignsFrom(List<CampaignFromSum> campaignsFrom, Integer campaignId,
            Float sumToTransfer) {
        campaignsFrom.add(new CampaignFromSum()
                .withObjectId(String.valueOf(campaignId))
                .withCampaignFromSum(String.valueOf(sumToTransfer)));
    }

    protected void addCampaignsTo(List<CampaignToSum> campaignsTo, Integer campaignId,
            Float sumToTransfer) {
        campaignsTo.add(new CampaignToSum()
                .withObjectId(String.valueOf(campaignId))
                .withCampaignToSum(String.valueOf(sumToTransfer)));
    }
}
