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
import ru.yandex.autotests.direct.cmd.data.transfer.TransferDoneRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
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
public class TransferManyCampaignsToManyValidationTest extends TransferDoneBetweenClientValidationBaseTest {
    public BannersRule bannersRule1 = new TextBannersRule().withUlogin(AGENCY_CLIENT);
    public BannersRule bannersRule2 = new TextBannersRule().withUlogin(AGENCY_CLIENT);
    public BannersRule bannersRule3 = new TextBannersRule().withUlogin(AGENCY_CLIENT);
    public BannersRule bannersRule4 = new TextBannersRule().withUlogin(AGENCY_CLIENT);

    public TransferManyCampaignsToManyValidationTest() {
        cmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule1, bannersRule2, bannersRule3, bannersRule4)
                .as(Logins.AGENCY);
    }

    @Test
    @Description("Проверяем, что нельзя перенести деньги c нескольких кампаний на несколько")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10038")
    public void manyCampaignsToManyValidationTest() {
        createInvoice(Logins.AGENCY, AGENCY_CLIENT, bannersRule1.getCampaignId());
        createInvoice(Logins.AGENCY, AGENCY_CLIENT, bannersRule2.getCampaignId());
        List<CampaignFromSum> campaignsFrom = new ArrayList<>();
        addCampaignsFrom(campaignsFrom, bannersRule1.getCampaignId().intValue(), sumToTransfer);
        addCampaignsFrom(campaignsFrom, bannersRule2.getCampaignId().intValue(), sumToTransfer);
        List<CampaignToSum> campaignsTo = new ArrayList<>();
        addCampaignsTo(campaignsTo, bannersRule3.getCampaignId().intValue(), sumToTransfer);
        addCampaignsTo(campaignsTo, bannersRule4.getCampaignId().intValue(), sumToTransfer);
        TransferDoneRequest request = new TransferDoneRequest()
                .withClientFrom(AGENCY_CLIENT)
                .withClientTo(ANOTHER_AGENCY_CLIENT)
                .withTransferFrom(campaignsFrom.get(0).getObjectId())
                .withTransferFromRadio(campaignsFrom.get(0).getObjectId())
                .withCampaignFromSums(campaignsFrom)
                .withCampaignToSums(campaignsTo);

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps().postTransferDone(request);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.MANY_TO_MANY.toString()));
    }

}
