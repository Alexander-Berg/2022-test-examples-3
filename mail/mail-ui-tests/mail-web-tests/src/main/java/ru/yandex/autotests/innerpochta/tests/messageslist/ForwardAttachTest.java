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

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Пересылка писем с аттачами")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ForwardAttachTest extends BaseTest {

    private static final String FWD = "Fwd: ";
    private static final String EXPECTED_SUBJECT = "Fwd: (Без темы)";
    private static final String SUBJECT = "with attach";

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
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomString());
        user.apiMessagesSteps()
            .sendMailWithAttachmentsAndHTMLBody(
                lock.firstAcc().getSelfEmail(),
                SUBJECT,
                getRandomString(),
                PDF_ATTACHMENT
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переслать несколько писем с аттачами")
    @TestCaseId("4287")
    public void shouldForwardMessageWithAttach() {
        user.messagesSteps().clicksOnMultipleMessagesCheckBox(1, 0);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton());
        user.composeSteps().shouldSeeSubject(FWD)
            .shouldSeeMessageAsAttachment(1, SUBJECT)
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.messagesSteps().shouldSeeMessageWithSubject(EXPECTED_SUBJECT);
        user.defaultSteps().onMouseHover(onMessagePage().displayedMessages().list().get(0).attachments().list().get(0))
            .shouldSee(onMessagePage().displayedMessages().list().get(0).attachments().list().get(0).emlPreviewBtn());
    }
}
