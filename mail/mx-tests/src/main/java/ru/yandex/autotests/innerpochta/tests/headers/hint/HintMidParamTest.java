package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FOLDER_DRAFT;

/**
 * User: alex89
 * Date: 06.08.13
 *  * todo доделать
 */
//@Stories("FASTSRV")
//@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
//@Aqua.Test(title = "Тестирование copy_to_inbox",
//        description = "Проверяем, что при выставлении copy_to_inbox=1 письмо кладется во входящие и не минует фильтры")
//@Title("HintCopyToInboxParamTest.Тестирование copy_to_inbox")
//@Description("Проверяем, что при выставлении copy_to_inbox=1 письмо кладется во входящие и не минует фильтры")
public class HintMidParamTest {
    private static final String LABEL_BY_FILTER = "отфильтровано";
    private static final String TEXT_FOR_FILTER = "Раздражитель фильтров.";
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private static User receiver;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessage() throws Exception {
        msg = new TestMessage(
                new File(this.getClass().getClassLoader().getResource("simple-attach.eml").getFile()));
        msg.setSubject("DraftPqTest ");
        msg.setText("first-draft");
        msg.setHeader(X_YANDEX_HINT, createHintValue().addLabel("symbol:draft_label").addFolder(FOLDER_DRAFT).encode());
        msg.setRecipient(receiver.getEmail());
        msg.setFrom(receiver.getEmail());
        msg.saveChanges();
        String serverResponse = sendByNsls(msg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFirstLetter = extractedParamFromLog(MID_LOG_PATTERN, sessionLog);

    }

    @Test
    public void testXYandexHintCopyToInboxValidParam() throws IOException, MessagingException {
        msg = new TestMessage(
                new File(this.getClass().getClassLoader().getResource("simple-attach.eml").getFile()));
        msg.setSubject("DraftPqTest " );
        msg.setText("second-draft");
        msg.setHeader(X_YANDEX_HINT,
                createHintValue().addLabel("symbol:draft_label").addMid(midOfFirstLetter).addFolder(FOLDER_DRAFT).encode());
        msg.setRecipient(receiver.getEmail());
        msg.setFrom(receiver.getEmail());
        msg.saveChanges();
        String serverResponse = sendByNsls(msg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLog(MID_LOG_PATTERN, sessionLog);

        assertThat("Mid второго письма должен совпадать с mid первого [MPROTO-1974]", midOfSecondLetter,
                equalTo(midOfFirstLetter));
    }

}
