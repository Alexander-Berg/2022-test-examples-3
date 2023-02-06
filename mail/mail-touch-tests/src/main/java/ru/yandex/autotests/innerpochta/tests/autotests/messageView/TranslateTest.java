package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableSortedMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на переводчик в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TRANSLATE)
public class TranslateTest {

    private Message msg_deutsche;

    private static final String TEXT = "I want you to translate this",
        TEXT_RU = "Я хочу, чтобы вы перевели это",
        TEXT_DEUTSCH = "Ich möchte, dass du das übersetzst",
        LANGUAGE_CAPITAL_LETTER = "Арабский",
        LANGUAGE = "арабский",
        LANGUAGE_EN = "английский",
        LANGUAGE_RU = "русский",
        LANGUAGE_DE = "deutsch",
        LANGUAGE_EN_EN = "english";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        Message msg = steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomString(), TEXT);
        msg_deutsche = steps.user().apiMessagesSteps().sendMailWithNoSave(
            accLock.firstAcc(),
            getRandomString(),
            TEXT_DEUTSCH
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()));
    }

    @Test
    @Title("Не должны увидеть переводчик, если выключена настройка")
    @TestCaseId("1097")
    public void shouldNotSeeTranslate() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем переводчик",
            of(SETTINGS_PARAM_TRANSLATE, FALSE)
        );
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageView().toolbar())
            .shouldNotSee(steps.pages().touch().messageView().translator().translateBtn());
    }

    @Test
    @Title("Должны сохранить письмо переведённым при выходе из него")
    @TestCaseId("1115")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSaveTranslateAfterCloseMsgView() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().translateBtn())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .clicksOn(steps.pages().touch().messageList().messages().get(1))
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().msgBody(), TEXT_RU);
    }

    @Test
    @Title("Должны выключить переводчик через попап")
    @TestCaseId("1109")
    public void shouldSwitchOffTranslatorInPopup() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().closeBtn())
            .clicksOn(steps.pages().touch().messageView().confirmTranslatorBtn())
            .shouldNotSee(
                steps.pages().touch().messageView().translatorPopup(),
                steps.pages().touch().messageView().translator()
            );
        assertEquals(
            "Настройка «Предлагать перевод писем» должна быть выключена",
            steps.user().apiSettingsSteps().getUserSettings(SETTINGS_PARAM_TRANSLATE),
            STATUS_FALSE
        );
    }

    @Test
    @Title("Должны закрыть попап «Скрыть переводчик» по крестику")
    @TestCaseId("1108")
    public void shouldCloseTranslatorPopup() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().closeBtn())
            .clicksOn(steps.pages().touch().messageView().closeTranslatorPopupBtn())
            .shouldNotSee(steps.pages().touch().messageView().translatorPopup())
            .shouldSee(steps.pages().touch().messageView().translator());
        assertEquals(
            "Настройка «Предлагать перевод писем» должна быть включена",
            steps.user().apiSettingsSteps().getUserSettings(SETTINGS_PARAM_TRANSLATE),
            STATUS_TRUE
        );
    }

    @Test
    @Title("Должны закрыть попап выбора языка источника")
    @TestCaseId("1284")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseChoiceSourceLangList() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().sourceLangBtn())
            .clicksOn(steps.pages().touch().messageView().closeChoiceLangListBtn())
            .shouldNotSee(steps.pages().touch().messageView().choiceLangList());
    }

    @Test
    @Title("Должны закрыть попап выбора языка перевода")
    @TestCaseId("1111")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseChoiceTranslateLangList() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().translateLangBtn())
            .clicksOn(steps.pages().touch().messageView().closeChoiceLangListBtn())
            .shouldNotSee(steps.pages().touch().messageView().choiceLangList());
    }

    @Test
    @Title("Должны поменять язык источника")
    @TestCaseId("1285")
    public void shouldChangeSourceLang() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().sourceLangBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().choiceLangList(), LANGUAGE_CAPITAL_LETTER);
        checkSelectedSourceLang(LANGUAGE);
    }

    @Test
    @Title("Должны поменять язык перевода")
    @TestCaseId("1099")
    public void shouldChangeTranslateLang() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().translateLangBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().choiceLangList(), LANGUAGE_CAPITAL_LETTER);
        checkSelectedLang(LANGUAGE);
    }

    @Test
    @Title("Должны выбрать текущий язык источника")
    @TestCaseId("1286")
    public void shouldSelectCurrentSourceLang() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().sourceLangBtn())
            .clicksOn(steps.pages().touch().messageView().choiceLangList().waitUntil(not(empty())).get(0));
        checkSelectedSourceLang(LANGUAGE_EN);
    }

    @Test
    @Title("Должны выбрать текущий язык перевода")
    @TestCaseId("1287")
    public void shouldSelectCurrentTranslateLang() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().translator().translateLangBtn())
            .clicksOn(steps.pages().touch().messageView().choiceLangList().waitUntil(not(empty())).get(0));
        checkSelectedLang(LANGUAGE_RU);
    }

    @Test
    @Title("Язык должен определяться по тексту сообщения и языку интерфейса")
    @TestCaseId("1096")
    @DoTestOnlyForEnvironment("Android")
    public void shouldAutoSelectLangs() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg_deutsche.getMid()) + "?lang=en")
            .shouldContainText(steps.pages().touch().messageView().translator().sourceLangBtn(), LANGUAGE_DE)
            .shouldContainText(steps.pages().touch().messageView().translator().translateLangBtn(), LANGUAGE_EN_EN);
    }

    @Step("Проверяем, что попап закрылся и выбран нужный язык перевода")
    private void checkSelectedLang(String lang) {
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().choiceLangList())
            .shouldContainText(steps.pages().touch().messageView().translator().translateLangBtn(), lang);
    }

    @Step("Проверяем, что попап закрылся и выбран нужный язык исходника")
    private void checkSelectedSourceLang(String lang) {
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().choiceLangList())
            .shouldContainText(steps.pages().touch().messageView().translator().sourceLangBtn(), lang);
    }
}
