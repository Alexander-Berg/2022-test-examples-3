package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
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
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.HANDLER_DO_UNLABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_LID;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;


@Aqua.Test
@Title("Тест на метки")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.LABELS)
public class LabelMessagesFromInboxWithCustomLabelsTest extends BaseTest {

    private String subject;
    private Message msg1;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static ProxyServerRule serverProxyRule =
        proxyServerRule(proxyParamsCheckFilter(HANDLER_DO_LABEL, HANDLER_DO_UNLABEL));

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void logIn() throws IOException {
        subject = Utils.getRandomName();
        msg1 = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Ставим на письмо две метки")
    @TestCaseId("1519")
    public void testLabelMessageWithTwoCustomMarksFromInbox() {
        String labelName = Utils.getRandomString();
        String secondLabelName = Utils.getRandomString();
        Label label1 = user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        Label label2 = user.apiLabelsSteps().addNewLabel(secondLabelName, LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject)
            .markMessageWithCustomLabel(labelName);
        user.defaultSteps().shouldBeParamsInRequest(
            serverProxyRule.parseParams(HANDLER_DO_LABEL),
            of(
                MESSAGES_PARAM_IDS, msg1.getMid(),
                LABELS_PARAM_LID, label1.getLid()
            )
        );
        System.out.println("!-!-!-!");
        user.messagesSteps().markMessageWithCustomLabel(secondLabelName);
        user.defaultSteps().shouldBeParamsInRequest(
            serverProxyRule.parseParams(HANDLER_DO_LABEL),
            of(
                MESSAGES_PARAM_IDS, msg1.getMid(),
                LABELS_PARAM_LID, label2.getLid()
            )
        );
        user.messagesSteps().shouldSeeThatMessageIsLabeledWithMultipleMarks(subject, labelName, secondLabelName);
//        user.leftColumnSteps().shouldSeeCustomLabelCounter(labelName, 1); //TODO: return after DARIA-71227
        user.messagesSteps().unlabelsMessageWithCustomMark()
            .unlabelsMessageWithCustomMark()
            .shouldSeeThatMessageIsNotLabeledWithCustomMark(subject);
        user.leftColumnSteps().shouldNotSeeCustomLabelCounter(labelName);
    }

    @Test
    @Title("Ставим метку на сообщение в треде")
    @TestCaseId("1520")
    public void testLabelMessageInThreadWithNewCustomMarkFromInbox() {
        String labelName = user.apiLabelsSteps().addNewLabel(Utils.getRandomString(), LABELS_PARAM_GREEN_COLOR)
            .getName();
        user.apiMessagesSteps().sendMail(lock.firstAcc(), subject, "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().expandsMessagesThread(subject)
            .selectMessagesInThreadCheckBoxWithNumber(0)
            .markMessageWithCustomLabel(labelName)
            .shouldSeeThatMessageInThreadIsLabeledWith(labelName, 0);
//        user.leftColumnSteps().shouldSeeCustomLabelCounter(labelName, 1); //TODO: return after DARIA-71227
        user.messagesSteps().unlabelsMessageWithCustomMark()
            .shouldSeeThatAllMessagesInThreadIsNotLabeledWithMarks();
        user.leftColumnSteps().shouldNotSeeCustomLabelCounter(labelName);
    }
}
