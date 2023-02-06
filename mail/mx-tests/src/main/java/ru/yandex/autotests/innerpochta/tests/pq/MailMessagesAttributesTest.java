package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessagesPqTable.mailMessagesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.tests.pq.PqTestMsgs.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;

/**
 * User: alex89
 * Date: 05.05.15
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи верных attributes в таблицу mail.messages постгреса",
        description = "Проверяем на различных письмах,что верно записали attributes в таблицу mail.messages постгреса")
@Title("MailMessagesAttributesTest. Поверка записи верных attributes в таблицу mail.messages постгреса")
@Description("Проверяем на различных письмах,что верно записали attributes в таблицу mail.messages постгреса")
@RunWith(Parameterized.class)
public class MailMessagesAttributesTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private String sessionLog;
    private String mid;
    private String expectedStid;

    @Parameterized.Parameter(0)
    public TestMessage testMsg;
    @Parameterized.Parameter(1)
    public String expectedAttributes;
    @Parameterized.Parameter(2)
    public String caseComment;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();

        data.add(new Object[]{getMixedPostmasterMsg(), "{postmaster}", "c mixed=16[MPROTO-1770]"});
        data.add(new Object[]{getSpamMsg(), "{spam}", " со спамом (X-Yandex-Spam=4)"});
        data.add(new Object[]{getMixedSpamMsg(), "{spam}", "c mixed=4[MPROTO-1770]"});
        data.add(new Object[]{getAppendMsg(), "{append}", "c mixed=65536" +
                " (как будто доставленного в результате выполнения операции imap append)[MPROTO-1770]"});
        data.add(new Object[]{getRealAppendMsg(), "{append}", "полученного в результате настоящего imap append"});
        data.add(new Object[]{getSharedMsg(), "{mulca-shared}", "расшаренного c TO,BCC[MPROTO-1662],[MPROTO-1770]"});
        data.add(new Object[]{getSharedMsg2(), "{mulca-shared}", "расшаренного c 2 TO[MPROTO-1662],[MPROTO-1770]"});
        data.add(new Object[]{getSharedMsg3(), "{mulca-shared}", "расшаренного c TO, CC[MPROTO-1662],[MPROTO-1770]"});
        data.add(new Object[]{getSharedMsg4(), "{mulca-shared}", "расшаренного c BCC,CC[MPROTO-1662],[MPROTO-1770]"});
        data.add(new Object[]{getMixedSmsMsg(), "{}", "c mixed=8192[MPROTO-1770]"});
        return data;
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        log.info(format("Отправили письмо %s с темой %s", testMsg.getSubject(), caseComment));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
//        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
//                containsString("message stored in db=pg backed")));
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        expectedStid = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);
    }


    @Test
    public void shouldSeeAttributesStidAttachesFirstlineSubjectInMailMessagesPqTable() throws MessagingException {
        assertThat(format("Записались неверные данные в таблицу mail.messages для письма %s", caseComment),
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasAttributes(equalTo(expectedAttributes)),
                        hasStid(equalTo(expectedStid)),
                        hasAttaches(equalTo("{}")),
                        hasFirstLine(equalTo(FIRSTLINE)),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));
    }
}
