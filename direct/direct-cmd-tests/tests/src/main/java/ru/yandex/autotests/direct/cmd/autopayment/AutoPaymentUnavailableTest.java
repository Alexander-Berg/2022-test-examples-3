package ru.yandex.autotests.direct.cmd.autopayment;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutoPaymentSettingsErrorsEnum;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.CampaignType;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка недоступности получения данных автоплатежа контроллером autopaySettings")
@Stories(TestFeatures.Wallet.AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AutoPaymentUnavailableTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String user;
    @Parameterized.Parameter(1)
    public String client;
    @Parameterized.Parameter(2)
    public String errorTest;


    @Parameterized.Parameters(name = "Под {0} для {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null, "at-direct-kzt", AutoPaymentSettingsErrorsEnum.AUTOPAYMENT_UNAVAILABLE.toString()},
                {Logins.AGENCY, "at-direct-b-ag-os", CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()},
                {null, Logins.CLIENT_WITHOUT_WALLET, AutoPaymentSettingsErrorsEnum.WALLET_NOT_FOUND.toString()},//без
                // общего счета
                {null, "stdmotors-click", AutoPaymentSettingsErrorsEnum.AUTOPAYMENT_UNAVAILABLE.toString()}, // с
                // фишками
                {"at-direct-backend-rus-os15", "at-direct-backend-rus-os15",
                        AutoPaymentSettingsErrorsEnum.AUTOPAYMENT_UNAVAILABLE.toString()}// без промодерированной
                // кампании
        });
    }

    @Before
    public void before() {
        cmdRule.getApiStepsRule().as("at-direct-super", client);
        if (!client.equals("at-direct-backend-rus-os15") && !client.equals("stdmotors-click")) {
            List<Long> campaignIds = cmdRule.apiSteps().campaignSteps().getAllCampaignIds();
            Long campaignId;
            if (campaignIds.isEmpty()) {
                // не уверен что правильно расставил авторизацию кто под кем создает кампании.
                // на момент починки — кампанию нужно создавать только для CLIENT_WITHOUT_WALLET и тест работает
                if (user != null) {
                    cmdRule.getApiStepsRule().as(user);
                    campaignId = cmdRule.apiSteps().campaignSteps().addDefaultCampaign(client, CampaignType.TEXT);
                } else {
                    campaignId = cmdRule.apiSteps().campaignSteps().addDefaultCampaign(CampaignType.TEXT);
                }
            } else {
                campaignId = campaignIds.get(0);
            }
            cmdRule.apiSteps().makeCampaignModerated(campaignId.intValue());
        }
        if (user != null) {
            cmdRule.cmdSteps().authSteps().authenticate(User.get(user));
        }
    }

    @Test
    @Description("Проверка невозможности открыть страницу автопополнения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9039")
    public void checkNoRightsForAutoPayment() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps().getAutopaySettings(client);
        assertThat("нет прав для выполнения операции", errorResponse.getError(),
                containsString(errorTest));
    }

}
