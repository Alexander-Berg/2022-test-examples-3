package ru.yandex.autotests.innerpochta.tests.xiva;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MultipleWindowsHandler;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Тест на синхронизацию двух вкладок")
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.TWO_TABS)
public class XivaStoryTwoTabsSyncTest extends BaseTest {

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddFolderIfNeedRule addFolderIfNeed = addFolderIfNeed(() -> user);
    private final String subject = Utils.getRandomString();
    private final String label = Utils.getRandomString();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addFolderIfNeed);

    @Before
    public void logIn() {
        user.apiLabelsSteps().addNewLabel(label, LABELS_PARAM_GREEN_COLOR);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Видим сообщение отправленное в другой вкладке")
    @TestCaseId("1908")
    public void shouldSeeSentMessage() {
        user.leftColumnSteps().opensSentFolder();
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        String expectedSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMail(lock.firstAcc(), expectedSubject, "");
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(expectedSubject);
    }

    @Test
    @Title("Перемещение сообщения подтягивается из другой вкладки")
    @TestCaseId("1904")
    public void shouldSeeMovedMessage() {
        user.leftColumnSteps().expandFoldersIfCan()
            .opensCustomFolder(0);
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().foldersNavigation().customFolders().get(2)
        )
            .switchesOnMainWindow(windowsHandler)
            .shouldSeeWithWaiting(onMessagePage().displayedMessages(), XIVA_TIMEOUT);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subject);
    }

    @Test
    @Title("Удаление сообщения подтягивается из другой вкладки")
    @TestCaseId("1905")
    public void shouldNotSeeDeletedMessage() {
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).avatarAndCheckBox())
            .clicksOn(onMessagePage().toolbar().deleteButton())
            .switchesOnMainWindow(windowsHandler)
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).subject());
    }

    @Test
    @Title("Проставление сообщению метки «Важно» подтягивается из другой вкладки")
    @TestCaseId("1910")
    public void shouldSeeImportantMessage() {
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.messagesSteps()
            .labelsMessageImportant(onMessagePage().displayedMessages().list().get(0).subject().getText());
        user.defaultSteps().switchesOnMainWindow(windowsHandler)
            .shouldSee(onMessagePage().displayedMessages().list().get(0).subject());
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject);
    }

    @Test
    @Title("Проставление сообщению пользовательской метки подтягивается из другой вкладки")
    @TestCaseId("1909")
    public void shouldSeeCustomLabel() {
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).avatarAndCheckBox())
            .clicksOn(user.pages().MessagePage().toolbar().markMessageDropDown())
            .shouldSee(user.pages().MessagePage().labelsDropdownMenu())
            .clicksOn(onMessagePage().labelsDropdownMenu().customMarks().get(0));
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subject);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(label, subject);
    }

    @Test
    @Title("Помечаем сообщение как «Спам» и видим изменение в другой вкладке")
    @TestCaseId("1906")
    public void shouldSeeMessageInSpam() {
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).avatarAndCheckBox())
            .clicksOn(onMessagePage().toolbar().spamButton())
            .switchesOnMainWindow(windowsHandler)
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).subject());
        user.leftColumnSteps().opensSpamFolder();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).subject());
    }

    @Test
    @Title("Помечаем сообщение прочитанным и видим изменение в другой вкладке")
    @TestCaseId("1907")
    public void shouldSeeThatMessageIsRead() {
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).avatarAndCheckBox())
            .clicksOn(onMessagePage().toolbar().markAsReadButton())
            .switchesOnMainWindow(windowsHandler)
            .clicksOn(onMessagePage().displayedMessages().list().get(0).avatarAndCheckBox());
        user.messagesSteps().shouldSeeThatMessageIsReadWithWaiting();
    }
}
