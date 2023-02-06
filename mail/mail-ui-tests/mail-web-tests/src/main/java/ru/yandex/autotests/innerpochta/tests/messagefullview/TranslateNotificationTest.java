package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на плашку перевода в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.HEAD)
public class TranslateNotificationTest extends BaseTest {

    private static final String HELP_URL = "https://yandex.ru/support/mail/web/letter/translation.html";
    private static final String TRANSLATE_NOTIFICATION = "Язык письма — турецкий. Перевести на русский?";
    private static final String TRANSLATION_DONE_NOTIFICATION = "оригинал: турецкий, переведено на русский.";
    private static final String TRANSLATED_TEXT = "о, да, детка!";
    private static final String ORIGINAL_TEXT = "oh, evet bebeğim!";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), ORIGINAL_TEXT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
    }

    @Test
    @Title("Тест на плашку перевода.")
    @TestCaseId("1620")
    public void messageHeadTranslate() {
        user.defaultSteps().shouldHasText(
            onMessageView().translateNotification().text(),
            TRANSLATE_NOTIFICATION
        )
            .clicksOn(onMessageView().translateNotification().translateButton())
            .shouldSee(onMessageView().translateNotification())
            .shouldHasText(onMessageView().translateNotification().textTranslated(), TRANSLATION_DONE_NOTIFICATION)
            .shouldHasText(onMessageView().messageTextBlock().text(), TRANSLATED_TEXT)
            .clicksOn(onMessageView().translateNotification().revert())
            .shouldHasText(onMessageView().messageTextBlock().text(), ORIGINAL_TEXT);
    }

    @Test
    @Title("Клик на крестик и вопросик на плашке перевода. Проверяем закрытие и переход в хелп.")
    @TestCaseId("1621")
    public void messageHeadTranslateMiniButton() {
        user.defaultSteps().shouldSee(onMessageView().translateNotification())
            .clicksOn(onMessageView().translateNotification().closeButton())
            .shouldNotSee(onMessageView().translateNotification())
            .refreshPage()
            .shouldSee(onMessageView().translateNotification())
            .clicksOn(onMessageView().translateNotification().helpButton())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(HELP_URL);
    }
}
