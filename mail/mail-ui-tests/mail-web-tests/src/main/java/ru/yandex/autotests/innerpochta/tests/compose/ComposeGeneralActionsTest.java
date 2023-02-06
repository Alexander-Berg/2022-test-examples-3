package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

@Aqua.Test
@Title("Тест на оформление письма")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeGeneralActionsTest extends BaseTest {

    private static final String TEXT_WITH_SPACES =
        "первая        строка     с        пробелами\n" +
            "вторая        строка         с            пробелами\n" +
            "третья       строка           с               пробелами\n";
    private static final String LINK_TEXT = "1";
    private static final String LINK = "http://ya.ru/";
    private static final String CHANGED_LINK_TEXT = "линк";
    private static final String CHANGED_LINK = "http://yandex.ru/";
    private static final String DISK_LINK = "https://disk.yandex.ru/i/HBh0-6b0i7hhMw";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, FALSE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Проверяем форматирование письма пробелами")
    @TestCaseId("4484")
    public void shouldSeeFormattingWithSpaces() {
        String sbj = Utils.getRandomName();
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT_WITH_SPACES)
            .appendTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .appendTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.messageViewSteps().shouldSeeCorrectMessageText(TEXT_WITH_SPACES);
    }

    @Test
    @Title("Добавляем/изменяем ссылку при помощи кнопки \"Вставить/изменить ссылку\"")
    @TestCaseId("3351")
    public void composeActionsAddAndChangeLink() {
        addLinkInCompose(LINK, LINK_TEXT);
        user.defaultSteps().closeOpenedWindow()
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().bodyInput(), LINK_TEXT);
        addLinkInCompose(CHANGED_LINK, CHANGED_LINK_TEXT);
    }


    @Test
    @Title("Добавляем в письмо ссылку на Диск и отправляем с ней письмо")
    @TestCaseId("4460")
    public void shouldSeeDiskLinkInMessage() {
        String subject = getRandomName();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .appendTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .appendTextInElement(onComposePopup().expandedPopup().sbjInput(), subject)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), DISK_LINK)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.messageViewSteps().shouldSeeCorrectMessageText(DISK_LINK);
        user.defaultSteps().clicksOnLink(DISK_LINK)
            .shouldSeeNewWindowOpenedWithUrl(DISK_LINK, 1);
    }

    @Step("Добавляем ссылку в письмо")
    private void addLinkInCompose(String link, String text) {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().toolbarBlock().addLinkBtn())
            .shouldSee(onComposePage().addLinkPopup())
            .inputsTextInElementClearingThroughHotKeys(onComposePage().addLinkPopup().hrefInput(), link)
            .inputsTextInElementClearingThroughHotKeys(onComposePage().addLinkPopup().textInput(), text)
            .clicksOn(onComposePage().addLinkPopup().addLinkBtn());
        user.composeSteps().shouldSeeTextAreaContains(text);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().bodyInput())
            .clicksOnLink(text)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(link);
    }
}
