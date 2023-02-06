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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;

@Aqua.Test
@Title("Отправка писем")
@Description("Тестируем композ")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class ComposeTest extends BaseTest {

    private static final String TEXT = "Some text to test";

    private String to;
    private String subj = Utils.getRandomString();
    private String subjSuffix = Utils.getRandomString();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @Before
    public void logIn() throws InterruptedException {
        to = lock.firstAcc().getSelfEmail();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().openLightMail();
    }

    @Test
    @Title("Отправляем и получаем сообщения")
    @TestCaseId("26")
    public void testSendAndGetAndShowMessage() {
        user.liteInterfaceSteps().entersMail(to, subj, TEXT);
        user.composeLiteSteps().clicksSendButton();
        user.defaultSteps().shouldBeOnUrl(containsString("/lite/inbox?ids=&executed_action=message_send"));
        user.liteMailboxSteps().clicksOnMailWithSubject(subj);
        user.defaultSteps().shouldSee(onMessageLitePage().msgHeadBlock())
            .shouldSee(onMessageLitePage().msgBodyBlock())
            .shouldHasText(onMessageLitePage().subjectHeader(), subj);
        user.liteMailboxSteps().clicksOnDeleteButton();
        user.defaultSteps().shouldBeOnUrl(containsString("executed_action=delete"));
    }

    @Test
    @Title("Сохраняем в черновик")
    @TestCaseId("27")
    public void testSaveToDraft() {
        user.liteInterfaceSteps().entersMail(to, subj, TEXT);
        user.composeLiteSteps().clicksSaveButton();
        user.defaultSteps().shouldBeOnUrl(containsString("/draft"));
        user.liteMailboxSteps().clicksOnDraftsLink()
            .clicksWithoutWaitOnMailWithSubject(subj);
        user.composeLiteSteps().shouldSeeToFieldEquals(to + ", ")
            .shouldSeeSubjectFieldEquals(subj)
            .shouldSeeSendInputEquals(TEXT + "\n");
    }

    @Test
    @Title("Отправляем черновик")
    @TestCaseId("28")
    public void testSendFromDraft() {
        user.liteMailboxSteps().clicksOnDraftsLink()
            .clicksOnMailNumber(0);
        String subject = user.composeLiteSteps().remembersSubjectFromSubjectInputField();
        user.composeLiteSteps().entersTo(lock.firstAcc().getSelfEmail())
            .entersSubject(subjSuffix);
        user.composeLiteSteps().clicksSendButton();
        user.defaultSteps().shouldBeOnUrl(containsString("/lite/inbox?ids=&executed_action=message_send"));
        user.liteMailboxSteps().clicksOnMailWithSubject(subject + subjSuffix);
        user.defaultSteps().shouldHasText(onMessageLitePage().subjectHeader(), subject + subjSuffix);
        user.liteMailboxSteps().clicksOnDeleteButton()
            .clicksOnDraftsLink()
            .clicksOnMailNumber(0);
        user.composeLiteSteps().shouldNotSeeEqualSubjectInInputField(subject + subjSuffix);
    }

    @Test
    @Title("Пересылаем несколько сообщений")
    @TestCaseId("29")
    public void testForwardSomeMails() {
        user.liteMailboxSteps().checkMessageWithNumber(0)
            .checkMessageWithNumber(1)
            .clicksOnForwardButton();
        user.composeLiteSteps().shouldSeeAttachesCheckboxCount(2)
            .clearsSubject()
            .entersSubject(Utils.getRandomString())
            .clicksSaveButton()
            .shouldSeeAttachesCount(2)
            .shouldSeeAttachesCheckboxCount(0);
    }
}
