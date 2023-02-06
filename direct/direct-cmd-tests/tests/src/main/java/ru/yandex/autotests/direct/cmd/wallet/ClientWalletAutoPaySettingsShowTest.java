package ru.yandex.autotests.direct.cmd.wallet;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.wallet.AutoPay;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Просмотр автопополнения общего счета")
@Stories(TestFeatures.Wallet.CLIENT_WALLET)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class ClientWalletAutoPaySettingsShowTest {

    private static final String CLIENT = "at-direct-daybudget-os6";

    @ClassRule
    public static final ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Long walletId;

    @Before
    public void before() {
        // тест будет падать, если у клиента нет ни одной кампании, поэтому добавляем
        api.userSteps.campaignSteps().addDefaultTextCampaign(CLIENT);

        walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();

        AutoPaymentHelper.deleteAutopaymentDB(walletId, CLIENT);
        AutoPaymentHelper.enableAutopaymentDB(walletId, CLIENT);
    }

    @Test
    @Description("Автопопполнение в ответе clientWallet")
    @TestCaseId("10987")
    public void checkAutoPaymentClientWallet() {
        AutoPay autoPay = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getAutoPay();
        assumeThat("блок autoPayment вернулся", autoPay.getAutopaySettings(),
                IsNull.notNullValue());

        assertThat("блок autoPayment показывается", autoPay.getAutopaySettings().getWalletCid(),
                IsEqual.equalTo(String.valueOf(walletId)));
    }
}
