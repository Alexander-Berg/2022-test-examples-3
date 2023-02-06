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

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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
 * <p>
 * Используется для тестрования БАЗ
 * <p>
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование imap",
        description = "Проверяем, что при выставлении imap параметра, письма доставляются")
@Title("HintImapParamTest.Тестирование imap")
@Description("Проверяем, что при выставлении imap параметра, письма доставляются")
@RunWith(Parameterized.class)
public class HintImapParamTest {
    private static final List<String> IMAP_PARAMS = asList("-1", "1", "3");
    private static final List<String> NO_IMAP_PARAMS = asList("0", "aaaaa");
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
    private String messageId;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String imapParamValue;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        List<String> allTestCases = new ArrayList<String>();
        allTestCases.addAll(IMAP_PARAMS);
        allTestCases.addAll(NO_IMAP_PARAMS);
        for (String value : allTestCases)
            data.add(new Object[]{value});
        return data;
    }

    @BeforeClass
    public static void setReceiverAndSenderAndClearMailboxes() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        inMailbox(receiver).clearAll();
    }

    @Before
    public void prepareTestMessage() throws IOException, MessagingException {
        hintValue = createHintValue().addImap(imapParamValue);
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.setText(randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();
    }

    @Test //todo доделать бы.
    public void shouldSeeThatLetterWithXYandexHintImapParamWasSent() throws IOException, MessagingException {
        log.info("Отправляем письмо с X-Yandex-Hint=" + hintValue);
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
