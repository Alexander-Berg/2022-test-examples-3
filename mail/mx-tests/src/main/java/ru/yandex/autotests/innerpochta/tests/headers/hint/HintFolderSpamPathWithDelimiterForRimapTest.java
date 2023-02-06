package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.headers.HeadersData;
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
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.HintData.convertXYandexHintStandardFolderPathToWmiFolderPath;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: stassiak
 * Date: 03.10.12
 * https://jira.yandex-team.ru/browse/MAILPROTO-971
 * https://jira.yandex-team.ru/browse/MAILPROTO-1303
 * https://st.yandex-team.ru/MPROTO-131
 * <p>
 * Используется для тестрования БАЗ
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование параметра folder_spam_path",
        description = "Отправляем письмо с заданными folder_spam_path, folder_path и folder_path_delim, смотрим, " +
                "что письмо доставлено в нужную папку, и контролируем в логах тип папки и разделитель")
@Title("HintFolderSpamPathWithDelimiterForRimapTest.Тестирование параметра folder_spam_path")
@Description("Отправляем письмо с заданными folder_spam_path, folder_path и folder_path_delim, смотрим, " +
        "что письмо доставлено в нужную папку, и контролируем в логах тип папки и разделитель")
@RunWith(Parameterized.class)
public class HintFolderSpamPathWithDelimiterForRimapTest {
    private static final List<String> DELIMITERS = Arrays.asList("/", "%", "@", "|", "^");
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;
    private String expectedWmiFolderPath;
    private String expectedWmiSpamFolderPath;
    private String folderPathWithNewDelimiter;
    private String folderSpamPathWithNewDelimiter;
    private String messageId;
    private String messageSubject;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String folderPath;
    @Parameterized.Parameter(1)
    public String targetFolderPath;
    @Parameterized.Parameter(2)
    public String folderSpamPath;
    @Parameterized.Parameter(3)
    public String targetFolderSpamPath;
    @Parameterized.Parameter(4)
    public String typeInfo;
    @Parameterized.Parameter(5)
    public String typeSpamInfo;
    @Parameterized.Parameter(6)
    public String delimiter;

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
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"\\Inbox", "\\Inbox", "\\Spam/fSpamPath", "\\Spam",
                ":type=1;]", ":path=fSpamPath;:delimiter=/;:parent_type=4;]", getRandomDelimiter()});
        data.add(new Object[]{"\\Spam/NoSpam", "\\Spam", "\\Sent", "\\Sent",
                ":path=NoSpam;:delimiter=/;:parent_type=4;]", ":type=2;]", getRandomDelimiter()});
        data.add(new Object[]{"\\Trash/vfd/аа4", "\\Trash/vfd/аа4", "\\Trash/fSpam", "\\Trash/fSpam",
                ":path=vfd/аа4;:delimiter=/;:parent_type=3;]", ":path=fSpam;:delimiter=/;:parent_type=3;]",
                getRandomDelimiter()});
        data.add(new Object[]{"\\Spam/dd", "\\Spam", "\\Trash/fSpamPath", "\\Trash/fSpamPath",
                ":path=dd;:delimiter=/;:parent_type=4;]", ":path=fSpamPath;:delimiter=/;:parent_type=3;]",
                getRandomDelimiter()});
        data.add(new Object[]{"\\Drafts/sdf", "\\Drafts/sdf", "\\Drafts/123Spam", "\\Drafts/123Spam",
                ":path=sdf;:delimiter=/;:parent_type=5;]", ":path=123Spam;:delimiter=/;:parent_type=5;]",
                getRandomDelimiter()});
        data.add(new Object[]{"\\Archive", "\\Archive", "Archive", "Archive", ":type=7;]",
                ":path=Archive;:delimiter=/;:parent_type=0;]",
                getRandomDelimiter()});
        data.add(new Object[]{"Archive", "Archive", "Отправленные/subfolder1", "Отправленные/subfolder1",
                ":path=Archive;:delimiter=/;:parent_type=0;]",
                ":path=Отправленные/subfolder1;:delimiter=/;:parent_type=0;]", getRandomDelimiter()});
        data.add(new Object[]{"Archive0", "Archive0", "Archive0", "Archive0",
                ":path=Archive0;:delimiter=/;:parent_type=0;]", ":path=Archive0;:delimiter=/;:parent_type=0;]",
                getRandomDelimiter()});
        data.add(new Object[]{"Archive0", "Archive0", "\\Archive/archive", "\\Archive",
                ":path=Archive0;:delimiter=/;:parent_type=0;]", ":path=archive;:delimiter=/;:parent_type=7;]",
                getRandomDelimiter()});
        return data;
    }

    private static String getRandomDelimiter() {
        return DELIMITERS.get(new Random().nextInt(DELIMITERS.size()));
    }

    @BeforeClass
    public static void setReceiverAndSenderAndClearMailboxes() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        inMailbox(receiver).clearAll().deleteAllFolders();
    }

    @Before
    public void sendTestMessage() throws Exception {
        expectedWmiFolderPath =
                convertXYandexHintStandardFolderPathToWmiFolderPath(targetFolderPath.replace("|", " "));
        expectedWmiSpamFolderPath =
                convertXYandexHintStandardFolderPathToWmiFolderPath(targetFolderSpamPath.replace("|", " "));
        //замена replace("|", " ") осуществляется для контроля https://jira.yandex-team.ru/browse/MAILPROTO-1861
        folderPathWithNewDelimiter = folderPath.replace("/", delimiter);
        folderSpamPathWithNewDelimiter = folderSpamPath.replace("/", delimiter);
        messageSubject = format("%sfPath(%s), fSpamPath(%s)", randomAlphanumeric(20), folderPath, folderSpamPath);
        typeInfo = typeInfo.replace("/", delimiter);
        typeSpamInfo = typeSpamInfo.replace("/", delimiter);

        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(messageSubject);
        msg.setHeader(X_YANDEX_HINT, createHintValue().addFolderPath(folderPathWithNewDelimiter)
                .addFolderSpamPath(folderSpamPathWithNewDelimiter)
                .addFolderPathDelim(delimiter).encode());
        msg.saveChanges();
    }

    @Test
    public void shouldSeeLetterDeliveryToCorrectFolderPath() throws IOException, MessagingException {
        log.info("Проверяем, что для НЕспамовых писем учитывается параметр folder_path");
        messageId = sendMessageByNsls(msg);
        log.info("folder path=" + folderPathWithNewDelimiter + "; delimiter=" + delimiter);
        assertThat("Папка не создалась!", inMailbox(receiver).getFid(expectedWmiFolderPath), not(isEmptyOrNullString()));
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        inMailbox(receiver).inFolder(expectedWmiFolderPath).shouldSeeLetterWithSubject(messageSubject);
    }

    @Test
    public void shouldSeeSpamLetterDeliveryToCorrectFolderSpamPath() throws IOException, MessagingException {
        log.info("Проверяем, что для спамовых писем учитывается параметр folder_spam_path");
        msg.addHeader(HeadersData.HeaderNames.X_YANDEX_SPAM.getName(), "4");
        msg.saveChanges();
        messageId = sendMessageByNsls(msg);
        log.info("folder spam path=" + folderSpamPathWithNewDelimiter + "; delimiter=" + delimiter);
        assertThat("Папка не создалась!", inMailbox(receiver).getFid(expectedWmiSpamFolderPath), not(isEmptyOrNullString()));
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        inMailbox(receiver).inFolder(expectedWmiSpamFolderPath).shouldSeeLetterWithSubject(messageSubject);
    }

    //  @Test
    public void shouldSeeFolderPathTypeInMailLogDebug() throws IOException, MessagingException {
        messageId = sendMessageByNsls(msg);
        log.info("folder path=" + folderPathWithNewDelimiter + "; delimiter=" + delimiter);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        assertThat("Не найден требуемый тип папки!", sessionLog, containsString(typeInfo));
    }

    // @Test
    public void shouldSeeFolderSpamPathTypeInMailLogDebug() throws IOException, MessagingException {
        msg.addHeader(HeadersData.HeaderNames.X_YANDEX_SPAM.getName(), "4");
        msg.saveChanges();
        messageId = sendMessageByNsls(msg);
        log.info("folder path=" + folderPathWithNewDelimiter + "; delimiter=" + delimiter);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        assertThat("Не найден требуемый тип папки!", sessionLog, containsString(typeSpamInfo));
    }
}
