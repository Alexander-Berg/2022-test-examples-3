package ru.yandex.market.jmf.module.mail.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.configuration.api.Property;
import ru.yandex.market.jmf.configuration.api.PropertyTypes;
import ru.yandex.market.jmf.configuration.impl.ConfigurationServiceImpl;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.module.mail.EmailBodyLimiter;
import ru.yandex.market.jmf.module.mail.impl.EmailBodyLimiterImpl;
import ru.yandex.market.jmf.module.mail.impl.MimeMessageWrapper;
import ru.yandex.market.jmf.utils.html.Htmls;

public class MimeMessageWrapperTest {

    private static final Property<Long> EMAIL_BODY_MAX_LENGTH =
            Property.of("emailBodyMaxLength", PropertyTypes.INTEGER);

    private final EmailBodyLimiter emailBodyLimiter;

    private final Htmls htmls;
    private final ConfigurationService configurationService;


    public MimeMessageWrapperTest() {
        this.htmls = new Htmls(s -> s);
        this.configurationService = Mockito.mock(ConfigurationServiceImpl.class);
        this.emailBodyLimiter = new EmailBodyLimiterImpl(configurationService, htmls);
    }

    @BeforeEach
    public void setUp() {
        Mockito.when(configurationService.getValue(EMAIL_BODY_MAX_LENGTH.key())).thenReturn(null);
        Mockito.when(configurationService.getValue(Mockito.eq(EMAIL_BODY_MAX_LENGTH), Mockito.anyLong())).thenCallRealMethod();
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(configurationService);
    }

