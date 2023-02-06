package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxResumeAutopayRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка возобновения автоплатежа контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_RESUME_AUTOPAY)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_RESUME_AUTOPAY)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
public class AjaxResumeAutopayTest extends AutoPaymentTestBase {
    private static final String CLIENT = "at-direct-backend-rus-os6";

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Before
    @Override
    public void before() {
        super.before();

        AutoPaymentHelper.enableAutopaymentDB(walletId, CLIENT);
        AutoPaymentHelper.setAutoPaymentTriesNum(walletId, CLIENT, 4);
    }

    @Test
    @Description("Проверка отключения автопополнения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9027")
    public void checkSaveAutoPayment() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxResumeAutopay(new AjaxResumeAutopayRequest()
                        .withWalletCid(String.valueOf(walletId)));
        assumeThat("при возобновлении автоплатежа ошибок нет", errorResponse.getError(),
                nullValue());

        AutopaySettingsRecord autopaySettings = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .autopaySettingsSteps().getAutopaySettings(walletId);
        assertThat("попытки автопополнения восстановлены", autopaySettings.getTriesNum(),
                equalTo(0));
    }
}
