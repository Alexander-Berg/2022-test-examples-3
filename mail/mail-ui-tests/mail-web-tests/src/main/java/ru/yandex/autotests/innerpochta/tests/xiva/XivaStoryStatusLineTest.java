package ru.yandex.autotests.innerpochta.tests.xiva;

import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.matchers.ContainsTextMatcher.containsText;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на строку уведомления при действиях с письмами")
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.NOTIFY_LINE)
public class XivaStoryStatusLineTest extends BaseTest {

    private static final String COUNT_MSG = "1";
    private static final String CUSTOM_FOLDER = "test";
    private static final String MESSAGES_DELETED = "2 сообщения удалены.";
    private static final String MESSAGE_MOVED_PATTERN = "Перекладывать все письма от «%s» в папку «%s»?";
    private static final String MESSAGES_MOVED = "2 сообщения перемещены.";
    private static final String MESSAGES_ARE_SPAM = "2 сообщения помечены как спам.";

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 2);
        user.apiSettingsSteps().callWithListAndParams(
                "Включаем показ 1 письма на странице, раскрываем все папки",
                of(
                        SETTINGS_PARAM_MESSAGES_PER_PAGE, COUNT_MSG,
                        FOLDERS_OPEN, user.apiFoldersSteps().getAllFids()
                )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Видим уведомление при удалении нескольких писем")
    @TestCaseId("1899")
    public void deleteMultipleMessagesNotify() {
        loadMoreAndCheckMessages();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton());
        checksStatusLineWithWaiting(MESSAGES_DELETED);
    }

    @Test
    @Title("Видим уведомление при перемещении письма")
    @TestCaseId("1900")
    public void moveMessageNotify() {
        user.messagesSteps().clicksOnMessageCheckBox();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown());
        MailElement folder = user.defaultSteps().shouldSeeElementInList(
            onMessagePage().moveMessageDropdownMenu().customFolders(),
            CUSTOM_FOLDER
        );
        user.defaultSteps().onMouseHoverAndClick(folder);
        checksStatusLineWithWaiting(String.format(MESSAGE_MOVED_PATTERN, lock.firstAcc().getSelfEmail(), CUSTOM_FOLDER));
    }

    @Test
    @Title("Видим уведомление при перемещении нескольких писем")
    @TestCaseId("1901")
    public void moveMultipleMessagesNotify() {
        loadMoreAndCheckMessages();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown());
        MailElement folder = user.defaultSteps().shouldSeeElementInList(
            onMessagePage().moveMessageDropdownMenu().customFolders(),
            CUSTOM_FOLDER
        );
        user.defaultSteps().onMouseHoverAndClick(folder);
        checksStatusLineWithWaiting(MESSAGES_MOVED);
    }

    @Test
    @Title("Видим уведомление при перемещении нескольких писем в спам")
    @TestCaseId("1903")
    public void labelMultipleMessagesAsSpamNotify() {
        loadMoreAndCheckMessages();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton());
        checksStatusLineWithWaiting(MESSAGES_ARE_SPAM);
    }

    @Step("Должны увидеть сообщение в строке уведомлений «{1}», ждём с задержкой")
    public void checksStatusLineWithWaiting(String message) {
        assertThat(
            "Текст в уведомлении не содержит нужного значения",
            webDriverRule,
            withWaitFor(containsText(message), XIVA_TIMEOUT)
        );
    }

    @Step("Подгружаем вторую страницу писем и выделяем 2 письма")
    private void loadMoreAndCheckMessages() {
        user.defaultSteps().clicksOn(onMessagePage().loadMoreMessagesButton());
        user.messagesSteps().clicksOnMultipleMessagesCheckBox(0, 1);
    }
}
