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
@Description("Негативные кейсы на создание самоходного клиента в Директе, который уже есть в балансе в фишках")
@Stories(TestFeatures.Client.SEARCH_CLIENT_ID)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SEARCH_CLIENT_ID)
@Tag(ObjectTag.USER)
@RunWith(Parameterized.class)
public class CreateFixedClientFromBalanceNegativeTest extends ClientFromBalanceBaseTest {

    @Parameterized.Parameter(0)
    public Integer clientBalanceRegion;

    @Parameterized.Parameter(1)
    public String clientDirectRegion;

    @Parameterized.Parameter(2)
    public String clientDirectCurrency;

    @Parameterized.Parameters(name = "Клиент из Баланса со страной {0} и валютой {1} " +
            "создается в Директе со страной {2} и валютой {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Integer.parseInt(Geo.BELORUSSIA.getGeo()), Geo.UKRAINE.getGeo(), Currency.YND_FIXED.value()},
                {Integer.parseInt(Geo.BELORUSSIA.getGeo()), Geo.UKRAINE.getGeo(), Currency.UAH.value()},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("не разрешаем создание самоходного клиента в Директе с такими данными в Балансе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9584")
    public void createFixedClientWithRegionAndCurrency() {
        user = cmdRule.cmdSteps().userExternalServicesSteps().createBalanceFixedClient(null, clientBalanceRegion);

        cmdRule.cmdSteps().сlientsSteps().getSaveClientIDFromCreateUser(
                user.getLogin(),
                Integer.parseInt(clientDirectRegion),
                clientDirectCurrency);
    }
}
