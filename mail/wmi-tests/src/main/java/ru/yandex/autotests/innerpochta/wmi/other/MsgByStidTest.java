package ru.yandex.autotests.innerpochta.wmi.other;

import ch.ethz.ssh2.Connection;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.tests.unstable.SendMessageProvider;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageByStidObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageSourceObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.util.ssh.SSHCommands.executeCommAndResturnResultAsString;
import static ru.yandex.autotests.innerpochta.util.ssh.SSHCommands.getSSHConnection;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.filter.CheckHeaderFilter.iso8859toUTF8Value;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 17.10.12
 * Time: 19:24
 * <p/>
 * [DARIA-16281]
 * <p/>
 * [DARIA-16276]
 * [DARIA-22931]
 * [DARIA-31454]
 * <p/>
 * wmidb01d.mail.yandex.net:27017
 * <p/>
 * Актуализировали crypto:
 * [DARIA-32261]
 */
@Aqua.Test
@Title("Отправка СМС писем")
@Description("Отправляем смс письма. Проверяем работоспособность ссылок")
@Features(MyFeatures.WMI)
@Stories(MyStories.SMS)
@Credentials(loginGroup = "SMS")
@Issues({@Issue("DARIA-16281"), @Issue("DARIA-16276"), @Issue("DARIA-22931"), @Issue("DARIA-31454"),
        @Issue("DARIA-32261"), @Issue("DARIA-44582")})
@Ignore("MAILDEV-772")
public class MsgByStidTest extends BaseTest {

    public static final String X_YANDEX_SMS_DIGEST = "X-YandexSms-Digest";
    public static final String HEADER_WITH_MD5 = X_YANDEX_SMS_DIGEST + ": ";
    public static final String X_YANDEX_SMS_PHONE = "X-YandexSms-Phone";
    public static final String X_YANDEX_SMS_HOST = "X-YandexSms-Host";
    public static final String HOST = "unconfigured.yandex.ru";
    public static final String MXFRONT_HOST = "mxback-qa3.cmail.yandex.net";
    public static final int MXFRONT_PORT = 25;
    //отправляем зараженую ссылку в теле письма
    public static final String URL = "http://yandex.ru";
    //отправляем зараженую ссылку в теле письма [DARIA-42806]
    public static final String URL_INFECTED = "http://mixisjp.com";
    //телефон, на который отправляем sms
    private final String phone = getPhone("88123157505");
    private Long start = 666L;
    private Long end = 666L;
    private String hash;

