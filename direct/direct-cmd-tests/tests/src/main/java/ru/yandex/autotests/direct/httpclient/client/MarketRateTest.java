package ru.yandex.autotests.direct.httpclient.client;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.clients.SaveSettingsParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.common.api45.ClientInfo;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 28.11.14
 *         https://st.yandex-team.ru/TESTIRT-3287
 */

@Aqua.Test
@Description("Проверка параметра Рейтинг магазина на маркете (show_market_rating) в настройках пользователя")
@Stories(TestFeatures.Client.MARKET_RATE)
@Features(TestFeatures.CLIENT)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_SETTINGS)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class MarketRateTest {

    private static final String SUPER = Logins.SUPER;
    private static final String CLIENT = "at-direct-backend-c";//-market-c";
    private static final String MARKET_RATE_PATH = "$.show_market_rating";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public Integer marketRateAtSaveSettings;
    @Parameterized.Parameter(value = 1)
    public Integer marketRateAtUserSettings;
    @Parameterized.Parameter(value = 2)
    public String displayStoreRating;
    @Parameterized.Parameter(value = 3)
    public String description;

    private SaveSettingsParameters saveSettingsParameters;
    private CSRFToken csrfToken;


    @Parameterized.Parameters(name = "Изменение настройки пользователя: {3}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {null, 0, "No", "Отключаем показ рейтинга магазина"},
                {1, 1, "Yes", "Включаем показ рейтинга магазина"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(SUPER, User.get(SUPER).getPassword());
        PropertyLoader<SaveSettingsParameters> propertyLoader = new PropertyLoader<>(SaveSettingsParameters.class);
        saveSettingsParameters = propertyLoader.getHttpBean("saveSettingsParameters");
        csrfToken = getCsrfTokenFromCocaine(User.get(SUPER).getPassportUID());

    }

    @Test
    @Description("Проверка через апи сохранения настройки показа рейтинга магазина")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10427")
    public void saveMarketRateAndCheckAtApi() {
        saveSettingsParameters.setShowMarketRating(marketRateAtSaveSettings);
        cmdRule.oldSteps().onSaveSettings().saveUserSettings(csrfToken, saveSettingsParameters);
        ClientInfo clientInfo = cmdRule.apiSteps().clientSteps().getClientInfo(CLIENT);
        assertThat("настройки клиента в апи соответсвуют ожиданиям", clientInfo.getDisplayStoreRating(),
                equalTo(displayStoreRating));
    }

    @Test
    @Description("Проверяем, что после сохранения настройки показа рейтинга магазина она отображается кооректно " +
            "контроллером  userSettings")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10428")
    public void saveMarketRateAndCheckAtUserSettings() {
        saveSettingsParameters.setShowMarketRating(marketRateAtSaveSettings);
        cmdRule.oldSteps().onSaveSettings().saveUserSettings(csrfToken, saveSettingsParameters);
        DirectResponse directResponse = cmdRule.oldSteps().onUserSettings().openUserSettings(CLIENT);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(directResponse, hasJsonProperty(MARKET_RATE_PATH,
                equalTo(marketRateAtUserSettings)));
    }
}
