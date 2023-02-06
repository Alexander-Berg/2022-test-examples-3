package ru.yandex.autotests.direct.cmd.clients.frombalance.client;

import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание самоходного клиента в Директе, который уже есть в балансе в фишках")
@Stories(TestFeatures.Client.SEARCH_CLIENT_ID)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SEARCH_CLIENT_ID)
@Tag(ObjectTag.USER)
@RunWith(Parameterized.class)
public class CreateFixedClientFromBalanceTest extends ClientFromBalanceBaseTest {

    @Parameterized.Parameter(0)
    public Integer clientBalanceRegion;

    @Parameterized.Parameter(1)
    public String clientDirectRegion;

    @Parameterized.Parameter(2)
    public String clientDirectCurrency;

    @Parameterized.Parameters(name = "Клиент со страной в Балансе {0} создается в Директе со страной {1} и валютой {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Integer.parseInt(Geo.BELORUSSIA.getGeo()), Geo.BELORUSSIA.getGeo(), Currency.BYN.value()},
                {null, Geo.TURKEY.getGeo(), Currency.TRY.value()},
//                {null, Geo.UKRAINE.getGeo(), Currency.UAH.value()},
                {null, Geo.GERMANY.getGeo(), Currency.USD.value()},
        });
    }

    @Test
    @Description("создание самоходного клиента в Директе с данными из Баланса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9585")
    public void createFixedClientWithRegionAndCurrency() {
        user = cmdRule.cmdSteps().userExternalServicesSteps().createBalanceFixedClient(null, clientBalanceRegion);
        RedirectResponse response = cmdRule.cmdSteps().сlientsSteps().getSaveClientIDFromCreateUser(
                user.getLogin(),
                Integer.parseInt(clientDirectRegion),
                clientDirectCurrency);

        assertThat("произошло сохранение нового клиента в Директе", response.getLocationParam(LocationParam.CMD),
                equalTo(CMD.SHOW_CAMPS.toString()));
    }
}
