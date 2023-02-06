package ru.yandex.autotests.innerpochta.tests.messagefullview;

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

import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Аттачи в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.ATTACHES)
public class MessageviewAttachesTest extends BaseTest {

    private static final String MSG_WITH_DOC = "Письмо с документом";
    private static final String DV_URL = "https://docs.yandex.ru";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            MSG_WITH_DOC,
            getRandomString(),
            WORD_ATTACHMENT
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Просмотр файла через DV из тела письма")
    @TestCaseId("1987")
    public void shouldOpenAttachmentInNewTab() {
        user.messagesSteps().clicksOnMessageWithSubject(MSG_WITH_DOC);
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(0))
            .clicksOn(onMessageView().attachments().list().get(0).show())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DV_URL));
    }
}
