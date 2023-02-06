package ru.yandex.autotests.innerpochta.tests.xiva;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

/**
 * @author sbdsh
 */
@Aqua.Test
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.INCOMING_MESSAGES)
@RunWith(DataProviderRunner.class)
@Title("Тесты на получение новых сообщений в папки и треды")
public class XivaIncomingMessages extends BaseTest {

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    public static final int THREAD_SIZE = 3;
    public static final int MESSAGE_PER_PAGE = 5;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] userInterface2PaneAnd3PaneVertical() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @DataProvider
    public static Object[][] userInterface2PaneAnd3PaneHorizontal() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL}
        };
    }

    @Test
    @Title("Видим письмо во входящих без рефреша")
    @TestCaseId("2386")
    @UseDataProvider("userInterface2PaneAnd3PaneVertical")
    public void shouldSeeMessageInInbox(String layout) {
        user.apiSettingsSteps().callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        String expectedSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc().getSelfEmail(),
            expectedSubject,
            Utils.getRandomString()
        );
        user.messagesSteps().shouldSeeMessageWithSubjectWithoutRefresh(expectedSubject);
    }

    @Test
    @Title("Видим письмо в пользовательской папке без рефреша")
    @TestCaseId("2388")
    @UseDataProvider("userInterface2PaneAnd3PaneHorizontal")
    public void shouldSeeMessageInCustomFolder(String layout) {
        String expectedSubject = Utils.getRandomString();
        createFolderAndFilter(expectedSubject);
        user.apiSettingsSteps().callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(0);
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(
            lock.firstAcc().getSelfEmail(),
            expectedSubject,
            ""
        );
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(expectedSubject);
    }

    @Test
    @Title("Новое письмо добавляется к раскрытому треду")
    @TestCaseId("2389")
    @UseDataProvider("userInterface2PaneAnd3PaneVertical")
    public void shouldSeeMessageInThread(String layout) {
        String expectedSubject = Utils.getRandomString(), expectedSubject2 = Utils.getRandomString();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), expectedSubject, THREAD_SIZE);
        Folder folder = createFolderAndFilter(expectedSubject);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), expectedSubject2, "");
        user.apiSettingsSteps().callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().expandsMessagesThread(expectedSubject)
            .shouldSeeThreadCounter(expectedSubject, THREAD_SIZE);
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithoutCheck(expectedSubject, lock.firstAcc(), "");
        user.messagesSteps().shouldSeeThreadCounter(expectedSubject, THREAD_SIZE + 2)
            .shouldSeeMessageFoldersInThread(folder.getName(), 0);
        user.defaultSteps().shouldHasText(
            onMessagePage().displayedMessages().list().get(0).subject(),
            expectedSubject2
        );
    }

    @Test
    @Title("Создается тред из письма, которое загрузилось после нажатия на кнопку еще")
    @TestCaseId("2391")
    public void shouldCreateThreadWithMessageAfterMoreButton() {
        String expectedSubject = Utils.getRandomString();
        user.apiSettingsSteps().callWith(
           of(
               SETTINGS_PARAM_MESSAGES_PER_PAGE, String.valueOf(MESSAGE_PER_PAGE),
               SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
           )
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), expectedSubject, "");
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MESSAGE_PER_PAGE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().loadMoreMessagesButton());
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(expectedSubject, lock.firstAcc(), "");
        user.defaultSteps().shouldHasText(
            onMessagePage().displayedMessages().list().get(0).subject(),
            expectedSubject
        );
        user.messagesSteps().expandsMessagesThread(expectedSubject).shouldSeeThreadCounter(expectedSubject, 2);
    }

    @Test
    @Title("Создается тред из письма, которое находится под кнопкой еще")
    @TestCaseId("2391")
    public void shouldCreateThreadWithMessageUnderMoreButton() {
        String expectedSubject = Utils.getRandomString();
        user.apiSettingsSteps().callWith(
            of(SETTINGS_PARAM_MESSAGES_PER_PAGE,
                String.valueOf(MESSAGE_PER_PAGE),
                SETTINGS_PARAM_LAYOUT,
                LAYOUT_2PANE)
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), expectedSubject, "");
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MESSAGE_PER_PAGE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(expectedSubject, lock.firstAcc(), "");
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(expectedSubject);
        user.messagesSteps().expandsMessagesThread(expectedSubject).shouldSeeThreadCounter(expectedSubject, 2);
    }

    @Step("Создаем тему и письма с этой темой перекладываем в пользовательскую папку")
    private Folder createFolderAndFilter(String expectedSubject) {
        Folder folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomString());
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            expectedSubject,
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            user.apiFoldersSteps().getFolderBySymbol(folder.getName()).getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        );
        return folder;
    }
}
