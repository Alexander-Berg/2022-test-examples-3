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
@Title("Тесты на счетчики папок, очистку")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class FoldersNavigationTest extends BaseTest {

    private static final String MAIN_SUPPORT_LINK = "https://yandex.ru/support/mail/";

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
    public void logIn() throws IOException {
        subject = user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Очищаем папку «Удалённые» находясь в ней")
    @TestCaseId("1506")
    public void testCleanTrashFolderInFolderHeader() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.TRASH)
            .clicksOn(onMessagePage().clearTrashButton());
        user.defaultSteps().clicksOn(onMessagePage().clearFolderPopUp().clearFolderButton());
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }

    @Test
    @Title("Очищаем папку «Спам» находясь в ней")
    @TestCaseId("1507")
    public void testCleanSpamFolderInFolderHeader() {
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.SPAM)
            .shouldSee(onMessagePage().clearTrashButton())
            .clicksOn(onMessagePage().notificationBlock().cleanSpamFolderButton())
            .clicksOn(onMessagePage().clearFolderPopUpOld().cancelButtonOld())
            .shouldNotSee(onMessagePage().clearFolderPopUpOld().cancelButtonOld())
            .clicksOn(onMessagePage().notificationBlock().cleanSpamFolderButton())
            .clicksOn(onMessagePage().clearFolderPopUpOld().closeButtonOLd())
            .shouldNotSee(onMessagePage().clearFolderPopUpOld().cancelButtonOld())
            .clicksOn(onMessagePage().notificationBlock().cleanSpamFolderButton())
            .clicksOn(onMessagePage().clearFolderPopUpOld().clearFolderButtonOld());
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }
}
