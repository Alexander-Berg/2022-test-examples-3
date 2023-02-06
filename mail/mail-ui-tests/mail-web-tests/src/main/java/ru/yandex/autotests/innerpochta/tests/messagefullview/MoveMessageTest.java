package ru.yandex.autotests.innerpochta.tests.messagefullview;

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

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_FILTER_NOTIFICATION;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на перемещение писем")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class MoveMessageTest extends BaseTest {

    private static final String USER_FOLDER = Utils.getRandomName();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final String subject = Utils.getRandomString();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane и раскрываем все папки",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids(),
                SHOW_FILTER_NOTIFICATION, STATUS_ON
            )
        );
        user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переносим письмо в папку при просмотре письма.")
    @TestCaseId("1632")
    public void messageViewMoveMessageToFolder() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldNotSee(onMessageView().toolbar().markAsReadButton())
            .clicksOn(onMessageView().toolbar().moveMessageBtn())
            .shouldSee(onMessageView().moveMessageDropdownMenu())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(1));
        user.leftColumnSteps().shouldSeeCurrentFolderIs(USER_FOLDER);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensCustomFolder(USER_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }
}
