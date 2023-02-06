package ru.yandex.tikaite.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tika.mime.MediaType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.tikaite.mimeparser.NestedMessageHandler;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

public class OnlineMailHandlerTest extends TestBase {
    private static final String LOCALHOST = "http://localhost:";
    private static final String PATH = "/mail/";
    private static final String CONTENT_TYPE =
        "application/json; charset=UTF-8";
    private static final String MID = "100500";
    private static final Long SUID = 9000L;
    private static final String MDB = "mdb303";
    private static final String ONE = "1";
    private static final String ONE_ONE = "1.1";
    private static final String ONE_ONE_ONE = "1.1.1";
    private static final String ONE_ONE_TWO = "1.1.2";
    private static final String ONE_TWO = "1.2";
    private static final String ONE_THREE = "1.3";
    private static final String ONE_FOUR = "1.4";
    private static final String ONE_FIVE = "1.5";
    private static final String RATIO_4X3 = "4:3";
    private static final String BOUNDARY =
        "content-type: multipart/mixed; boundary=\"myboundary\"";
    private static final String BASE64 = "\ncontent-transfer-encoding: base64";
    private static final String HELLO_WORLD = "Hello, world";
    private static final String TEXT = "Hello header\nHello, world\ncell text";
    private static final String CONTENT_HTML = "content-type"
        + MailMetaInfo.HEADERS_SEPARATOR
        + MediaType.TEXT_HTML.getBaseType().toString();
    private static final String PDF = "pdf";
    private static final String TEXT_PDF = "txt text pdf";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String ATTACHMENT = "attachment";
    private static final String INLINE = "inline";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String AUDIO_MPEG = "audio/mpeg";
    private static final String PHOTOSHOP = "image/vnd.adobe.photoshop";
    private static final String PHOTOSHOP_META =
        "photoshop:ColorMode:RGB Color\n"
        + "tiff:BitsPerSample:8\nContent-Type:image/vnd.adobe.photoshop";
    private static final String JPEG = "jpe jpeg jpg";
    private static final String MP3 = "mp3 mpga";
    private static final String PSD = "psd";
    private static final String SQUARE = "1:1";
    private static final String HEADER_MESSAGE_RFC822 =
        "content-type: message/rfc822\n";
    private static final String HEADER_TEXT_PLAIN_UTF_8 =
        "content-type: text/plain; charset=\"utf-8\"";
    private static final String HEADER_TEXT_HTML_UTF_8 =
        "content-type: text/html; charset=\"utf-8\"";
    private static final String HEADER_TEXT_HTML_KOI8_R =
        "content-type: text/html; charset=koi8-r";
    private static final String HEADER_TEXT_PLAIN_KOI8_R =
        "content-type: text/plain; charset=koi8-r";
    private static final String HEADER_TEXT_HTML_WINDOWS_1251 =
        "content-type: text/html; charset=\"Windows-1251\"";
    private static final String CONTENT_TYPE_IMAGE_JPEG =
        "Content-Type:" + IMAGE_JPEG;
    private static final String CONTENT_TYPE_HTML_ISO_8859_1 =
        "Content-Type:text/html; charset=ISO-8859-1";
    private static final String CONTENT_TYPE_HTML_KOI8_R =
        "Content-Type:text/html; charset=KOI8-R";
    private static final String CONTENT_TYPE_HTML_UTF_8 =
        "Content-Type:text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_HTML_WINDOWS_1251 =
        "Content-Type:text/html; charset=windows-1251";
    private static final String CONTENT_TYPE_PLAIN_ISO_8859_1 =
        "Content-Type:text/plain; charset=ISO-8859-1";
    private static final String CONTENT_TYPE_PLAIN_KOI8_R =
        "Content-Type:text/plain; charset=KOI8-R";
    private static final String CONTENT_TYPE_PLAIN_UTF_8 =
        "Content-Type:text/plain; charset=UTF-8";
    private static final String CONTENT_TYPE_PLAIN_WINDOWS_1252 =
        "Content-Type:text/plain; charset=windows-1252";
    private static final String CONTENT_TYPE_PDF =
        "Content-Type:application/pdf";
    private static final String DETEMPL_SUFFIX =
        "[extractor]\nsanitizing-config = "
        + Paths.getSourcePath(
            "mail/library/html/sanitizer/sanitizer2_config/configs"
            + "/detempl.conf");
    private static final String FILTER_FIELDS_SUFFIX =
        "[extractor.fields-filter]\n"
        + "library = "
        + Paths.getBuildPath(
            "mail/so/libs/unperson/jniwrapper/libunperson-jniwrapper.so")
        + "\nctor = JniWrapperCreateUnperson\n"
        + "dtor = JniWrapperDestroyUnperson\n"
        + "main16 = JniWrapperUnpersonText\n"
        + "filter-fields = body_text\n"
        + "bypass-fields = pure_body, headers\n";
    private static final String SANITIZE_HTML = "?sanitize-html";

    public OnlineMailHandlerTest() {
        super(false, 0L);
    }

