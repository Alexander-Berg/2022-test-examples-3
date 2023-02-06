package ru.yandex.autotests.innerpochta.tests.leftpanel;

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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на кнопку непрочитано в ЛК")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.TOOLBAR)
@UseCreds({ReadUnreadFromDropDownMenuTest.CREDS})
public class ReadUnreadFromDropDownMenuTest extends BaseTest {

    public static final String CREDS = "ToolBarReadUnread";
    private static final int SENT_MSG_COUNT = 2;

    private String subject, secondSubject;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        subject = Utils.getRandomName();
        secondSubject = Utils.getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), secondSubject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Тест на прочитано/непрочитано нескольких сообщений через тулбар")
    @TestCaseId("1569")
    public void testLabelMultipleMessagesReadAndUnread() {
        user.messagesSteps().selectMessageWithSubject(subject, secondSubject)
            .labelsMessageAsReadFromDropDownMenu();
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject, secondSubject);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().selectMessageWithSubject(subject, secondSubject)
            .labelsMessageAsUnreadFromDropDownMenu();
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldSeeMessageWithSubject(subject, secondSubject);
    }

    @Test
    @Title("Прыщ непрочитанности появляется при наличии непрочитанных писем")
    @TestCaseId("2067")
    public void shouldSeeMarkReadIcon() {
        user.settingsSteps().showMarkMsgAsReadPopup();
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), SENT_MSG_COUNT);
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().foldersNavigation().markReadIcon())
            .clicksOn(onMessagePage().foldersNavigation().markReadIcon())
            .shouldSee(onMessagePage().markAsReadPopup())
            .turnTrue(onMessagePage().markAsReadPopup().doNotAskAgainCheckbox())
            .clicksOn(onMessagePage().markAsReadPopup().agreeBtn())
            .shouldNotSee(onMessagePage().markAsReadPopup())
            .clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldNotSeeMessagesPresent();
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), SENT_MSG_COUNT);
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .shouldSee(onMessagePage().foldersNavigation().markReadIcon())
            .clicksOn(onMessagePage().foldersNavigation().markReadIcon())
            .shouldNotSee(onMessagePage().markAsReadPopup())
            .clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldNotSeeMessagesPresent();
    }
}
