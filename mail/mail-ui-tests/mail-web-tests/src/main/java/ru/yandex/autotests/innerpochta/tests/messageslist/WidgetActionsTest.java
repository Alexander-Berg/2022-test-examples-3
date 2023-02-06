package ru.yandex.autotests.innerpochta.tests.messageslist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.resources.MoveAllMessagesToFolderRule.moveAllMessagesToFolderRule;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveOldMessagesRule.removeOldMessagesRule;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SHOW_WIDGETS_DECOR;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Действия с письмами с виджетами")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@Description("Юзеру каждый день приходят письма с виджетами. Пак: 5d2f30a18a90ab681d205407")
public class WidgetActionsTest extends BaseTest {

    private final String MSG_SUBJ_SUBSTR_TO_SELECT = "Your Booking Details";
    private final String CUSTOM_FOLDER_NAME = "CUSTOM_FOLDER";
    private final String OLDER_THAN_DAYS = "14";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(moveAllMessagesToFolderRule(user, SPAM, INBOX))
        .around(moveAllMessagesToFolderRule(user, TRASH, INBOX))
        .around(moveAllMessagesToFolderRule(user, CUSTOM_FOLDER_NAME, INBOX))
        .around(removeOldMessagesRule(user, INBOX, OLDER_THAN_DAYS));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем объединяем аватары с чекбоксами",
            of(
                SETTINGS_SHOW_WIDGETS_DECOR, TRUE,
                SETTINGS_PARAM_MESSAGE_AVATARS, TRUE,
                SETTINGS_PARAM_MESSAGE_UNION_AVATARS, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переслать письмо-виджет")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldForwardWidgetMessage() {
        String msgSubject = getFullSubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        MessageBlock m = getMostRecentMessageBySubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        user.defaultSteps().clicksOn(m.avatarAndCheckBox(), user.pages().MessagePage().toolbar().forwardButton());
        user.composeSteps().shouldSeeSubject("Fwd: " + msgSubject);
    }

    @Test
    @Title("Удалить письмо-виджет")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldDeleteWidgetMessage() {
        String msgSubject = getFullSubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        moveToTrash(MSG_SUBJ_SUBSTR_TO_SELECT);
        user.messagesSteps().shouldNotSeeMessageWithSubject(msgSubject);
        user.leftColumnSteps().opensTrashFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
        user.defaultSteps().shouldNotSee(
            user.pages().MessagePage().displayedMessages().list().get(0).widgetTicket(),
            user.pages().MessagePage().displayedMessages().list().get(0).widget()
        );
    }

    @Test
    @Title("Переместить письмо-виджет в спам")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMoveWidgetToSpamFolder() {
        String msgSubject = getFullSubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        moveToSpam(msgSubject);
        user.messagesSteps().shouldNotSeeMessageWithSubject(msgSubject);
        user.leftColumnSteps().opensSpamFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
        user.defaultSteps().shouldNotSee(
            user.pages().MessagePage().displayedMessages().list().get(0).widgetTicket(),
            user.pages().MessagePage().displayedMessages().list().get(0).widget()
        );
    }

    @Test
    @Title("Переместить письмо-виджет из спама")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMoveWidgetFromSpamFolder() {
        String msgSubject = getFullSubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        moveToSpam(msgSubject);
        user.leftColumnSteps().opensSpamFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
        moveFromSpam(msgSubject);
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().inbox());
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
        user.defaultSteps().shouldSee(getMostRecentMessageBySubject(msgSubject).widgetTicket());
    }

    @Test
    @Title("Переместить письмо-виджет в кастомную папку")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMoveWidgetToCustomFolder() {
        MessageBlock msgBlock = getMostRecentMessageBySubject(MSG_SUBJ_SUBSTR_TO_SELECT);
        String fullSubject = getFullSubject(msgBlock);
        user.defaultSteps().clicksOn(
            msgBlock.avatarAndCheckBox(),
            user.pages().MessagePage().toolbar().moveMessageDropDown()
        ).clicksOnElementWithText(
            user.pages().MessagePage().moveMessageDropdownMenu().customFolders(), CUSTOM_FOLDER_NAME
        );
        user.messagesSteps().shouldNotSeeMessageWithSubject(fullSubject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER_NAME);
        user.defaultSteps().shouldSee(
            user.pages().MessagePage().displayedMessages().list().get(0).widgetTicket()
        );
    }

    @Step("Перемещаем письмо с темой «{0}» в удаленные")
    private void moveToTrash(String subject) {
        user.defaultSteps().clicksOn(
            getMostRecentMessageBySubject(subject).avatarAndCheckBox(),
            user.pages().MessagePage().toolbar().deleteButton()
        );
    }

    @Step("Перемещаем письмо с темой «{0}» в спам")
    private void moveToSpam(String subject) {
        user.defaultSteps().clicksOn(
            getMostRecentMessageBySubject(subject).avatarAndCheckBox(),
            user.pages().MessagePage().toolbar().spamButton()
        );
    }

    @Step("Перемещаем письмо с темой «{0}» из спама")
    private void moveFromSpam(String subject) {
        user.defaultSteps().clicksOn(
            getMostRecentMessageBySubject(subject).avatarAndCheckBox(),
            user.pages().MessagePage().toolbar().notSpamButton()
        );
    }

    @Step("Получаем полную тему письма, содержащую подстроку «{0}»")
    private String getFullSubject(String subjectSubstring) {
        return getFullSubject(getMostRecentMessageBySubject(subjectSubstring));
    }

    @Step("Получаем полную тему письма")
    private String getFullSubject(MessageBlock messageBlock) {
        return messageBlock.subject().getText();
    }

    @Step("Получаем последнее письмо с темой «{0}»")
    private MessageBlock getMostRecentMessageBySubject(String subject) {
        return user.pages().MessagePage().displayedMessages().list().filter(
            msg -> msg.subject().getText().contains(subject)
        ).get(0);
    }
}