    @Test
    public void from() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_html.msg");
        String actual = new InternetAddress(msg.getFrom(), msg.getFromPersonal()).toUnicodeString();
        Assertions.assertEquals("\"Тест Тестович\" <nnty3ik@yandex.ru>", actual, "Должны получить отправителя");
    }

    @Test
    public void to() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_html.msg");
        String actual = new InternetAddress(msg.getTo(), msg.getToPersonal()).toUnicodeString();
        Assertions.assertEquals("\"Тест Тестович\" <nnty3ik@yandex.ru>", actual, "Должны получить адресата");
    }

    @Test
    public void subject() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_html.msg");
        String actual = msg.getSubject();
        Assertions.assertEquals("Тема письма кириллицей", actual, "Должны получить тему письма");
    }

    /**
     * Проверяем, что корректно обрабатывается Subject,
     * в котором некоторые многобайтные символы UTF-8 разбиты по соседним фрагментам
     */
    @Test
    public void cutSubject() throws Exception {
        MimeMessageWrapper msg = openMessage("/cut_subject.eml");
        String actual = msg.getSubject();
        String expected = "БЕРУ: изменить заказ 22574480 - Изменить адрес доставки, Изменить телефон получателя, № " +
                "855857002. Заказ: D0003969420. Обращение: 11082020-2474";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void htmlMessage_textBody() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_html.msg");
        String actual = msg.getHtmlBody(new ArrayList<>(), htmls, emailBodyLimiter);
        Assertions.assertEquals("<div>HTML тело письма</div><div>из</div><div" +
                ">нескольких</div><div>строк</div>", actual, "Должны получить текст письма");
    }

    // Кейс - https://testpalm.yandex-team.ru/testcase/ocrm-997
    // Кейс - https://testpalm.yandex-team.ru/testcase/ocrm-998
    // Кейс - https://testpalm.yandex-team.ru/testcase/ocrm-999
    @ParameterizedTest
    @CsvSource({
            "0,-1",
            "1,1000",
            "1001,1001"
    })
    public void limitHtml(long limit, long expectedSize) throws Exception {
        //длинный текст
        MimeMessageWrapper msg = openMessage("/message_limit.msg");

        //присвоить значение констатне 0,1,1001
        Mockito.when(configurationService.getValue(EMAIL_BODY_MAX_LENGTH.key())).thenReturn(limit);

        //вызвать метод обрезания
        String actual = msg.getHtmlBody(new ArrayList<>(), htmls, emailBodyLimiter);

        //сравнить результат
        Assertions.assertEquals(expectedSize == -1 ? msg.getOriginalBody().length() : expectedSize, actual.length());
    }

    @Test
    public void attachments_withEmptyAttachmentList() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_attachment.eml");
        String expected = "<div></div><div></div><div></div><div>-- </div><div>С уважением,</div><div>Виталий " +
                "Дорогин</div><div>https://staff.yandex-team.ru/vdorogin</div><div></div>";
        String actual = msg.getHtmlBody(new ArrayList<>(), htmls, emailBodyLimiter);
        Assertions.assertEquals(expected, actual, "Должны получить текст письма");
        List<DataSource> attachments = msg.getAttachmentList();
        Assertions.assertEquals(Arrays.asList("Picture-1.png", "Картинка-1.png", "Картинка-2" +
                        ".png", "Picture-2.png"),
                attachments.stream().map(DataSource::getName).collect(Collectors.toList()), "Должны получить вложение");
    }

    @Test
    public void attachments_withFillAttachmentList() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_attachment.eml");

        List<Attachment> msgAttachments = new ArrayList<>() {
        };
        msgAttachments.add(attachment("=?US-ASCII?B?0JrQsNGA0YLQuNC90LrQsC0xLnBuZw==?=", "477151570697653@iva5" +
                "-c4dd0484b46b.qloud-c.yandex.net", "cid:477151570697653@iva5-c4dd0484b46b.qloud-c.yandex.net"));
        msgAttachments.add(attachment("Picture-1.png", "477141570697653@iva5-c4dd0484b46b.qloud-c.yandex.net",
                "cid:477141570697653@iva5-c4dd0484b46b.qloud-c.yandex.net"));

        String expected = "<div> &#61;?US-ASCII?B?0JrQsNGA0YLQuNC90LrQsC0xLnBuZw&#61;&#61;?&#61; (см. " +
                "вложения) </div><div> Picture-1.png (см. вложения) </div><div></div><div>-- </div><div>С уважением," +
                "</div><div>Виталий " +
                "Дорогин</div><div>https://staff.yandex-team.ru/vdorogin</div><div></div>";
        String actual = msg.getHtmlBody(msgAttachments, htmls, emailBodyLimiter);
        Assertions.assertEquals(expected, actual, "Должны получить текст письма");
        List<DataSource> attachments = msg.getAttachmentList();
        Assertions.assertEquals(Arrays.asList("Picture-1.png", "Картинка-1.png", "Картинка-2" +
                        ".png", "Picture-2.png"),
                attachments.stream().map(DataSource::getName).collect(Collectors.toList()), "Должны получить вложение");
    }

    private Attachment attachment(String name, String cid, String url) {

        return new Attachment() {
            @Override
            public String getEntity() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getCid() {
                return cid;
            }

            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public <T> T getAttribute(String code) {
                return null;
            }

            @Override
            public <T> T getAttribute(Attribute attribute) {
                return null;
            }

            @Nonnull
            @Override
            public Metaclass getMetaclass() {
                return null;
            }

            @Override
            public String getDownloadUrl() {
                return null;
            }

            @Override
            public Fqn getFqn() {
                return null;
            }

            @Override
            public String getGid() {
                return null;
            }

            @Override
            public Long getSize() {
                return null;
            }

            @Override
            public String getMdsBucketName() {
                return null;
            }

            @Override
            public String getMdsKey() {
                return null;
            }

            @Override
            public OffsetDateTime getCreationTime() {
                return null;
            }
        };
    }

    /**
     * Проверяем, что цитаты не изменяются
     */
    @Test
    public void quotes() throws Exception {
        MimeMessageWrapper msg = openMessage("/message_quote.eml");
        String actual = msg.getHtmlBody(new ArrayList<>(), htmls, emailBodyLimiter)
                .replaceAll("&amp;sign&#61;\\w+", "");
        Assertions.assertEquals("<div>gdfgdfgdfgdfgdfgdfg</div><div>\u00A0</div><div>05" +
                ".07.2019, 16:27, &#34;Support Service&#34; &lt;supermailreadertest&#64;yandex.ru&gt;" +
                ":</div><blockquote>Добрый день!<br />Благодарим за обращение в Службу заботы о " +
                "клиентах" +
                " Яндекс.Маркета.<br />Мы получили Ваше письмо и зарегистрировали его под номером № 100402.<br />Мы " +
                "ответим вам в течение 24 часов.<br />---<br />С уважением,<br />Служба заботы о клиентах Яндекс" +
                ".Маркета<br /><a href=\"" +
                "https://market.yandex.ru/" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">https://market.yandex" +
                ".ru/</a></blockquote>", actual, "Должны получить текст письма");
    }

    @Test
    public void replyTo() throws Exception {
        MimeMessageWrapper msg = openMessage("/reply_to.eml");
        List<String> replyToList = msg.getReplyTo();

        Assertions.assertNotNull(replyToList);
        Assertions.assertEquals(1, replyToList.size());
        Assertions.assertEquals("test@test.te", replyToList.get(0));
    }

    @Test
    public void multipleReplyTo() throws Exception {
        MimeMessageWrapper msg = openMessage("/multiple_reply_to.eml");
        List<String> replyToList = msg.getReplyTo();

        Assertions.assertNotNull(replyToList);
        Assertions.assertEquals(3, replyToList.size());
        Assertions.assertEquals("test@test.te", replyToList.get(0));
        Assertions.assertEquals("test2@test.te", replyToList.get(1));
        Assertions.assertEquals("serg0321@mail.ru", replyToList.get(2));
    }

    @Test
    public void replyTo_multiline() throws Exception {
        MimeMessageWrapper msg = openMessage("/reply_to_multiline.eml");
        List<String> replyToList = msg.getReplyTo();

        Assertions.assertNotNull(replyToList);
        Assertions.assertEquals(2, replyToList.size());
        Assertions.assertEquals("test@test.te", replyToList.get(0));
        Assertions.assertEquals("wheelers@yandex.ru", replyToList.get(1));
    }

    @Test
    public void brokenReplyTo() throws Exception {
        MimeMessageWrapper msg = openMessage("/broken_reply_to.eml");
        List<String> replyToList = msg.getReplyTo();

        Assertions.assertNotNull(replyToList);
        Assertions.assertEquals(1, replyToList.size());
        Assertions.assertEquals("test1 <test@test.te>, Pupkin Vasya <test2@te@st.te>", replyToList.get(0));
    }

    @Test
    public void multipart() throws Exception {
        MimeMessageWrapper msg = openMessage("/multipart_email.eml");
        String actual = msg.getHtmlBody(new ArrayList<>(), htmls, emailBodyLimiter);
        Assertions.assertTrue(actual.contains("410012098823885"));
    }

    private MimeMessageWrapper openMessage(String path) throws MessagingException, IOException {
        byte[] resource = ResourceHelpers.getResource(path);
        MimeMessage msg = new MimeMessage(null, new ByteArrayInputStream(resource));
        return new MimeMessageWrapper(msg);
    }
}
