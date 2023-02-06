package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.MxConstants;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

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
public class HintDraftSormTest {
    private static final String LABEL_BY_FILTER = "отфильтровано";
    private static final String TEXT_FOR_FILTER = "Раздражитель фильтров.";
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private static User receiver1;
    private static User receiver2;

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
    public static void setReceivers() throws Exception {
        List<User> receivers = accountRule.getReceiverUsers();
        receiver1 = receivers.get(0);
        receiver2 = receivers.get(1);
    }

    @Before
    public void prepareTestMessage() throws Exception {
        inMailbox(receiver1).clearAll();

        msg = new TestMessage();
        msg.setSubject("DraftPqTest 1");
        msg.setText("first-draft");
        msg.setHeader(X_YANDEX_HINT, createHintValue().addLabel("symbol:draft_label").addFolder(MxConstants.PG_FOLDER_DRAFT).encode());
        msg.setRecipient(receiver1.getEmail());
        msg.setFrom(receiver1.getEmail());
        msg.saveChanges();
        String serverResponse = sendByNsls(msg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFirstLetter = extractedParamFromLog(MID_LOG_PATTERN, sessionLog);
    }

  //  @Test
    public void testXYandex() throws IOException, MessagingException {
        inMailbox(receiver1).inFolder(MxConstants.PG_FOLDER_DRAFT)
                .shouldSeeLetterWithSubjectAndContent(msg.getSubject(), containsString("first-draft"));
        //  assertThat("stid второго письма должен совпадать сo stid первого [MPROTO-1974]", stidOfSecondLetter,
        //          not(equalTo(stidOfFirstLetter)));
    }

    @Test
    public void testXYandexHintCopyToInboxValidParam() throws IOException, MessagingException {
        msg = new TestMessage(
                new File(this.getClass().getClassLoader().getResource("simple-attach.eml").getFile()));
        msg.setSubject("DraftPqTest 1" );
        msg.setText("second-draft");
        msg.setHeader(X_YANDEX_HINT,
                createHintValue().addLabel("symbol:draft_label").addMid(midOfFirstLetter).addFolder(MxConstants.PG_FOLDER_DRAFT).encode());
        msg.setRecipient(receiver1.getEmail());
        msg.setFrom(receiver1.getEmail());
        msg.saveChanges();
        String serverResponse = sendByNsls(msg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLog(MID_LOG_PATTERN, sessionLog);

        assertThat("Mid второго письма должен совпадать с mid первого [MPROTO-1974]", midOfSecondLetter,
                equalTo(midOfFirstLetter));
        inMailbox(receiver1).inFolder(MxConstants.PG_FOLDER_DRAFT)
                .shouldSeeLetterWithSubjectAndContent(msg.getSubject(), containsString("second-draft"));
    }

}