    @Test
    public void testGet() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            Server server = new Server(ServerTest.getConfig(1)))
        {
            server.start();
            HttpGet get = new HttpGet(LOCALHOST + server.port() + PATH);
            HttpResponse response = client.execute(get);
            Assert.assertEquals(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "Mail body expected",
                EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        }
    }

    @Test
    public void testMailWithoutMdb() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            Server server = new Server(ServerTest.getConfig(1)))
        {
            server.start();
            HttpPost post = new HttpPost(LOCALHOST + server.port() + PATH);
            post.setEntity(new StringEntity(
                "X-Yandex-Mid: 100500\r\nX-Yandex-Suid: 9000\r\n"
                + "To: hello@world.ru\r\nhi!"));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"prefix\":\"9000\",\"docs\":[{\"mid\":\"100500\","
                        + "\"headers\":\"to: hello@world.ru\",\"hdr_to\":"
                        + "\"hello@world.ru\",\"suid\":\"9000\","
                        + "\"hdr_to_email\":\"hello@world.ru\\n\","
                        + "\"hdr_to_normalized\":\"hello@world.ru\\n\","
                        + "\"hid\":\"1\",\"url\":\"100500/1\",\"built_date\":"
                        + "\"<any value>\",\"parsed\":false}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void checkMail(final String name, final Json json)
        throws Exception
    {
        checkMail(name, json, "");
    }

    private void checkMail(
        final String name,
        final Json json,
        final String suffix)
        throws Exception
    {
        checkMail(name, json, suffix, "");
    }

    // CSOFF: ParameterNumber
    private void checkMail(
        final String name,
        final Json json,
        final String suffix,
        final String uriSuffix)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            Server server = new Server(ServerTest.getConfig(1, suffix)))
        {
            server.start();
            HttpPost post = new HttpPost(
                LOCALHOST + server.port() + PATH + name + uriSuffix);
            URL url = getClass().getResource(name);
            File file;
            if (url == null) {
                file = new File(Paths.getSandboxResourcesRoot() + '/' + name);
            } else {
                file = new File(url.toURI());
            }
            post.setEntity(new FileEntity(file));
            checkMail(client, post, json);
        }
    }
    // CSON: ParameterNumber

    public static void checkMail(
        final CloseableHttpClient client,
        final HttpUriRequest request,
        final Json json)
        throws Exception
    {
        try (CloseableHttpResponse response = client.execute(request)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpEntity entity = response.getEntity();
            Assert.assertEquals(
                CONTENT_TYPE,
                entity.getContentType().getValue());
            String text = CharsetUtils.toString(entity);
            try {
                json.assertEquals(text);
            } catch (Throwable t) {
                System.err.println("Json " + text + " caused:");
                t.printStackTrace();
                throw t;
            }
        }
    }

    @Test
    public void testComplete() throws Exception {
        String mid = "2310000000061351000";
        final Long suid = 94762000L;
        Json json = new Json(
            "return-path: <>\n"
            + "x-original-to: service@services.mail.yandex.net\n"
            + "delivered-to: save@service5h.mail.yandex.net\n"
            + "received: from mxfront4h.mail.yandex.net "
            + "(mxfront4h.mail.yandex.net [93.158.130.232])\t"
            + "by service5h.mail.yandex.net (Yandex) with ESMTP id 6576D40013"
            + "\tfor <service@services.mail.yandex.net>; "
            + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)\n"
            + "received: from mxfront4h.mail.yandex.net ([127.0.0.1])"
            + "\tby mxfront4h.mail.yandex.net with LMTP id 4HNeKe76"
            + "\tfor <shlex@yandex.ru>; Wed, 21 Mar 2012 17:04:17 +0400\n"
            + "received: from gizmo.7host.ru (gizmo.7host.ru [89.249.22.205])"
            + "\tby mxfront4h.mail.yandex.net (nwsmtp/Yandex) with ESMTP id "
            + "4GW4FSLH-4GW4O7pe;\tWed, 21 Mar 2012 17:04:16 +0400\n"
            + "x-yandex-front: mxfront4h.mail.yandex.net\n"
            + "x-yandex-timemark: 1332335056\n"
            + "x-yandex-spam: 1\n"
            + "received: by gizmo.7host.ru (Postfix, from userid 501)"
            + "\tid 909B4114049B; Wed, 21 Mar 2012 17:04:16 +0400 (MSK)\n"
            + "to: shlex@yandex.ru\n"
            + "cc: shlex@mail.ru\n"
            + "bcc: devnull@yandex.ru\n"
            + "reply-to: devbull@yandex.ru\n"
            + "subject: ERROR in clear cash\n"
            + "message-id: <20120321130416.909B4114049B@gizmo.7host.ru>\n"
            + "date: Wed, 21 Mar 2012 17:04:16 +0400 (MSK)\n"
            + "from: ta@gizmo.7host.ru (ta)\n"
            + "x-yandex-forward: 916f01d73bdf8e7470a814134b4bb096",
            mid,
            suid,
            "mdb3020");
        String to = "shlex@yandex.ru\n";
        String cc = "shlex@mail.ru\n";
        String bcc = "devnull@yandex.ru\n";
        String replyTo = "devbull@yandex.ru\n";
        String from = "info@delafisha.ru\n";
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, mid);
        doc.put(
            MailMetaInfo.STID,
            "3310.94762220.75948018434057933525251075000");
        doc.put(MailMetaInfo.SUID, suid.toString());
        doc.put(MailMetaInfo.FID, "2310000460006266000");
        String receivedDate = "1332335057";
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, "4GW4FSLH-4GW4O7pe");
        doc.put(MailMetaInfo.ALL_SMTP_IDS, "4GW4FSLH-4GW4O7pe");
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.THREAD_ID_FIELD, "1611100000239724000");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, "shlex@yandex.ru");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, "shlex@mail.ru");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, "devnull@yandex.ru");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bcc);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, "devbull@yandex.ru");
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyTo);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"Деловая Афиша\" <info@delafisha.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Деловая " + "Афиша\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "ERROR in clear cash");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "ERROR in clear cash УШЕЛ: Can't execute "
            + "cURL connection. The requested URL returned error: 404");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        checkMail("complete.raw.txt", json);
    }

    @Test
    public void testPdfAttach() throws Exception {
        String headers =
            "h5+)!9: content-disposition\n"
            + "hduutvl: content-type\n"
            + "v1: to\n"
            + "e%: cc\n"
            + "d'%: bcc\n"
            + "h63/: from\n"
            + "80%,49\": reply-to\n"
            + "6;=\"9$!: subject\n"
            + ">$#.2;: x-yandex-fid\n"
            + "kjrgxzc: x-yandex-label\n"
            + "yztsziq: x-yandex-login\n"
            + ">$#5-9: x-yandex-mdb\n"
            + ">$#52;: x-yandex-mid\n"
            + "egnyweo: x-yandex-msgtype\n"
            + "dneadtg: x-yandex-notifymsg\n"
            + "!.#-68!: x-yandex-received\n"
            + "atxqolr: x-yandex-ssuid\n"
            + "9)#'?*\": x-yandex-stid\n"
            + "llffnwm: x-yandex-suid\n"
            + "qyvcmifa: x-yandex-threadid\n"
            + "zqpvos: x-yandex-timemark\n"
            + "ywjsciea: x-yandex-uid\n"
            + BOUNDARY;
        Json json = new Json(headers, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, TEXT);
        doc.put(CommonFields.TITLE, "Hello " + "page");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-disposition: attachment; filename=\"file.pdf\""
            + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.ATTACHNAME, "file.pdf");
        doc.put(MailMetaInfo.ATTACHSIZE, 4578L);
        doc.put(MailMetaInfo.ATTACHTYPE, PDF);
        doc.put(MailMetaInfo.MD5, "B21888CA0ACE49E7858B3D3C36BC780E");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, APPLICATION_PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "Hello, world\n1");
        doc.put(CommonFields.PRODUCER, "GPL Ghostscript 9.05");
        doc.put(CommonFields.TITLE, "test.dvi");
        doc.put(
            CommonFields.TOOL,
            "dvips(k) 5.991 Copyright 2011 Radical Eye Software");
        doc.put(CommonFields.PAGES, 1L);
        doc.put(CommonFields.CREATED, 1351598102L);
        doc.put(CommonFields.MODIFIED, 1351598102L);
        doc.put(
            CommonFields.META,
            new Json.Headers(
                "Content-Type:application/pdf\nparser:pdfclown"));
        doc = json.createDoc(ONE_THREE, headers + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, false);
        doc.put(CommonFields.MIMETYPE, "video/x-msvideo");
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        checkMail("pdfattach.raw.txt", json);
    }

    @Test
    public void testTrashAttach() throws Exception {
        Json json = new Json(BOUNDARY, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, TEXT);
        doc.put(CommonFields.TITLE, "Hello page");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);

        doc = json.createDoc(
            ONE_TWO,
            BOUNDARY
            + "\ncontent-type: application/pdf; name=\"bad-base64-start.pdf\""
            + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.ATTACHNAME, "bad-base64-start.pdf");
        doc.put(MailMetaInfo.ATTACHTYPE, TEXT_PDF);
        doc.put(MailMetaInfo.ATTACHSIZE, 9L);
        doc.put(MailMetaInfo.MD5, "B601FA74337119672336618217DCEB76");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/pdf");
        doc.put(CommonFields.BODY_TEXT, Json.ANY_VALUE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.META, "Content-Type:text/plain; charset=IBM866");

        doc = json.createDoc(
            ONE_THREE,
            BOUNDARY
            + "\ncontent-type: application/pdf; name=\"bad-base64.pdf\""
            + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.ATTACHNAME, "bad-base64.pdf");
        doc.put(MailMetaInfo.ATTACHTYPE, PDF);
        doc.put(MailMetaInfo.ATTACHSIZE, 99996L);
        doc.put(MailMetaInfo.MD5, "3BD82B90DD2867AAA8093881A4CAEE0A");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.PARSED, false);
        doc.put(CommonFields.MIMETYPE, APPLICATION_PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.META, CONTENT_TYPE_PDF);

        doc = json.createDoc(
            ONE_FOUR,
            BOUNDARY
            + "\ncontent-type: application/pdf; name=\"bad.pdf\""
            + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.ATTACHNAME, "bad.pdf");
        doc.put(MailMetaInfo.ATTACHTYPE, PDF);
        doc.put(MailMetaInfo.ATTACHSIZE, 100000L);
        doc.put(MailMetaInfo.MD5, "3D13E59413462C30F6CD96168B28FFB8");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.PARSED, false);
        doc.put(CommonFields.MIMETYPE, APPLICATION_PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.META, CONTENT_TYPE_PDF);

        checkMail("trashattach.raw.txt", json);
    }

    @Test
    public void testHtml() throws Exception {
        Json json = new Json(CONTENT_HTML, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            "quoted text\nhere is some additional text, Дима Потапов "
            + "potapov.d@gmail.com asdfas34234");
        String pureBody = "some body\nafter quotation after comment";
        doc.put(MailMetaInfo.PURE_BODY, pureBody);
        doc.put(CommonFields.TITLE, "some " + "title");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        String file = "html.raw.txt";
        checkMail(file, json);
        doc.put(
            MailMetaInfo.HTML_BODY,
            "\n<html><body>\n    \n    <p style=\"padding:2px\">some body</p>"
            + "\n    <img src=\"https://ya.ru/\" style=\""
            + "border:0;display:block\" />\n"
            + "    <blockquote><p>quoted text</p>here is some additional text"
            + ", Дима Потапов "
            + "<a href=\"mailto:potapov.d@gmail.com\">potapov.d@gmail.com</a>"
            + " asdfas34234</blockquote>"
            + "\n    after quotation\t\tafter comment\n  </body></html>\n");
        checkMail(
            file,
            json,
            DETEMPL_SUFFIX,
            SANITIZE_HTML);
        doc.put(
            MailMetaInfo.HTML_BODY,
            "\n<html><body>\n    \n    <p style=\"padding:2px\">some body</p>"
            + "\n    <img src=\"https://ya.ru/\" style=\""
            + "border:0;display:block\" />\n"
            + "    <blockquote><p>quoted</p></blockquote></body></html>");
        checkMail(
            file,
            json,
            DETEMPL_SUFFIX + "\nmax-sanitizing-length = 420\n",
            SANITIZE_HTML);
        checkMail(
            file,
            json,
            DETEMPL_SUFFIX,
            SANITIZE_HTML + "&max-sanitizing-length=420");
        doc.remove(MailMetaInfo.HTML_BODY);
        doc.remove(CommonFields.TITLE);
        doc.put(CommonFields.BODY_TEXT, "quoted text");
        doc.put(MailMetaInfo.PURE_BODY, "some body\nafter quotat");
        final long length = 22L;
        doc.put(CommonFields.TRUNCATED, length);
        checkMail(file, json, "[extractor]\nmail-length-limit = " + length);

        doc.remove(MailMetaInfo.MID);
        doc.remove(MailMetaInfo.SUID);
        doc.remove(MailMetaInfo.HID);
        doc.remove(MailMetaInfo.URL);
        doc.remove(CommonFields.BUILT_DATE);
        doc.remove(CommonFields.MIMETYPE);
        doc.remove(MailMetaInfo.CONTENT_TYPE);
        doc.remove(CommonFields.TRUNCATED);
        doc.remove(CommonFields.META);
        doc.put(MailMetaInfo.PURE_BODY, pureBody);
        doc.put(
            CommonFields.BODY_TEXT,
            "quoted text\n"
            + "here is some additional text, %FirstName_1993367800% "
            + "%LastName_1350509857% %Uri_mailto_gmail.com_1158255410% "
            + "asdfas%Number_2549979593%");
        checkMail(file, json, FILTER_FIELDS_SUFFIX, "?filter-fields");

        doc.put(
            CommonFields.BODY_TEXT,
            "quoted text\n"
            + "here is some additional text, Дима Потапов"
            + " %Uri_mailto_gmail.com_1158255410% "
            + "asdfas%Number_2549979593%");
        checkMail(
            file,
            json,
            FILTER_FIELDS_SUFFIX,
            "?filter-fields&filter-fields-options=FirstName,LastName");
    }

    @Test
    public void testDirectMailRequest() throws Exception {
        String requestHead = "POST /mail?filter-fields HTTP/1.1\r\n\r\n";
        String requestBody =
            "From: analizer@yandex.ru\r\n"
            + "Content-Type: text/plain; charset=utf-8\r\n\r\n"
            + "Привет, Дима, как дела\uD83D\uDE03\n"
            + "> Цитатка, Дима, тута\uD83D\uDE03";
        try (Server server =
                new Server(ServerTest.getConfig(1, FILTER_FIELDS_SUFFIX)))
        {
            server.start();

            try (Socket socket = new Socket("localhost", server.port());
                BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8)))
            {
                socket.getOutputStream().write(
                    requestHead.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                socket.getOutputStream().write(
                    requestBody.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                socket.shutdownOutput();

                String line = reader.readLine();
                while (!line.isEmpty()) {
                    line = reader.readLine();
                }

                Assert.assertNotNull(reader.readLine());

                YandexAssert.check(
                    new JsonChecker(
                        "{\"docs\":[{\"headers\":\"from: analizer@yandex.ru\\n"
                        + "content-type: text/plain; charset=utf-8\","
                        + "\"body_text\":\"%FirstName_950267855%, "
                        + "%FirstName_1993367800%, тута\uD83D\uDE03\","
                        + "\"pure_body\":\"Привет, Дима, как дела"
                        + "\uD83D\uDE03\",\"parsed\":true}]}"),
                    reader.readLine());
            }
        }
    }

    @Test
    public void testDoubleHtml() throws Exception {
        Json json = new Json("content-type: TEXT/PLAIN", MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/plain");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "some body\nanother body");
        doc.put(CommonFields.TITLE, "some title");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        checkMail("html.double.raw.txt", json);
    }

    @Test
    public void testBadHtml() throws Exception {
        Json json = new Json(CONTENT_HTML, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "here the text\ncontinued\nthe sudden death is coming!");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        checkMail("html.bad.txt", json);
    }

    @Test
    public void testRecoverEofHtml() throws Exception {
        Json json = new Json(CONTENT_HTML, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "the sudden death is here");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        checkMail("html.recover.eof.txt", json);
    }

    @Test
    public void testTextPlain() throws Exception {
        Json json = new Json(
            HEADER_TEXT_PLAIN_UTF_8
            + "\nsubject: =?badenc?b?helo?= =?koi8-r?b?0NLJ18XU?=."
            + "=?utf-8?b?0LTQstC10YDRjA==?=.=?koi8-r?q?=CD_=C9=D2?=."
            + "=?utf-8?q?=hh?=",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.X_URLS, "http://home.ru\n");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Здравствуй, мир! http://home.ru/");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "=?badenc?b?helo?= привет.дверь.м ир.=?utf-8?q?=hh?=");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        checkMail("plain.raw.txt", json);
    }

    @Test
    public void testCorruptedBase64() throws Exception {
        Json json = new Json(
            "content-type: image/jpeg; name=\"dansk.jpg\"" + BASE64,
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.ATTACHNAME, "dansk.jpg");
        doc.put(MailMetaInfo.ATTACHSIZE, 5404842L);
        doc.put(MailMetaInfo.ATTACHTYPE, JPEG);
        doc.put(MailMetaInfo.MD5, "518F07BC726DD654302C25886F02D24F");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.CREATED, 1343832545L);
        doc.put(CommonFields.MODIFIED, Json.ANY_VALUE);
        doc.put(CommonFields.MIMETYPE, IMAGE_JPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.MANUFACTURER, "Phase One");
        doc.put(CommonFields.MODEL, "P30");
        doc.put(CommonFields.WIDTH, 6496L);
        doc.put(CommonFields.HEIGHT, 4872L);
        doc.put(CommonFields.RATIO, RATIO_4X3);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.EXIF_ORIENTATION, 1L);
        doc.put(
            CommonFields.META,
            new Json.Contains("Content-Type:image/jpeg"));
        checkMail("corrupted.base64.eml", json);
    }

    @Test
    public void testMessageType() throws Exception {
        String mid = "2170000000015519390";
        final Long suid = 355453075L;
        Json json = new Json(
            "received: from mxback1m.mail.yandex.net ([127.0.0.1])\tby "
            + "mxback1m.mail.yandex.net with LMTP id ePOC319a\tfor <info@cpu-i"
            + "mperia.ru>; Fri, 3 \u0014χ\u000f\u0085Ψξ��ιίξ��\u000f\u001f "
            + "2014 23:40:25 +0400\n"
            + "received: from localhost ([127.0.0.1])\t"
            + "by hampers64.yandex.ru with LMTP id CbT0HhTc\t"
            + "for <miomimi@yandex.ru>; Thu, 18 Apr 2013 18:12:37 +0400\n"
            + "from: (old) miomimi@yandex.ru\n"
            + "to: (old) miomimi@yandex.ru\n"
            + "subject: (old) Test passing types to services\n"
            + "date: Thu, 18 Apr 2013 18:12:37 +0400\n"
            + "return-path: miomimi@yandex.ru\n"
            + "x-yandex-forward: ca88c29f319ac14af650235cbb787f44",
            mid,
            suid,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "quote");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "This is a test.\nAfter\nhttp://url.ru");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        // Yandex.Mail takes the last subject
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "(old) Test passing types to services");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, "<miomimi@yandex.ru>");
        String mioMail = "miomimi@yandex.ru\n";
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            mioMail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            mioMail);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, "miomimi@yandex.ru");
        doc.put(MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL, mioMail);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            mioMail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"\" <miomimi@yandex.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            mioMail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            mioMail);
        doc.put(MailMetaInfo.DRAFT, Boolean.TRUE.toString());
        doc.put(
            MailMetaInfo.MESSAGE_TYPE,
            "4 people 7 notification 12 greeting 15 s_datingsite "
            + "22 personalnews");
        doc.put(MailMetaInfo.X_URLS, "http://url.ru\n");
        String receivedDate = "1366294357";
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(
            MailMetaInfo.STID,
            "6429.355453075.5876650216377314753442158924");
        doc.put(MailMetaInfo.MID, mid);
        doc.put(MailMetaInfo.SUID, suid.toString());
        doc.put(MailMetaInfo.FID, "2170000700000016935");
        doc.put(MailMetaInfo.THREAD_ID_FIELD, "2170000000012799252");
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("with LMTP id ePOC319a"));
        String file = "types.eml";
        checkMail(file, json);
        doc.remove(MailMetaInfo.X_URLS);
        checkMail(file, json, "[extractor]\nno-xurls-types = 2, 14, 15");
    }

    @Test
    public void testFb2() throws Exception {
        String headers =
            "received: from mxback-qa.mail.yandex.net ([127.0.0.1])"
            + "\tby mxback-qa.mail.yandex.net with LMTP id 614OjGQj"
            + "\tfor <mailsearchtest@yandex.ru>; "
            + "Mon, 3 Jun 2013 17:06:01 +0400\n"
            + "received: from searchweb-qa.yandex.ru "
            + "(searchweb-qa.yandex.ru [95.108.252.70])"
            + "\tby mxback-qa.mail.yandex.net (nwsmtp/Yandex) with ESMTP "
            + "id jwg1jkovnx-61GuKEUn;\tMon,  3 Jun 2013 17:06:01 +0400\n"
            + "x-yandex-front: mxback-qa.mail.yandex.net\n"
            + "x-yandex-timemark: 1370264761\nx-yandex-spam: 1\n"
            + "authentication-results: mxback-qa.mail.yandex.net; "
            + "dkim=pass header.i=@yandex.ru\n"
            + "received: from dhcp-2-71-ben.yandex.net "
            + "(v10-166-220.yandex.net [84.201.166.220])"
            + "\tby searchweb-qa.yandex.ru (Yandex) with ESMTP id C911A2DF4CC"
            + "\tfor <mailsearchtest@yandex.ru>; Mon,  3 Jun 2013 17:06:00 "
            + "+0400 (MSK)\n"
            + "dkim-signature: v=1; a=rsa-sha256; c=relaxed/relaxed; "
            + "d=yandex.ru; s=mail;"
            + "\tt=1370264761; "
            + "bh=PE/5U7JZf2LLTFWp1/QmJyCl0XkKxm3bLQQGW0rxdHI=;"
            + "\th=From:To:Subject:Date;"
            + "\tb=U/hE5+TdVzWuGETsReMb1G1M0kuvO6Qql4bJKGKClJHSOyexE4qsgu1"
            + "BciT2QHtf5"
            + "\t /5bIO2fTyLWMt6iqtjIgVlQy6Zxjz4DaPJDtNgWCM0SZSuWXprtRLLAtu"
            + "PC0s6qPQ3\t +15EzUXDVaAx+Yq+p7C3R0NXYJ2eklha1ljuB6kM=\n"
            + "from: Anastasia Klishevich <terpsihora4@yandex.ru>\n"
            + "to: mailsearchtest@yandex.ru\n"
            + "subject: =?UTF-8?Q?=D0=94=D0=BE=D1=81=D1=82=D0=BE=D0=B5=D0=B2?="
            + " =?UTF-8?Q?=D1=81=D0=BA=D0=B8=D0=B9:_t?= =?UTF-8?Q?KrazlpIwGwKE"
            + "mj?=\n"
            + "mime-version: 1.0\n"
            + "content-type: Multipart/mixed;"
            + "\tboundary=\"----==--bound.55479.web28h.yandex.ru\"\n"
            + "message-id: <20130603130600.C911A2DF4CC@searchweb-qa.yandex.ru>"
            + "\ndate: Mon,  3 Jun 2013 17:06:00 +0400 (MSK)\n"
            + "return-path: terpsihora4@yandex.ru\n"
            + "x-yandex-forward: 6c3cccf2c9d93bc87f977dfa1346d539";
        Json json = new Json(headers, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + "\ncontent-transfer-encoding: 8bit"
            + "\ncontent-type: text/html; charset=koi8-r");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "--\nBest regards,\nAnastasia");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_KOI8_R);
        String subject = "Достоевский: tKrazlpIwGwKEmj";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        String mail = "mailsearchtest@yandex.ru";
        String fromComplete = "Anastasia Klishevich <terpsihora4@yandex.ru>";
        String fromName = "Anastasia Klishevich\n";
        String fromBare = "terpsihora4@yandex.ru\n";
        String receivedDate = "1370264761";
        String smtpId = "jwg1jkovnx-61GuKEUn";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            mail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            mail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, fromComplete);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));

        doc = json.createDoc(
            ONE_TWO,
            headers + "\ncontent-disposition: attachment;"
            + "\tfilename=\"Dostoevskij_-_Mal'chik_u_KHrista_na_elke .fb2\""
            + BASE64
            + "\ncontent-type: text/xml;"
            + "\tname=\"Dostoevskij_-_Mal'chik_u_KHrista_na_elke .fb2\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, "application/x-fictionbook+xml");
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/xml");
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Contains("Федор Достоевский\n"
                    + "МАЛЬЧИК У ХРИСТА НА ЕЛКЕ"),
                new Json.Contains("Но я романист, и, кажется, "
                    + "одну «историю» сам сочинил.")));
        doc.put(
            CommonFields.META,
            "Content-Type:application/x-fictionbook+xml");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            mail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            mail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, fromComplete);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "Dostoevskij_-_Mal'chik_u_KHrista_na_elke .fb2");
        doc.put(MailMetaInfo.ATTACHSIZE, 193447L);
        doc.put(MailMetaInfo.ATTACHTYPE, "fb2");
        doc.put(MailMetaInfo.MD5, "1BC7639050E263C89F5CD0193684918E");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://jurgennt.nextmail.ru\nhttp://fictionbook.ws\n");
        String description = "стало быть, лишь начинал профессию.";
        doc.put(CommonFields.AUTHOR, "Федор Михайлович Достоевский");
        doc.put(CommonFields.TITLE, "Мальчик у Христа на елке");
        doc.put(CommonFields.GENRE, "prose_classic\nprose_rus_classic");
        doc.put(CommonFields.DESCRIPTION, new Json.Contains(description));
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));
        checkMail("fb2.eml", json);
    }

    @Test
    public void testHtmlBadNamedEntity() throws Exception {
        Json json = new Json(CONTENT_HTML, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(
            MailMetaInfo.X_URLS,
            "http://pg.com/root?param1=1"
            + "&param2=param2&param3&flag\n");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "some body&\n"
            + "procter&gamble site: "
            + "http://pg.com/root?param1=1&param2=param2&param3&flag\n"
            + "eee&#x65#&#X65#&#101#\n&#5e;&#X4K;");
        doc.put(CommonFields.TITLE, "some title™");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        checkMail("html.named.ent.raw.txt", json);
    }

    @Test
    public void testJpg() throws Exception {
        String headers = "content-type: multipart/mixed; boundary=047d7b6dc"
            + "4a03c20fe04de554af9";
        Json json = new Json(headers, MID, SUID, MDB);
        String text = "My kitties are awesome!";
        Map<String, Object> doc = json.createDoc(
            ONE_ONE_ONE,
            headers
            + "\ncontent-type: multipart/alternative; boundary=047d7b6dc4a03c"
            + "20fa04de554af7\ncontent-type: text/plain; charset=UTF-8");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, text);
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);

        doc = json.createDoc(
            ONE_ONE_TWO,
            headers
            + "\ncontent-type: multipart/alternative; boundary=047d7b6dc4a03c2"
            + "0fa04de554af7\ncontent-type: text/html; charset=UTF-8");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, text);
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);

        doc = json.createDoc(
            ONE_TWO,
            headers + "\ncontent-type: image/jpeg; name=\"kotik.jpeg\""
            + "\ncontent-disposition: attachment; filename=\"kotik.jpeg\""
            + BASE64
            + "\nx-attachment-id: f_hhj7nbx10");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_JPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.HEIGHT, 600L);
        doc.put(CommonFields.WIDTH, 900L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, "3:2");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                CONTENT_TYPE_IMAGE_JPEG + "\nNumber of Components:3\n"
                + "Resolution Units:inch\n"
                + "Data Precision:8 bits\n"
                + "tiff:BitsPerSample:8\n"
                + "Compression Type:Baseline\n"
                + "Number of Tables:4 Huffman tables\n"
                + "Component 1:Y component: Quantization table 0, Sampling f"
                + "actors 1 horiz/1 vert\n"
                + "Component 2:Cb component: Quantization table 1, "
                + "Sampling factors 1 horiz/1 vert\n"
                + "Component 3:Cr component: Quantization table 1, Sampling "
                + "factors 1 horiz/1 vert\n"
                + "Thumbnail Height Pixels:0\nThumbnail Width Pixels:0\n"
                + "X Resolution:240 dots\n"
                + "Y Resolution:240 dots"));
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "kotik.jpeg");
        doc.put(MailMetaInfo.ATTACHSIZE, 100303L);
        doc.put(MailMetaInfo.ATTACHTYPE, JPEG);
        doc.put(MailMetaInfo.MD5, "CD4967B21B63E44A0C728F67E12BAAB4");
        doc = json.createDoc(
            ONE_THREE,
            headers
            + "\ncontent-type: image/jpeg; name=\"kotiki.jpg\""
            + "\ncontent-disposition: attachment; filename=\"kotiki.jpg\""
            + BASE64
            + "\nx-attachment-id: f_hhj7nbxo1");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_JPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.HEIGHT, 480L);
        doc.put(CommonFields.WIDTH, 640L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, RATIO_4X3);
        doc.put(CommonFields.COMMENT, "Lavc53.61.100");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                CONTENT_TYPE_IMAGE_JPEG
                + "\nNumber of Components:3\nResolution Units:none\n"
                + "Data Precision:8 bits\ntiff:BitsPerSample:8\n"
                + "Compression Type:Baseline\nComponent 1:Y component: "
                + "Quantization table 0, Sampling factors 2 horiz/2 vert\n"
                + "Component 2:Cb component: Quantization table 0, "
                + "Sampling factors 1 horiz/1 vert\nComponent 3:Cr component: "
                + "Quantization table 0, "
                + "Sampling factors 1 horiz/1 vert\nX Resolution:1 dot\n"
                + "JPEG Comment:Lavc53.61.100"
                + "\nThumbnail Height Pixels:0\nThumbnail Width Pixels:0\n"
                + "Y Resolution:1 dot\nNumber of Tables:4 Huffman tables"));
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "kotiki.jpg");
        doc.put(MailMetaInfo.ATTACHSIZE, 15968L);
        doc.put(MailMetaInfo.ATTACHTYPE, JPEG);
        doc.put(MailMetaInfo.MD5, "40905B870FC8522DBB94749254E57844");
        checkMail("kitties.eml", json);
    }

    @Test
    public void testBadEncoding() throws Exception {
        Json json = new Json(
            "content-type: text/plain; charset=\"utf-8\" hello, world",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        checkMail("bad-encoding.raw.txt", json);
    }

    @Test
    public void testVeryBadEncoding() throws Exception {
        Json json = new Json(
            "content-type: text/plain; charset=\"hello\" world",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Hello, world!");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        checkMail("very-bad-encoding.raw.txt", json);
    }

    @Test
    public void testNoEncoding() throws Exception {
        Json json = new Json(
            "content-type: text/plain; charset=\"no\"",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "No encoding for the old man");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        checkMail("no-encoding.raw.txt", json);
    }

    @Test
    public void testRfc822Root() throws Exception {
        String headers =
            "to: Vasya Pupkin <vasya@pupkin.com>\n" + HEADER_MESSAGE_RFC822;
        Json json = new Json(headers, MID, SUID, MDB);
        String to = "vasya@pupkin.com\n";
        String from = "putin@voffka.com\n";
        String toComplete = "Vasya Pupkin <vasya@pupkin.com>";
        String toDisplayName = "Vasya Pupkin\n";
        String fromComplete = "Vovka Putin <putin@voffka.com>";
        String fromDisplayName = "Vovka Putin\n";
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.CONTENT_TYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO,
            toComplete);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toDisplayName);
        doc.put(MailMetaInfo.ATTACHNAME, NestedMessageHandler.EML);
        doc.put(MailMetaInfo.ATTACHSIZE, 0L);
        doc.put(MailMetaInfo.ATTACHTYPE, NestedMessageHandler.ATTACHTYPE);

        doc = json.createDoc(
            ONE_ONE,
            headers + "from: Vovka Putin <putin@voffka.com>\n");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO,
            toComplete);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toDisplayName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            fromComplete);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromDisplayName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Remember, remember the fifth of November");
        checkMail("rfc822.root.txt", json);
    }

    @Test
    public void testRfc822WithAttach() throws Exception {
        Json json = new Json(HEADER_MESSAGE_RFC822, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.CONTENT_TYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(MailMetaInfo.ATTACHNAME, NestedMessageHandler.EML);
        doc.put(MailMetaInfo.ATTACHSIZE, 0L);
        doc.put(MailMetaInfo.ATTACHTYPE, NestedMessageHandler.ATTACHTYPE);

        doc = json.createDoc(
            ONE_ONE_ONE,
            HEADER_MESSAGE_RFC822
            + "content-type: multipart/mixed; boundary=\"mixed2\""
            + "\ncontent-type: text/plain; name=\"simple.txt\"");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "This is an attach");
        doc.put(MailMetaInfo.ATTACHNAME, "simple.txt");
        doc.put(MailMetaInfo.ATTACHSIZE, 17L);
        doc.put(MailMetaInfo.ATTACHTYPE, "txt text");
        doc.put(MailMetaInfo.MD5, "24298514B7BBC17C0137AA2132BE966E");
        checkMail("rfc822.root.with-attach.txt", json);

        doc.put(CommonFields.BODY_TEXT, "This i");
        checkMail(
            "rfc822.root.with-attach.txt",
            json,
            "extractor.max-part-length = 6");
    }

    @Test
    public void testRfc822BoundlessAttach() throws Exception {
        String headers =
            "from: \"Me\" <me@ya.ru>\n"
            + "content-type: multipart/mixed; boundary=\"rootbound\"";
        Json json = new Json(headers, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        String from = "\"Me\" <me@ya.ru>";
        String fromBare = "me@ya.ru\n";
        String fromName = "Me\n";
        String fromNormalized = "me@yandex.ru\n";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);

        String nestedHeaders =
            headers
            + "\ncontent-type: message/rfc822; name=\"rfc822.eml\"";
        String attachname = "rfc822.eml";

        doc = json.createDoc(ONE_TWO, nestedHeaders);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.CONTENT_TYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(MailMetaInfo.ATTACHNAME, attachname);
        doc.put(MailMetaInfo.ATTACHSIZE, 0L);
        doc.put(MailMetaInfo.ATTACHTYPE, NestedMessageHandler.ATTACHTYPE);

        String nestedHeaders2 =
            nestedHeaders
            + "\ncontent-type: message/rfc822; name=\"rfc822attach.eml\"";
        doc = json.createDoc("1.2.1", nestedHeaders2);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.CONTENT_TYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.ATTACHNAME, attachname);
        doc.put(MailMetaInfo.ATTACHSIZE, 0L);
        doc.put(MailMetaInfo.ATTACHTYPE, NestedMessageHandler.ATTACHTYPE);

        String nestedHeaders3 =
            nestedHeaders2 + "\nfrom: \"Me again\" <me@mail.ru>";
        doc = json.createDoc("1.2.1.1", nestedHeaders3);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromBare);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.BODY_TEXT, "Hello again");
        doc.put(MailMetaInfo.ATTACHNAME, attachname);
        doc.put(MailMetaInfo.ATTACHSIZE, 11L);
        doc.put(MailMetaInfo.ATTACHTYPE, "txt text eml mime");
        doc.put(MailMetaInfo.MD5, "3D67B96CDE18C245B65A83ECAFF2C906");
        checkMail("rfc822.boundless.attach.txt", json);
    }

    @Test
    public void testMalformed() throws Exception {
        Json json = new Json(HEADER_TEXT_PLAIN_UTF_8, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Привет, мир Как поживаешь?");
        checkMail("malformed.raw.txt", json);
    }

    // CSOFF: MethodLength
    @Test
    public void testMp3() throws Exception {
        String headers =
            "return-path: <>\nx-original-to: "
            + "service@services.mail.yandex.net\ndelivered-to: "
            + "search@service1h.mail.yandex.net\nreceived: from mxback8h.m"
            + "ail.yandex.net (mxback8h.mail.yandex.net [93.158.130.147])\tby "
            + "service1h.mail.yandex.net (Yandex) with ESMTP id 0EE972140E0B\t"
            + "for <service@services.mail.yandex.net>; "
            + "Tue, 11 Jun 2013 00:22:30 +0400 (MSK)\n"
            + "received: from mxback8h.mail.yandex.net ([127.0.0.1])\t"
            + "by mxback8h.mail.yandex.net with LMTP id MOcONKti\t"
            + "for <anttanya@yandex.ru>; Tue, 11 Jun 2013 00:22:24 +0400\n"
            + "received: from web2h.yandex.ru (web2h.yandex.ru "
            + "[84.201.186.31])\tby mxback8h.mail.yandex.net (nwsmtp/Yandex) "
            + "with ESMTP id ODkQBdK5rP-MKuiU7c1;\t"
            + "Tue, 11 Jun 2013 00:22:20 +0400\n"
            + "x-yandex-front: mxback8h.mail.yandex.net\n"
            + "x-yandex-timemark: 1370895740\n"
            + "authentication-results: mxback8h.mail.yandex.net; dkim=pass "
            + "header.i=@yandex.ru\n"
            + "received: from 127.0.0.1 (localhost.localdomain [127.0.0.1])\t"
            + "by web2h.yandex.ru (Yandex) with ESMTP id BFA4B4EF002E;\t"
            + "Tue, 11 Jun 2013 00:22:18 +0400 (MSK)\ndkim-signature: "
            + "v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail;\tt="
            + "1370895739; bh=/zUzcFliqjNVexEFswGhSIF2xJnwGoPdxwiUOjD5Jow=;\t"
            + "h=From:To:Subject:Date;\t"
            + "b=As/gcssrI/hygajRpM913ZtwS1gJi0OR0KM3Qj26hVqBzgxC8WRtnGMG1OyXk"
            + "q7BW\t /qVFW0IBj3smk3kY1opCpEQ8hKrjcTu4moct2klXvqrQNh9H3Kq3XoKn"
            + "+En8vFKPyw\t K4K/3FbYUm63pMlrIfA3hUEHUxAHrSgfwvNHhowk=\n"
            + "x-yandex-spam: 1\nx-yandex-front: web2h.yandex.ru\n"
            + "x-yandex-timemark: 1370895738\n"
            + "received: from [91.196.99.139] ([91.196.99.139]) by "
            + "web2h.yandex.ru with HTTP;\tTue, 11 Jun 2013 00:22:15 +0400\n"
            + "from: =?koi8-r?B?9Mndxc7LzyDz18XUwQ==?= <vetish@yandex.ru>\n"
            + "envelope-from: vetish@yandex.kz\nto: "
            + "=?koi8-r?B?4c7Uz9vLyc7BIPTB1NjRzsE=?= <anttanya@yandex.ru>\n"
            + "subject: =?koi8-r?B?zcDaycvB?=\nmime-version: 1.0\n"
            + "message-id: <966051370895735@web2h.yandex.ru>\n"
            + "x-mailer: Yamail [ http://yandex.ru ] 5.0\n"
            + "date: Tue, 11 Jun 2013 00:22:15 +0400\n"
            + "content-type: multipart/mixed;\tboundary=\"----==--bound.96609."
            + "web2h.yandex.ru\"\n"
            + "x-yandex-forward: cf1a82f0de81e13e01945cf1c6836fe1\n";
        Object meta = new Json.Headers(
            "xmpDM:audioChannelType:Stereo\n"
            + "channels:2\n"
            + "xmpDM:audioSampleRate:44100\n"
            + "version:MPEG 3 Layer III Version 1\n"
            + "xmpDM:audioCompressor:MP3\n"
            + "samplerate:44100\n"
            + "Content-Type:audio/mpeg");
        Json json = new Json(headers, MID, SUID, MDB);
        String smtpId = "ODkQBdK5rP-MKuiU7c1";
        String fid = "2340000580003231309";
        String stid = "7282.57098615.90618606412080250865190200155";
        String tid = "2340000002467998308";
        String receivedDate = "1370895750";
        String to =
            "\"Антошкина Татьяна\" <anttanya@yandex.ru>";
        String toEmail = "anttanya@yandex.ru";
        String toName = "Антошкина Татьяна\n";
        String from = "\"Тищенко Света\" <vetish@yandex.ru>";
        String fromEmail = "vetish@yandex.ru";
        String fromName = "Тищенко Света\n";
        String subject = "мюзика";
        String type = "4 people";
        String artist = "Macy Gray";
        String album = "Big";
        String genre = "Soul And R&B";
        String released = "2007";
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "content-disposition: inline;\tfilename=\"narod_attachment_links"
            + ".html\"\ncontent-transfer-encoding: 7bit\n"
            + "content-type: text/html; charset=UTF-8;\tname=\"narod_"
            + "attachment_links.html\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HAS_ATTACHMENTS, Boolean.TRUE.toString());
        doc.put(MailMetaInfo.MESSAGE_TYPE, type);
        doc.put(MailMetaInfo.FID, fid);
        doc.put(MailMetaInfo.THREAD_ID_FIELD, tid);
        doc.put(MailMetaInfo.STID, stid);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, fromEmail);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "06 Ghetto Love.mp3 (7616284)\n07 One for Me.mp3 (10040452)\n"
            + "08 Strange Behavior.mp3 (8687428)\n09 Slowly.mp3 (9459988)\n"
            + "10 Get Out.mp3 (9725164)\n"
            + "11 Treat Me Like Your Money.mp3 (8362744)\n"
            + "12 Everybody.mp3 (7850140)\n"
            + "01 Finally Made Me Happy.mp3 (9791980)\n"
            + "02 Shoo Be Doo.mp3 (9868192)");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://yadi.sk/d/LDL4KWZl5gyAD\n"
            + "http://yadi.sk/d/7-cUP0fL5gyBO\n"
            + "http://yadi.sk/d/98tp9K1p5gyC6\n"
            + "http://yadi.sk/d/eGWL3NCB5gyCk\n"
            + "http://yadi.sk/d/__6nG9Q85gyDs\n"
            + "http://yadi.sk/d/l7_nRS5M5gyEr\n"
            + "http://yadi.sk/d/lAtpqZsj5gyFS\n"
            + "http://yadi.sk/d/fRYUaSJ_5gyGY\n"
            + "http://yadi.sk/d/-KRsQkve5gyH9\n");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE);
        doc.put(MailMetaInfo.ATTACHNAME, "narod_attachment_links.html");
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));

        doc = json.createDoc(
            ONE_TWO,
            headers
            + "content-disposition: attachment;\tfilename=\"03 What I "
            + "Gotta Do.mp3\""
            + BASE64
            + "\ncontent-type: audio/mpeg;\tname=\"03 What I Gotta Do.mp3\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.FID, fid);
        doc.put(MailMetaInfo.THREAD_ID_FIELD, tid);
        doc.put(MailMetaInfo.STID, stid);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.HAS_ATTACHMENTS, Boolean.TRUE.toString());
        doc.put(MailMetaInfo.MESSAGE_TYPE, type);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, fromEmail);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, AUDIO_MPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            "What I Gotta Do\nWhat I Gotta Do\nMacy Gray\nBig, track 3\n2007\n"
            + "Soul And R&B\n120.6319");
        doc.put(CommonFields.META, meta);
        doc.put(CommonFields.GENRE, genre);
        doc.put(CommonFields.ALBUM, album);
        doc.put(CommonFields.RELEASED, released);
        doc.put(CommonFields.ARTIST, artist);
        doc.put(CommonFields.AUTHOR, artist);
        doc.put(
            CommonFields.COMPOSER,
            "Caleb Speir/Jason Villaroman/Jeremy Ruzumna/Josh Lopez"
            + "/Natalie Hinds");
        doc.put(CommonFields.DURATION, 121L);
        doc.put(CommonFields.TRACK_NUMBER, 3L);
        doc.put(CommonFields.TITLE, "What I Gotta Do");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "03 What I Gotta Do.mp3");
        doc.put(MailMetaInfo.ATTACHTYPE, MP3);
        doc.put(MailMetaInfo.ATTACHSIZE, 7306809L);
        doc.put(MailMetaInfo.MD5, "BB14CC02B82049A015FEE55436568266");
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));

        doc = json.createDoc(
            ONE_THREE,
            headers
            + "content-disposition: attachment;\tfilename=\"04 Okay.mp3\""
            + BASE64
            + "\ncontent-type: audio/mpeg;\tname=\"04 Okay.mp3\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.THREAD_ID_FIELD, tid);
        doc.put(MailMetaInfo.STID, stid);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.HAS_ATTACHMENTS, Boolean.TRUE.toString());
        doc.put(MailMetaInfo.MESSAGE_TYPE, type);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, fromEmail);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.FID, fid);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, AUDIO_MPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            "Okay\nOkay\nMacy Gray\nBig, track 4\n2007\n"
            + "Soul And R&B\n172.23663");
        doc.put(CommonFields.GENRE, genre);
        doc.put(CommonFields.ALBUM, album);
        doc.put(CommonFields.RELEASED, released);
        doc.put(CommonFields.ARTIST, artist);
        doc.put(CommonFields.AUTHOR, artist);
        doc.put(
            CommonFields.COMPOSER,
            "Caleb Speir/Justin Timberlake/Natalie Hinds/Will Adams");
        doc.put(CommonFields.DURATION, 173L);
        doc.put(CommonFields.TRACK_NUMBER, 4L);
        doc.put(CommonFields.TITLE, "Okay");
        doc.put(CommonFields.META, meta);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "04 Okay.mp3");
        doc.put(MailMetaInfo.ATTACHTYPE, MP3);
        doc.put(MailMetaInfo.ATTACHSIZE, 9480612L);
        doc.put(MailMetaInfo.MD5, "6BFD91DEA5431F48E67923DCEB60E421");
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));

        doc = json.createDoc(
            ONE_FOUR,
            headers
            + "content-disposition: attachment;\tfilename=\"05 Glad You're "
            + "Here.mp3\""
            + BASE64
            + "\ncontent-type: audio/mpeg;\tname=\"05 Glad You're Here.mp3\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.STID, stid);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.HAS_ATTACHMENTS, Boolean.TRUE.toString());
        doc.put(MailMetaInfo.MESSAGE_TYPE, type);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, fromEmail);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.FID, fid);
        doc.put(MailMetaInfo.THREAD_ID_FIELD, tid);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, AUDIO_MPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            "Glad You're Here\nGlad You're Here\nFergie/Macy Gray\n"
            + "Big, track 5\n2007\nSoul And R&B\n115.39513");
        doc.put(CommonFields.GENRE, genre);
        doc.put(CommonFields.ALBUM, album);
        doc.put(CommonFields.RELEASED, released);
        doc.put(CommonFields.ARTIST, "Fergie/Macy Gray\nMacy Gray");
        doc.put(CommonFields.AUTHOR, "Fergie/Macy Gray");
        doc.put(
            CommonFields.COMPOSER,
            "Cassandra O'Neil/Joe Solo/Justin Meldal-Johnson/Natalie Hinds/"
            + "Trevor Lawrence");
        doc.put(CommonFields.DURATION, 116L);
        doc.put(CommonFields.TRACK_NUMBER, 5L);
        doc.put(CommonFields.TITLE, "Glad You're Here");
        doc.put(CommonFields.META, meta);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "05 Glad You're Here.mp3");
        doc.put(MailMetaInfo.ATTACHTYPE, MP3);
        doc.put(MailMetaInfo.ATTACHSIZE, 7031154L);
        doc.put(MailMetaInfo.MD5, "2ABD01AE9071EDC87DB1313F7A460BBA");
        doc.put(
            MailMetaInfo.RECEIVED_PARSE_ERROR,
            new Json.Contains("doesn't belong to Yandex"));

        checkMail("online.mp3.eml", json);
    }
    // CSON: MethodLength

    // CSOFF: MethodLength
    @Test
    public void testBadPsd() throws Exception {
        String headers =
            "received: from mxfront4o.mail.yandex.net "
            + "([127.0.0.1])\tby mxfront4o.mail.yandex.net with LMTP id ww6i4I"
            + "JP\tfor <a@annachapman.ru>; Thu, 26 Sep 2013 15:58:58 +0400\n"
            + "received: from f107.i.mail.ru (f107.i.mail.ru [94.100.178.76])"
            + "\tby mxfront4o.mail.yandex.net (nwsmtp/Yandex) with ESMTP id px"
            + "UsyC3CTk-wwImebMg;\tThu, 26 Sep 2013 15:58:58 +0400\n"
            + "x-yandex-front: mxfront4o.mail.yandex.net\n"
            + "x-yandex-timemark: 1380196738\n"
            + "authentication-results: mxfront4o.mail.yandex.net; spf=pass (mx"
            + "front4o.mail.yandex.net: domain of mail.ru designates 94.100.17"
            + "8.76 as permitted sender) smtp.mail=maria.frid@mail.ru; dkim=pa"
            + "ss header.i=@mail.ru\n"
            + "x-yandex-spam: 1\ndkim-signature: v=1; a=rsa-sha256; q=dns/txt;"
            + " c=relaxed/relaxed; d=mail.ru; s=mail2;\th=References:In-Reply-"
            + "To:Content-Type:Message-ID:Reply-To:Date:Mime-Version:Subject:T"
            + "o:From; bh=HzWRtoB3J3zjilovdh+X/VPkYJxX0CGhefs3LNFhAEY=;\tb=n+x"
            + "l4+nnbrl2Q7yJy31v+rq+KnGlp8ymwGpDynIz+A3QEJHKXRhDqQ7nQnwxt5xMTx"
            + "zcTm5Chzt3P2t9L5P7zdH4EEB4xE2fy9YiMVkRgadVRuELWFywCcU2NQ4zjHbg0"
            + "gPug3ejakRnbFbF3s8GgIjW0O30a+fbfu8ltrQx9aM=;\n"
            + "received: from mail by f107.i.mail.ru with local (envelope-from"
            + " <maria.frid@mail.ru>)\tid 1VPADh-00077p-VM\tfor a@annachapman."
            + "ru; Thu, 26 Sep 2013 15:58:58 +0400\nreceived: from [94.79.36.2"
            + "18] by e.mail.ru with HTTP;\tThu, 26 Sep 2013 15:58:57 +0400\n"
            + "from: =?UTF-8?B?bWFyaWEgZnJpZA==?= <maria.frid@mail.ru>\n"
            + "to: =?UTF-8?B?QW5uYQ==?= <a@annachapman.ru>\n"
            + "disposition-notification-to: =?UTF-8?B?bWFyaWEgZnJpZA==?= <mari"
            + "a.frid@mail.ru>\n"
            + "subject: =?UTF-8?B?UmU6INGA0LDQsdC+0YLRiyDQsiDRhNC+0YDQvNCw0YLQ"
            + "tSBQU0Q=?=\nmime-version: 1.0\nx-mailer: Mail.Ru Mailer 1.0\n"
            + "x-originating-ip: [94.79.36.218]\n"
            + "date: Thu, 26 Sep 2013 15:58:57 +0400\n"
            + "reply-to: =?UTF-8?B?bWFyaWEgZnJpZA==?= <maria.frid@mail.ru>\n"
            + "x-priority: 3 (Normal)\n"
            + "message-id: <1380196737.772172790@f107.i.mail.ru>\n"
            + "content-type: multipart/mixed;\tboundary=\"----lYB40I1S-GlNm71c"
            + "AsuFLe2Bh:1380196737\"\nx-mras: Ok\nx-spam: undefined\n"
            + "in-reply-to: <31081813-50DD-40FE-AC03-C5AA83E7F97E@annachapman."
            + "ru>\nreferences: <90AB1FAA-5EE5-4DC4-BBAF-E1BECD7FBE32@annachap"
            + "man.ru> <31081813-50DD-40FE-AC03-C5AA83E7F97E@annachapman.ru>\n"
            + "return-path: maria.frid@mail.ru\n"
            + "x-yandex-forward: 71a44a162081201b7cfc2fc1415b103a\n";
        Json json = new Json(headers, MID, SUID, MDB);
        String smtpId = "pxUsyC3CTk-wwImebMg";
        String receivedDate = "1380196738";
        String to = "Anna <a@annachapman.ru>";
        String toEmail = "a@annachapman.ru";
        String toName = "Anna\n";
        String from = "maria frid <maria.frid@mail.ru>";
        String fromEmail = "maria.frid@mail.ru";
        String fromName = "maria frid\n";
        String subject = "Re: работы в формате PSD";
        String md5 = "80C16CC96CA6EEEBA2C2C4E0547DA60B";
        String md5Small = "B99405C66C68A44982BBC54C0BEFD185";
        String text = "Странно получается, при пересылке все эти работы всего "
            + "по 40 байт, а у меня на компе каждая по 900 МБ..";
        Map<String, Object> doc = json.createDoc(
            ONE_ONE_ONE,
            headers
            + "content-type: multipart/alternative;\tboundary=\"--ALT--lYB40I"
            + "1S1380196737\"\ncontent-type: text/plain; charset=utf-8"
            + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, text);

        doc = json.createDoc(
            ONE_ONE_TWO,
            headers
            + "content-type: multipart/alternative;\tboundary=\"--ALT--lYB40I1"
            + "S1380196737\"\n"
            + "content-type: text/html; charset=utf-8" + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, text);

        doc = json.createDoc(
            ONE_TWO,
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQsNC90YIxLnBzZAou?"
            + "=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17717L);
        doc.put(CommonFields.WIDTH, 17717L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "кант1.psd\n.");

        doc = json.createDoc(
            ONE_THREE,
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQsNC90YIyLnBzZ"
            + "A==?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17717L);
        doc.put(CommonFields.WIDTH, 17717L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "кант2.psd");

        doc = json.createDoc(
            ONE_FOUR,
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQsNC90YIzLnBz"
            + "ZA==?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17717L);
        doc.put(CommonFields.WIDTH, 17717L);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "кант3.psd");

        doc = json.createDoc(
            ONE_FIVE,
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQut"
            + "GAMS5wc2Q=?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(CommonFields.ORIENTATION, CommonFields.PORTRAIT);
        doc.put(CommonFields.HEIGHT, 17812L);
        doc.put(CommonFields.WIDTH, 17724L);
        doc.put(CommonFields.RATIO, "4453:4431");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, "8271D6371DD3DF7E8C49796179F07386");
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени кр1.psd");

        String file = "badpsd.eml";
        checkMail(file, json, "\nextractor.max-parts = 6");

        doc = json.createDoc(
            "1.6",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQutG"
            + "AMi5wc2Q=?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени кр2.psd");

        doc = json.createDoc(
            "1.7",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQv9C+0"
            + "LvRg9C60YAxLnBzZA==?=\"\ncontent-disposition: attachment"
            + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.PORTRAIT);
        doc.put(CommonFields.HEIGHT, 17713L);
        doc.put(CommonFields.WIDTH, 17658L);
        doc.put(CommonFields.RATIO, "17713:17658");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, "6A497701F06E2D8617684BD1841DCDE2");
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени полукр1.psd");

        doc = json.createDoc(
            "1.8",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQv9C+"
            + "0LvRg9C60YAyLnBzZA==?=\"\ncontent-disposition: attachment"
            + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17717L);
        doc.put(CommonFields.RATIO, "17717:17716");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, "E919951B16A6A41CED93AC83D9B578D9");
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени полукр2.psd");

        doc = json.createDoc(
            "1.9",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQv9GAM"
            + "S5wc2Q=?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени пр1.psd");

        doc = json.createDoc(
            "1.10",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0L7Qu9C10L3QuCDQv9GA"
            + "Mi5wc2Q=?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "олени пр2.psd");

        doc = json.createDoc(
            "1.11",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC"
            + "70Lgg0LrRgDEucHNk?=\"\ncontent-disposition: attachment"
            + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(CommonFields.PARSED, true);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли кр1.psd");

        doc = json.createDoc(
            "1.12",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC70L"
            + "gg0LrRgDIucHNk?=\"\ncontent-disposition: attachment"
            + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли кр2.psd");

        doc = json.createDoc(
            "1.13",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC70"
            + "Lgg0L/QvtC70YPQutGAMS5wc2Q=?=\"\ncontent"
            + "-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли полукр1.psd");

        doc = json.createDoc(
            "1.14",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC7"
            + "0Lgg0L/QvtC70YPQutGAMi5wc2Q=?=\"\ncontent-"
            + "disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли полукр2.psd");

        doc = json.createDoc(
            "1.15",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC70Lgg0"
            + "L/RgDEucHNk?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли пр1.psd");

        doc = json.createDoc(
            "1.16",
            headers
            + "content-type: image/psd; name=\"=?UTF-8?B?0LrQvtGA0LDQsdC70Lgg"
            + "0L/RgDIucHNk?=\"\ncontent-disposition: attachment" + BASE64);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(CommonFields.MIMETYPE, PHOTOSHOP);
        doc.put(MailMetaInfo.CONTENT_TYPE, "image/psd");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.META, PHOTOSHOP_META);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.HEIGHT, 17716L);
        doc.put(CommonFields.WIDTH, 17716L);
        doc.put(CommonFields.RATIO, SQUARE);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 40L);
        doc.put(MailMetaInfo.MD5, md5Small);
        doc.put(MailMetaInfo.ATTACHTYPE, PSD);
        doc.put(MailMetaInfo.ATTACHNAME, "корабли пр2.psd");

        checkMail(file, json);
    }

    @Test
    public void testTurkish() throws Exception {
        String headers =
            "received: from mxback8o.mail.yandex.net ([127.0.0.1])\t"
            + "by mxback8o.mail.yandex.net with LMTP id T8VWBPWt\tfor "
            + "<ditekrestorasyon@yandex.ru>; Wed, 11 Dec 2013 06:29:08 +0400\n"
            + "received: from rpop2o.mail.yandex.net (rpop2o.mail.yandex.net "
            + "[37.140.190.59])\tby mxback8o.mail.yandex.net (nwsmtp/Yandex) "
            + "with ESMTP id iYqKDhjD3O-T8Veqosl;\tWed, 11 Dec 2013 06:29:08 "
            + "+0400\nx-yandex-front: mxback8o.mail.yandex.net\n"
            + "x-yandex-timemark: 1386728948\nx-yandex-spam: 1\n"
            + "x-yandex-pop-server: pop3.live.com\n"
            + "x-yandex-rpop-id: 2240000000000118165\n"
            + "x-yandex-rpop-info: mkalkan@pop3.live.com\n"
            + "received: from mkalkan@pop3.live.com ([65.55.162.199])\tby "
            + "mail.yandex.ru with POP3 id sSS8wihYna6x\tfor 625959104@2240000"
            + "000000118165; Wed, 11 Dec 2013 06:29:08 +0400\n"
            + "x-store-info: i1mvqhPkdZwu3DNZ/OabHTcl0lVw0VvWbffqmtr5uy7wR6xR4"
            + "4D5BDmrMLushhpbV3RR2nXfQsEF6PA1Ew6wGpwMVadb8PmTck8hIkxgbqB1aIIs"
            + "urz7s66zSQkhgI0bUjy4wtVda9Y=\nx-sid-pra: cfeh_"
            + "donemsel_hesap_bildirim_cetveli@cignafinans.com.tr\n"
            + "x-auth-result: NONE\nx-sid-result: NONE\n"
            + "x-message-status: n:n\nx-message-delivery: "
            + "Vj0xLjE7dXM9MTtsPTE7YT0xO0Q9MTtHRD0xO1NDTD0w\nx-message-info: "
            + "/Afko6AgMSwAiQ52xyLpA8t1y2dUPNRjyhizkFCm61MtrF2FY+0IA0r54wdpZkr"
            + "V0dsd+ZsRpDzvjCLqzQPnJOduXhOseKk5I/3exqv++Ixj7ZsHxPFu3Xo/TMBWOZ"
            + "F+Y+PC86I5F+m+pPNIl76HUmXzfTEzAkmGint9vdaDggWPtmePCjRKTx4m4xJWf"
            + "OAf7BRwo77dieW01bnyFFeotzQ1iRpn7K2X\n"
            + "received: from outconn.finansbank.com.tr ([62.108.64.51]) by "
            + "COL0-MC3-F31.Col0.hotmail.com with Microsoft "
            + "SMTPSVC(6.0.3790.4900);\t Tue, 10 Dec 2013 18:26:21 -0800\n"
            + "received: from feprddb01.finansemeklilik.com.tr (10.81.226.21) "
            + "by outconn.finansbank.com.tr (62.108.64.51) with Microsoft SMTP"
            + " Server id 14.1.323.0; Wed, 11 Dec 2013 04:26:18 +0200\n"
            + "message-id: <-1027248639.1386728779763.JavaMail.javamailuser@lo"
            + "calhost>\ndate: Wed, 11 Dec 2013 02:26:19 +0000\n"
            + "from: \"CFEH Müsteri Hizmetleri\"\t"
            + "<cfeh_donemsel_hesap_bildirim_cetveli@cignafinans.com.tr>\n"
            + "to: <mkalkan@hotmail.com.tr>\n"
            + "subject: =?ISO-8859-9?Q?D=F6nemsel_Hesap_Bildirim_Cetveli?=\n"
            + "mime-version: 1.0\ncontent-type: multipart/mixed;\tboundary=\""
            + "----=_Part_52_-282062161.1386728779745\"\n"
            + "j-mailer: FEHAS-JMailer\nx-originalarrivaltime: 11 Dec 2013 "
            + "02:26:21.0394 (UTC) FILETIME=[61646320:01CEF618]\n"
            + "return-path: ditekrestorasyon@yandex.ru\n"
            + "x-yandex-forward: 15b519a054c4f4cf815c6e734fa98ab0";
        String to = "mkalkan@hotmail.com.tr";
        String from = "\"CFEH Müsteri Hizmetleri\"\t"
            + "<cfeh_donemsel_hesap_bildirim_cetveli@cignafinans.com.tr>";
        String fromEmail =
            "cfeh_donemsel_hesap_bildirim_cetveli@cignafinans.com.tr\n";
        String fromName = "CFEH Müsteri Hizmetleri\n";
        String subject = "Dönemsel Hesap Bildirim Cetveli";
        String received = "1386728948";
        String smtpId = "iYqKDhjD3O-T8Veqosl";
        Object receivedParseError =
            new Json.Contains("from outconn.finansbank.com.tr");
        Json json = new Json(headers, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "\ncontent-type: text/html; charset=\"ISO-8859-9\""
            + "\ncontent-transfer-encoding: quoted-printable");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, received);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_PARSE_ERROR, receivedParseError);
        doc.put(MailMetaInfo.RECEIVED_DATE, received);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + to + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.X_URLS, "http://www.cignafinans.com.tr\n");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Sayın MUSTAFA KALKAN\n"
            + "Sahibi olduğunuz bireysel emeklilik sözleşmesi için "
            + "hazırlanmış olan 11/03/2013 - 11/12/2013 hesap dönemine "
            + "ait dönemsel hesap bildirim cetveliniz ekli dosyada bilginize "
            + "sunulmuştur.\nİyi günler dileriz,\n"
            + "Cigna Finans Emeklilik ve Hayat A.Ş.\n"
            + "Çağrı Merkezi: 444 0 984\n"
            + "www.cignafinans.com.tr");
        doc.put(
            CommonFields.META,
            "Content-Type:text/html; charset=ISO-8859-9");
        doc.put(MailMetaInfo.X_URLS, "http://www.cignafinans.com.tr\n");
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-type: application/octet-stream; name=\"737109.pdf\""
            + "\ncontent-disposition: attachment; filename=\"737109.pdf\""
            + "\ncontent-transfer-encoding: 7bit"
            + "\ncontent-description: 737109.pdf");
        doc.put(CommonFields.PARSED, true);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, received);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_PARSE_ERROR, receivedParseError);
        doc.put(MailMetaInfo.RECEIVED_DATE, received);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + to + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(CommonFields.MIMETYPE, APPLICATION_PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/octet-stream");
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("EYF: Emeklilik Yatırım Fonu\n"));
        doc.put(MailMetaInfo.X_URLS, "http://www.cignafinans.com.tr\n");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                CONTENT_TYPE_PDF
                + "\npdf:PDFVersion:1.4\n"
                + "pdf:docinfo:created:2013-12-11T04:26:17Z\n"
                + "pdf:docinfo:creator:Oracle Reports\n"
                + "pdf:docinfo:creator_tool:Oracle10gR2 AS Reports Services\n"
                + "pdf:docinfo:modified:2013-12-11T04:26:17Z\n"
                + "pdf:docinfo:producer:Oracle PDF driver\n"
                + "pdf:encrypted:false\n"
                + "pdf:hasMarkedContent:false\npdf:hasXFA:false\n"
                + "pdf:hasXMP:false\n"
                + "dc:format:application/pdf; version=1.4"
                + PdfBoxTest.PDFBOX_META));
        doc.put(CommonFields.AUTHOR, "Oracle Reports");
        doc.put(CommonFields.PRODUCER, "Oracle PDF driver");
        doc.put(CommonFields.TOOL, "Oracle10gR2 AS Reports Services");
        doc.put(CommonFields.CREATED, 1386735977L);
        doc.put(CommonFields.MODIFIED, 1386735977L);
        doc.put(CommonFields.PAGES, 2L);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "737109.pdf");
        doc.put(MailMetaInfo.ATTACHSIZE, 467162L);
        doc.put(MailMetaInfo.ATTACHTYPE, PDF);
        doc.put(MailMetaInfo.MD5, "0E74879DA55AEECFB228670430D5F097");
        checkMail("turkish.eml", json);
    }
    // CSON: MethodLength

    @Test
    public void testPlainRtfAttach() throws Exception {
        String contentType = "content-type: multipart/mixed; boundary=001a11c2"
            + "3f66f8827604e0655a02";
        Json json = new Json(contentType, MID, SUID, MDB);
        String headers =
            "\ncontent-type: multipart/alternative; boundary=001a11c23f66f88"
            + "27304e0655a00\ncontent-type: text/html; charset=KOI8-R\ncontent"
            + "-transfer-encoding: quoted-printable";
        Map<String, Object> doc =
            json.createDoc(
                ONE_ONE_ONE,
                contentType + headers.replaceAll("t/html", "t/plain"));
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        String first = "idea when this would be resolved?";
        String second = "please see the attachment for the queries";
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Contains(first),
                new Json.Not(new Json.Contains(second))));
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Not(new Json.Contains(first)),
                new Json.Contains(second)));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://apple.com\nhttps://duckduckgo.com/?q=you+can+only+cut+"
            + "people+out+of+your+life\nhttp://yandex.com\nhttp://yandex.ru"
            + "\nhttps://duckduckgo.com\n");

        doc = json.createDoc(ONE_ONE_TWO, contentType + headers);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains(
                "Michail, please let us know if you can resolve this or "));
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Not(new Json.Contains(first)),
                new Json.Contains(second)));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_KOI8_R);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://apple.com\n"
            + "https://duckduckgo.com/?q=you+can+only+cut+people+out+of+your"
            + "+life\nhttp://yandex.com\nhttp://yandex.ru\n"
            + "https://duckduckgo.com\n");

        doc = json.createDoc(
            ONE_TWO,
            contentType
            + "\ncontent-type: application/rtf; name=\"Yandex relevancy 1.rtf"
            + "\"\ncontent-disposition: attachment; filename=\"Yandex relevanc"
            + "y 1.rtf\"\ncontent-transfer-encoding: base64\n"
            + "x-attachment-id: f_hikpyijt0");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/rtf");
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains(
                "Resource interpreted as Font but "
                + "transferred with MIME type"));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "Yandex relevancy 1.rtf");
        doc.put(MailMetaInfo.ATTACHSIZE, 9243L);
        doc.put(MailMetaInfo.ATTACHTYPE, "txt text rtf");
        doc.put(MailMetaInfo.MD5, "7AD179CCF0AF96009DE7A676BC9141CB");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://blogspot.hu\n"
            + "http://www.ladbrokes.be/foot-panierAdd.php?id=79775\n"
            + "http://gmail.com\n"
            + "http://www.rt.com\n"
            + "http://vastustructures.blogspot.in/?view=sidebar\n"
            + "http://www.rameyagrup.com/Rameya-Emlak.php\n"
            + "http://www.swiss.com/feedback\n"
            + "klttps://plus.google.com/105603648193527516331\n"
            + "http://www.youtube22.com/watch?v=jDd9cwqoni8\n"
            + "http://www.huayiinc.com\n"
            + "http://www.facebook.compluginslike.php?api_key=\n"
            + "http://bilgininyolu1152.tr.gg\n"
            + "http://nzbindex.nl\n"
            + "http://related.com\n"
            + "http://cyberlex.tumblr.com\n"
            + "http://www.start\n"
            + "http://sampling.com/sm/Sheba102813\n");
        checkMail("plain.rtf.eml", json);
    }

    @Test
    public void testSmallHtml() throws Exception {
        String contentType = "content-type: multipart/alternative;\tboundary="
            + "\"_000_3E4FC93E8F1E744895875653BFACAFEF1B18BEA3A7FINEldyandexr_"
            + '"';
        String headers =
            "\ncontent-type: text/html; charset=\"koi8-r\"\ncontent-"
            + "transfer-encoding: quoted-printable";
        Json json = new Json(contentType, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            contentType + headers.replaceAll("/html", "/plain"));
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains(
                "Задачи расставлены в порядке уменьшения важности"));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        doc.put(MailMetaInfo.X_URLS, "http://staff/ivanov\n");

        doc = json.createDoc(ONE_TWO, contentType + headers);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("Ничего по классам запросов"));
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Толик, к черту презентацию, это "
            + "условность. Я по сути\nспрашиваю.\n--\nМитя\n"
            + "http://staff/ivanov");
        doc.put(MailMetaInfo.X_URLS, "http://staff/ivanov\n");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_KOI8_R);
        checkMail("small.html.eml", json);
    }

    @Test
    public void testTableBodyHtml() throws Exception {
        String contentType = "content-type: multipart/alternative; boundary="
            + "--boundary_5_37e2ac6b-497e-4b56-85f5-b91158b2088a";
        Json json = new Json(contentType, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            contentType + '\n' + HEADER_TEXT_PLAIN_UTF_8 + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "This is service message sended from site. "
            + "Details in attachment.");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);

        doc = json.createDoc(
            ONE_TWO,
            contentType + '\n' + HEADER_TEXT_HTML_UTF_8 + BASE64);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains("ИП Куликов"),
                new Json.Contains("Вид торговли:")));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        checkMail("table.body.html.eml", json);
    }

    @Test
    public void testOctetStreamHtml() throws Exception {
        Json json = new Json(HEADER_TEXT_HTML_KOI8_R + BASE64, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(
            MailMetaInfo.X_URLS,
            "http://swarm.yandex.net\n"
            + "http://staff.yandex-team.ru/nyu\n"
            + "http://staff.yandex-team.ru/dpuchkin\n");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("Yury Sheglov wrote:"));
        doc.put(
            MailMetaInfo.PURE_BODY,
            "+ sidorov@, terry@, violin@\n"
            + "30.08.2013, 20:06, \"Denis Puchkin\" "
            + "<dpuchkin@yandex-team.ru>:");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_KOI8_R);
        checkMail("octet-stream.html.eml", json);
    }

    @Test
    public void testOctetStreamPlain() throws Exception {
        Json json = new Json(
            HEADER_TEXT_PLAIN_KOI8_R
            + "\ncontent-disposition: inline\n"
            + "content-transfer-encoding: 8bit",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE);
        doc.put(MailMetaInfo.X_URLS, "http://staff.yandex-team.ru/stunder\n");
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        String first = "Три хоста поднять так и не получилось.";
        String second =
            "Ориентировочные сроки восстановления - после 16 часов.";
        String third =
            "На данный момент несколько хостов кластера поднять не удалось.";
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains(first),
                new Json.Not(new Json.Contains(second)),
                new Json.Not(new Json.Contains(third))));
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Not(new Json.Contains(first)),
                new Json.Contains(second),
                new Json.Contains(third)));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_KOI8_R);
        checkMail("octet-stream.plain.eml", json);
    }

    @Test
    public void testHtmlXml() throws Exception {
        Json json = new Json(HEADER_TEXT_HTML_UTF_8, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.X_URLS, Json.ANY_VALUE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains("Avenue of the Americas and 165 Halsey."),
                new Json.Contains("global leader for data centers")));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(CommonFields.TITLE, "NYIIX expansion PR");
        checkMail("html.xml.eml", json);
    }

    // CSOFF: MethodLength
    @Test
    public void testZipStreamOdt() throws Exception {
        String headers = "from: Mego Meto <meta.user@yandex.ru>\n"
            + "to: mailsearchtest@yandex.ru; Ivan Pupkin <pukin.user2@yandex.k"
            + "z>\ncc: yndx-webdav-auto599@yandex.ru\n"
            + "bcc: yndx-webdav-auto598@yandex.com\nreply-to: yndx-webdav-auto"
            + "597@yandex.kz\nsubject: sggunday monday dghjkdg9204-\nmime-vers"
            + "ion: 1.0\ncontent-type: multipart/mixed;\tboundary=\"----==--bo"
            + "und.26648.web10e.yandex.ru\"\nreturn-path: mobav13@yandex.ru\n";
        String to =
            "mailsearchtest@yandex.ru; Ivan Pupkin <pukin.user2@yandex.kz>";
        String toEmail =
            "mailsearchtest@yandex.ru;IvanPupkin<pukin.user2@yandex.kz>\n";
        String toNormalized =
            "mailsearchtest@yandex.ru;ivanpupkin<pukin.user2@yandex.kz>\n";
        String cc = "yndx-webdav-auto599@yandex.ru";
        String bcc = "yndx-webdav-auto598@yandex.com";
        String bccNormalized = "yndx-webdav-auto598@yandex.ru\n";
        String from = "Mego Meto <meta.user@yandex.ru>";
        String fromEmail = "meta.user@yandex.ru\n";
        String fromName = "Mego Meto\n";
        String fromNormalized = "meta-user@yandex.ru\n";
        String replyTo = "yndx-webdav-auto597@yandex.kz";
        String replyToNormalized = "yndx-webdav-auto597@yandex.ru\n";
        String subject = "sggunday monday dghjkdg9204-";
        Json json = new Json(headers, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "content-transfer-encoding: 7bit\ncontent-type: text/plain");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, bcc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bccNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyToNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "today is the ..///first day of my life");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(
            ONE_TWO,
            headers
            + "content-disposition: attachment;\tfilename=\"=?UTF-8?B?VW50aXRs"
            + "ZWQkXiYkLC4ucGDihJbQttC/0LXQsy5qcGVn?=\"\ncontent-transfer-enco"
            + "ding: base64\ncontent-type: image/jpeg;\tname=\"=?UTF-8?B?VW50a"
            + "XRsZWQkXiYkLC4ucGDihJbQttC/0LXQsy5qcGVn?=\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toNormalized);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyToNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, bcc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bccNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_JPEG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        String meta =
            "Number of Components:3\nR"
            + "esolution Units:inch\nData Precision:8 bits\ntiff:BitsPerSample"
            + ":8\nCompression Type:Baseline\nComponent 1:Y component: Quantiz"
            + "ation table 0, Sampling factors 2 horiz/2 vert\nComponent 2:Cb "
            + "component: Quantization table 1, Sampling factors 1 horiz/1 ver"
            + "t\nComponent 3:Cr component: Quantization table 1, Sampling fac"
            + "tors 1 horiz/1 vert\nX Resolution:96 dots\nContent-Type:image/j"
            + "peg\nY Resolution:96 dots\nNumber of Tables:4 Huffman tables\n"
            + "Thumbnail Height Pixels:0\nThumbnail Width Pixels:0";
        doc.put(CommonFields.META, new Json.Headers(meta));
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "Untitled$^&$,..p`№жпег.jpeg");
        doc.put(CommonFields.WIDTH, 1920L);
        doc.put(CommonFields.HEIGHT, 1080L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, "16:9");
        doc.put(MailMetaInfo.ATTACHSIZE, 677695L);
        doc.put(MailMetaInfo.MD5, "A058FDB900499F86F8004E1F2C5D0673");
        doc.put(MailMetaInfo.ATTACHTYPE, JPEG);

        doc = json.createDoc(
            ONE_THREE,
            headers
            + "content-disposition: attachment;\tfilename=\"Java SE 7u25.dotx"
            + "\"\ncontent-transfer-encoding: base64\ncontent-type: applicati"
            + "on/x-zip;\tname=\"Java SE 7u25.dotx\"");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, bcc);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bccNormalized);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyToNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            "application/vnd.openxmlformats-officedocument.wordprocessingml"
            + ".template");
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/x-zip");
        doc.put(
            CommonFields.BODY_TEXT,
            "Java Platform, Enterprise Edition, ск"
            + "орочено Java EE (до версії 5.0 — Java 2 Enterprise Edition або "
            + "J2EE) — обчислювальна корпоративна платформа Java. Платформа на"
            + "дає API та виконавче середовище для розробки і виконання корпор"
            + "ативного програмного забезпечення, включаючи мережеві та веб се"
            + "рвіси, та інші масштабовані, розподілені застосунки. Java EE ро"
            + "зширює стандартну платформу Java (Java SE - Java Standart Editi"
            + "on)[1].\nJ2EE є промисловою технологією і в основному використо"
            + "вується в високопродуктивних проектах, в яких необхідна надійні"
            + "сть, масштабованість, гнучкість.\nКомпанія Oracle, яка придбала"
            + " Sun (фірму, що створила Java), активно просуває Java EE у зв'я"
            + "зці з своїми технологіями, зокрема з СУБД Oracle.");
        meta =
            "cp:revision:2\nmeta:last-author:Irina Kurganova\nLast-Author:Irin"
            + "a Kurganova\nApplication-Name:Microsoft Office Word\nApplicatio"
            + "n-Version:14.0000\nCharacter-Count-With-Spaces:1434\nextended-p"
            + "roperties:Template:Java SE 7u25.dotx\nmeta:line-count:10\nWord-"
            + "Count:214\nmeta:paragraph-count:2\nextended-properties:AppVersi"
            + "on:14.0000\nLine-Count:10\nextended-properties:Application:Micr"
            + "osoft Office Word\nParagraph-Count:2\nRevision-Number:2\nTempla"
            + "te:Java SE 7u25.dotx\nmeta:character-count:1222\nmeta:word-coun"
            + "t:214\nCharacter Count:1222\nmeta:character-count-with-spaces:1"
            + "434\nContent-Type:application/vnd.openxmlformats-officedocument"
            + ".wordprocessingml.template\n"
            + "extended-properties:DocSecurityString:None";
        doc.put(CommonFields.META, new Json.Headers(meta));
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "Java SE 7u25.dotx");
        String author = "Irina Kurganova";
        doc.put(CommonFields.AUTHOR, author);
        doc.put(CommonFields.CREATED, 1374163740L);
        doc.put(CommonFields.MODIFIED, 1374163740L);
        doc.put(CommonFields.PAGES, 1L);
        doc.put(MailMetaInfo.ATTACHSIZE, 19310L);
        doc.put(MailMetaInfo.MD5, "ED0B4F9EE962A6220D9E98485D39CA9E");
        doc.put(MailMetaInfo.ATTACHTYPE, "dot dotx");
        String xurls =
            "http://uk.wikipedia.org/wiki/Java\n"
            + "http://uk.wikipedia.org/wiki/API\n"
            + "http://uk.wikipedia.org/wiki/%D0%97%D0%B0%D1%81%D1%82%D0%BE%D1"
            + "%81%D1%83%D0%BD%D0%BA%D0%B8\n"
            + "http://uk.wikipedia.org/w/index.php?title=Java_SE&action=edit"
            + "&redlink=1\n"
            + "http://uk.wikipedia.org/w/index.php?title=Java_Standart_Edition"
            + "&action=edit&redlink=1\n"
            + "http://uk.wikipedia.org/wiki/Java_EE\n"
            + "http://uk.wikipedia.org/wiki/Oracle\n"
            + "http://uk.wikipedia.org/wiki/Oracle_Database\n";
        doc.put(MailMetaInfo.X_URLS, xurls);

        doc = json.createDoc(
            ONE_FOUR,
            headers
            + "content-disposition: attachment;\tfilename=\"Java SE 7u25.odt\""
            + "\ncontent-transfer-encoding: base64\ncontent-type: application"
            + "/vnd.oasis.opendocument.text;\tname=\"Java SE 7u25.odt\"");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyToNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, bcc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bccNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            "application/vnd.oasis.opendocument.text");
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.BODY_TEXT,
            "Java Development Kit (сокращенно JDK)"
            + " — бесплатно распространяемый компанией Oracle Corporation (ран"
            + "ее Sun Microsystems) комплект разработчика приложений на языке "
            + "Java, включающий в себя компиляторJava (javac), стандартные биб"
            + "лиотеки классов Java, примеры, документацию, различные утилиты "
            + "и исполнительную систему Java (JRE). В состав JDK не входит инт"
            + "егрированная среда разработки на Java, поэтому разработчик, исп"
            + "ользующий только JDK, вынужден использовать внешний текстовый р"
            + "едактор и компилировать свои программы, используя утилиты коман"
            + "дной строки.\nВсе современные интегрированные среды разработки "
            + "приложений на Java, такие, как NetBeans IDE, Sun Java Studio Cr"
            + "eator, IntelliJ IDEA, Borland JBuilder, Eclipse, опираются на с"
            + "ервисы, предоставляемые JDK. Большинство из них для компиляции "
            + "Java-программ используют компилятор из комплекта JDK. Поэтому э"
            + "ти среды разработки либо включают в комплект поставки одну из в"
            + "ерсий JDK, либо требуют для своей работы предварительной инстал"
            + "ляции JDK на машине разработчика.\nДоступны полные исходные тек"
            + "сты JDK, включая исходные тексты самого Java-компилятора javac."
        );
        meta =
            "editing-cycles:2\nWord-Co"
            + "unt:407\nEdit-Time:PT60S\nmeta:paragraph-count:5\ngenerator:Mic"
            + "rosoftOffice/14.0 MicrosoftWord\nParagraph-Count:5\nmeta:charac"
            + "ter-count:2728\nmeta:word-count:407\nnbPara:5\ninitial-creator:"
            + "Irina Kurganova\nCharacter Count:2728\nmeta:initial-author:Irin"
            + "a Kurganova\nnbWord:407\nContent-Type:application/vnd.oasis.ope"
            + "ndocument.text\nnbCharacter:2728";
        doc.put(CommonFields.META, new Json.Headers(meta));
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "Java SE 7u25.odt");
        doc.put(CommonFields.AUTHOR, author);
        doc.put(CommonFields.PAGES, 1L);
        doc.put(CommonFields.CREATED, 1374163740L);
        doc.put(CommonFields.MODIFIED, 1374163740L);
        doc.put(MailMetaInfo.ATTACHSIZE, 9632L);
        doc.put(MailMetaInfo.MD5, "96A450578F454774DA74F14750931E26");
        doc.put(MailMetaInfo.ATTACHTYPE, "odt");
        xurls =
            "http://ru.wikipedia.org/wiki/Oracle_Corporation\n"
            + "http://ru.wikipedia.org/wiki/Sun_Microsystems\n"
            + "http://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D0%BC%D"
            + "0%BF%D0%BB%D0%B5%D0%BA%D1%82_%D1%80%D0%"
            + "B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%"
            + "82%D1%87%D0%B8%D0%BA%D0%B0_%D0%BF%D1%80"
            + "%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%"
            + "D0%B8%D0%B9\n"
            + "http://ru.wikipedia.org/wiki/Java\n"
            + "http://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D0%BC"
            + "%D0%BF%D0%B8%D0%BB%D1%8F%D1%82%D0%BE%"
            + "D1%80\n"
            + "http://ru.wikipedia.org/wiki/Javac\n"
            + "http://ru.wikipedia.org/wiki/JRE\n"
            + "http://ru.wikipedia.org/wiki/%D0%98%D0%BD%D1%82%"
            + "D0%B5%D0%B3%D1%80%D0%B8%D1%80%D0%BE%D0"
            + "%B2%D0%B0%D0%BD%D0%BD%D0%B0%D1%8F_%D1"
            + "%81%D1%80%D0%B5%D0%B4%D0%B0_%D1%80%D0"
            + "%B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%"
            + "82%D0%BA%D0%B8\n"
            + "http://ru.wikipedia.org/wiki/NetBeans_IDE\n"
            + "http://ru.wikipedia.org/w/index.php?title=Sun_Java_Studio_Creat"
            + "or&action=edit&redlink=1\n"
            + "http://ru.wikipedia.org/wiki/IntelliJ_IDEA\n"
            + "http://ru.wikipedia.org/wiki/JBuilder\n"
            + "http://ru.wikipedia.org/wiki/Eclipse_(%D1%81%D1%80%D0"
            + "%B5%D0%B4%D0%B0_%D1%80%D0%B0%D0%B7%D1"
            + "%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%"
            + "B8)\n";
        doc.put(MailMetaInfo.X_URLS, xurls);

        doc = json.createDoc(
            ONE_FIVE,
            headers + "content-disposition: attachment;\tfilename=\"sonar-runn"
            + "er-1.0-sources.jar\"\ncontent-transfer-encoding: base64\nconten"
            + "t-type: application/x-zip;\tname=\"sonar-runner-1.0-sources.jar"
            + '"');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL,
            cc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            cc + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyToNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.BCC, bcc);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.EMAIL,
            bcc + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.BCC + MailMetaInfo.NORMALIZED,
            bccNormalized);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, "application/java-archive");
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/x-zip");
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Contains("META-INF/MANIFEST.MF"),
                new Json.Contains("org/sonar/runner/Launcher.java"),
                new Json.Contains("Sonar Standalone Runner")));
        doc.put(CommonFields.META, "Content-Type:application/java-archive");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://sonar-project.properties\nhttp://localhost:9000\n");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "sonar-runner-1.0-sources.jar");
        doc.put(MailMetaInfo.ATTACHSIZE, 5015L);
        doc.put(MailMetaInfo.MD5, "1C094493733EE79417C02405F26F9BAB");
        doc.put(MailMetaInfo.ATTACHTYPE, "jar");

        checkMail("attachext12.eml", json);
    }
    // CSON: MethodLength

    @Test
    public void testPub() throws Exception {
        Json json = new Json("content-type: badtype" + BASE64, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, "application/x-mspublisher");
        doc.put(MailMetaInfo.CONTENT_TYPE, "badtype");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, "Content-Type:application/x-mspublisher");
        checkMail("mspublisher.eml", json);
    }

    @Test
    public void testConflictHtmlCharset() throws Exception {
        Json json = new Json(HEADER_TEXT_HTML_WINDOWS_1251, MID, SUID, MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(MailMetaInfo.X_URLS, Json.ANY_VALUE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains("Акционерное общество"));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_WINDOWS_1251);
        doc.put(CommonFields.TITLE, "Приглашение к участию в аукционе - ЕЭТП");
        checkMail("conflict-html-charset.eml", json);
    }

    @Test
    public void testSanitizeTruncatedHtml() throws Exception {
        String headers =
            "received: from yaback1o.mail.yandex.net ([127.0.0.1])\tby "
            + "yaback1o.mail.yandex.net with LMTP id FKFYXLYY\tfor <analizer@"
            + "yandex.ru>; Sat, 7 Oct 2017 23:58:32 +0300\nreceived: from taxi"
            + "-stq04h.taxi.yandex.net (taxi-stq04h.taxi.yandex.net [93.158."
            + "128.200])\tby yaback1o.mail.yandex.net (nwsmtp/Yandex) with "
            + "ESMTP id wupIbRvQIT-wWpahUXl;\tSat, 07 Oct 2017 23:58:32 +0300"
            + "\nx-yandex-front: yaback1o.mail.yandex.net\nx-yandex-timemark: "
            + "1507409912\nx-yandex-local: yes\ndkim-signature: v=1; a=rsa-"
            + "sha256; c=relaxed/relaxed; d=taxi.yandex.ru; s=mail;\tt="
            + "1507409912; bh=ylyA9OcrS+dtR+RHsr7oDcmygzsSSPoEht/5/Lk6//A=;\th"
            + "=From:To:Date:Subject:Message-Id;\tb=VtQuum0vGHsp2dRBF/"
            + "CHNoOptNInsiafii/aSQKbQckGqQh+SEmwblyW6tBdJHVDX\t "
            + "1HfKPYSYLbpyzJwmVXrJiADl4r4OLcJdtpi/3pTzVyVfnwHhrR3Hv2crzfqJ8pC"
            + "VYh\t r4MD+NYtRIB2MTqKbv+AHsOmyffOyzNc8UpzTdUM=\nauthentication"
            + "-results: yaback1o.mail.yandex.net; dkim=pass header.i=@taxi."
            + "yandex.ru\ncontent-transfer-encoding: binary\ncontent-type: "
            + "multipart/alternative; boundary=\"_----------="
            + "_15074099127408440\"\nmime-version: 1.0\nfrom: =?utf-8?B?"
            + "WWFuZGV4LlRheGk=?= <no-reply@taxi.yandex.ru>\nto: analizer@"
            + "yandex.ru\ndate: Sat, 7 Oct 2017 23:58:32 +0300\nsubject: =?"
            + "utf-8?B?WWFuZGV4LlRheGkgcmlkZSByZWNlaXB0IGZvciA3IE9jdG9iZXIsIDI"
            + "wMTc=?=\nmessage-id: dd4fed8c5d424723ba162ff242164b8a\nx-mailer"
            + ": XmlToMime/0.1\nreturn-path: no-reply@taxi.yandex.ru\nx-yandex"
            + "-forward: 8fc19e2779405517e1e0d1080eb0446c\n";
        String commonHeaders =
            "content-disposition: inline\n"
            + "content-transfer-encoding: base64\n"
            + "mime-version: 1.0\n"
            + "date: Sat, 7 Oct 2017 23:58:32 +0300\n";
        String receivedDate = "1507409912";
        String from = "Yandex.Taxi <no-reply@taxi.yandex.ru>";
        String fromName = "Yandex.Taxi\n";
        String fromEmail = "no-reply@taxi.yandex.ru\n";
        String to = "analizer@yandex.ru";
        String subject = "Yandex.Taxi ride receipt for 7 October, 2017";
        String smtpId = "wupIbRvQIT-wWpahUXl";

        Json json = new Json(headers, null, null, (String) null);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + commonHeaders + HEADER_TEXT_PLAIN_UTF_8);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Support service\nRide receipt\n7 October, 2017\n"
            + "Taxi was ordered on 7 October, 2017 at 23:48\nRide cost 247 rub"
            + "\nService class: Economy\nRide duration: 13 min\n"
            + "From: ulitsa Rodionova, 45\nTo: ulitsa Karla Marksa, 32\n"
            + "Taxi company name: Zvezdniy +78314161676\n"
            + "Driver: Gaibov Azimgon Mirzomamatovich +79036064697\n"
            + "Car: gold Daewoo Nexia О327МА152\nIf you have any questions, "
            + "please don’t hesitate to contact our support service <https://"
            + "yandex.ru/support/taxi/troubleshooting/review.xml>.\nthe Yandex"
            + ".Taxi team\nUnsubscribe: <https://taxi.yandex.com/email/"
            + "unsubscribe/?confirmation_code=4e65bfa803cc25f15291ab6ba483ef6b"
            + "c113b8e3761bd1cadd70a278>");
        doc.put(
            MailMetaInfo.X_URLS,
            "https://yandex.ru/support/taxi/troubleshooting/review.xml\n"
            + "http://yandex.taxi\nhttps:"
            + "//taxi.yandex.com/email/unsubscribe/?confirmation_code=4e65bfa8"
            + "03cc25f15291ab6ba483ef6bc113b8e3761bd1cadd70a278\n");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);

        doc = json.createDoc(
            ONE_TWO,
            headers + commonHeaders + HEADER_TEXT_HTML_UTF_8);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Ride receipt\n7 October, 2017\nYou ordered a taxi on 7 October, "
            + "2017 at 23:48\nulitsa Rodionova, 45\nulitsa Karla Marksa, 32\n"
            + "Ride cost — 247 rub\nService class\nRide duration\nRide cost\n"
            + "Economy\n13 min\n247 rub\nTaxi company\nDriver\nCar\nZvezdniy\n"
            + "Gaibov Azimgon Mirzomamatovich\ngold Daewoo Nexia\n+78314161676"
            + "\n+79036064697\nО327МА152\nIf you have any questions, please "
            + "don’t hesitate to\ncontact support\n.\nthe Yandex.Taxi team\n"
            + "Unsubscribe from ride receipts");
        doc.put(
            MailMetaInfo.X_URLS,
            "https://taxi.yandex.com\n"
            + "https://yandex.ru/support/taxi/troubleshooting/review.xml\n"
            + "http://yandex.taxi\n"
            + "https://taxi.yandex.com/email/unsubscribe/?"
            + "confirmation_code=4e65bfa803cc25f15291ab6ba483ef6bc113b8e3761bd"
            + "1cadd70a278\n");
        doc.put(CommonFields.TITLE, "Yandex.Taxi ride receipt");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);

        String file = "taxi.eml";
        checkMail(file, json);
        doc.put(
            MailMetaInfo.HTML_BODY,
            new Json.Contains("</table>\n</div></body>"));
        checkMail(
            file,
            json,
            DETEMPL_SUFFIX,
            SANITIZE_HTML);
    }

    @Test
    public void testMalformedDoctype() throws Exception {
        Json json = new Json(
            "content-type: text/html; charset=utf-8\n"
            + "content-transfer-encoding: quoted-printable",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.TITLE, "Mamsy");
        String pureBody =
            "Вы получили это письмо, потому что подписаны на рассылку на сайте"
            + " Mamsy.ru\nРАСПРОДАЖИ\nПРИГЛАШЕНИЯ\nБЛОГ\nАККАУНТ";
        doc.put(MailMetaInfo.PURE_BODY, pureBody);
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://mamsy.ru\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_1\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_2\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_3\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_4\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_5\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_6\n"
            + "http://link.mamsy.ru/u/nrd.php?p=8w1mBqJg9i_10386_89951_1_7\n");
        checkMail("missing-text.eml", json);
    }

    @Test
    public void testUnencodedHeaders() throws Exception {
        Json json = new Json(
            "x-yandex-fwd: NzA1MzkyMzU5ODExOTU1MTM5NywxMzU5MTI4Nzg4NzY5NzYyOTM"
            + "\nto: \"=?utf-8?B?bjY5LTY5=?=\" <n69-69@ya.ru>\n"
            + "from: Финансовая поддержка <hello@yandex-team.ru>\n"
            + "subject: Здеся тема\ncontent-type: text/html",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO,
            "\"n69-69\" <n69-69@ya.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            "n69-69@ya.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            "n69-69@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            "n69-69\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "Финансовая поддержка <hello@yandex-team.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            "hello@yandex-team.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            "hello@yandex-team.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Финансовая поддержка\n");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, "Здеся тема");
        doc.put(CommonFields.BODY_TEXT, "");
        String pureBody =
            "Билет №40852\nВ paмках акции пo поддepжкe нaceления, в уcлoвиях "
            + "кaрaнтинa, Гослотo дарит Bам oдин билет на онлaйн тиpаж "
            + "Вcерoссийcкoй официaльной лoтеpеи.\nПpизовoй фoнд - болee "
            + "однoгo миллиapда pyблeй. Глaвный пpиз тиpaжa - бoлeе\n"
            + "116 110 730 RUB\n.\nЧтoбы вoспoльзовaться Bашим билетом и "
            + "учaствовать в pозыгрыше, пepeйдитe нa oфициальный сaйт Руccкoгo"
            + " лoто пo cсылкe:\nЗаpегистриpовать билет и принять учacтиe в "
            + "рoзыгрышe!\nДанный билет являeтся пoдарoчным и не подлeжит "
            + "пeредачи трeтьим лицaм. Срок дeйcтвия билетa - дo 28.04.2020.\n"
            + "* T.к. aкция являетcя блaготвоpитeльной и pозыгрыш проxодит "
            + "полностью онлaйн (выдачa выигpышей бyдет пpоизводиться oнлайн "
            + "пeревoдом) - eсли Bы выигpaете, можeт потpeбoвaться оплата доп."
            + " pacходoв нa пepeвoд Вaшего выигpышa.";
        doc.put(MailMetaInfo.PURE_BODY, pureBody);
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://loto-yubileynoye.site?hash=QrWUDJWrY95lk6nd\n");
        checkMail("unencoded-headers.eml", json);
    }

    @Test
    public void testContentTypeMultipartImpostor() throws Exception {
        String headers =
            "content-type: text/plain; charset=\"utf-8\"\n"
            + "content-transfer-encoding: quoted-printable\n"
            + "subject: =?utf-8?q?=D0=9E=D1=82=D0=B2=D0=B5=D1=82_=D0=BD=D0=B0_"
            + "=D1=84=D0=BE=D1=80?= =?utf-8?q?=D0=BC=D1=83_=D0=9D=D0=BE=D0=B2="
            + "D0=B0=D1=8F_=D1=84=D0=BE=D1=80?= =?utf-8?q?=D0=BC=D0=B0?=\n"
            + "subject: =?utf-8?b?0KFv0YbQuNCw0LvRjNC9YdGPINC/0YDQvtCz0YDQsNC8"
            + "0LzQsCDRhNC40L1h0L3RgdC+0LJv0Lkg0L/QvtC00LTQtdGA0LbQutC4INC90LB"
            + "j0LXQu2XQvdC40Y8uINCY0L3RgdGC0YB50LrRhtC40Lgg0LLQvdGD0YJw0Lgu?="
            + "\ncontent-type: multipart/mixed; "
            + "boundary=\"2266588554920156230\"";
        Json json = new Json(headers, MID, SUID, MDB);
        String subject =
            "Сoциальнaя программа финaнсовoй поддержки наcелeния. "
            + "Инстрyкции внутpи.";

        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "\ncontent-type: text/html; charset=3D\"utf-8\"\n"
            + "mime-version: 1.0\ncontent-transfer-encoding: base64");
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Здравствуйте!\nB pамкаx благoтворительнoй акции пo поддeржкe "
            + "нaселeния, во вpемя сaмоизoляции, Pусскоe лотo дapит Bам 1\n"
            + "бeспрoигрышный\nбилет на онлaйн тиpаж Всеросcийcкoй oфициальнoй"
            + " лотepeи. Пpизoвой фонд тирaжa - бoлeе однoго млpд RUB. Главный"
            + " пpиз тиpaжa - болee\n112 170 380 pублей\n.\nЧтобы "
            + "заpегистриpoвать Вaш билeт и yчаствoвaть в розыгpыше, перeйдите"
            + " на официальный сайт Pосcийскогo лoтo пo этой сcылке:\n"
            + "Пoдтвeрдить свое учacтие!\nДанный билет являeтся пoдapoчным и "
            + "нe пoдлeжит передaчи трeтьим лицaм.\n* Тaк кaк акция прoводится"
            + " нa блaгoтвoрительной оcнoве и рoзыгpыш прoвoдитcя полностью "
            + "онлайн (в том числе выдaча выигpышeй бyдeт ocyщeствлятьcя "
            + "онлайн пeрeводом) - в слyчae побeды, может пoтpeбoвaтьcя oплaтa"
            + " доп. зaтрaт нa пepeвод Baшегo выигpыша.");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://lotohelp.space?hash=bRK4OggYixWUpNTUtQ5\n");

        checkMail("content-type-multipart-impostor.eml", json);
    }

    @Test
    public void testBadSecondBoundary() throws Exception {
        Json json = new Json(
            "content-type: multipart/alternative; boundary=my_boundary\n"
            + "content-type: multipart/alternative; charset=",
            MID,
            SUID,
            MDB);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(MailMetaInfo.PURE_BODY, "Body");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        checkMail("bad-second-boundary.eml", json);
    }
}

