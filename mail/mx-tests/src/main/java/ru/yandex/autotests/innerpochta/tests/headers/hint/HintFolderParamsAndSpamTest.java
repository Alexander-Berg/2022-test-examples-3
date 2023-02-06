package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
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
import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.X_YANDEX_SPAM;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.*;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 24.06.2015
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование fid,folder,folderpath  для спамовых писем",
        description = "Отправляем письма с парметрами fid,folder,folderpath в X-Yandex-Hint и X-Yandex-Spam=4, " +
                "проверяем, что письмо доставилось в нужную папку")
@Title("HintFolderParamsAndSpamTest.Тестирование fid,folder,folderpath  для спамовых писем")
@Description("Отправляем письма с парметрами fid,folder,folderpath в X-Yandex-Hint и X-Yandex-Spam=4, " +
        "проверяем, что письмо доставилось в нужную папку")
@RunWith(Parameterized.class)
public class HintFolderParamsAndSpamTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;
    private String messageId;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public String expectedFolderPath;

    @ClassRule
    public static AccountRule accountRule;
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        accountRule = new AccountRule().with(HintFolderParamsAndSpamTest.class);
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();

        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DEFAULT), PG_FOLDER_DEFAULT});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DRAFT), PG_FOLDER_DRAFT});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_SPAM), PG_FOLDER_SPAM});
        data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT)), PG_FOLDER_DRAFT});
        data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_SPAM)), PG_FOLDER_SPAM});
        data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DELETED)), PG_FOLDER_DELETED});
        data.add(new Object[]{createHintValue().addFolderPath("\\Archive/subfolder"), PG_FOLDER_SPAM});

        return data;
    }

    @BeforeClass
    public static void clearMailboxes() throws Exception {
        inMailbox(receiver).clearAll();
    }

    @Before
    public void sentTestMessage() throws IOException, MessagingException {
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + expectedFolderPath + " " + randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.saveChanges();
        log.info("Отправили СПАМОВОЕ письмо с X-Yandex-Hint=" + hintValue);
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
    }

    @Test
    public void shouldSeeSpmLetterInCorrectFolderIfXYandexHintFolderParamsAreUsed()
            throws IOException, MessagingException {
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        inMailbox(receiver).inFolder(expectedFolderPath).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
