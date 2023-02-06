package ru.yandex.autotests.innerpochta.tests.messageslist;


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
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Сохранение аттачей на ядиск")
@Features({FeaturesConst.SAVE_TO_DISK, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.SAVE_TO_DISK)
@Stories(FeaturesConst.ATTACHES)
public class SaveAttachmentToDiskTest extends BaseTest {

    private static final String THREAD_SUBJ = "attachment";
    private static final String MESSAGE_SUBJ = "attachments";
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сохраняем аттачмент на ядиск в просмотре письма в списке писем")
    @TestCaseId("3605")
    public void shouldSaveAttachmentFromMessageCompactView() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(MESSAGE_SUBJ);
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(2))
            .clicksOn(onMessageView().attachments().list().get(2).save())
            .shouldSee(onMessageView().saveToDiskPopup());
    }

    @Test
    @Title("Сохраняем аттачмент на ядиск в списке писем")
    @TestCaseId("3605")
    public void shouldSaveAttachmentFromMessageList() {
        user.defaultSteps().onMouseHover(onMessagePage().displayedMessages().list().get(0).attachments().list().get(0))
            .clicksOn(onMessagePage().displayedMessages().list().get(0).attachments().list().get(0).save())
            .shouldSee(onMessagePage().saveToDiskPopup());
    }

    @Test
    @Title("Сохраняем аттачмент на ядиск в шапке треда")
    @TestCaseId("3605")
    public void shouldSaveAttachmentFromThreadHeader() {
        user.defaultSteps().onMouseHover(onMessagePage().displayedMessages().list().get(1).attachments().list().get(0))
            .clicksOn(onMessagePage().displayedMessages().list().get(1).attachments().list().get(0).save())
            .shouldSee(onMessagePage().saveToDiskPopup());
    }

    @Test
    @Title("Сохраняем аттачмент на ядиск в письме в треде")
    @TestCaseId("3605")
    public void shouldSaveAttachmentFromMessageInThread() {
        user.messagesSteps().expandsMessagesThread(THREAD_SUBJ);
        user.defaultSteps()
            .onMouseHover(onMessagePage().displayedMessages().messagesInThread().get(1).attachments().list().get(0))
            .clicksOn(onMessagePage().displayedMessages().messagesInThread().get(1).attachments().list().get(0).save())
            .shouldSee(onMessagePage().saveToDiskPopup());
    }

    @Test
    @Title("Сохраняем аттачмент на ядиск в просмотре письма")
    @TestCaseId("3605")
    public void shouldSaveAttachmentFromMessageView() {
        user.apiSettingsSteps().callWithListAndParams(
                "Выключаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(MESSAGE_SUBJ);
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(3))
            .clicksOn(onMessageView().attachments().list().get(3).save())
            .shouldSee(onMessageView().saveToDiskPopup());
    }
}
