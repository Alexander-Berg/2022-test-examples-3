package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PRIORITY_TAB;

/**
 * * @author eremin-n-s
 */
@Aqua.Test
@Title("Почта 360 - Таб «Главное")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TABS)
@RunWith(DataProviderRunner.class)
public class PriorityTabTest extends BaseTest {

    private String sbj = getRandomString();

    public AccLockRule lock = AccLockRule.use().useTusAccount(MAIL360_PAID);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(
                FOLDER_TABS, TRUE,
                PRIORITY_TAB, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Выключаем таб «Главное» через настройки")
    @TestCaseId("5980")
    public void shouldTurnOffPriorityTabThroughSetting() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().priorityTab())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .refreshPage()
            .opensFragment(QuickFragments.INBOX)
            .shouldSee(
                onMessagePage().inboxTab(),
                onMessagePage().newsTab(),
                onMessagePage().socialTab(),
                onMessagePage().attachmentsTab()
            )
            .shouldNotSee(onMessagePage().priorityTab());
    }

    @Test
    @Title("Снимаем приоритетную метку с треда")
    @TestCaseId("5977")
    public void shouldUnMarkPriorityToAllThread() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), sbj, 5);
        user.apiLabelsSteps().unPriorityLetters();
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).priorityMark())
            .clicksOn(onMessagePage().displayedMessages().list().get(0).priorityMarkActive())
            .clicksOn(onMessagePage().priorityTab());
        user.messagesSteps().shouldNotSeeMessageWithSubject(sbj);
    }

    @Test
    @Title("Снимаем приоритетную метку с письма")
    @TestCaseId("5978")
    public void shouldUnMarkPriorityToMessage() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), sbj, "");
        user.apiLabelsSteps().unPriorityLetters();
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).priorityMark())
            .clicksOn(onMessagePage().displayedMessages().list().get(0).priorityMarkActive())
            .clicksOn(onMessagePage().priorityTab());
        user.messagesSteps().shouldNotSeeMessageWithSubject(sbj);
    }

    @Test
    @Title("Пометка письма приоритетным")
    @TestCaseId("5975")
    public void shouldMarkPriorityToMessage() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), sbj, "");
        user.apiLabelsSteps().unPriorityLetters();
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).priorityMark())
            .clicksOn(onMessagePage().priorityTab());
        user.messagesSteps().shouldSeeMessageWithSubject(sbj);
    }
}
