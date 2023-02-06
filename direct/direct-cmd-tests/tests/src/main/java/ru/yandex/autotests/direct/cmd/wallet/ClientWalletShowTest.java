package ru.yandex.autotests.direct.cmd.wallet;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampOptionsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.utils.date.DBTimeConverter.jodaTimeToDb;
import static ru.yandex.autotests.direct.utils.date.DBTimeConverter.jodaToSql;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Просмотр общего счета")
@Stories(TestFeatures.Wallet.CLIENT_WALLET)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class ClientWalletShowTest {

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

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campStopStatSteps().deleteCampStopStat(walletId);

        CampOptionsRecord campOptions =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampOptionsById(walletId);
        campOptions.setSmsFlags("paused_by_day_budget_sms");
        campOptions.setEmailNotifications("paused_by_day_budget");
        TestEnvironment.newDbSteps().campaignsSteps().updateCampOptions(campOptions);
    }

    @Test
    @Description("Общий счет в ответе clientWallet")
    @TestCaseId("10988")
    public void checkDayBudgetClientWallet() {
        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок нотификаций присутстует", wallet.getSelf().getSmsFlags(), notNullValue());

        assertThat("уведомление по sms включено", wallet.getSelf().getSmsFlags().getPausedByDayBudgetSms(),
                equalTo(1));
        assertThat("уведомление по email включено", wallet.getSelf().getEmailNotifications().getPausedByDayBudget(),
                equalTo(1));
    }

    @Test
    @Description("Общий счет в ответе clientWallet")
    @TestCaseId("10989")
    public void checkDayBudgetStatClientWallet() {
        Date date = new Date();
        date = DateUtils.round(date, Calendar.SECOND);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campStopStatSteps()
                .createCampStopStat(walletId, new Timestamp(date.getTime()));
        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();
        assumeThat("блок статистики присутстует", wallet.getSelf().getCampStopDailyBudgetStats(), notNullValue());

        assertThat("статистика дневного бюджета приходит", wallet.getSelf().getCampStopDailyBudgetStats(),
                containsInAnyOrder(format.format(date)));
    }
}
