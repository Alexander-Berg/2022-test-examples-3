package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.ProxyParamsCheckFilter.proxyParamsCheckFilter;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.HANDLER_DO_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

@Aqua.Test
@Title("Тест на метку “Важное“")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.LABELS)
public class LabelMessagesImportantFromInboxTest extends BaseTest {

    private static final String THREAD_SUBJECT = "thread";
    private static final int THREAD_COUNT = 4;
    private static final String SUBJECT_N1 = "subject1";
    private static final String SUBJECT_N2 = "subject5";
    private static final String MSG_PER_PAGE = "2";
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(proxyParamsCheckFilter(HANDLER_DO_LABEL));

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void logIn() throws IOException {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SUBJECT_N1, "");
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SUBJECT_N2, "");
        user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD_SUBJECT, THREAD_COUNT);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-horizontal",
            of(SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Ставим метку «Важное» на целый тред")
    @TestCaseId("1533")
    public void testLabelThreadImportant() {
        user.messagesSteps().selectMessageWithSubject(THREAD_SUBJECT)
            .labelsMessageImportantFromDropDownMenu()
            .expandsMessagesThread(THREAD_SUBJECT)
            .shouldSeeThatMessagesInThreadAreImportant(0, 1, 2, 3);
    }

    @Test
    @Title("Ставим метку «Важное» на одно сообщение в треде")
    @TestCaseId("1534")
    public void testLabelOneMessageInThreadImportant() {
        user.messagesSteps().expandsMessagesThread(THREAD_SUBJECT)
            .selectMessagesInThreadCheckBoxWithNumber(0)
            .labelsMessageImportantFromDropDownMenu();
        String threadMsgMid = user.messagesSteps().getMidFromThreadByIndex(0);
        user.defaultSteps().shouldBeParamsInRequest(
            serverProxyRule.parseParams(HANDLER_DO_LABEL), of(MESSAGES_PARAM_IDS, threadMsgMid)
        );
    }

    @Test
    @Title("Ставим метку «Важное» на сообщения с разных страниц")
    @TestCaseId("1535")
    public void testLabelMessagesInDifferentPagesImportantFromInbox() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_MESSAGES_PER_PAGE, MSG_PER_PAGE));
        user.defaultSteps().refreshPage();
        user.messagesSteps().loadsMoreMessages()
            .selectMessageWithSubject(SUBJECT_N1, SUBJECT_N2)
            .labelsMessageImportantFromDropDownMenu()
            .shouldSeeThatMessageIsImportant(SUBJECT_N1, SUBJECT_N2)
            .unlabelsMessageImportantFromDropDownMenu();
    }
}
