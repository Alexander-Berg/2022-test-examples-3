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
import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.*;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 06.08.13
 * <p>
 * * Используется для тестрования БАЗ
 * <p>
 * 17:11:11: если A не внутри входящих то чтобы письмо попало в B надо указать A | B
 * <p>
 * 17:11:33: А folderpath тогда на что?
 * 17:13:32: в folder делимитр нельзя указывать и он папки не создает
 * 17:13:51: он для корневых папок
 * <p>
 * 17:14:14: А вот если два фолдера подряд указать?
 * 17:14:30: неопределенный результат
 * 17:14:38: скорее всего последний будет
 * <p>
 * Выявлены такие приоритеты  fid, folderpath, folder - такие преоритеты.
 * <p>
 * fid  и folderpath вместе могут встречаться?
 * 18:24:47: Вообще не должны.
 * folder_path делался для использования только из RIMAP, чтобы дерево папок создавать в процессе покладки
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование fid,folder",
        description = "Отправляем письма с парметрами fid,folder,folderpath в X-Yandex-Hint, " +
                "проверяем, что письмо доставилось в нужную папку")
@Title("HintFolderTest.Тестирование fid,folder")
@Description("Отправляем письма с парметрами fid,folder,folderpath в X-Yandex-Hint, " +
        "проверяем, что письмо доставилось в нужную папку")
@RunWith(Parameterized.class)
public class  HintFolderTest {
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
        accountRule = new AccountRule().with(HintFolderTest.class);
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();

        Collection<Object[]> data = new LinkedList<Object[]>();
        //folder
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DEFAULT), PG_FOLDER_DEFAULT});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DRAFT), PG_FOLDER_DRAFT});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_SPAM), PG_FOLDER_SPAM});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DELETED), PG_FOLDER_DELETED});
        data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_OUTBOX), PG_FOLDER_OUTBOX});
        data.add(new Object[]{createHintValue().addFolder("ещёНесозданнаяПапка"), PG_FOLDER_DEFAULT});
        data.add(new Object[]{createHintValue().addFid("123456789"), PG_FOLDER_DEFAULT}); //несуществующий fid
        data.add(new Object[]{createHintValue().addFid("папкаВнутриВходящих"), PG_FOLDER_DEFAULT});//некорректный fid
        if (mxTestProps().isCorpServer()) {
            //для тестирования баз и корпа - будем доставать fid-ы папок с помощью WMI.
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DEFAULT)),
                    PG_FOLDER_DEFAULT});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT)),
                    PG_FOLDER_DRAFT});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_SPAM)),
                    PG_FOLDER_SPAM});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DELETED)),
                    PG_FOLDER_DELETED});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_OUTBOX)),
                    PG_FOLDER_OUTBOX});
            //главенство fid
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT))
                    .addFolder(PG_FOLDER_OUTBOX),
                    PG_FOLDER_DRAFT});
        } else {
            data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_DELAYED), PG_FOLDER_DELAYED});
            data.add(new Object[]{createHintValue().addFolder("Archive"), "Archive"});
            data.add(new Object[]{createHintValue().addFolder("одинаковоеНазвание"), "одинаковоеНазвание"});
            data.add(new Object[]{createHintValue().addFolder("одинаковоеНазвание|одинаковоеНазвание"),
                    "одинаковоеНазвание|одинаковоеНазвание"});
            data.add(new Object[]{createHintValue().addFolder("папкаВнутриВходящих"), "папкаВнутриВходящих"});
            data.add(new Object[]{createHintValue().addFolder("папкаВнутриВходящих|папкаВнутриВходящих2"),
                    "папкаВнутриВходящих|папкаВнутриВходящих2"});
            //fid
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DEFAULT)),
                    PG_FOLDER_DEFAULT});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT)),
                    PG_FOLDER_DRAFT});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_SPAM)),
                    PG_FOLDER_SPAM});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DELETED)),
                    PG_FOLDER_DELETED});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_OUTBOX)),
                    PG_FOLDER_OUTBOX});
            //главенство fid
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT))
                    .addFolder(PG_FOLDER_OUTBOX),
                    PG_FOLDER_DRAFT});
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid("Archive")), "Archive"});
            data.add(new Object[]{createHintValue().addFid("11"),
                    "папкаВнутриВходящих"});
            data.add(new Object[]{createHintValue().addFid("10"), "одинаковоеНазвание|одинаковоеНазвание"});
            data.add(new Object[]{createHintValue().addFid("9"), "одинаковоеНазвание"});
            data.add(new Object[]{createHintValue().addFid("12"), "папкаВнутриВходящих|папкаВнутриВходящих2"});

            //fid+folder - главенствует fid
            data.add(new Object[]{createHintValue().addFid(inMailbox(receiver).getFid(PG_FOLDER_DRAFT))
                    .addFolder(PG_FOLDER_OUTBOX), PG_FOLDER_DRAFT});
            data.add(new Object[]{createHintValue().addFolder(PG_FOLDER_OUTBOX)
                    .addFid(inMailbox(receiver).getFid(PG_FOLDER_SPAM)),
                    PG_FOLDER_SPAM});
            //folderpath+fid+folder -главенствует  fid
            data.add(new Object[]{createHintValue().addFid("9")
                    .addFolderPath("\\Archive/subfolder").addFolder(PG_FOLDER_OUTBOX), "одинаковоеНазвание"});
        }
        //folderpath+folder -главенствует  folderpath
        data.add(new Object[]{createHintValue().addFolderPath("\\Archive").addFolder(PG_FOLDER_OUTBOX),
                "archive"});
        return data;
    }

    @BeforeClass
    public static void clearMailbox() {
        inMailbox(receiver).clearAll();
    }

    @Before
    public void sentTestMessage() throws IOException, MessagingException {
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + expectedFolderPath + " " + randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
    }

    @Test
    public void shouldSeeLetterWithXYandexHintFolderParamsDeliveredInTargetFolder()
            throws IOException, MessagingException {
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        inMailbox(receiver).inFolder(expectedFolderPath).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
