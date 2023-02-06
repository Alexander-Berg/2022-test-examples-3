package ru.yandex.autotests.direct.cmd.transfer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferDoneErrors;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок переноса средств между клиентами (контроллер transfer_done)")
@Stories(TestFeatures.Transfer.TRANSFER_DONE)
@Features(TestFeatures.TRANSFER)
@Tag(CmdTag.TRANSFER_DONE)
@Tag(CampTypeTag.TEXT)
public class TransferDoneBetweenClientsValidationTest extends TransferDoneBetweenClientValidationBaseTest{


    public BannersRule bannersRule1 = new TextBannersRule().withUlogin(AGENCY_CLIENT);

    public TransferDoneBetweenClientsValidationTest() {
        cmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule1)
                .as(Logins.AGENCY);
    }
    @Test
    @Description("Проверяем, что нельзя перенести деньги с кампании на нее же")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10036")
    public void fromOneToOneCampaignValidationTest() {
        createInvoice(Logins.AGENCY, AGENCY_CLIENT, bannersRule1.getCampaignId());

        List<CampaignFromSum> campaignsFrom = new ArrayList<>();
        addCampaignsFrom(campaignsFrom, bannersRule1.getCampaignId().intValue(), sumToTransfer);
        List<CampaignToSum> campaignsTo = new ArrayList<>();
        addCampaignsTo(campaignsTo, bannersRule1.getCampaignId().intValue(), null);

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps()
                .transferDone(campaignsFrom, campaignsTo, TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TextResourceFormatter.resource(TransferDoneErrors.CAMPAIGN_SHOULD_NOT_BE_AT_CAMPAIGNS_FROM).
                        args(String.valueOf(bannersRule1.getCampaignId())).toString().replace("\\n", "\n")));
    }
}
