package ru.yandex.autotests.direct.cmd.clients.frombalance.subclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.clients.frombalance.ClientFromBalanceBaseTest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание агентского клиента в Директе, который уже есть в балансе в фишках")
@Stories(TestFeatures.Client.SEARCH_CLIENT_ID)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SEARCH_CLIENT_ID)
@Tag(ObjectTag.USER)
@RunWith(Parameterized.class)
public class CreateAgencyFixedClientFromBalanceTest extends ClientFromBalanceBaseTest {

    @Parameterized.Parameter(0)
    public Integer agencyId;

    @Parameterized.Parameter(1)
    public String agencyLogin;

    @Parameterized.Parameter(2)
    public String clientBalanceRegion;

    @Parameterized.Parameter(3)
    public String clientDirectRegion;

    @Parameterized.Parameter(4)
    public String clientDirectCurrency;

    @Parameterized.Parameters(name = "агенство {1} клиент из Баланса со страной {2} " +
            "создается в Директе со страной {3} и валютой {4}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.GERMANY.getGeo(), Currency.EUR.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.AUSTRIA.getGeo(), Currency.EUR.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, Geo.BELORUSSIA.getGeo(), Geo.GERMANY.getGeo(), Currency.EUR.value()},
                {AGENCY_BELARUS_CLIENT_ID, AGENCY_BELARUS, Geo.BELORUSSIA.getGeo(), Geo.BELORUSSIA.getGeo(), Currency.BYN.value()},
        });
    }

    @Test
    @Description("создание самоходного клиента в Директе с данными из Баланса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9589")
    public void createAgencyFixedClientFromBalance() {
        user = cmdRule.cmdSteps().userExternalServicesSteps().createBalanceFixedClient(
                agencyId,
                getRegion(clientBalanceRegion)
        );
        cmdRule.cmdSteps().authSteps().authenticate(User.get(agencyLogin));
        RedirectResponse response = cmdRule.cmdSteps().сlientsSteps().getSaveClientIDFromCreateUser(
                user.getLogin(),
                getRegion(clientDirectRegion),
                clientDirectCurrency);

        assertThat("произошло сохранение нового клиента в Директе", response.getLocationParam(LocationParam.CMD),
                equalTo(CMD.NEW_CAMP_TYPE.toString()));
    }
}
