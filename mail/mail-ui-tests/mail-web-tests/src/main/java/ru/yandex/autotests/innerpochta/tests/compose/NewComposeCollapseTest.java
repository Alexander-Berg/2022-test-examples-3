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
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
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
@Title("Новый композ - Сворачивание / Разворачивание попапа")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
@RunWith(DataProviderRunner.class)
public class NewComposeCollapseTest extends BaseTest {
    String msgSubject;
    String msgTo;

    private static final int MAX_COMPOSES_IN_STACK = 20;
    private static final int COMPOSES_FOR_STACK = 12;
    private static final String COMPOSE_STACK_TEXT = "Еще 8 черновиков";

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
    @Title("Сворачиваем попап композа по кнопке")
    @TestCaseId("5504")
    @UseDataProvider("layouts")
    public void shouldSeeCollapsedComposePopup(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().expandedPopup().hideBtn());
        shouldSeeCollapsedComposeAndDraft();
    }

    @Test
    @Title("Сворачиваем попап композа по клику вне композа")
    @TestCaseId("5737")
    @UseDataProvider("layouts")
    public void shouldSeeCollapsedComposePopupOffsetClick(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onMessagePage().foldersNavigation().draftFolder())
            .shouldBeOnUrlWith(QuickFragments.DRAFT);
        shouldSeeCollapsedComposeAndDraft();
        user.defaultSteps().clicksOn(onComposePopup().composeThumb().get(0).theme())
            .shouldNotSee(onComposePopup().composeThumb())
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTo(), msgTo)
            .shouldHasValue(onComposePopup().expandedPopup().sbjInput(), msgSubject);
    }

    @Test
    @Title("Разворачиваем попап композа по кнопке")
    @TestCaseId("5507")
    @UseDataProvider("layouts")
    public void shouldSeeExpandedComposeAfterButtonClick(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().expandedPopup().hideBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .clicksOn(onComposePopup().composeThumb().get(0).expandBtn())
            .shouldSee(onComposePopup().composePopup())
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTo(), msgTo)
            .shouldHasValue(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTitle(), msgSubject);
    }

    @Test
    @Title("Разворачиваем попап композа по клику на тему")
    @TestCaseId("5507")
    @UseDataProvider("layouts")
    public void shouldSeeExpandedComposeAfterClick(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().expandedPopup().hideBtn())
            .shouldNotSee(onComposePopup().composePopup())
            .clicksOn(onComposePopup().composeThumb().get(0).theme())
            .shouldSee(onComposePopup().composePopup())
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTo(), msgTo)
            .shouldHasValue(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTitle(), msgSubject);
    }

    @Test
    @Title("Сворачиваем несколько композов для формирования стека")
    @TestCaseId("5511")
    @UseDataProvider("layouts")
    public void shouldFormComposeStack(String layouts) {
        user.composeSteps().switchLayoutAndOpenCompose(layouts);
        user.composeSteps().fillComposeStack(COMPOSES_FOR_STACK);
        user.defaultSteps().shouldSee(onComposePopup().composeStack())
            .shouldContainText(onComposePopup().composeStackTitle(), COMPOSE_STACK_TEXT)
            .clicksOn(onComposePopup().composeStack())
            .shouldSee(onComposePopup().composeStackContent());
        for (int i = 0; i <= 5; i++) {
            checkComposeStackThumb(i);
        }
    }

    @Test
    @Title("Разворачиваем композ из стека и закрываем его")
    @TestCaseId("5511")
    @UseDataProvider("layouts")
    public void shouldRemoveMsgFromStackAfterClose(String layouts) {
        user.composeSteps().switchLayoutAndOpenCompose(layouts);
        user.composeSteps().fillComposeStack(COMPOSES_FOR_STACK);
        user.defaultSteps().clicksOn(onComposePopup().composeStack());
        String msgSubjInStack = onComposePopup().composeStackThumb().get(2).theme().getText();
        user.defaultSteps().clicksOn(onComposePopup().composeStackThumb().get(2))
            .clicksOn(onComposePopup().expandedPopup().closeBtn(), onComposePopup().composeStack());
        for (int i = 0; i <= 5; i++) {
            user.defaultSteps().
                shouldNotContainText(onComposePopup().composeStackThumb().get(i).theme(), msgSubjInStack);
        }
    }

    @Test
    @Title("Переполняем стек свернутых композов и проверяем, что письма уходят из стека")
    @TestCaseId("5511")
    @UseDataProvider("layouts")
    public void shouldGoFromStackAfterOverflow(String layouts) {
        user.composeSteps().switchLayoutAndOpenCompose(layouts);
        user.composeSteps().fillComposeStack(MAX_COMPOSES_IN_STACK);
        user.defaultSteps().clicksOn(onComposePopup().composeStack())
            .scrollDown(onComposePopup().composeStackDropout());
        String lastMsgSubj = onComposePopup().composeStackThumb().get(14).theme().getText();
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().sbjInput(), getRandomString());
        user.hotkeySteps().pressCombinationOfHotKeys(
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL),
            "s"
        );
        user.defaultSteps().clicksOn(onComposePopup().composeStack())
            .scrollDown(onComposePopup().composeStackDropout())
            .shouldNotContainText(onComposePopup().composeStackThumb().get(14).theme(), lastMsgSubj);
    }

    @Step("Проверяем что композ свернулся и черновик сохранился")
    private void shouldSeeCollapsedComposeAndDraft() {
        user.defaultSteps().shouldNotSee(onComposePopup().composePopup())
            .shouldSee(
                onComposePopup().composeThumb().get(0),
                onComposePopup().composeThumb().get(0).avatar(),
                onComposePopup().composeThumb().get(0).theme(),
                onComposePopup().composeThumb().get(0).expandBtn(),
                onComposePopup().composeThumb().get(0).closeBtn()
            )
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Step("Проверяем все элементы свернутого композа в стеке")
    private void checkComposeStackThumb(int count) {
        user.defaultSteps().shouldSee(
            onComposePopup().composeStackThumb().get(count).avatar(),
            onComposePopup().composeStackThumb().get(count).theme(),
            onComposePopup().composeStackThumb().get(count).closeBtn(),
            onComposePopup().composeStackThumb().get(count).expandBtn()
        );
    }
}
