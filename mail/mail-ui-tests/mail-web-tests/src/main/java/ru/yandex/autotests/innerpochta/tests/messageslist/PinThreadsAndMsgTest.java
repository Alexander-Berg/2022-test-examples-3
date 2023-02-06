package ru.yandex.autotests.innerpochta.tests.messageslist;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_WIDGETS;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Проверяем “Запинивание“ писем и тредов")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class PinThreadsAndMsgTest extends BaseTest {

    private static final String CUSTOM_FOLDER = Utils.getRandomName();
    private static final int THREAD_SIZE = 3;

    private String subject;
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
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем тредный режим, выключаем панель виджетов",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, TRUE,
                SHOW_WIDGETS, FALSE
            )
        );
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().setsWindowSize(1600, 300);
        user.leftColumnSteps().openFolders();
    }

    @Test
    @Title("Пиним одиночные письма через залипающий тулбар")
    @TestCaseId("1019")
    public void shouldPinMessage() {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject)
            .scrollDownPage();
        user.defaultSteps().shouldSee(onMessagePage().stickyToolBar())
            .clicksOn(onMessagePage().stickyToolBar().pinBtn());
        user.messagesSteps().scrollUpPage()
            .shouldSeeThatMessageIsPinned(subject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeThatMessageIsPinned(subject);
    }

    @Test
    @Title("Распиниваем одиночные письма через залипающий тулбар")
    @TestCaseId("3300")
    public void shouldUnpiMessage() {
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.apiLabelsSteps().pinLetter(msg);
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().shouldSeeThatMessageIsPinned(msg.getSubject())
            .clicksOnMessageCheckBoxWithSubject(msg.getSubject())
            .scrollDownPage();
        user.defaultSteps().shouldSee(onMessagePage().stickyToolBar())
            .clicksOn(onMessagePage().stickyToolBar().unPinBtn());
        user.messagesSteps().scrollUpPage()
            .shouldSeeThatMessageIsUnPinned(msg.getSubject());
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldNotSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Смотрим запиненное письмо треда в 3пейне в тредном и нетредном режиме")
    @TestCaseId("1022")
    public void shouldPinMsgFromThread() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        user.defaultSteps().refreshPage();
        subject = user.apiMessagesSteps().sendThread(lock.firstAcc(), Utils.getRandomName(), THREAD_SIZE).getSubject();
        user.messagesSteps().expandsMessagesThread(subject)
            .selectMessagesInThreadCheckBoxWithNumber(1);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().pinBtn());
        user.messagesSteps().shouldSeeThatMessageIsPinned(subject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeThatMessageIsPinned(subject);
        user.apiSettingsSteps()
            .callWithListAndParams("Выключаем тредный режим", of(SETTINGS_FOLDER_THREAD_VIEW, FALSE));
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldPinOnlyOneMsgInThread(subject);
    }

    @Test
    @Title("Не показываем запиненные в шаблонах, черновиках, спаме")
    @TestCaseId("978")
    public void shouldNotSeePinnedMsgInDraft() {
        String subj = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj, "");
        user.defaultSteps().setsWindowSize(1920, 1080)
            .shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().shouldSeeMessageWithSubject(subj);
        user.defaultSteps()
            .clicksOn(
                user.messagesSteps().findMessageBySubject(subj).avatarAndCheckBox(),
                onMessagePage().toolbar().pinBtn()
            );
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeThatMessageIsPinned(subj);
        user.defaultSteps()
            .shouldContainText(onMessagePage().displayedMessages().list().get(0).folder(), INBOX_RU)
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subj);
        user.defaultSteps().opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subj);
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subj);
    }
}
