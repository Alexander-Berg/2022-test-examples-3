package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
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
import java.util.List;

import static java.lang.String.format;
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
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование email",
        description = "Отправляем письма с парметром email в X-Yandex-Hint, т.е." +
                " указываем адресата для кого этот хинт применяется.Чтобы проверить," +
                " что свойства хинта применяются -записываем в хинт проставление метки.Итого: отслеживаем по метке.")
@Title("HintEmailParamTest.Тестирование email")
@Description("Отправляем письма с парметром email в X-Yandex-Hint, т.е. " +
        "указываем адресата для кого этот хинт применяется.Чтобы проверить, " +
        "что свойства хинта применяются -записываем в хинт проставление метки.Итого: отслеживаем по метке.")
public class HintEmailParamTest {
    private static final String LABEL = "vtnrf0wmmailru";
    private static final String DOMAIN_PREF = "domain_";

    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
    private static User sender;
    private static User receiver1;
    private static User receiver2;
    private static User receiver3;

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
    public static void setReceiverAndSenderAndClearMailboxes() throws Exception {
        List<User> receivers = accountRule.getReceiverUsers();
        sender = accountRule.getSenderUser();
        receiver1 = receivers.get(0);
        receiver2 = receivers.get(1);
        receiver3 = receivers.get(2);
        inMailbox(receiver1).clearAll();
        inMailbox(receiver2).clearAll();
        inMailbox(receiver3).clearAll();
    }

    @Before
    public void prepareTestMessage() throws Exception {
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(receiver1.getEmail()));
        msg.setRecipient(Message.RecipientType.CC, new InternetAddress(receiver2.getEmail()));
        msg.setRecipient(Message.RecipientType.BCC, new InternetAddress(receiver3.getEmail()));
        msg.saveChanges();
    }

    @Test
    public void testXYandexHintEmailSingleParam() throws IOException, MessagingException {
        hintValue = hintValue.addLabel(DOMAIN_PREF+LABEL).addEmail(receiver1.getEmail());
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(format("1 Хинт.Ждем, что метка проставится только в %s %s",
                receiver1.getEmail(), randomAlphanumeric(10)));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver1).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
        inMailbox(receiver2).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
        inMailbox(receiver3).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
    }

    @Test
    public void testXYandexHintTwoEmailParams() throws IOException, MessagingException {
        hintValue = hintValue.addLabel(DOMAIN_PREF+LABEL).addEmail(receiver1.getEmail());
        msg.addHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();
        XYandexHintValue hintValue2 = createHintValue().addLabel(DOMAIN_PREF+LABEL).addEmail(receiver3.getEmail());
        msg.addHeader(X_YANDEX_HINT, hintValue2.encode());
        msg.setSubject(format("2 Хинта. Ждем, что метка проставится в %s и %s %s",
                receiver1.getEmail(), receiver3.getEmail(),randomAlphanumeric(10)));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue + " с X-Yandex-Hint=" + hintValue2);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver1).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
        inMailbox(receiver3).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
        inMailbox(receiver2).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
    }

    @Test
    public void testXYandexHintEmailAlienParam() throws IOException, MessagingException {
        hintValue = hintValue.addLabel(DOMAIN_PREF+LABEL).addEmail("hint-label-test2@ya.ru");
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(format(
                "Проставили в email почту не из множества получателей. Ожидаем, что метки нигде не будет %s",
                randomAlphanumeric(10)));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver1).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
        inMailbox(receiver2).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
        inMailbox(receiver3).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL);
    }

    @Test
    public void testXYandexHintEmailSingleWrongParam() throws IOException, MessagingException {
        hintValue = hintValue.addLabel(DOMAIN_PREF+LABEL).addEmail("");
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(format("Проставили в email не адрес почты. Ожидаем, что параметр проигнорируется. %s",
                randomAlphanumeric(50)));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver1).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
        inMailbox(receiver2).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
        inMailbox(receiver3).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL);
    }
}
