package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.MessageHeaderMatcher.hasHeader;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.PG_FOLDER_OUTBOX;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FOLDER_OUTBOX;

/**
 * User: alex89
 * Date: 06.08.13
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование bcc",
        description = "Отправляем письма с парметрами lid,label в X-Yandex-Hint, " +
                "проверяем, что нужная метка поставилась")
@Title("HintBccTest.Тестирование bcc")
@Description("Отправляем письма с парметрами lid,label в X-Yandex-Hint, проверяем, что нужная метка поставилась")
@RunWith(Parameterized.class)
public class HintBccTest {
    private static String outboxFolderFid;
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;
    private String messageId;
    private static User sender;
    private static User receiver1;
    private static User receiver2;

    @Parameterized.Parameter(0)
    public String bcc;

    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static AccountRule accountRule = new AccountRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"mx-test-user12@ya.ru"});
        data.add(new Object[]{"mx-test-user'@ya.ru"});
        data.add(new Object[]{"\"Friend\" <pleskav+promo@ya.ru>"});
        data.add(new Object[]{"=?utf-8?B?0K/QvdC00LXQutGBLtCf0L7Rh9GC0LA=?= <noreply@yandex.ru>"});
        data.add(new Object[]{"=?utf-8?B?4czFy9PBzsTSIOvP3svB0qPX?= <kochkareff@mail.ru>"});
        data.add(new Object[]{"=?utf-8?B?4czFy9PBzsTSIOvP3svB0qPX?= <" + randomAlphanumeric(100) + "@mail.ru>"});
        data.add(new Object[]{randomAlphanumeric(15)});
        data.add(new Object[]{""});
        return data;
    }

    @BeforeClass
    public static void setReceiversAndSenderAndClearMailboxes() throws Exception {
        List<User> receivers = accountRule.getReceiverUsers();
        sender = accountRule.getSenderUser();
        receiver1 = receivers.get(0);
        receiver2 = receivers.get(1);
        inMailbox(receiver1).clearAll();
        inMailbox(receiver2).clearDefaultFolder();
        outboxFolderFid = inMailbox(receiver1)
                .getFid(mxTestProps().isCorpServer() ? FOLDER_OUTBOX : PG_FOLDER_OUTBOX);
    }

    @Before
    public void prepareTestMessage() throws IOException, MessagingException {
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver1.getEmail());
        msg.setText(bcc);
        msg.setSubject("HintBccTest" + randomAlphanumeric(15));
        msg.setHeader(X_YANDEX_HINT, createHintValue().addBcc(bcc).addFid(outboxFolderFid)
                .addCopyToInbox("1").addEmail(receiver1.getEmail()).encode());
        msg.setRecipient(Message.RecipientType.BCC, new InternetAddress(receiver2.getEmail(), receiver2.getEmail()));
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
    }

    @Test
    public void shouldSeeBccHeaderAddition() throws IOException, MessagingException {
        log.info("Отправили письмо с bcc=" + bcc);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        TestMessage receivedMsgToMainRcptInInbox = inMailbox(receiver1).getMessageWithSubject(msg.getSubject());
        assertThat("BCC должен добавляться у письма во Входящих!",
                receivedMsgToMainRcptInInbox, hasHeader("BCC", equalTo(bcc)));

        TestMessage receivedMsgToMainRcptInOutbox = inMailbox(receiver1)
                .inFolderWithFid(outboxFolderFid).getMessageWithSubject(msg.getSubject());
        assertThat("BCC должен добавляться у письма в Отправленных!",
                receivedMsgToMainRcptInOutbox, hasHeader("BCC", equalTo(bcc)));

        TestMessage receivedMsgToBccInInbox = inMailbox(receiver2).getMessageWithSubject(msg.getSubject());
        assertThat("BCC не должен добавляться у второго получателя!", receivedMsgToBccInInbox, not(hasHeader("BCC")));
    }
}
