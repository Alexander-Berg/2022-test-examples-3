package ru.yandex.autotests.direct.cmd.clients.frombalance.client;

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
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Негативные кейсы на создание самоходного клиента в Директе, который уже есть в балансе")
@Stories(TestFeatures.Client.SEARCH_CLIENT_ID)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SEARCH_CLIENT_ID)
@Tag(ObjectTag.USER)
@RunWith(Parameterized.class)
public class CreateClientFromBalanceNegativeTest extends ClientFromBalanceBaseTest {

    @Parameterized.Parameter(0)
    public String clientBalanceRegion;

    @Parameterized.Parameter(1)
    public String clientBalanceCurrency;

    @Parameterized.Parameter(2)
    public String clientDirectRegion;

    @Parameterized.Parameter(3)
    public String clientDirectCurrency;

    @Parameterized.Parameters(name = "Клиент из Баланса со страной {0} и валютой {1} " +
            "создается в Директе со страной {2} и валютой {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Geo.GERMANY.getGeo(), Currency.EUR.value(), Geo.GERMANY.getGeo(), Currency.RUB.value()},
                {Geo.GERMANY.getGeo(), Currency.EUR.value(), Geo.RUSSIA.getGeo(), Currency.EUR.value()},
                {Geo.GERMANY.getGeo(), Currency.EUR.value(), Geo.RUSSIA.getGeo(), Currency.RUB.value()},
                {Geo.GERMANY.getGeo(), null, Geo.RUSSIA.getGeo(), Currency.USD.value()},
                {Geo.GERMANY.getGeo(), null, Geo.RUSSIA.getGeo(), Currency.CHF.value()},
                {Geo.GERMANY.getGeo(), null, Geo.RUSSIA.getGeo(), Currency.RUB.value()},
                {Geo.GERMANY.getGeo(), null, Geo.GERMANY.getGeo(), Currency.RUB.value()},
                {Geo.GERMANY.getGeo(), null, Geo.GERMANY.getGeo(), Currency.UAH.value()},
                {Geo.RUSSIA.getGeo(), Currency.RUB.value(), Geo.BELORUSSIA.getGeo(), Currency.YND_FIXED.value()},
                {Geo.GERMANY.getGeo(), null, Geo.BELORUSSIA.getGeo(), Currency.YND_FIXED.value()},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("не разрешаем создание самоходного клиента в Директе с такими данными в Балансе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9582")
    public void createClientWithRegionAndCurrency() {
        user = cmdRule.cmdSteps().userExternalServicesSteps().createBalanceClient(
                getRegion(clientBalanceRegion),
                clientBalanceCurrency
        );
        cmdRule.cmdSteps().сlientsSteps().getSaveClientIDFromCreateUser(
                user.getLogin(),
                getRegion(clientDirectRegion),
                clientDirectCurrency);
    }
}
