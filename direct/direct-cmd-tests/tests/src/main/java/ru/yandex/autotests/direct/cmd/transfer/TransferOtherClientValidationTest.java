package ru.yandex.autotests.direct.cmd.transfer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок переноса средств между клиентами (контроллер transfer_done)")
@Stories(TestFeatures.Transfer.TRANSFER_DONE)
@Features(TestFeatures.TRANSFER)
@Tag(CmdTag.TRANSFER_DONE)
@Tag(CampTypeTag.TEXT)
public class TransferOtherClientValidationTest extends TransferDoneBetweenClientValidationBaseTest{
    public BannersRule bannersRule1 = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    public BannersRule bannersRule2 = new TextBannersRule().withUlogin(Logins.DEFAULT_CLIENT);

    public TransferOtherClientValidationTest() {
        cmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule1, bannersRule2);
    }

    @Test
    @Description("Проверяем валидацию кампании другого клиента в списке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10037")
    public void otherClientValidationTest() {
        cmdRule.apiSteps().makeCampaignActiveV5(CLIENT_LOGIN, bannersRule1.getCampaignId());
        createInvoice(CLIENT_LOGIN, CLIENT_LOGIN, bannersRule1.getCampaignId());

        sumToTransfer = 1001f;
        List<CampaignFromSum> campaignsFrom = new ArrayList<>();
        addCampaignsFrom(campaignsFrom, bannersRule1.getCampaignId().intValue(), sumToTransfer);
        List<CampaignToSum> campaignsTo = new ArrayList<>();
        addCampaignsTo(campaignsTo, bannersRule2.getCampaignId().intValue(), null);

        ErrorResponse
                response = cmdRule.cmdSteps().transferDoneSteps().transferDoneErrorResponse(CLIENT_LOGIN, CLIENT_LOGIN,
                campaignsFrom, campaignsTo, TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответствует ожиданиям", response.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

}
