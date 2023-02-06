package ru.yandex.autotests.direct.cmd.transfer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferDoneErrors;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public class TransferNoAccountAtCampaignsFromListTest extends TransferDoneBetweenClientValidationBaseTest{
    public BannersRule bannersRule1 = new TextBannersRule().withUlogin(AGENCY_CLIENT);
    public BannersRule bannersRule2 = new TextBannersRule().withUlogin(Logins.CLIENT_WITH_ACCOUNT);
    @Rule
    public DirectCmdRule otherCmdRule;

    public TransferNoAccountAtCampaignsFromListTest() {
        otherCmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule1)
                .as(Logins.AGENCY);
        cmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule2);

    }

    @Test
    @Description("Проверяем, что нельзя переносить деньги клиенту с общим счетом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10039")
    public void noAccountAtCampaignsFromListTest() {
        createInvoice(Logins.MANAGER, Logins.CLIENT_WITH_ACCOUNT, bannersRule2.getCampaignId());
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.CLIENT_WITH_ACCOUNT));
        cmdRule.getApiStepsRule().as(Logins.MANAGER);

        List<CampaignFromSum> campaignsFrom = new ArrayList<>();
        addCampaignsFrom(campaignsFrom, bannersRule1.getCampaignId().intValue(), null);
        List<CampaignToSum> campaignsTo = new ArrayList<>();
        addCampaignsTo(campaignsTo, bannersRule2.getCampaignId().intValue(), sumToTransfer);

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps().transferDone(
                campaignsFrom, campaignsTo, TransferTypeEnum.FROM_ONE_TO_MANY);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.LESS_THAN_ZERO.toString()));
    }
}
