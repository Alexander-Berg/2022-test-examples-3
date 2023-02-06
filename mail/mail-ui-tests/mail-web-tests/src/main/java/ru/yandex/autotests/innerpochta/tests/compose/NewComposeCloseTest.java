package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.UNDEFINED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_AUTOSAVE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SAVE_SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Закрытие попапа")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
@RunWith(DataProviderRunner.class)
public class NewComposeCloseTest extends BaseTest {
    String msgSubject;
    String msgTo;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        msgSubject = getRandomString();
        msgTo = lock.firstAcc().getSelfEmail();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем автосохранение, 2pane и сбрасываем свёрнутые композы",
            of(
                SETTINGS_ENABLE_AUTOSAVE, STATUS_ON,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_STORED_COMPOSE_STATES, UNDEFINED,
                SETTINGS_SAVE_SENT, STATUS_FALSE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Закрываем пустой попап композа")
    @TestCaseId("5503")
    public void shouldCloseComposePopup() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().closeBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .shouldNotSee(onComposePopup().composeThumb())
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }

    @Test
    @Title("Закрываем сохранённый попап композа")
    @TestCaseId("5503")
    @UseDataProvider("layouts")
    public void shouldSeeCloseComposePopup(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject);
        user.hotkeySteps().pressHotKeysWithDestination(
            onComposePopup().expandedPopup().sbjInput(),
            Keys.chord(Keys.CONTROL, "s")
        );
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().closeBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .shouldNotSee(onComposePopup().composeThumb())
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Test
    @Title("Закрываем свёрнутый композ")
    @TestCaseId("5509")
    @UseDataProvider("layouts")
    public void shouldCloseCollapsedCompose(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().expandedPopup().hideBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .clicksOn(onComposePopup().composeThumb().get(0).closeBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .shouldNotSee(onComposePopup().composeThumb())
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Test
    @Title("Уходим со страницы до сохранения черновика")
    @TestCaseId("5520")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66827")
    public void shouldSeeConfirmAlertAfterClose() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .refreshPage();
        hideAlert();
        user.defaultSteps().shouldContainValue(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .opensFragment(QuickFragments.COMPOSE);
        hideAlert();
        user.defaultSteps().shouldContainValue(onComposePopup().expandedPopup().sbjInput(), msgSubject);
    }

    @Step("Скрываем браузерный алерт")
    private void hideAlert() {
        try {
            Alert alert = webDriverRule.getDriver().switchTo().alert();
            alert.dismiss();
        } catch (NoAlertPresentException e) {
            throw new NoAlertPresentException("Браузерный алерт отсутствует! \n" + e.getMessage());
        }
    }
}
