package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на драг-н-дроп на тулбар")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
@RunWith(DataProviderRunner.class)
public class DragAndDropMoreMessagesTest extends BaseTest {

    private static final int COUNT_MSG = 320;
    private static final String THREAD_TEXT = "Удалить 20 писем? Если среди них есть письма " +
        "из цепочек, то цепочки распадутся и их нельзя будет восстановить в том же виде.";
    private static final String NOT_THREAD_TEXT = "Удалить 20 писем? Их можно будет восстановить из папки «Удалённые»" +
        " в течение 31 дня.";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
            {TRUE, THREAD_TEXT},
            {FALSE, NOT_THREAD_TEXT}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2 pane, треды",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE,
                SETTINGS_PARAM_MESSAGES_PER_PAGE, COUNT_MSG
            )
        );
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 5);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Отмена в попапе удаления писем при драг-н-дропе на тулбар")
    @TestCaseId("4000")
    @UseDataProvider("data")
    public void shouldNotDeleteMessages(Boolean setting, String text) {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 15);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем/выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, setting)
        );
        user.defaultSteps().refreshPage()
            .shouldSeeWithWaiting(onMessagePage().displayedMessages().list().get(19), 20);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().toolbar().deleteButton()
        )
            .shouldSee(onHomePage().notification())
            .shouldSeeThatElementHasText(onHomePage().notification(), text)
            .clicksOn(onHomePage().cancelButton());
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Драг-н-дроп писем на кнопку «Прочитано»")
    @TestCaseId("4003")
    public void shouldSeeMessagesReadAfterDnD() {
        user.apiMessagesSteps().markLetterRead(user.apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0));
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().toolbar().markAsReadButton()
        );
        user.messagesSteps().selectsAllDisplayedMessagesInFolder()
            .shouldSeeThatMessageIsRead();
    }

    @Test
    @Title("Драг-н-дроп писем на кнопку «Не прочитано»")
    @TestCaseId("4002")
    public void shouldSeeMessagesUnreadAfterDnD() {
        user.apiMessagesSteps().markAllMsgRead();
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().toolbar().markAsUnreadButton()
        );
        user.messagesSteps().selectsAllDisplayedMessagesInFolder()
            .shouldSeeThatMessageIsNotRead();
    }

    @Test
    @Title("Драг-н-дроп писем из корзины на кнопку «Удалить»")
    @TestCaseId("4078")
    public void shouldNotSeeMessagesInTrashAfterDnDOnDeleteButton() {
        user.apiMessagesSteps().deleteAllMessagesInFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().toolbar().deleteButton()
        );
        user.messagesSteps().shouldNotSeeMessagesPresent();
    }
}
