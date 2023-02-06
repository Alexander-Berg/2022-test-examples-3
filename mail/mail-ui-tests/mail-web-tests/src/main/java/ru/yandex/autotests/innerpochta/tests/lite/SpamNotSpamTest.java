package ru.yandex.autotests.innerpochta.tests.lite;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;

@Aqua.Test
@Title("Отправляем письма в спам")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class SpamNotSpamTest extends BaseTest {

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @Before
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().openLightMail();
    }

    @Test
    @Title("Делаем спамом и не спамом все сообщения")
    @TestCaseId("41")
    public void testSpamAllMessages() {
        user.liteMailboxSteps().turnsCheckboxCheckAllTo();
        Integer mailCount = user.liteMailboxSteps().remembersMailCount();
        user.liteMailboxSteps().clicksOnToSpamButton();
        user.defaultSteps().shouldBeOnUrl(containsString("lite/inbox?executed_action=tospam&count=" + mailCount));
        user.liteMailboxSteps().clicksOnSpamLink()
            .shouldSeeMailCountIs(mailCount)
            .turnsCheckboxCheckAllTo()
            .clicksOnNotSpamButton();
        user.defaultSteps().shouldBeOnUrl(containsString("/lite/spam?executed_action=notspam&count=" + mailCount));
        user.liteMailboxSteps().shouldSeeMailCountIs(0);
    }

    @Test
    @Title("Делаем спамом и не спамом одно сообщение")
    @TestCaseId("42")
    public void testSpamNoSpamOneMsg() {
        user.liteMailboxSteps().clicksOnMailNumber(1)
                .clicksOnToSpamButton();
        user.defaultSteps().shouldBeOnUrl(containsString("executed_action=tospam"));
        user.liteMailboxSteps().clicksOnSpamLink()
            .shouldSeeMailCountIs(1)
            .clicksOnMailNumber(0)
            .clicksOnNotSpamButton();
        user.defaultSteps().shouldBeOnUrl(containsString("executed_action=notspam"));
        user.liteMailboxSteps().shouldSeeMailCountIs(0);
    }
}
