package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DND;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author sbdsh
 */

@Aqua.Test
@Title("Тесты на тулбар в списке писем")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarTest extends BaseTest {

    private static final int THREAD_SIZE = 3;
    private String threadSubj;
    private String subj;

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
    public void logIn() throws IOException {
        threadSubj = getRandomString();
        subj = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "").getSubject();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), threadSubj, THREAD_SIZE);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane, dnd и тредный режим",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL,
                SETTINGS_DND, true,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(subj, threadSubj);
    }


    @Test
    @Title("Пиним треды в тредном режиме")
    @TestCaseId("1020")
    public void shouldSeePinnedThread() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, TRUE)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).expandThread());
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(threadSubj);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().pinBtn());
        user.messagesSteps().shouldSeeThatMessageIsPinned(threadSubj);

    }

    @Test
    @Title("Пиним треды в нетредном режиме")
    @TestCaseId("2156")
    public void shouldSeePinnedOnlyOneMsg() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(
                onMessagePage().displayedMessages().list().get(2).avatarAndCheckBox(),
                onMessagePage().toolbar().pinBtn()
            )
            .shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().shouldPinOnlyOneMsgInThread(threadSubj);
    }

    @Test
    @Title("Пиним одиночное письмо")
    @TestCaseId("1021")
    public void shouldSeePinnedSingleMsg() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .clicksOn(
                user.messagesSteps().findMessageBySubject(subj).avatarAndCheckBox(),
                onMessagePage().toolbar().pinBtn()
            );
        user.messagesSteps().shouldSeeThatMessageIsPinned(subj);
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subj);
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subj);
    }

    @Test
    @Title("Удалить через д-н-д на тулбар")
    @TestCaseId("1026")
    public void shouldSeeMsgInTrash() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .dragAndDrop(
                user.messagesSteps().findMessageBySubject(subj).subject(),
                onMessagePage().toolbar().deleteButton()
            )
            .opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subj);
    }

    @Test
    @Title("Спам через д-н-д на тулбар")
    @TestCaseId("1029")
    public void shoudSeemsgInSpam() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .dragAndDrop(
                user.messagesSteps().findMessageBySubject(subj).subject(),
                onMessagePage().toolbar().spamButton()
            )
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(subj);
    }

    @Test
    @Title("Прочитанность через тулбар")
    @TestCaseId("1030")
    public void shouldChangeReadStatusByDnd() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subj);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsReadButton())
            .refreshPage();
        user.messagesSteps().selectMessageWithSubject(subj);
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().shouldSeeThatMessageIsRead();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsUnreadButton())
            .refreshPage();
        user.messagesSteps().selectMessageWithSubject(subj)
            .shouldSeeThatMessageIsNotRead();
    }

    @Test
    @Title("Новая метка через тулбар")
    @TestCaseId("1961")
    public void shouldSeeNewLabelOnMsg() {
        String labelName = getRandomString();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subj);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markMessageDropDown())
            .shouldSee(onMessageView().labelsDropdownMenu())
            .clicksOn(onMessageView().labelsDropdownMenu().createNewLabel());
        user.settingsSteps().inputsLabelName(labelName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newLabelPopUp().createMarkButton())
            .shouldNotSee(onFoldersAndLabelsSetup().newLabelPopUp());
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(labelName, subj);
    }
}
