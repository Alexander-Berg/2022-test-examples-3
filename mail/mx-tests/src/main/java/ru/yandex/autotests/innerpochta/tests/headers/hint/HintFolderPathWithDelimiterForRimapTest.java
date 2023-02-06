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
import java.nio.charset.Charset;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
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
 * <p>
 * Используется для тестрования БАЗ
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование параметра folder_path и folder_path_delim",
        description = "Отправляем письмо с заданными folder_path и folder_path_delim, смотрим, " +
                "что письмо доставлено в нужную папку, и контролируем в логах тип папки и разделитель")
@Title("HintFolderPathWithDelimiterForRimapTest.Тестирование параметра folder_path и folder_path_delim")
@Description("Отправляем письмо с заданными folder_path и folder_path_delim, смотрим, " +
        "что письмо доставлено в нужную папку и контролируем в логах тип папки и разделитель")
@RunWith(Parameterized.class)
public class HintFolderPathWithDelimiterForRimapTest {
    private static final List<String> DELIMITERS = Arrays.asList("/", "%", "@", "|", "^");
    private static String delimiter;
    private Logger log = LogManager.getLogger(this.getClass());
    private String wmiFolderPath;
    private String folderPathWithNewDelimiter;
    private String messageId;
    private String messageSubject;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String folderPath;
    @Parameterized.Parameter(1)
    public String targetFolderPath;
    @Parameterized.Parameter(2)
    public String typeInfo;

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
        data.add(new Object[]{"\\Inbox","\\Inbox", ":type=1;]"});
        data.add(new Object[]{"\\Sent","\\Sent", ":type=2;]"});
        data.add(new Object[]{"\\Trash/vfd/аа4","\\Trash/vfd/аа4", ":path=vfd/аа4;:delimiter=/;:parent_type=3;]"});
        data.add(new Object[]{"\\Spam/dd","\\Spam", ":path=dd;:delimiter=/;:parent_type=4;]"});
        data.add(new Object[]{"\\Drafts/sdf", "\\Drafts/sdf",":path=sdf;:delimiter=/;:parent_type=5;]"});
        data.add(new Object[]{"\\Deleted", "\\Deleted", ":path=\\Deleted;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"greeting","greeting", ":path=greeting;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Inbox/gmail","\\Inbox/gmail", ":path=gmail;:delimiter=/;:parent_type=1;]"});
        data.add(new Object[]{"gmailjet/jetlinersearch","gmailjet/jetlinersearch",
                ":path=gmailjet/jetlinersearch;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{new String("huan/Carlos/I de Borbón/y Borbón-Dos/Sicilias".getBytes(),
                Charset.forName("ASCII")),new String("huan/Carlos/I de Borbón/y Borbón-Dos/Sicilias".getBytes(),
                Charset.forName("ASCII")), "n-Dos/Sicilias;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Inbox","Inbox", ":path=Inbox;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Sent","Sent",  ":path=Sent;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Trash", "Trash",":path=Trash;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Spam", "Spam",":path=Spam;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Drafts", "Drafts",":path=Drafts;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Deleted","Deleted", ":path=Deleted;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Spam/Leonhard/Euler","\\Spam", ":path=Leonhard/Euler;:delimiter=/;:parent_type=4;]"});
        data.add(new Object[]{"", "",""});
        data.add(new Object[]{"Мухаммед/Фарах/Айдид","Мухаммед/Фарах/Айдид", ":path=Мухаммед/Фарах/Айдид;:delimiter=/;:parent_type=0;]"});

        data.add(new Object[]{"ааааааааааааааааааааааааааааааааааааааaaaa",
                "ааааааааааааааааааааааааааааааааааааааaaaa",
                ":path=ааааааааааааааааааааааааааааааааааааааaaaa;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Archive","\\Archive", ":type=7;]"});
        data.add(new Object[]{"Archive","Archive", ":path=Archive;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Отправленные/subfolder", "Отправленные/subfolder",
                ":path=Отправленные/subfolder;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Archive2","Archive2", ":path=Archive2;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Archive/archive", "\\Archive", ":path=archive;:delimiter=/;:parent_type=7;]"});
        return data;
    }

    @BeforeClass
    public static void setReceiverAndSenderAndClearMailboxes() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        inMailbox(receiver).clearAll().deleteAllFolders();
    }

    @Before
    public void sendTestMessage() throws Exception {
        wmiFolderPath = convertXYandexHintStandardFolderPathToWmiFolderPath(targetFolderPath.replace("|", " "));
        //замена replace("|", " ") осуществляется для контроля https://jira.yandex-team.ru/browse/MAILPROTO-1861
        delimiter = DELIMITERS.get(new Random().nextInt(DELIMITERS.size()));
        folderPathWithNewDelimiter = folderPath.replace("/", delimiter);
        messageSubject = format("%s(%s)", randomAlphanumeric(20), folderPath);
        typeInfo = typeInfo.replace("/", delimiter);

        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(messageSubject);
        msg.setHeader(X_YANDEX_HINT, createHintValue().addFolderPath(folderPathWithNewDelimiter)
                .addFolderPathDelim(delimiter).encode());
        msg.saveChanges();
        messageId = sendMessageByNsls(msg);
    }


    @Test
    public void shouldSeeLetterDeliveryToCorrectFolderPath() throws IOException, MessagingException {
        log.info("folder path=" + folderPathWithNewDelimiter + "; delimiter=" + delimiter);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        assertThat("Папка не создалась!", inMailbox(receiver).getFid(wmiFolderPath), not(isEmptyOrNullString()));
        inMailbox(receiver).inFolder(wmiFolderPath).shouldSeeLetterWithSubject(messageSubject);
    }

   // @Test
    public void shouldSeeFolderPathTypeInMailLogDebug() throws IOException, MessagingException {
        log.info("folder path=" + folderPathWithNewDelimiter + "; delimiter=" + delimiter);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        assertThat("Не найден требуемый тип папки!", sessionLog, containsString(typeInfo));
    }
}
