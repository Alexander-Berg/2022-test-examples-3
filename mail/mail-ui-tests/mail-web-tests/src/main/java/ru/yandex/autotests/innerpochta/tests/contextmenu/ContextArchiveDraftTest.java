package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

/**
 * Created by mabelpines on 05.10.15.
 */
@Aqua.Test
@Title("Проверяем пункты КМ “Дописать“, “Архив“, “Создать шаблон“, “Пометить все письма прочитанными“.")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextArchiveDraftTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, user.apiFoldersSteps().getAllFids())
        );
        user.apiFoldersSteps().createTemplateFolder();
        user.apiFoldersSteps().createArchiveFolder();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем пункт “Дописать“ для черновиков")
    @TestCaseId("2029")
    public void shouldOpenDraftInCompose() {
        String draftBody = user.apiMessagesSteps().createDraftMessage();
        user.defaultSteps().opensFragment(QuickFragments.DRAFT)
            .rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject());
        user.messagesSteps().shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).updateDraft())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().shouldSeeTextAreaContains(draftBody);
    }

    @Test
    @Title("Клик по “Архив“ в КМ отправляет тред в папку “Архив“.")
    @TestCaseId("2030")
    public void shouldMoveToArchive() {
        String subject = getRandomString();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), subject, 4);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).archive())
            .opensFragment(QuickFragments.ARCHIVE);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Клик по “Пометить все письма прочитанными“ в КМ - не видим счетчик непрочитанных")
    @TestCaseId("2032")
    public void shouldMakeAllMessagesRead() {
        String subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "")
            .getSubject();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        user.defaultSteps().shouldSee(onMessagePage().foldersNavigation().inboxUnreadCounter());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inboxFolder());
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).markAllRead())
            .shouldNotSee(onMessagePage().foldersNavigation().inboxUnreadCounter());
    }
}
