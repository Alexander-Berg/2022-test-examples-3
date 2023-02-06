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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Пометка и снятие метки с писем")
@Description("Проставляем и снимаем метку с одиночных писем и с группы писем")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class MarkUnmarkLabelsInLiteTest extends BaseTest {

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
    @Title("Проставляем и снимаем метку с одного письма")
    @TestCaseId("36")
    public void testMarkUnmarkFirstMsgAsHightPriority() {
        user.liteMailboxSteps().checkMessageWithNumber(0);
        user.liteInterfaceSteps().markMsgWithLabel(1);
        user.liteMailboxSteps().shouldSeeMailNumberAsImportant(0, true)
            .checkMessageWithNumber(0);
        user.liteInterfaceSteps().unmarkMsgWithFromLabel(1);
        user.liteMailboxSteps().shouldSeeMailNumberAsImportant(0, false);
    }

    @Test
    @Title("Проставляем и снимаем метку с группы")
    @TestCaseId("37")
    public void testMarkUnmarkAllMsgAsHightPriority() {
        user.liteMailboxSteps().turnsCheckboxCheckAllTo();
        user.liteInterfaceSteps().markMsgWithLabel(1);
        user.liteMailboxSteps().shouldSeeThatAllMessagesIsImportant()
            .turnsCheckboxCheckAllTo();
        user.liteInterfaceSteps().unmarkMsgWithFromLabel(1);
        user.liteMailboxSteps().shouldSeeImportantFlagsCountIs(0);

    }

    @Test
    @Title("Проставляем и снимаем метку при просмотре письма")
    @TestCaseId("38")
    public void testMarkUnmarkMsgAsHightPriorityFromViewingMsg() {
        user.liteMailboxSteps().clicksOnMailNumber(0);
        user.defaultSteps().shouldNotSee(onMessageLitePage().hightPriorityFlag());
        user.liteInterfaceSteps().markMsgWithLabel(1);
        user.defaultSteps().shouldSee(onMessageLitePage().hightPriorityFlag());
        user.liteInterfaceSteps().unmarkMsgWithFromLabel(1);
        user.defaultSteps().shouldNotSee(onMessageLitePage().hightPriorityFlag());
    }
}
