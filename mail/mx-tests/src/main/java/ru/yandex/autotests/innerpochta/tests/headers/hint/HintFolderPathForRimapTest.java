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
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.X_YANDEX_UNIQ;
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
 * "test-box2@yandex.ru", "12345678"
 * <p>
 * Используется для тестрования БАЗ
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование параметра folder_path",
        description = "Отправляем письмо с заданным folder_path, смотрим, " +
                "что письмо доставлено в нужную папку, и контролируем в логах тип папки")
@Title("HintFolderPathForRimapTest.Тестирование параметра folder_path")
@Description("Отправляем письмо с заданным folder_path, смотрим, " +
        "что письмо доставлено в нужную папку, и контролируем в логах тип папки")
@RunWith(Parameterized.class)
public class HintFolderPathForRimapTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private String wmiFolderPath;
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
        String randomFolder = randomAlphanumeric(10);
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"\\Inbox/v№fd?/" + randomFolder, "\\Inbox/v№fd?/" + randomFolder,
                format(":path=v№fd?/%s;:delimiter=/;:parent_type=1;]", randomFolder)});
        data.add(new Object[]{"\\Inbox","\\Inbox", ":type=1;]"});
        data.add(new Object[]{"\\Sent","\\Sent", ":type=2;]"});
        data.add(new Object[]{"\\Sent/1fold","\\Sent/1fold", ":type=2;]"});
        data.add(new Object[]{"\\Trash/vf!&$#d/аа4","\\Trash/vf!&$#d/аа4", ":path=vf!&$#d/аа4;:delimiter=/;:parent_type=3;]"});
        data.add(new Object[]{"\\Spam/dd","\\Spam", ":path=dd;:delimiter=/;:parent_type=4;]"});
        data.add(new Object[]{"\\Drafts/sd@f","\\Drafts/sd@f", ":path=sd@f;:delimiter=/;:parent_type=5;]"});
        data.add(new Object[]{"\\Deleted","\\Deleted", ":path=\\Deleted;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"#greeting#","#greeting#", ":path=#greeting#;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"*greeting\\+","*greeting\\+", ":path=*greeting\\+;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"$50%","$50%", ":path=$50%;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Inbox/gmail","\\Inbox/gmail", ":path=gmail;:delimiter=/;:parent_type=1;]"});
        data.add(new Object[]{"jetlinersearch","jetlinersearch",
                ":path=jetlinersearch;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"gmailjet/jet<l>i{ner},sea:rch","gmailjet/jet<l>i{ner},sea:rch",
                ":path=gmailjet/jet<l>i{ner},sea:rch;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{new String("huan/Carlos/I de Borbón/y Borbón-Dos/Sicilias".getBytes(),
                Charset.forName("ASCII")),new String("huan/Carlos/I de Borbón/y Borbón-Dos/Sicilias".getBytes(),
                Charset.forName("ASCII")), "n-Dos/Sicilias;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Inbox","Inbox", ":path=Inbox;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Sent","Sent", ":path=Sent;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Trash","Trash", ":path=Trash;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Spam","Spam", ":path=Spam;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Drafts","Drafts", ":path=Drafts;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Deleted","Deleted", ":path=Deleted;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Spam/Leonhard/Euler","\\Spam", ":path=Leonhard/Euler;:delimiter=/;:parent_type=4;]"});
        data.add(new Object[]{"","", ""});
        data.add(new Object[]{"Мухаммед/Фарах/Айдид", "Мухаммед/Фарах/Айдид",
                ":path=Мухаммед/Фарах/Айдид;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"ааааааааааааааааааааааааааааааааааааааaaaa","ааааааааааааааааааааааааааааааааааааааaaaa",
                ":path=ааааааааааааааааааааааааааааааааааааааaaaa;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Archive","\\Archive", ":type=7;]"});
        data.add(new Object[]{"Archive","Archive", ":path=Archive;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Отправленные/[sub]fo=lder","Отправленные/[sub]fo=lder",
                ":path=Отправленные/[sub]fo=lder;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"Archive2","Archive2", ":path=Archive2;:delimiter=/;:parent_type=0;]"});
        data.add(new Object[]{"\\Archive/archive","\\Archive", ":path=archive;:delimiter=/;:parent_type=7;]"});
      //  data.add(new Object[]{"Sup|Folder/SuFolderr", ":path=Sup Folder/SuFolderr;:delimiter=/;:parent_type=0;]"});
      //  data.add(new Object[]{"Super||Folder/SuFolder", ":path=Super  Folder/SuFolder;:delimiter=/;:parent_type=0;]"});
      //  data.add(new Object[]{"New Folder/SuFolder", ":path=New Folder/SuFolder;:delimiter=/;:parent_type=0;]"});
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
        messageSubject = format("%s(%s)", randomAlphanumeric(20), folderPath);
        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(messageSubject);
        msg.setHeader(X_YANDEX_HINT, createHintValue().addFolderPath(folderPath).encode());
        msg.setHeader(X_YANDEX_UNIQ.getName(), randomAlphanumeric(50));//т.к. у корп. полльзователя включен трединг
        msg.saveChanges();
        messageId = sendMessageByNsls(msg);
    }

    @Test
    public void shouldSeeLetterDeliveryToCorrectFolderPath() throws IOException, MessagingException {
        log.info(folderPath);

        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);

        assertThat("Папка не создалась!", inMailbox(receiver).getFid(wmiFolderPath), not(isEmptyOrNullString()));
        inMailbox(receiver).inFolder(wmiFolderPath).shouldSeeLetterWithSubject(messageSubject);
    }

    //@Test
    public void shouldSeeFolderPathTypeInMailLogDebug() throws IOException, MessagingException {
        log.info(folderPath);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
      //  assertThat("Не найден требуемый тип папки!", sessionLog, containsString(typeInfo));
    }
}
