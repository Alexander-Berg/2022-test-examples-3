package ru.yandex.autotests.direct.cmd.wallet;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.wallet.AjaxSaveWalletSettingsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampOptionsRecord;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка изменения настроек ОС")
@Stories(TestFeatures.Wallet.AJAX_SAVE_WALLET_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class AjaxSaveWalletSettingsTest {

    private static final String CLIENT = "at-direct-daybudget-os4";

    @ClassRule
    public static final ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Long walletId;
    private AjaxSaveWalletSettingsRequest request;

    @Before
    public void before() {
        // тест будет падать, если у клиента нет ни одной кампании, поэтому добавляем
        api.userSteps.campaignSteps().addDefaultTextCampaign(CLIENT);

        walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();

        clearFlags();
        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("уведомления sms выключены", wallet.getSelf().getSmsFlags().getPausedByDayBudgetSms(),
                nullValue());
        assumeThat("уведомления email выключены", wallet.getSelf().getEmailNotifications().getPausedByDayBudget(),
                nullValue());

        request = new AjaxSaveWalletSettingsRequest().withCid(walletId).withUlogin(CLIENT);
    }

    @Test
    @Description("Включение уведомлений о дневном бюджете ОС по sms")
    @TestCaseId("10983")
    public void checkEnableDayBudgetSms() {
        request.withPausedByDayBudgetSms(1);
        CommonResponse response = cmdRule.cmdSteps().walletSteps().postAjaxSaveWalletSettings(request);
        assumeThat("изменение пар-ров ОС просшло успешно", response.getResult(), equalTo(CommonResponse.RESULT_OK));

        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок нотификаций присутстует", wallet.getSelf().getSmsFlags(), notNullValue());
        assertThat("уведомление по sms включено", wallet.getSelf().getSmsFlags().getPausedByDayBudgetSms(),
                equalTo(1));
    }

    @Test
    @Description("Выключение уведомлений о дневном бюджете ОС по sms")
    @TestCaseId("10984")
    public void checkDisableDayBudgetSms() {
        enableSms();
        request.withPausedByDayBudgetSms(0);
        CommonResponse response = cmdRule.cmdSteps().walletSteps().postAjaxSaveWalletSettings(request);
        assumeThat("изменение пар-ров ОС просшло успешно", response.getResult(), equalTo(CommonResponse.RESULT_OK));

        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок нотификаций присутстует", wallet.getSelf().getSmsFlags(), notNullValue());
        assertThat("уведомление по sms выключено", wallet.getSelf().getSmsFlags().getPausedByDayBudgetSms(),
                nullValue());
    }

    @Test
    @Description("Включение уведомлений о дневном бюджете ОС по email")
    @TestCaseId("10985")
    public void checkEnableDayBudgetEmail() {
        request.withEmailNotifyPausedByDayBudgetEmail(1);
        CommonResponse response = cmdRule.cmdSteps().walletSteps().postAjaxSaveWalletSettings(request);
        assumeThat("изменение пар-ров ОС просшло успешно", response.getResult(), equalTo(CommonResponse.RESULT_OK));

        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок нотификаций присутстует", wallet.getSelf().getEmailNotifications(), notNullValue());
        assertThat("уведомление по email включен", wallet.getSelf().getEmailNotifications().getPausedByDayBudget(),
                equalTo(1));
    }

    @Test
    @Description("Выключение уведомлений о дневном бюджете ОС по email")
    @TestCaseId("10986")
    public void checkDisableDayBudgetEmail() {
        enableEmail();
        request.withEmailNotifyPausedByDayBudgetEmail(0);
        CommonResponse response = cmdRule.cmdSteps().walletSteps().postAjaxSaveWalletSettings(request);
        assumeThat("изменение пар-ров ОС просшло успешно", response.getResult(), equalTo(CommonResponse.RESULT_OK));

        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок нотификаций присутстует", wallet.getSelf().getEmailNotifications(), notNullValue());
        assertThat("уведомление по email выключен", wallet.getSelf().getEmailNotifications().getPausedByDayBudget(),
                nullValue());
    }

    private void clearFlags() {
        CampOptionsRecord campOptions =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampOptionsById(walletId);
        campOptions.setSmsFlags("");
        campOptions.setEmailNotifications("");
        TestEnvironment.newDbSteps().campaignsSteps().updateCampOptions(campOptions);
    }

    private void enableSms() {
        CampOptionsRecord campOptions =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampOptionsById(walletId);
        campOptions.setSmsFlags("paused_by_day_budget_sms");
        TestEnvironment.newDbSteps().campaignsSteps().updateCampOptions(campOptions);
    }

    private void enableEmail() {
        CampOptionsRecord campOptions =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampOptionsById(walletId);
        campOptions.setEmailNotifications("paused_by_day_budget");
        TestEnvironment.newDbSteps().campaignsSteps().updateCampOptions(campOptions);
    }
}
