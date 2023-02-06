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

@Aqua.Test
@Title("Прочитанность одного и группы писем")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class MarkUnmarkReadInLiteTest extends BaseTest {

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
    @Title("Делаем прочитанными, непрочитанными и снова прочитанными все сообщения")
    @TestCaseId("39")
    public void testMarkUnmarkAllMsgAsRead() {
        user.liteMailboxSteps().turnsCheckboxCheckAllTo();
        user.liteInterfaceSteps().marksRead();
        user.liteMailboxSteps().shouldSeeUnreadMsgsCountIs(0)
            .turnsCheckboxCheckAllTo();
        user.liteInterfaceSteps().marksUnread();
        user.liteMailboxSteps().shouldSeeThatAllMsgsInPageIsUnread()
            .turnsCheckboxCheckAllTo();
        user.liteInterfaceSteps().marksRead();
        user.liteMailboxSteps().shouldSeeUnreadMsgsCountIs(0);
    }

    @Test
    @Title("Делаем прочитанными, непрочитанными и снова прочитанными одно сообщение")
    @TestCaseId("40")
    public void testMarkUnmarkReadOneMsg() {
        user.liteMailboxSteps().checkMessageWithNumber(1);
        user.liteInterfaceSteps().marksRead();
        user.liteMailboxSteps().shouldSeeThatMsgHaveIconUnread(1, false)
            .checkMessageWithNumber(1);
        user.liteInterfaceSteps().marksUnread();
        user.liteMailboxSteps().shouldSeeThatMsgHaveIconUnread(1, true)
            .checkMessageWithNumber(1);
        user.liteInterfaceSteps().marksRead();
        user.liteMailboxSteps().shouldSeeThatMsgHaveIconUnread(1, false);
    }
}
