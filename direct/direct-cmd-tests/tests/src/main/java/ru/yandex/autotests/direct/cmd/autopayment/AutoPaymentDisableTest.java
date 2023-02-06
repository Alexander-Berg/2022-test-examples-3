package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxSaveAutopaySettingsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.common.api45.Account;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка отключение автоплатежа контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_SAVE_AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_SAVE_AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
public class AutoPaymentDisableTest extends AutoPaymentTestBase {
    private static final String CLIENT = "at-direct-backend-rus-os3";

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Before
    @Override
    public void before() {
        super.before();

        AutoPaymentHelper.enableAutopaymentDB(walletId, CLIENT);
    }

    @Test
    @Description("Проверка отключения автопополнения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9031")
    public void checkSaveAutoPayment() {
        disableAutoPayment();

        WalletCampaignsRecord actualWalletCampaigns = TestEnvironment.newDbSteps()
                .useShardForLogin(CLIENT).walletCampaignsSteps()
                .getWalletCampaigns(walletId);
        assertThat("автопополнение отключено", actualWalletCampaigns.getAutopayMode(),
                equalTo(WalletCampaignsAutopayMode.none));
    }

    @Test
    @Description("После отключения автопополнения деньги не списываются")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9032")
    public void checkMoneyAfterDisableAutoPayment() {
        disableAutoPayment();

        AutoPaymentHelper.runAutoPaymentScript(cmdRule, walletId, CLIENT);

        assertThat("деньги на ОС не изменились",
                ((Account) cmdRule.apiSteps().financeSteps().getAccount(walletId.intValue())).getAmount(),
                equalTo(0f));
    }

    private void disableAutoPayment() {
        CommonResponse commonResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxSaveAutopaySettings(AjaxSaveAutopaySettingsRequest
                        .getDisableAutopayRequest(String.valueOf(walletId)));
        assumeThat("контроллер выполнился успешно", commonResponse.getResult(),
                equalTo("ok"));
    }
}
