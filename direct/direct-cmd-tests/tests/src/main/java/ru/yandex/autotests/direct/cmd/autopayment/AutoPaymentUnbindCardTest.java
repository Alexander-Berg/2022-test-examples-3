package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutopaySettingsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutopaySettingsPaymethodType;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка автопополнения после отвязки карты")
@Stories(TestFeatures.Wallet.AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
public class AutoPaymentUnbindCardTest extends AutoPaymentTestBase {

    private static final String CLIENT = "at-direct-backend-rus-os5";

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Before
    public void before() {
        super.before();
        AutoPaymentHelper.enableAutopaymentDB(walletId, CLIENT);
    }

    @Test
    @Description("Проверяем отвязку карты")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9040")
    public void checkUnbindCard() {
        CommonResponse response = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxUnbindCard(payMethodId, AutopaySettingsPaymethodType.card);
        assumeThat("карта отвязалась", response.getResult(), equalTo("ok"));
        AutopaySettingsResponse actualResponse = cmdRule.cmdSteps().autopaySettingsSteps().getAutopaySettings(CLIENT);
        assertThat("карты нет в списке", actualResponse.getAutopaySettings().getCards(),
                hasSize(0));
    }

}
