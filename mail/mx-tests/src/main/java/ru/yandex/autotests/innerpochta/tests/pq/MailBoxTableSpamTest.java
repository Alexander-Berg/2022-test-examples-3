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

import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.hasFid;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable.mailBoxTableInfoFromPq;
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
@Aqua.Test(title = "Поверка записи верных fid в таблицу mail.box постгреса при отправке спамовых и неспамовых писем",
        description = "Проверяем на различных письмах,что верно записали fid в таблицу mail.box постгреса")
@Title("MailBoxTableSpamTest. Поверка записи верных fid в таблицу mail.box постгреса при отправке спамовых " +
        "и неспамовых писем")
@Description("Проверяем на различных письмах,что верно записали fid в таблицу mail.box постгреса")
@RunWith(Parameterized.class)
public class MailBoxTableSpamTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private String sessionLog;
    private String mid;

    @Parameterized.Parameter(0)
    public TestMessage testMsg;
    @Parameterized.Parameter(1)
    public String expectedFid;
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

        data.add(new Object[]{getMixedPostmasterMsg(), DEFAULT_FOLDER_FID, "c mixed=16"});
        data.add(new Object[]{getRealPostmasterMsg(), DEFAULT_FOLDER_FID,
                "c постмастеровым получателем"});
        data.add(new Object[]{getSpamMsg(), SPAM_FOLDER_FID, "со спамом (X-Yandex-Spam=4)[MPROTO-1867]"});
        data.add(new Object[]{getMixedSpamMsg(), DEFAULT_FOLDER_FID, "c mixed=4[MPROTO-1770]"});
        data.add(new Object[]{getRealPostmasterSpamMsg(), SPAM_FOLDER_FID,
                "c постмастеровым получателем, спамом (X-Yandex-Spam=4)[MPROTO-1867]"});
        data.add(new Object[]{getAppendMsg(), DEFAULT_FOLDER_FID, "c mixed=65536" +
                " (как будто доставленного в результате выполнения операции imap append)"});
        data.add(new Object[]{getSharedMsg(), DEFAULT_FOLDER_FID, "расшаренного c TO,BCC[MPROTO-1662]"});
        data.add(new Object[]{getSharedMsg2(), DEFAULT_FOLDER_FID, "расшаренного c 2 TO[MPROTO-1662]"});
        data.add(new Object[]{getSharedMsg3(), DEFAULT_FOLDER_FID, "расшаренного c TO, CC[MPROTO-1662]"});
        data.add(new Object[]{getSharedMsg4(), DEFAULT_FOLDER_FID, "расшаренного c BCC,CC[MPROTO-1662]"});
        data.add(new Object[]{getMixedSmsMsg(), DEFAULT_FOLDER_FID, "c mixed=8192"});
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
    }

    @Test
    public void shouldSeeCorrectFidFieldsInMailBoxPqTable() {
        assertThat(format("Записали неверный fid в таблицу mail.box при отправке письма %s", caseComment),
                mailBoxTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                hasFid(equalTo(expectedFid)));
    }
}
