package ru.yandex.autotests.innerpochta.tests.messageslist;

import ch.lambdaj.function.closure.Switcher;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на выделение писем и статуслайн")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class MessageSelectionTest extends BaseTest {

    private static final String THREAD = "thread";
    private static final String MORE_THAN_EQUALTO_5_MSG_DELETED = "Выбрано %d писем";
    private static final String LESS_THAN_5_MSG_DELETED = "Выбрано %d письма";
    private static final int MSG_COUNT = 3;
    private static final int THREAD_SIZE = 4;
    private static final String SUBJ3 = "subj 3";
    private static final String SUBJ2 = "subj 2";

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
    public void logIn() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
        user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD, THREAD_SIZE);
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(THREAD);
    }

    @Test
    @Title("Выделяем несколько обычных писем и проверяем сообщение о выбранных")
    @TestCaseId("4469")
    public void testSelectMessages() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(SUBJ2);
        user.defaultSteps().shouldNotSee(onMessagePage().inboxMsgInfoline());
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(SUBJ3);
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(2));
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(7));
    }

    @Test
    @Title("Выделяем письма в треде и проверяем сообщение о выбранных")
    @TestCaseId("4470")
    public void testSelectMessagesFromThread() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        user.defaultSteps().refreshPage();
        user.messagesSteps().expandsMessagesThread(THREAD)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        user.defaultSteps().shouldNotSee(onMessagePage().inboxMsgInfoline());
        user.messagesSteps().selectMessagesInThreadCheckBoxWithNumber(1);
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(2));
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(SUBJ2);
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(3));
        user.messagesSteps().selectMessageWithSubject(THREAD);
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(5));
        user.messagesSteps().deselectMessageCheckBoxWithSubject(SUBJ2, THREAD);
        user.defaultSteps().shouldNotSee(onMessagePage().inboxMsgInfoline());
    }


    @Test
    @Title("Выделяем все письма и проверяем сообщение о выбранных")
    @TestCaseId("2081")
    public void testSelectAllMessagesInFolder() {
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().shouldContainText(onMessagePage().inboxMsgInfoline().msgCount(), countString(7))
            .clicksOn(onMessagePage().inboxMsgInfoline().deselectLink());
        user.messagesSteps().shouldSeeThatMessagesAreNotSelected();
    }

    @Test
    @Title("Попап балковых операций пропадает при снятии выделения с писем")
    @TestCaseId("4278")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-67935")
    public void shouldCloseBulkPopup() {
        user.leftColumnSteps().opensCustomFolder(0);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldSee(onHomePage().notification())
            .clicksOn(onMessagePage().inboxMsgInfoline().deselectLink())
            .shouldNotSee(onHomePage().notification());
    }

    private String countString(int delCount) {
        return new Switcher<String>()
            .addCase(greaterThanOrEqualTo(5), format(MORE_THAN_EQUALTO_5_MSG_DELETED, delCount))
            .addCase(greaterThan(1), format(LESS_THAN_5_MSG_DELETED, delCount)).exec(delCount);
    }
}
