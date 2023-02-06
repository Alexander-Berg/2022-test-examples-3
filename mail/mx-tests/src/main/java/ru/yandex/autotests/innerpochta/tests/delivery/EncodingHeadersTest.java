package ru.yandex.autotests.innerpochta.tests.delivery;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.RetryRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

@Stories("NWSMTP")
@Feature("Базовые проверки доставки")
@Aqua.Test(title = "Заголовки в различных кодировках",
        description = "Проверяем отправку сообщений с заголовками From, To, CC, BCC в различных кодировках")
@Title("EncodingHeadersTest.Заголовки в различных кодировках")
@Description("Проверяем отправку сообщений с заголовками From, To, CC, BCC в различных кодировках.")
@RunWith(Parameterized.class)
public class EncodingHeadersTest {
    private static final String FROM = "Лориэрик aka Ленин, Октябрьская Революция," +
            "Индустриализация, Электрификация, Радиофикация и Коммунизм";
    private static final String TO = "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios " +
            "Cipriano de la Santísima Trinidad Mártir Patricio Ruiz y Picasso ";
    private static final String CC = "Atilla Yayla (d. 3 Mart 1957, Kırşehir ) Türk siyaset bilimci, akademisyen";
    private static final String BCC = "Sébastien Goutal Prima";
    private static final String[] ENC_METHODS = {"B", "b", "Q", "q"};
    private static final String DOMEN_PART = "aWKab";

    private static int i = 0;
    private final Logger log = LogManager.getLogger(this.getClass());
    private final static String server = mxTestProps().getMxServer();
    private final static Integer port = mxTestProps().getMxPort();
    private String to;
    private String cc;
    private String bcc;
    private String domain;

    @Parameterized.Parameter(0)
    public String encodingType;
    @Parameterized.Parameter(1)
    public String encodingMethod;

    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);
    @ClassRule
    public static AccountRule accountRule = new AccountRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String method : ENC_METHODS) {
            data.add(new Object[]{"utf-8", method});
            data.add(new Object[]{"UTF-16", method});
            data.add(new Object[]{"KOI8-R", method});
            data.add(new Object[]{"koi8-r", method});
            data.add(new Object[]{"cp1251", method});
            data.add(new Object[]{"Cp1250", method});
            data.add(new Object[]{"windows-1256", method});
            data.add(new Object[]{"MacCyrillic", method});
            data.add(new Object[]{"maccyrillic", method});
            data.add(new Object[]{"cp855", method});
            data.add(new Object[]{"Cp866", method});
            data.add(new Object[]{"ISO-8859-5", method});
            data.add(new Object[]{"ISO-8859-9", method});
            data.add(new Object[]{"iso-8859-13", method});
            data.add(new Object[]{"UTF-16LE", method});
            data.add(new Object[]{"utf-16be", method});
            data.add(new Object[]{"iso-8859-1", method});
        }
        return data;
    }

    @Before
    public void initHeaders() throws UnsupportedEncodingException, EncoderException {
        domain = format("%s%s.ru", DOMEN_PART, (i++));
        String from = MimeUtility.encodeWord(FROM, encodingType, encodingMethod.toUpperCase());
        log.info(from);
        log.info(new String(FROM.getBytes(encodingType)));
        to = "=?" + encodingType + "?" + encodingMethod + "?" + encodeInBase64(TO, encodingType, encodingMethod) + "?=";
        log.info(to);
        cc = "=?" + encodingType + "?" + encodingMethod + "?" + encodeInBase64(CC, encodingType, encodingMethod) + "?=";
        log.info(cc);
        bcc = MimeUtility.encodeWord(BCC, encodingType, encodingMethod.toUpperCase());
        log.info(bcc);
    }

    @Test
    public void shouldSeeDeliveryOfLetterWithHeadersInDifferentEncodings()
            throws IOException, MessagingException, InterruptedException {
        log.info("Проверяем, что правильная ли или неправильная кодировка не влияют на доставку сообщения");
        TestMessage msg = new TestMessage();
        User sender = accountRule.getSenderUser();
        User receiver = accountRule.getReceiverUser();
        msg.setFrom(sender.getEmail());
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver.getEmail(), to.trim()));
        msg.addRecipient(Message.RecipientType.CC, new InternetAddress("cc1@" + domain, cc));
        msg.addRecipient(Message.RecipientType.CC, new InternetAddress("cc2@" + domain, bcc));
        msg.setSubject(randomAlphanumeric(20));
        msg.setText(randomAlphanumeric(30));
        msg.saveChanges();
        String serverResponse = sendByNwsmtp(msg, server, port, sender);
        log.info(serverResponse);
        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject());
    }

    private String encodeInBase64(String headerValue, String encodeType, String encMethod)
            throws EncoderException {
        if (encMethod.equalsIgnoreCase("b")) {
            return String.valueOf(encodeBase64String(headerValue.getBytes(Charset.forName(encodeType))));
        }
        return new QuotedPrintableCodec(encodeType).encode(headerValue);
    }
}