    @Before
    public void setUp() throws Exception {
        start = new Date().getTime() / 1000;

        clean.subject(start.toString());

        send(phone, HOST, start.toString(), HOST + start.toString() + "\n" +
                URL + "\n" + URL_INFECTED
        );
        waitWith.subj(start.toString()).waitDeliver();
        end = new Date().getTime() / 1000;
        hash = calculateHash(start, HOST, end);

        logger.info("Web link: " + props().betaHost() + "/sms/index.jsx?id=" + hash);
    }


    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).inbox().outbox();

    @Test
    @Issue("DARIA-16281")
    public void testUploadAtt() throws Exception {
        logger.warn("[DARIA-16281] Хеш как гет-параметр");
        shouldNotHaveErrorsAndContainsHost(
                jsx(MessageByStid.class)
                        .params(new MessageByStidObj().setHashAsIs(hash)
                                .setXmlVersion(Obj.XMLVERSION_DARIA2))
                        .get().via(authClient.notAuthHC())
        );

        logger.warn("Хеш как пост параметр");
        shouldNotHaveErrorsAndContainsHost(
                jsx(MessageByStid.class)
                        .params(new MessageByStidObj().setHash(hash))
                        .post().via(authClient.notAuthHC())
        );

        String urlCompHash = urlCompatibleHash(hash);

        shouldNotHaveErrorsAndContainsHost(
                jsx(MessageByStid.class)
                        .params(new MessageByStidObj().setHashAsIs(urlCompHash))
                        .get().via(authClient.notAuthHC())
        );

    }

    @Test
    @Issue("DARIA-31515")
    @Description("Проверяем работоспособность ссылок в смс\n" +
            "Проверяем что зараженные ссылки корректно определяются\n" +
            "http://wiki.yandex-team.ru/vasilishmelev/wmiVdirect\n" +
            "DARIA-31515")
    public void testVDirectUrls() throws Exception {
        logger.warn("[WMI-648] Формирование смс ссылки");
        MessageByStid msgByStid = jsx(MessageByStid.class)
                .params(new MessageByStidObj().setHashAsIs(urlCompatibleHash(hash))
                        .setXmlVersion(Obj.XMLVERSION_DARIA2))
                .get().via(authClient.notAuthHC());
        msgByStid.withDebugPrint();

        List<String> links = msgByStid.allLinks();

        String urlVdirect = links.get(0).replace(props().productionHost(),
                props().betaHost());

        logger.info("LINK: " + urlVdirect);

        assertThat("Смс не использует вдирект", urlVdirect, containsString("/sm.jsx?h="));

        DefaultHttpClient hc = authClient.notAuthHC();
        HttpClientParams.setRedirecting(hc.getParams(), false);

        Executor.newInstance(hc).execute(Request.Get(urlVdirect))
                .handleResponse(getLocationHandler(URL + "/"));
    }

    private ResponseHandler<Object> getLocationHandler(final String expectedUrl) {
        return new ResponseHandler<Object>() {
            @Override
            public Object handleResponse(HttpResponse response)
                    throws ClientProtocolException, IOException {
                assertThat("Не обнаружен редирект - получен неверный статус код",
                        response.getStatusLine().getStatusCode(), equalTo(HttpStatus.FOUND_302));
                Header header = response.getFirstHeader("Location");
                logger.warn("Редирект: " + iso8859toUTF8Value(header));
                assertThat("Редирект не совпадает с ожидаемым", iso8859toUTF8Value(header), is(expectedUrl));
                return null;
            }
        };
    }

    private String urlCompatibleHash(String hash) throws UnsupportedEncodingException {
        String decodedHash = URLDecoder.decode(hash, "UTF-8");
        warnIfHashNotComp(decodedHash);
        return decodedHash.replaceAll("\\+", "-")
                .replaceAll("=", "")
                .replaceAll("/", "_");
    }

    private void warnIfHashNotComp(String hash) {
        if (StringUtils.containsAny(hash, "=+/")) {
            logger.warn("ХЕШ СОДЕРЖИТ ЭТИ БУКАФКИ!!! " + hash);
        }
    }

    /**
     * Перестали на ручку показа письма из смс отдавать тег can_reply, чтобы в верстке не рисовалась форма ответа
     *
     * @param resp
     */
    private void shouldNotHaveErrorsAndContainsHost(MessageByStid resp) {
        resp.withDebugPrint().assertResponse(not(containsString("error")))
                .assertDocument("Содержимое письма при хэше в заголовке не должно быть пустым",
                        hasXPath("//content", containsString(HOST)))
                .assertDocument("Не должны мочь ответить [MAILPG-698]", not(hasXPath("//can_reply", equalTo("yes"))));
    }

    @Test
    @Issue("DARIA-22239")
    @Description("Проверяем, что присутствуют ссылки на аттачи")
    public void testDownloadAtt() throws Exception {
        logger.warn("[DARIA-22239]");
        MessageByStid resp = jsx(MessageByStid.class)
                .params(new MessageByStidObj().setHashAsIs(hash)
                        .setXmlVersion(Obj.XMLVERSION_DARIA2))
                .get().via(authClient.notAuthHC());

        assumeThat("Письмо не смогли прочитать", resp.toString(), not(containsString("error")));
        Thread.sleep(SECONDS.toMillis(3));
        jsx(SmsMessagePart.class)
                .params(new MessageByStidObj().setHash(hash).set("hid", "1.2,1.3"))
                .post().via(authClient.notAuthHC()).withDebugPrint()
                .assertResponse(not(containsString("error")))
                .and().assertDocument(hasXPath("//attach_redirect/@arg", not(equalTo(""))));
    }

    private String calculateHash(Long start, String host, Long end) throws Exception {
        String mid = api(MailBoxList.class).post().via(hc).getMidOfMessage(start.toString());
        String stId = api(Message.class).params(new MessageObj().setIds(mid)).post().via(hc).getStId();
        String raw = api(MessageSource.class).params(MessageSourceObj.getSourceByMid(mid)).post().via(hc).withDebugPrint().toString();

        Integer headerPos = raw.indexOf(HEADER_WITH_MD5);
        String md5 = raw.substring(headerPos + (HEADER_WITH_MD5).length(), headerPos + (HEADER_WITH_MD5).length() + 32);

        logger.info(HEADER_WITH_MD5 + md5);

        String time = getTime(md5, host, start, end);

        return getHash(time, stId);
    }


    public void send(String phone, String host, String subj, String body) throws Exception {
        TestMessage msg = new TestMessage();
        msg.setFrom(authClient.acc().getSelfEmail());
        msg.setRecipient(authClient.acc().getSelfEmail());


        logger.info(X_YANDEX_SMS_PHONE + ": " + phone);
        logger.info(X_YANDEX_SMS_HOST + ": " + host);


        msg.addHeader(X_YANDEX_SMS_PHONE, phone);
        msg.addHeader(X_YANDEX_SMS_HOST, host);
        msg.setSubject(subj);
        msg.setSentDate(new java.util.Date());

        Multipart mp = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart();
        part.setContent(body, "text/plain; charset=utf-8");
        mp.addBodyPart(part);


        File bmp = File.createTempFile("1x1", ".bmp");
        asByteSink(bmp).write(asByteSource(getResource("img/1x1.bmp")).read());

        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.attachFile(bmp);
        mp.addBodyPart(bodyPart);

        File attach = Util.generateRandomShortFile(Util.getRandomString() + ".bin", 64);

        MimeBodyPart bodyPart2 = new MimeBodyPart();
        bodyPart2.attachFile(attach);
        mp.addBodyPart(bodyPart2);

        msg.setContent(mp);


        msg.saveChanges();
        SendMessageProvider.send(msg, MXFRONT_HOST, MXFRONT_PORT);
    }

    /**
     * Отдаем бинарнику сначала timestamp и stid.
     * Именно в такой последовательности.
     */
    private static String getHash(String timestamp, String stid) throws IOException {
        String version = exec("/home/dskut/crypto/crypto " + stid + " " + timestamp);
        //пример правильного хэша DIsuTybYxuTe_EUTCv1U6-byMtYt-Ot03Ezsz9kB6BrHq715MBAOkJU5W5KYz2RCKR9asNnbIFJmK414WyqMrQ
        return StringUtils.chomp(version);
//        return ver.substring(0, version.indexOf(" "));
    }

    private static String getPhone(String phone) {
        try {
            String phoneHash = exec("echo -n \"" + phone + "\" |" +
                    " openssl bf -a -nosalt -iv \"416e797468696e67\" -K \"4b65794b65794b65794b65794b657921\"");
            return phoneHash.substring(0, phoneHash.indexOf("\n"));
        } catch (IOException e) {  // Ловим ошибку, чтобы можно было использовать метод при инициализации
            throw new RuntimeException("Ошибка шифрования телефона", e);
        }
    }

    private static String exec(String cmd) throws IOException {
        Logger logger = LogManager.getLogger(MsgByStidTest.class);
        Connection conn = getSSHConnection("wmidev-qa.yandex.ru", "robot-gerrit", "", logger);
        String version = executeCommAndResturnResultAsString(conn, cmd, logger);
        conn.close();
        return version;
    }


    public static String getTime(String hash, String host, Long start, Long end) throws Exception {
        LogManager.getLogger(MsgByStidTest.class).info(String.format("Ищем время в промежутке: (%s:%s)",
                start - 200, end + 200));
        for (int i = start.intValue() - 200; i < end.intValue() + 200; i++) {
            String md5 = DigestUtils.md5Hex(host + i);
            if (md5.equals(hash)) {
                LogManager.getLogger(MsgByStidTest.class).info("TIME: " + i);
                return Integer.toString(i);
            }
        }
        return null;
    }
}
