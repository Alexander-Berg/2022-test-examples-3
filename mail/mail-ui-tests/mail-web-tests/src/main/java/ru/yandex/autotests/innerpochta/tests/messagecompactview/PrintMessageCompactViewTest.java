package ru.yandex.autotests.innerpochta.tests.messagecompactview;

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

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PRINT_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Печать в просмотре письма из компактного режима")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.COMPACT_VIEW)
public class PrintMessageCompactViewTest extends BaseTest {

    private static final String SINGLE_MESSAGE_SUBJECT = "PRINT_MSG";
    private static final String THREAD_MESSAGE_SUBJECT = "PRINT_GROUP_MSG";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            SINGLE_MESSAGE_SUBJECT,
            getRandomString(),
            PDF_ATTACHMENT
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), THREAD_MESSAGE_SUBJECT, "");
        user.apiMessagesSteps()
            .sendMailWithAttachmentsToThreadWithSubject(lock.firstAcc().getSelfEmail(), THREAD_MESSAGE_SUBJECT,
                getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Печать одиночного письма")
    @TestCaseId("927")
    public void shouldPrintMessage() {
        String id = user.apiMessagesSteps().getMessageWithSubject(SINGLE_MESSAGE_SUBJECT).getMid();
        user.messagesSteps().clicksOnMessageWithSubject(SINGLE_MESSAGE_SUBJECT);
        user.defaultSteps().clicksOn(
            onMessageView().contentToolbarBlock().moreBtn(),
            onMessageView().miscField().printBtn()
        )
            .switchOnWindow(1)
            .shouldBeOnUrl(both(containsString(id)).and(containsString(PRINT_URL)));
    }

    @Test
    @Title("Печать группы писем")
    @TestCaseId("927")
    public void shouldPrintMessageGroup() {
        String tid = user.apiMessagesSteps().getMessageWithSubject(THREAD_MESSAGE_SUBJECT).getTid();
        user.messagesSteps().clicksOnMessageWithSubject(THREAD_MESSAGE_SUBJECT);
        user.defaultSteps().clicksOn(
            onMessageView().messageSubject().threadToolbarButton(),
            onMessageView().commonToolbar().printButton()
        )
            .switchOnWindow(1)
            .shouldBeOnUrl(both(containsString(tid)).and(containsString(PRINT_URL)));
    }
}
