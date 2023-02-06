package ru.yandex.autotests.direct.cmd.clients.frombalance.subclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.clients.frombalance.ClientFromBalanceBaseTest;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Негативные кейсы на создание агентского клиента в Директе, который уже есть в балансе в фишках")
@Stories(TestFeatures.Client.SEARCH_CLIENT_ID)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SEARCH_CLIENT_ID)
@Tag(ObjectTag.USER)
@RunWith(Parameterized.class)
public class CreateAgencyFixedClientFromBalanceNegativeTest extends ClientFromBalanceBaseTest {

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
            " создается в Директе со страной {3} и валютой {4}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.GERMANY.getGeo(), Currency.CHF.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.GERMANY.getGeo(), Currency.USD.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.GERMANY.getGeo(), Currency.YND_FIXED.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, null, Geo.UKRAINE.getGeo(), Currency.YND_FIXED.value()},
                {AGENCY_GERMAN_CLIENT_ID, AGENCY_GERMAN, Geo.UKRAINE.getGeo(), Geo.UKRAINE.getGeo(), Currency.YND_FIXED.value()},
                {AGENCY_BELARUS_CLIENT_ID, AGENCY_BELARUS, Geo.UKRAINE.getGeo(), Geo.UKRAINE.getGeo(), Currency.USD.value()},
                {AGENCY_BELARUS_CLIENT_ID, AGENCY_BELARUS, Geo.UKRAINE.getGeo(), Geo.GERMANY.getGeo(), Currency.EUR.value()},
                {AGENCY_BELARUS_CLIENT_ID, AGENCY_BELARUS, null, Geo.GERMANY.getGeo(), Currency.EUR.value()},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("не разрешаем создание агентского клиента в Директе с такими данными в Балансе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9588")
    public void createAgencyFixedClientFromBalance() {
        user = cmdRule.cmdSteps().userExternalServicesSteps().createBalanceFixedClient(
                agencyId,
                getRegion(clientBalanceRegion)
        );
        cmdRule.cmdSteps().authSteps().authenticate(User.get(agencyLogin));
        cmdRule.cmdSteps().сlientsSteps().getSaveClientIDFromCreateUser(
                user.getLogin(),
                getRegion(clientDirectRegion),
                clientDirectCurrency);

    }
}
