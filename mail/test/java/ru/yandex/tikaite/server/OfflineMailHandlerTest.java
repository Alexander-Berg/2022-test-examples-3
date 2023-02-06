package ru.yandex.tikaite.server;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.tika.mime.MediaType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.server.sync.ContentWriter;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.JsonSubsetChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.tikaite.mimeparser.NestedMessageHandler;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;
import ru.yandex.tikaite.util.TextExtractOptions;

public class OfflineMailHandlerTest extends TestBase {
    private static final String CRLF = "\r\n";
    private static final String ALTERNATIVE_COMPLETE =
        "alternative.complete.txt";
    private static final String SIMPLEST_QUOTED_PRINTABLE =
        "simplest.quoted-printable.txt";
    private static final String SIMPLE = "simple.txt";
    private static final String TXT = "txt text";
    private static final String RTF = "rtf";
    private static final String RTFATTACH = "rtfattach.txt";
    private static final long RTFATTACH_SIZE = 2405L;
    private static final String RTFATTACH_MD5 =
        "2F73DD5D374EBE2F56E05768552F4DBE";
    private static final String CONTENT_TYPE =
        "application/json; charset=UTF-8";
    private static final String HELLO_WORLD = "Hello, world";
    private static final String HELLO_WORLD_RUS = "Привет, мир";
    private static final String HELLO_AGAIN = "Hello again";
    private static final String YET_ANOTHER_HELLO = "Yet another hello";
    private static final String ATTACHMENT = "attachment";
    private static final String LOCALHOST = "http://localhost:";
    private static final String GET = "/get/";
    private static final String RAW = "?raw";
    private static final String PATH = GET + "somepath";
    private static final String NAME = "?name=mail";
    private static final String ONE = "1";
    private static final String ONE_ONE = "1.1";
    private static final String ONE_ONE_ONE = "1.1.1";
    private static final String ONE_ONE_TWO = "1.1.2";
    private static final String ONE_TWO = "1.2";
    private static final String ONE_TWO_ONE = "1.2.1";
    private static final String ONE_TWO_TWO = "1.2.2";
    private static final String ONE_THREE = "1.3";
    private static final String ONE_FOUR = "1.4";
    private static final String ONE_FIVE = "1.5";
    private static final String EMAIL = "analizer@yandex.ru";
    private static final String TO = "to: " + EMAIL;
    private static final String PLAIN = "\ncontent-type: text/plain";
    private static final String HTML = "\ncontent-type: text/html";
    private static final String XML = "\ncontent-type: text/xml";
    private static final String PNG = "\ncontent-type: image/png";
    private static final String IMAGE_PNG = "image/png";
    private static final String PDF = "application/pdf";
    private static final String APPLICATION_XML = "application/xml";
    private static final String PNG_META = "Content-Type:image/png\n";
    private static final String PNG_ATTACHTYPE = "png";
    private static final String UTF8 = "; charset=utf-8";
    private static final String MIXED =
        "content-type: multipart/mixed; boundary=mixed";
    private static final String ALTERNATIVE =
        "content-type: multipart/alternative; boundary=alternative";
    private static final String EIGHT_BIT =
        "\ncontent-transfer-encoding: 8bit";
    private static final String BASE64 =
        "\ncontent-transfer-encoding: base64";
    private static final String CONTENT_TYPE_7_BIT =
        "\ncontent-transfer-encoding: 7bit\ncontent-type: text/html";
    private static final String INLINE = "inline";
    private static final String CONTENT_DISPOSITION_INLINE =
        "\ncontent-disposition: inline";
    private static final String SPAM = "x-yandex-spam: 1\n";
    private static final Long CREATED = 1363262400L;
    private static final String APPLICATION_RTF = "application/rtf";
    private static final String SOME_TEXT = "some text here";
    private static final Object XML_ERROR = new Json.StartsWith(
        "org.apache.tika.exception.TikaException: XML parse error\n");
    private static final String HID_DEPTH_2 = "extractor.max-hid-depth = 2";
    private static final String MID = "100500";
    private static final Long SUID = Long.valueOf(9000);
    private static final String ALTERNATIVE_HEADERS =
        FieldName.CONTENT_TYPE.toLowerCase() + MailMetaInfo.HEADERS_SEPARATOR
        + "multipart/alternative; boundary=\"myboundary\"";
    private static final String POST_BODY_START =
        MailMetaInfo.X_YANDEX_MID + MailMetaInfo.HEADERS_SEPARATOR + MID + CRLF
        + MailMetaInfo.X_YANDEX_SUID + MailMetaInfo.HEADERS_SEPARATOR + SUID
        + CRLF;
    private static final String POST_BODY = POST_BODY_START + CRLF;
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
    private static final String CONTENT_TYPE_PLAIN_UTF_8 =
        "Content-Type:text/plain; charset=UTF-8";
    private static final String CONTENT_TYPE_PLAIN_WINDOWS_1252 =
        "Content-Type:text/plain; charset=windows-1252";
    private static final String CONTENT_TYPE_PLAIN_WINDOWS_1251 =
        "Content-Type:text/plain; charset=windows-1251";
    private static final String CONTENT_TYPE_XML =
        "Content-Type:text/xml";
    private static final String CONTENT_TYPE_RTF =
        "Content-Type:application/rtf";
    private static final String CONTENT_TYPE_PDF =
        "Content-Type:application/pdf";
    private static final String ZIP = "application/zip";
    private static final String CONTENT_TYPE_ZIP =
        "Content-Type:application/zip";

    private static final int TIMEOUT = 10000;

    public OfflineMailHandlerTest() {
        super(false, 0L);
    }

    @Test
    public void testGet() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(getClass().getResource(SIMPLE).toURI()));
            backend.start();
            server.start();
            HttpGet get = new HttpGet(
                LOCALHOST + server.port() + PATH + NAME
                + "&mid=100500&suid=9000&stid=123.456.789&mdb=mdb303"
                + "&received=1234567890"
                + "&reply-to=0JjQvdGC0LXRgNC90LXRgi3Qt9C%2B0L7Qs9C40L/QtdGA0Lz"
                + "QsNGA0LrQtdGCINCf0LXRgtCe0L3Qu9Cw0LnQvS7RgNGDIDx6YWthekBwZX"
                + "Qtb25saW5lLnJ1"
                + "&from=IlBvdGFwb3YgRG1pdHJ5IiA8cG90YXBvdi5kQGdtYWlsLmNvbT4="
                + "&to=IlBvdGFwb3YsIERtaXRyeSIgPHBvdGFwb3YuZEBnbWFpbC5jb20%2BL"
                + "CAiRG1pdHJ5IFBvdGFwb3YiIDxhbmFsaXplckB5YW5kZXgucnU%2B");
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            String text = EntityUtils.toString(entity);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                CONTENT_TYPE,
                entity.getContentType().getValue());
            Json json = new Json(MID, SUID);
            Map<String, Object> doc = json.createDoc(ONE);
            doc.put(CommonFields.PARSED, true);
            doc.put(CommonFields.BODY_TEXT, "");
            doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
            doc.put(
                CommonFields.MIMETYPE,
                MediaType.TEXT_PLAIN.getBaseType().toString());
            doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
            doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
            doc.put(MailMetaInfo.STID, "123.456.789");
            doc.put(MailMetaInfo.RECEIVED_DATE, "1234567890");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.FROM,
                "\"Potapov Dmitry\" <potapov.d@gmail.com>");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
                "potapov.d@gmail.com\n");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
                "potapovd@gmail.com\n");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.FROM
                + MailMetaInfo.DISPLAY_NAME,
                "Potapov Dmitry\n");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.TO,
                "\"Potapov, Dmitry\" <potapov.d@gmail.com>,"
                + " \"Dmitry Potapov\" <analizer@yandex.ru>");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
                "potapov.d@gmail.com\nanalizer@yandex.ru\n");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
                "potapovd@gmail.com\nanalizer@yandex.ru\n");
            doc.put(
                MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
                "Potapov, Dmitry\nDmitry Potapov\n");
            String replyToEmail = "zakaz@pet-online.ru\n";
            doc.put(
                MailMetaInfo.REPLY_TO_FIELD,
                "Интернет-зоогипермаркет ПетОнлайн.ру <zakaz@pet-online.ru");
            doc.put(
                MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
                "Интернет-зоогипермаркет ПетОнлайн.ру\n");
            doc.put(
                MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
                replyToEmail);
            doc.put(
                MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
                replyToEmail);
            json.assertEquals(text);
        }
    }

    @Test
    public void testUidGet() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                "<message>\n</message>\n\r\n\r\n" + HELLO_WORLD);
            backend.start();
            server.start();
            HttpGet get = new HttpGet(
                LOCALHOST + server.port() + PATH + NAME + "&mid=100500&uid=1");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity entity = response.getEntity();
                String text = EntityUtils.toString(entity);
                Assert.assertEquals(
                    CONTENT_TYPE,
                    entity.getContentType().getValue());
                Json json = new Json(null, MID, MailMetaInfo.UID, 1L);
                Map<String, Object> doc = json.createDoc(ONE);
                doc.put(CommonFields.PARSED, true);
                doc.put(MailMetaInfo.URL, "1_100500/1");
                doc.put(CommonFields.BODY_TEXT, "");
                doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
                doc.put(
                    CommonFields.MIMETYPE,
                    MediaType.TEXT_PLAIN.getBaseType().toString());
                doc.put(
                    MailMetaInfo.CONTENT_TYPE,
                    doc.get(CommonFields.MIMETYPE));
                doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
                json.assertEquals(text);
            }
        }
    }

    private void checkSimpleJson(final String body, final String json)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(getClass().getResource(SIMPLE).toURI()));
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(body + CRLF + CRLF));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(json),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPostWithoutMid() throws Exception {
        checkSimpleJson(
            MailMetaInfo.X_YANDEX_SUID + MailMetaInfo.HEADERS_SEPARATOR + SUID,
            "{\"prefix\":\"9000\",\"docs\":[{\"suid\":\"9000\",\"hid\":\"1\","
            + "\"built_date\":\"<any value>\",\"content_type\":\"text/plain\","
            + "\"body_text\":\"\",\""
            + "pure_body\":\"Hello, world\",\"meta\":\"Content-Type:text/plain"
            + "; charset=windows-1252\",\"parsed\":true,\"mimetype\":"
            + "\"text/plain\"}]}");
    }

    @Test
    public void testPostWithoutSuid() throws Exception {
        checkSimpleJson(
            MailMetaInfo.X_YANDEX_MID + MailMetaInfo.HEADERS_SEPARATOR + MID,
            "{\"docs\":[{\"mid\":\"100500\",\"hid\":\"1\",\"built_date\":\""
            + "<any value>\",\"content_type\":\"text/plain\","
            + "\"body_text\":\"\",\"pure_body\":\"Hello,"
            + " world\",\"meta\":\"Content-Type:text/plain; "
            + "charset=windows-1252\",\"parsed\":true,\"mimetype\":\""
            + "text/plain\"}]}");
    }

    @Test
    public void testPostWithoutMidAndSuid() throws Exception {
        checkSimpleJson(
            "",
            "{\"docs\":[{\"hid\":\"1\",\"built_date\":\"<any value>\","
            + "\"content_type\":\"text/plain\",\"body_text\":\"\","
            + "\"pure_body\":\"Hello, world\",\"meta\":\""
            + "Content-Type:text/plain; charset=windows-1252\",\"parsed\":true"
            + ",\"mimetype\":\"text/plain\"}]}");
    }

    @Test
    public void testShortBody() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, "<hello");
            backend.start();
            server.start();
            HttpPost post = new HttpPost(
                LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(POST_BODY));
            HttpResponse response = client.execute(post);
            Assert.assertEquals(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                response.getStatusLine().getStatusCode());
            YandexAssert.assertContains(
                "Stream is shorter than skip mark",
                EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        }
    }

    @Test
    public void testNoBody() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, "<Hello, world!");
            backend.start();
            server.start();
            HttpPost post = new HttpPost(
                LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(POST_BODY));
            HttpResponse response = client.execute(post);
            Assert.assertEquals(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                response.getStatusLine().getStatusCode());
            YandexAssert.assertContains(
                "Skip mark not found",
                EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        }
    }

    private static void checkMail(final String name, final Json json)
        throws Exception
    {
        checkMail(name, json, POST_BODY);
    }

    private static void checkMail(
        final String name,
        final Json json,
        final String body)
        throws Exception
    {
        checkMail(name, json, new StringEntity(body));
    }

    private static void checkMail(
        final String name,
        final Json json,
        final HttpEntity body)
        throws Exception
    {
        checkMail(name, json, body, "", "");
    }

    private static void checkMail(
        final String name,
        final Json json,
        final String body,
        final String configSuffix)
        throws Exception
    {
        checkMail(name, json, body, configSuffix, "");
    }

    private static void checkMail(
        final String name,
        final Json json,
        final String body,
        final String configSuffix,
        final String uriSuffix)
        throws Exception
    {
        checkMail(name, json, new StringEntity(body), configSuffix, uriSuffix);
    }

    private static void checkMail(
        final String name,
        final Json json,
        final HttpEntity body,
        final String configSuffix,
        final String uriSuffix)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server =
                new Server(
                    ServerTest.getConfig(
                        backend.port(),
                        configSuffix
                        + "\nstorage.uri-suffix = service=tikaite")))
        {
            URL url = OfflineMailHandlerTest.class.getResource(name);
            File file;
            if (url == null) {
                file = new File(Paths.getSandboxResourcesRoot() + '/' + name);
            } else {
                file = new File(url.toURI());
            }
            backend.add(GET + name + "?raw&service=tikaite", file);
            backend.start();
            server.start();
            HttpPost post = new HttpPost(
                LOCALHOST + server.port() + GET + name + NAME + uriSuffix);
            post.setEntity(body);
            OnlineMailHandlerTest.checkMail(client, post, json);
        }
    }

    @Test
    public void testSimple() throws Exception {
        Json json = new Json(MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        checkMail(SIMPLE, json);
    }

    @Test
    public void testSimpleCompletePost() throws Exception {
        Json json = new Json(MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        String from = "noreply2@e2.kuponator.ru\n";
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"Купонатор\" <noreply2@e2.kuponator.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Купонатор\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO,
            "\"Семинары по институциональному подходу\" "
            + "<instituitions@googlegroups.com>,\"Абрамов Роман "
            + "Николаевич\" <roman_na@mail.ru>,\"Акимкин "
            + "Евгений Михайлович\" <akimkin@isras.ru>,"
            + "\"Алейник Владимир Александрович\" "
            + "<aleinik2006@yandex.ru>,\"Валерий Алексеев\" "
            + "<v.alekseev@pressto.ru>,\"Алексеева Лада "
            + "Никитична\" <alexlada@mail.ru>,\"Астафьева "
            + "Ольга Николаевна\" <onastafieva@mail.ru>,"
            + "\"Афанасьев Георгий Эдгарович\" "
            + "<metod@metod.ru>,\"Балицкая Ксения\" "
            + "<sarbina_gugo@mail.ru>,\"Баранов Павел "
            + "Васильевич\" <baranov.p@gmail.com>,\"Батыгин "
            + "Кирилл\" <kirillbatygin@gmail.com>,\"Бахтурин "
            + "Дмитрий Александрович\" <rozmysl@mail.ru>,"
            + "\"Битехтина Любовь\" <l-bitehtina@yandex.ru>,"
            + "\"Олеся Бондаренко\" <o.bondarenko@gmail.com>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            "instituitions@googlegroups.com\n"
            + "roman_na@mail.ru\n"
            + "akimkin@isras.ru\n"
            + "aleinik2006@yandex.ru\n"
            + "v.alekseev@pressto.ru\n"
            + "alexlada@mail.ru\n"
            + "onastafieva@mail.ru\n"
            + "metod@metod.ru\n"
            + "sarbina_gugo@mail.ru\n"
            + "baranov.p@gmail.com\n"
            + "kirillbatygin@gmail.com\n"
            + "rozmysl@mail.ru\n"
            + "l-bitehtina@yandex.ru\n"
            + "o.bondarenko@gmail.com\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            "instituitions@googlegroups.com\nroman_na@mail.ru\n"
            + "akimkin@isras.ru\naleinik2006@yandex.ru\n"
            + "v.alekseev@pressto.ru\nalexlada@mail.ru\n"
            + "onastafieva@mail.ru\nmetod@metod.ru\n"
            + "sarbina_gugo@mail.ru\nbaranovp@gmail.com\n"
            + "kirillbatygin@gmail.com\nrozmysl@mail.ru\n"
            + "l-bitehtina@yandex.ru\nobondarenko@gmail.com\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            "Семинары по институциональному "
            + "подходу\nА"
            + "брамов Роман Николаевич\n"
            + "Акимкин Евгений Михайлович\n"
            + "Алейник Владимир Александрович\n"
            + "Валерий Алексеев\n"
            + "Алексеева Лада Никитична\n"
            + "Астафьева Ольга Николаевна\n"
            + "Афанасьев Георгий Эдгарович\n"
            + "Балицкая Ксения\n"
            + "Баранов Павел Васильевич\n"
            + "Батыгин Кирилл\n"
            + "Бахтурин Дмитрий Александрович\n"
            + "Битехтина Любовь\n"
            + "Олеся Бондаренко\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "ᴥ Море зовет, скидки манят! г. Краснодар");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1372426349");
        doc.put(
            MailMetaInfo.STID,
            "3772.252328120.132194756120010885460467129600");
        checkMail(
            SIMPLE,
            json,
            "X-Yandex-Mid: " + MID
            + "\nX-Yandex-Suid: " + SUID
            + "\nX-Yandex-Stid: 3772.252328120.132194756120010885460467129600"
            + "\nX-Yandex-Received: 1372426349"
            + "\nX-Yandex-Meta-HdrFrom: ItCa0YPQv9C+0L3QsNGC0L7RgCIgPG5vcmVwbH"
            + "kyQGUyLmt1cG9uYXRvci5ydT4="
            + "\nX-Yandex-Meta-HdrSubject: 4bSlINCc0L7RgNC1INC30L7QstC10YIsING"
            + "B0LrQuNC00LrQuCDQvNCw0L3Rj9GCISDQsy4g0JrRgNCw0YHQvdC+0LTQsNGA"
            + "\nX-Yandex-Meta-HdrTo: ItCh0LXQvNC40L3QsNGA0Ysg0L/QviDQuNC90YHR"
            + "gtC40YLRg9GG0LjQvtC90LDQu9GM0L3QvtC80YMg0L/QvtC00YXQvtC00YMiIDx"
            + "pbnN0aXR1aXRpb25zQGdvb2dsZWdyb3Vwcy5jb20+LCLQkNCx0YDQsNC80L7Qsi"
            + "DQoNC+0LzQsNC9INCd0LjQutC+0LvQsNC10LLQuNGHIiA8cm9tYW5fbmFAbWFpb"
            + "C5ydT4sItCQ0LrQuNC80LrQuNC9INCV0LLQs9C10L3QuNC5INCc0LjRhdCw0LnQ"
            + "u9C+0LLQuNGHIiA8YWtpbWtpbkBpc3Jhcy5ydT4sItCQ0LvQtdC50L3QuNC6INC"
            + "S0LvQsNC00LjQvNC40YAg0JDQu9C10LrRgdCw0L3QtNGA0L7QstC40YciIDxhbG"
            + "VpbmlrMjAwNkB5YW5kZXgucnU+LCLQktCw0LvQtdGA0LjQuSDQkNC70LXQutGB0"
            + "LXQtdCyIiA8di5hbGVrc2VldkBwcmVzc3RvLnJ1Piwi0JDQu9C10LrRgdC10LXQ"
            + "stCwINCb0LDQtNCwINCd0LjQutC40YLQuNGH0L3QsCIgPGFsZXhsYWRhQG1haWw"
            + "ucnU+LCLQkNGB0YLQsNGE0YzQtdCy0LAg0J7Qu9GM0LPQsCDQndC40LrQvtC70L"
            + "DQtdCy0L3QsCIgPG9uYXN0YWZpZXZhQG1haWwucnU+LCLQkNGE0LDQvdCw0YHRj"
            + "NC10LIg0JPQtdC+0YDQs9C40Lkg0K3QtNCz0LDRgNC+0LLQuNGHIiA8bWV0b2RA"
            + "bWV0b2QucnU+LCLQkdCw0LvQuNGG0LrQsNGPINCa0YHQtdC90LjRjyIgPHNhcmJ"
            + "pbmFfZ3Vnb0BtYWlsLnJ1Piwi0JHQsNGA0LDQvdC+0LIg0J/QsNCy0LXQuyDQkt"
            + "Cw0YHQuNC70YzQtdCy0LjRhyIgPGJhcmFub3YucEBnbWFpbC5jb20+LCLQkdCw0"
            + "YLRi9Cz0LjQvSDQmtC40YDQuNC70LsiIDxraXJpbGxiYXR5Z2luQGdtYWlsLmNv"
            + "bT4sItCR0LDRhdGC0YPRgNC40L0g0JTQvNC40YLRgNC40Lkg0JDQu9C10LrRgdC"
            + "w0L3QtNGA0L7QstC40YciIDxyb3pteXNsQG1haWwucnU+LCLQkdC40YLQtdGF0Y"
            + "LQuNC90LAg0JvRjtCx0L7QstGMIiA8bC1iaXRlaHRpbmFAeWFuZGV4LnJ1Piwi0"
            + "J7Qu9C10YHRjyDQkdC+0L3QtNCw0YDQtdC90LrQviIgPG8uYm9uZGFyZW5rb0Bn"
            + "bWFpbC5jb20+\n\n");
    }

    @Test
    public void testAlternativeComplete() throws Exception {
        String headers =
            "received: from smtp7.mail.yandex.net ([77.88.31.55])"
            + "\tby mxback15.mail.yandex.net with LMTP id bltGmICf"
            + "\tfor <analizer@yandex.ru>; Wed, 17 Oct 2012 18:37:47 +0400\n"
            + "received: from smtp7.mail.yandex.net (localhost [127.0.0.1])"
            + "\tby smtp7.mail.yandex.net (Yandex) with ESMTP id 8425315804C1"
            + "\tfor <analizer@yandex.ru>;"
            + " Wed, 17 Oct 2012 18:37:47 +0400 (MSK)\n"
            + "received: from dhcp168-83-red3.yandex.net"
            + " (dhcp168-83-red3.yandex.net [37.140.168.83])"
            + "\tby smtp7.mail.yandex.net (nwsmtp/Yandex) with SMTP id"
            + " YmY0GNCh-Z1YapQi9;\tWed, 17 Oct 2012 18:35:05 +0400\n"
            + "received: from smtp7.mail.yandex.net"
            + " (smtp7.mail.yandex.net [37.140.169.83])"
            + "\tby smtp8.mail.yandex.net (nwsmtp/Yandex) with SMTP id"
            + " ZZY0GNCh-Z1YapQi9;\tWed, 17 Oct 2012 18:35:05 +0400\n"
            + "x-yandex-front: smtp7.mail.yandex.net\n"
            + "x-yandex-timemark: 1350484505\n"
            + "message-id: <20121017183747.Z1YapQi9@smtp7.mail.yandex.net>\n"
            + "date: Wed, 17 Oct 2012 18:37:47 +0400\n"
            + SPAM
            + "dkim-signature: v=1; a=rsa-sha256; c=relaxed/relaxed;"
            + " d=yandex.ru; s=mail; t=1350484667;"
            + "\tbh=oT2f+naCCG2xTypnqagJN7BWzm9GgW/GOsVlyMOUMBA=;"
            + "\th=from:to:Subject:Content-Type;"
            + "\tb=jwEOj7pJS+z/tVYQnI827D6/VTs1pl910NA2SFPP4o"
            + "NtFaMyxVzy1r27C+pPBGlCO"
            + "\t XlYMwSbQiZbr3LTgdMOorsNmTmNunu/pvwLT3HUZhBv"
            + "S66C78vopMEeZL5cGynU4BB"
            + "\t +mi5hz3h4Jw9ritZamSHaLfH19wTMMseif6G8FHU=\n"
            + "from: dpotapov@yandex-team.ru\n"
            + "to: DIMITRY <analizer@yandex.ru>\n"
            + "subject: alternative test\n"
            + "content-type: multipart/alternative; boundary=myboundary\n"
            + "return-path: analizer@yandex.ru\n"
            + "x-yandex-forward: 8fc19e2779405517e1e0d1080eb0446c\n"
            + "content-type: text/html";
        Json json = new Json(headers, MID, SUID);
        String from = "dpotapov@yandex-team.ru";
        String to = "DIMITRY <analizer@yandex.ru>";
        String email = "analizer@yandex.ru\n";
        String name = "DIMITRY\n";
        String received = "1350484505";
        String gwReceived = "1350484667";
        String smtpId = "YmY0GNCh-Z1YapQi9";
        String subject = "alternative test";
        Object receivedParseError =
            new Json.Contains("Yandex MX impersonation detected");
        Map<String, Object> doc =
            json.createDoc(ONE_ONE, headers.replaceAll("t/html", "t/plain"));
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Hi, there!");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gwReceived);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_PARSE_ERROR, receivedParseError);
        doc.put(MailMetaInfo.RECEIVED_DATE, received);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            name);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);

        doc = json.createDoc(ONE_TWO, headers + BASE64);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        doc.put(MailMetaInfo.RECEIVED_DATE, received);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gwReceived);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_PARSE_ERROR, receivedParseError);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            name);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        checkMail(ALTERNATIVE_COMPLETE, json);
    }

    private void testRtfAttach(final TextExtractOptions.Mode mode)
        throws Exception
    {
        String headers = TO + '\n' + MIXED;
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc =
            json.createDoc(ONE_ONE, headers + CONTENT_TYPE_7_BIT);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-disposition: attachment;\tfilename=\"1.RTF\""
            + BASE64
            + "\ncontent-type: text/rtf;\tname=\"1.RTF\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, HELLO_WORLD_RUS);
        doc.put(CommonFields.MIMETYPE, APPLICATION_RTF);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/rtf");
        doc.put(CommonFields.META, CONTENT_TYPE_RTF);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.ATTACHNAME, "1.RTF");
        doc.put(MailMetaInfo.ATTACHSIZE, RTFATTACH_SIZE);
        doc.put(MailMetaInfo.ATTACHTYPE, RTF);
        doc.put(MailMetaInfo.MD5, RTFATTACH_MD5);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        checkMail(RTFATTACH, json, POST_BODY, "", '&' + mode.cgiFlag());
    }

    @Test
    public void testRtfAttachUltraFast() throws Exception {
        testRtfAttach(TextExtractOptions.Mode.ULTRA_FAST);
    }

    @Test
    public void testRtfAttachFast() throws Exception {
        testRtfAttach(TextExtractOptions.Mode.FAST);
    }

    @Test
    public void testRtfAttachSlow() throws Exception {
        testRtfAttach(TextExtractOptions.Mode.NORMAL);
    }

    @Test
    public void testRtfAttachTruncateHeaders() throws Exception {
        String headers =
            "to: analizer@yandex.r\ncontent-type: multipart/mixed; ";
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + "\ncontent-transfer-: 7bit\ncontent-type: text/html");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        String email = "analizer@yandex.r";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc = json.createDoc(
            ONE_TWO,
            headers + "\ncontent-dispositi: attachment;\tfilen");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, HELLO_WORLD_RUS);
        doc.put(CommonFields.MIMETYPE, APPLICATION_RTF);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/rtf");
        doc.put(CommonFields.META, CONTENT_TYPE_RTF);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email + '\n');
        // header parameters now parsed prior to truncation
        doc.put(MailMetaInfo.ATTACHNAME, "1.RTF");
        doc.put(MailMetaInfo.ATTACHSIZE, RTFATTACH_SIZE);
        doc.put(MailMetaInfo.ATTACHTYPE, RTF);
        doc.put(MailMetaInfo.MD5, RTFATTACH_MD5);
        checkMail(
            RTFATTACH,
            json,
            POST_BODY,
            "\nextractor.headers-length-limit = 60"
            + "\nextractor.header-length-limit = 17");
    }

    @Test
    public void testUtf8Attach() throws Exception {
        String headers =
            "to: =?utf-7?Q?+BB8EQAQ4BDIENQRC_+BDwEOARA-?= "
            + "<analizer@yandex.ru>\n"
            + MIXED
            + "\ncc: =?utf-7?Q?+BCEENQQ8BDUEPQQ+BDIEMA_+BBAEPQQ9BD?= "
            + "<gdou26skazka@yandex.ru>";
        String to = "Привет мир <analizer@yandex.ru>";
        String toName = "Привет мир\n";
        String cc = "Семенова Анн <gdou26skazka@yandex.ru>";
        String mail = "gdou26skazka@yandex.ru\n";
        String name = "Семенова Анн\n";
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + EIGHT_BIT + PLAIN);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/plain");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL, mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.DISPLAY_NAME,
            name);
        String filename =
            "=?UTF-8?B?0KDRg9GB0YHQutCw0LPQsCDQr9C30YvQutCw4oSiLnR4dA==?=";
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-disposition: attachment;\tfilename=\"" + filename
            + '"' + BASE64 + "\ncontent-type: text/rtf;\tname=\"" + filename
            + '"');
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, HELLO_WORLD_RUS);
        doc.put(CommonFields.MIMETYPE, APPLICATION_RTF);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/rtf");
        doc.put(CommonFields.META, CONTENT_TYPE_RTF);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            toName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC, cc);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.EMAIL, mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.NORMALIZED,
            mail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.CC + MailMetaInfo.DISPLAY_NAME,
            name);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "Русскага Языка\u2122.txt");
        doc.put(MailMetaInfo.ATTACHSIZE, RTFATTACH_SIZE);
        doc.put(MailMetaInfo.ATTACHTYPE, "rtf txt text");
        doc.put(MailMetaInfo.MD5, RTFATTACH_MD5);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        checkMail("utf8attach.txt", json);
    }

    @Test
    public void testWindows1251Base64Attach() throws Exception {
        String headers = TO + '\n' + MIXED;
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + EIGHT_BIT + PLAIN);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD_RUS);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, "Content-Type:text/plain; charset=KOI8-R");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-disposition: attachment"
            + BASE64
            + "\ncontent-type: text/plain; name=\"test.txt\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, HELLO_WORLD_RUS);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1251);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.ATTACHNAME, "test.txt");
        doc.put(MailMetaInfo.ATTACHSIZE, (long) HELLO_WORLD_RUS.length());
        doc.put(MailMetaInfo.ATTACHTYPE, TXT);
        doc.put(MailMetaInfo.MD5, "E6B06E8194EFDC7950D8CF54D550E6A5");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        checkMail("windows1251base64attach.txt", json);
    }

    @Test
    public void testAlternativeMixed() throws Exception {
        Json json = new Json(ALTERNATIVE + '\n' + MIXED + BASE64, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE_ONE, ALTERNATIVE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(ONE_TWO_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_AGAIN);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);

        doc = json.createDoc(ONE_TWO_TWO);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("Using Unicode with MIME"));
        doc.put(CommonFields.MIMETYPE, PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.TOOL, "MacWrite Pro: LaserWriter 8 8.1." + 1);
        doc.put(CommonFields.PAGES, 5L);
        doc.put(CommonFields.AUTHOR, "David Goldsmit" + 'h');
        doc.put(CommonFields.TITLE, "MIME&Unicode");
        doc.put(
            CommonFields.PRODUCER,
            "Acrobat Distiller Command 3.01 for Solaris 2.3 and later "
            + "(SPARC)");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                "pdf:PDFVersion:1.1\npdf:encrypted:false\n"
                + "pdf:hasMarkedContent:false\npdf:hasXFA:false\n"
                + "pdf:hasXMP:false\n"
                + "pdf:docinfo:creator:David Goldsmith\npdf:docinfo:"
                + "creator_tool:MacWrite Pro: LaserWriter 8 8.1.1\npdf:docinfo"
                + ":producer:Acrobat Distiller Command 3.01 for Solaris"
                + " 2.3 and later (SPARC)\npdf:docinfo:title:MIME&Unicode\n"
                + CONTENT_TYPE_PDF
                + "\ndc:format:application/pdf; version=1.1"
                + PdfBoxTest.PDFBOX_META));
        checkMail("alternative.mixed.txt", json);
    }

    @Test
    public void testMixedAlternative() throws Exception {
        Json json = new Json(MIXED + '\n' + ALTERNATIVE, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE_ONE_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(ONE_ONE_TWO);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_AGAIN);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(ONE_TWO, MIXED);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, YET_ANOTHER_HELLO);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        String file = "mixed.alternative.txt";
        checkMail(file, json);

        json = new Json(MIXED + '\n' + ALTERNATIVE, MID, SUID);
        doc = json.createDoc(ONE_TWO, MIXED);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, YET_ANOTHER_HELLO);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        checkMail(file, json, POST_BODY, HID_DEPTH_2);
    }

    @Test
    public void testMixedMixedAlternative() throws Exception {
        Json json = new Json(MIXED, MID, SUID);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE_ONE,
            MIXED + '\n' + MIXED + '2'
            + "\ncontent-type: text/plain; name=\"hello1.txt\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        doc.put(MailMetaInfo.ATTACHNAME, "hello1.txt");
        doc.put(MailMetaInfo.ATTACHSIZE, 12L);
        doc.put(MailMetaInfo.ATTACHTYPE, TXT);
        doc.put(MailMetaInfo.MD5, "BC6E6F16B8A077EF5FBC8D59D0B931B9");

        doc = json.createDoc(
            ONE_ONE_TWO,
            MIXED + '\n' + MIXED + "2\ncontent-disposition: attachment");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "Hello again\n>This is not a quote");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        doc.put(MailMetaInfo.ATTACHSIZE, 33L);
        doc.put(MailMetaInfo.ATTACHTYPE, TXT);
        doc.put(MailMetaInfo.MD5, "938827AAE7CF0581E088B7D0A8C4EAB2");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);

        doc = json.createDoc(ONE_TWO_ONE, MIXED + '\n' + ALTERNATIVE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, YET_ANOTHER_HELLO);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);

        doc = json.createDoc(ONE_TWO_TWO, MIXED + '\n' + ALTERNATIVE + BASE64);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("Using Unicode" + " with MIME"));
        doc.put(CommonFields.MIMETYPE, PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.TOOL, "MacWrite Pro: LaserWriter 8 8.1.1");
        doc.put(CommonFields.PAGES, 5L);
        doc.put(CommonFields.AUTHOR, "David Goldsmith");
        doc.put(CommonFields.TITLE, "MIME&Unicod" + 'e');
        doc.put(
            CommonFields.PRODUCER,
            "Acrobat Distiller Command 3.01 for Solaris 2.3 and later"
            + " (SPARC)");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                CONTENT_TYPE_PDF
                + "\npdf:PDFVersion:1.1"
                + "\npdf:encrypted:false\n"
                + "pdf:hasMarkedContent:false\npdf:hasXFA:false\n"
                + "pdf:hasXMP:false\n"
                + "pdf:docinfo:creator:David Goldsmith\n"
                + "pdf:docinfo:creator_tool:MacWrite Pro: LaserWriter 8 8.1.1"
                + "\npdf:docinfo:producer:Acrobat Distiller Command 3.01 for "
                + "Solaris 2.3 and later (SPARC)\n"
                + "pdf:docinfo:title:MIME&Unicode\n"
                + "dc:format:application/pdf; version=1.1"
                + PdfBoxTest.PDFBOX_META));
        checkMail("mixed.mixed.alternative.txt", json);
    }

    @Test
    public void testRfc822() throws Exception {
        String headers =
            "received: by localhost.mail.yandex.net with SMTP id abyr-ABYRWALG"
            + "; Wed, 17 Oct 2012 18:35:05 +0400\n"
            + TO + '\n' + MIXED + "\nsubject: root subject";
        String csv = "\ncontent-type: text/csv";
        String rootSubject = "root subject";
        String smtpId = "abyr-ABYRWALG";
        String attachSmtpId = "bitethe-dust";
        String gatewayReceivedDate = "1350484505";
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc =
            json.createDoc(ONE_ONE, headers + EIGHT_BIT + PLAIN + UTF8);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "forward test");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, rootSubject);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);

        String nestedHeaders =
            headers + EIGHT_BIT + "\ncontent-type: message/rfc822";
        String subject = "ещё один тест ջվգե afфыва ջգվ ı İ ß ẞ\n.";

        doc = json.createDoc(ONE_TWO, nestedHeaders);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(
            MailMetaInfo.CONTENT_TYPE,
            NestedMessageHandler.MESSAGE_RFC822);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, rootSubject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.ATTACHNAME, subject + NestedMessageHandler.EML);
        doc.put(MailMetaInfo.ATTACHSIZE, 0L);
        doc.put(MailMetaInfo.ATTACHTYPE, NestedMessageHandler.ATTACHTYPE);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);

        String file = "rfc822.txt";
        checkMail(file, json, POST_BODY, HID_DEPTH_2);

        String nestedHeaders2 =
            nestedHeaders
            + "\nreceived: by another.mail.yandex.net with SMTP id "
            + "bitethe-dust; Wed, 17 Oct 2012 18:35:05 +0400"
            + "\nfrom: Dmitry Potapov <Analizer@yandex.ru>"
            + "\nsubject: =?utf-8?B?0LXRidGRINC+0LTQuNC9INGC0LXRgdGCINW71b7V"
            + "o9WlIGFm0YTRi9Cy0LAg1bvVo9W+IMSxIMSwIMOf?=\t"
            + "=?utf-8?B?IOG6ngou?="
            + EIGHT_BIT + csv + UTF8;
        String text = "тест ջվգեջ asdf фыва ı İ ß ẞ";
        String from = "Dmitry Potapov <Analizer@yandex.ru>";
        String fromEmail = "Analizer@yandex.ru\n";
        String fromName = "Dmitry Potapov\n";

        doc = json.createDoc(ONE_TWO_ONE, nestedHeaders2);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, text);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/csv");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            rootSubject + '\n' + subject);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.ATTACH_SMTP_ID, attachSmtpId);
        doc.put(MailMetaInfo.ALL_ATTACH_SMTP_IDS, attachSmtpId);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);
        checkMail(file, json);
    }

    @Test
    public void testSimplestQuotedPrintable() throws Exception {
        Json json = new Json(FieldName.CONTENT_TRANSFER_ENCODING.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR + MimeUtil.ENC_QUOTED_PRINTABLE
            + "\ncontent-type: text/plain; charset=utf-8"
            + "\nsubject: =?badenc?b?helo?=",
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "=?badenc?b?helo?=");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "If you believe that truth=beauty, then surely mathematics is the "
            + "most beautiful branch of philosophy.");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        checkMail(SIMPLEST_QUOTED_PRINTABLE, json);
    }

    @Test
    public void testBadXml() throws Exception {
        Json json = new Json(FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + MediaType.APPLICATION_XML.getBaseType(),
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, SOME_TEXT);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.APPLICATION_XML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.ERROR, XML_ERROR);
        doc.put(CommonFields.META, "Content-Type:application/xml");
        checkMail("simplest.badxml.txt", json);
    }

    @Test
    public void testEmptyAlternative() throws Exception {
        checkMail("alternative.empty.txt", new Json(null, null, SUID));
    }

    @Test
    public void testEmptyXml() throws Exception {
        Json json = new Json(ALTERNATIVE_HEADERS + XML, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, APPLICATION_XML);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/xml");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, CONTENT_TYPE_XML);
        checkMail("alternative.empty.xml.txt", json);
    }

    @Test
    public void testAlternativeBadAlternative() throws Exception {
        Json json = new Json(ALTERNATIVE_HEADERS + XML, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, APPLICATION_XML);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/xml");
        doc.put(CommonFields.BODY_TEXT, SOME_TEXT);
        doc.put(CommonFields.ERROR, XML_ERROR);
        doc.put(CommonFields.META, CONTENT_TYPE_XML);
        checkMail("alternative.bad.txt", json);
    }

    @Test
    public void testAlternativeBadFirstAlternative() throws Exception {
        Json json = new Json(ALTERNATIVE_HEADERS, MID, SUID);
        Map<String, Object> doc =
            json.createDoc(ONE_ONE, ALTERNATIVE_HEADERS + XML);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, APPLICATION_XML);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/xml");
        doc.put(CommonFields.BODY_TEXT, SOME_TEXT);
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.META, CONTENT_TYPE_XML);

        doc = json.createDoc(ONE_TWO, ALTERNATIVE_HEADERS + PLAIN);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        checkMail("alternative.bad.first.txt", json);
    }

    @Test
    public void testAlternativeBadSecondAlternative() throws Exception {
        Json json = new Json(ALTERNATIVE_HEADERS + PLAIN, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);

        doc = json.createDoc(ONE_TWO, ALTERNATIVE_HEADERS + XML);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, APPLICATION_XML);
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/xml");
        doc.put(CommonFields.BODY_TEXT, SOME_TEXT);
        doc.put(CommonFields.ERROR, Json.ANY_VALUE);
        doc.put(CommonFields.META, CONTENT_TYPE_XML);
        checkMail("alternative.bad.second.txt", json);
    }

    @Test
    public void testUrlExtractor() throws Exception {
        Json json = new Json(MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(MailMetaInfo.MID, MID);
        doc.put(MailMetaInfo.SUID, SUID.toString());
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(CommonFields.TITLE, "http://google.com");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "http://someurl.here.info\nhttp://duplicate.com "
            + "http://duplicate.com\nwww.another.link.ru\n"
            + "www.yet.another.link.ru/some.html\nhttp://ya.ru\n"
            + "Read this email\nonline.\n"
            + "tip name\n"
            + "http://bad.\nurl\n.com\n"
            + "http://norm.org/\n"
            + "https://norm.org/path https://norm.org/path/\n"
            + "ftp://subpath.us/somepath/somehtml.html ftp"
            + "://subpath.us/somepath/\n"
            + "192.8.1.1 http://192.8.1.2 192.8.1.3:/ 192.8.1.4:8080/path\n"
            + "ftp://with.port ftp://with.port:21/\n"
            + "www.with.port www.with.port:80/\n"
            + "https://with.port https://with.port:443/\n"
            + "http://with.port/path http://with.port:80/path\n"
            + "http://me@yandex.ru/ http://@oauth.com/\n"
            + "http://some:port@yandex.ru/ http://port:123@oauth.com/\n"
            + "http://portless:@yandex.ru/ http://port:123@oauth.com:456/\n"
            + "http://2gis.ru www.2gis.ru:88/path\n"
            + "xn--d1acpjx3f.xn--p1ai http://яндекс.рф/s");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://someurl.here.info\n"
            + "http://duplicate.com\n"
            + "http://www.another.link.ru\n"
            + "http://www.yet.another.link.ru/some.html\n"
            + "http://yandex.ru\n"
            + "http://ya.ru\n"
            + "http://ibm.com/developerworks/ecma/campaign/er.jsp"
            + "?id=666&imid=100500&end\n"
            + "http://norm.org\n"
            + "https://norm.org/path\n"
            + "https://norm.org/path/\n"
            + "ftp://subpath.us/somepath/somehtml.html\n"
            + "ftp://subpath.us/somepath/\n"
            + "http://192.8.1.2\n"
            + "http://192.8.1.3\n"
            + "http://192.8.1.4:8080/path\n"
            + "ftp://with.port\n"
            + "http://www.with.port\n"
            + "https://with.port\n"
            + "http://with.port/path\n"
            + "http://me@yandex.ru\n"
            + "http://oauth.com\n"
            + "http://some:port@yandex.ru\n"
            + "http://port:123@oauth.com\n"
            + "http://portless:@yandex.ru\n"
            + "http://port:123@oauth.com:456\n"
            + "http://2gis.ru\n"
            + "http://www.2gis.ru:88/path\n"
            + "http://яндекс.рф\n"
            + "http://яндекс.рф/s\n"
            + "http://yandex.online/256//taxi_ya.php?sd==&\n"
            + "http://yandex.ru/prd.phpsfdsf?email=anna@yandex.ru&b=dGFQ==\n"
            + "http://yandex.online/256//taxi_ya.php?b=dleC5ydQ==&\n"
            + "http://yandex.ru"
            + "/prd.phpsfdsf?email=lider@yandex.ru&b=dGFydQ==\n");
        checkMail("urls.txt", json);
    }

    @Test
    public void testBareHtml() throws Exception {
        Json json = new Json(MailMetaInfo.SUBJECT
            + MailMetaInfo.HEADERS_SEPARATOR
            + "=?utf-8?b?0JPQsNC50LTQv9Cw0YDQujog0YMg0JLQsNGBINC90L7QstC+0LUg0"
            + "YHQvtC+0LHR?= =?utf-8?b?idC10L3QuNC1?=" + HTML + UTF8 + BASE64,
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "Гайдпарк: у Вас новое сообщение");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Здравствуйте, Николай Калугин!\n"
            + "Пользователь Alexander Marichev отправил "
            + "Вам\nсообщение\n.\nЭто письмо "
            + "отправлено автоматически. "
            + "Пожалуйста, не отвечайте на него.\n"
            + "Если Вы считаете, что получили его "
            + "по ошибке, сообщите об этом в "
            + "службу поддержки по этой ссылке:\n"
            + "http://gidepark.ru/user/feedback/"
            + "index\nС уважением, команда Гайдпарка"
            + "\nГайдпарк - социальная сеть");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://gidepark.ru/account/messages/dialog/2738922007\n"
            + "http://gidepark.ru/user/feedback/index\n"
            + "http://gidepark.ru\n");
        checkMail("gideparkru.txt", json);
    }

    @Test
    public void testDoubleContentType() throws Exception {
        Json json = new Json("", MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE, Json.ANY_VALUE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "Re: сообщение kaissylin : Дневник kaissylin (22:37 23-06-2017) "
            + "[6220941/417109184]");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains("Для вас актуальное предложение!"));
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_WINDOWS_1251);
        doc.put(CommonFields.TITLE, "LiveInternet – Уведомления по подписке");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://www.liveinternet.ru\n"
            + "https://www.liveinternet.ru/users/spanconku/\n"
            + "https://www.liveinternet.ru/users/spanconku/profile/\n"
            + "https://www.liveinternet.ru/users/kaissylin/\n"
            + "https://www.liveinternet.ru/users/kaissylin/profile/\n"
            + "https://www.liveinternet.ru/users/kaissylin/post417109184/"
            + "page1.html#BlCom696878565\n"
            + "http://nirvana.fm//go?goaHR0cDovL2JhbmstYXVkaS5ydQ\n"
            + "http://www.liveinternet.ru/member2.php?action="
            + "removesubscriptioncomment&jpostid=417109184\n"
            + "http://www.liveinternet.ru/member2.php?action="
            + "removesubscriptioncomment&type=allcomment\n");
        checkMail("double-content-type.eml", json);
    }

    @Test
    public void testBadOggAttach() throws Exception {
        String headers = TO + '\n' + MIXED;
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc =
            json.createDoc(ONE_ONE, headers + CONTENT_TYPE_7_BIT);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_ISO_8859_1);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-disposition: attachment;\tfilename=\"bad.ogg\""
            + BASE64
            + "\ncontent-type: audio/ogg;\tname=\"bad.ogg\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.MIMETYPE, "application/ogg");
        doc.put(MailMetaInfo.CONTENT_TYPE, "audio/ogg");
        doc.put(
            CommonFields.META,
            new Json.Headers(
                "streams-vorbis:1\nstreams-total:3\nstreams-video:1\n"
                + "streams-audio:1\nstreams-annodex:1\n"
                + "streams-theora:1\nstreams-metadata:1\n"
                + "Content-Type:application/ogg"));
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, EMAIL);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            EMAIL + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            EMAIL + '\n');
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHNAME, "bad.ogg");
        doc.put(MailMetaInfo.ATTACHTYPE, "ogx oga ogg");
        doc.put(MailMetaInfo.ATTACHSIZE, 23414L);
        doc.put(MailMetaInfo.MD5, "843E595996326D00B1E37D7B29D29FB5");
        checkMail("badogg.txt", json);
    }

    @Test
    public void testVideoletter() throws Exception {
        Json json = new Json(
            "received: from web149.yandex.ru ([95.108.130.106])\t"
            + "by mxback24.mail.yandex.net with LMTP id D5HmDhiR; bad time\n"
            + "received: from localhost (localhost.localdomain [127.0.0.1])\t"
            + "by web149.yandex.ru (Yandex) with ESMTP id C8EA35078021;\t"
            + "Tue, 26 Jul 2011 16:13:04 +0400 (MSD)\n"
            + SPAM
            + "x-yandex-front: web149.yandex.ru\n"
            + "x-yandex-timemark: 1311682385\n"
            + "received: from dhcp4-202.yandex.net (dhcp4-202.yandex.net "
            + "[77.88.4.202]) by web149.yandex.ru with HTTP;\t"
            + "Tue, 26 Jul 2011 16:13:05 +0400\n"
            + "from: \"\\\";\" <yantester@yandex.ru>\n"
            + "to: meta.user@yandex.ru\n"
            + "reply-to: rpop1@mail.ru\n"
            + "subject: =?koi8-r?B?98nExc8g0MnT2M3P?=\n"
            + "mime-version: 1.0\n"
            + "message-id: <30601311682385@web149.yandex.ru>\n"
            + "date: Tue, 26 Jul 2011 16:13:05 +0400\n"
            + "x-mailer: Yamail [ http://yandex.ru ] 5.0"
            + BASE64 + '\n'
            + "content-type: text/html; charset=koi8-r\n"
            + "return-path: yantester@yandex.ru\n"
            + "x-yandex-forward: d1ba1b54db9e02d112d1d53f4b223326\n"
            + "x-yandex-forward: 668d2a493799af92df17528795f04551",
            MID,
            SUID);
        Object receivedParseError = new Json.Contains("bad time");
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Открыть видеописьмо"
            + "\nПароль: gtgG4"
            + "\nhttp://video.yandex.ru/mail/jLW5_N9dFEgR_sfXyqd03XzrxXwWhIpG"
            + "\n--"
            + "\nололо");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_KOI8_R);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://video.yandex.ru/mail/"
            + "jLW5_N9dFEgR_sfXyqd03XzrxXwWhIpG\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"\\\";\" <yantester@yandex.ru>");
        String from = "yantester@yandex.ru\n";
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "\";\n");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, "meta.user@yandex.ru");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            "meta.user@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            "meta-user@yandex.ru\n");
        doc.put(MailMetaInfo.REPLY_TO_FIELD, "rpop1@mail.ru");
        String replyTo = "rpop1@mail.ru\n";
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyTo);
        // Yandex.Mail takes the last subject
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "Видео письмо");
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, "1311682384");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1311682385");
        doc.put(MailMetaInfo.RECEIVED_PARSE_ERROR, receivedParseError);
        checkMail(
            "videoletter.txt",
            json,
            POST_BODY_START + MailMetaInfo.X_YANDEX_META_HDRSUBJECT
            + MailMetaInfo.HEADERS_SEPARATOR
            + "0JfQsNC80LXQvdGR0L3QvdGL0Lkg0LfQsNCz0L7Qu9C+0LLQvtC6" + CRLF
            + CRLF);
    }

    @Test
    public void testCorruptedPdfAttach() throws Exception {
        Json json = new Json(
            "content-transfer-encoding: base64",
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, PDF);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PDF);
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.Contains("LOGOSOFT YAZILIM HIZ.SAN"));
        doc.put(
            CommonFields.META,
            new Json.Headers(
                "pdf:PDFVersion:1.2\n"
                + "dc:format:application/pdf; version=1.2\n"
                + "pdf:encrypted:false\n"
                + "pdf:hasMarkedContent:false\npdf:hasXFA:false\n"
                + "pdf:hasXMP:false\n"
                + CONTENT_TYPE_PDF
                + PdfBoxTest.PDFBOX_META));
        doc.put(CommonFields.PAGES, 2L);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://www.citi.com.tr\n"
            + "http://www.denizbank.com\n"
            + "http://www.citibank.com.tr\n");
        checkMail("corruptedpdf.eml", json);
    }

    @Test
    public void testMambaAttachtype() throws Exception {
        String headers =
            "received: from mxfront3j.mail.yandex.net ([127.0.0.1])\t"
            + "by mxfront3j.mail.yandex.net with LMTP id oIIehUKx\t"
            + "for <karlina.zoya@yandex.ru>; Mon, 8 Jul 2013 19:50:18 +0400\n"
            + "received: from support-love.com (support-love.com "
            + "[193.0.170.20])\tby mxfront3j.mail.yandex.net (nwsmtp/Yandex) "
            + "with ESMTP id 6C3qy5eIKU-oIs0hd7H;\t"
            + "Mon,  8 Jul 2013 19:50:18 +0400\n"
            + "x-yandex-front: mxfront3j.mail.yandex.net\n"
            + "x-yandex-timemark: 1373298618\n"
            + "authentication-results: mxfront3j.mail.yandex.net; spf=pass "
            + "(mxfront3j.mail.yandex.net: domain of support-love.com designat"
            + "es 193.0.170.20 as permitted sender) smtp.mail=new-messages@sup"
            + "port-love.com; dkim=pass header.i=@support-love.com\n"
            + SPAM
            + "received: from localhost (wscript4.lan [10.5.2.60])\tby "
            + "support-love.com (Postfix) with ESMTP id 92AEE1F290A4E\tfor <k"
            + "arlina.zoya@yandex.ru>; Mon,  8 Jul 2013 19:50:18 +0400 (MSK)\n"
            + "domainkey-signature: a=rsa-sha1; s=mail; d=support-love.com; c="
            + "simple; q=dns;\tb=ABmfNM5hgnFlNhZXG7/yGVOBBh3YszZQLaDWm3vftkMX5"
            + "GQsDine8UKDIErIUiKEn\tCD3swZ4MLSAF1rbnciQUIGUEetQ3+Sd0SX8kbDF/r"
            + "U5gELBftx6fEknV3QFTcJ5yH83\tyU7Vx74AjPtDZcjXnoQO/jmSCqfmTp5DjaF"
            + "kaH8=\n"
            + "dkim-signature: v=1; a=rsa-sha256; c=simple/simple; d=support-l"
            + "ove.com;\ts=mail; t=1373298618;\tbh=/hCNLqcEoQNvhBgDZVtxB94rl8O"
            + "qpD93ZTchlfF6Cuw=;\th=To:Subject:From:Reply-To:List-Unsubscribe"
            + ":Date;\tb=gqaBvk/dmHKLIYCirkA/iLlkb8u5BseNA0F17c6Xgre2W/qkd/HeM"
            + "07B3HrPqeupF\t ycWBZh+j3mZxcPdeVbsYA0yT25/lcToP9vbdp5+jfvgfPxWz"
            + "K9frIojyesQZtNynpC\t sacMjzJ95Rrg+UYHkPuPMr0rL56iuonEx1/DzGfk="
            + "\nto: <karlina.zoya@yandex.ru>\n"
            + "subject: =?utf-8?Q?=D0=A3=20=D0=B2=D0=B0=D1=81=201=20=D0=BD=D0="
            + "B5=D0=BF=D1=80=D0=BE=D1=87=D0=B8=D1=82=D0=B0=D0=BD=D0=BD=D0=BE="
            + "D0=B5=20=D1=81=D0=BE=D0=BE=D0=B1=D1=89=D0=B5=D0=BD=D0=B8=D0=B5?"
            + "=\nfrom: =?utf-8?Q?\"=D0=A1=D0=B5=D1=82=D1=8C=20=D0=B7=D0=BD=D0"
            + "=B0=D0=BA=D0=BE=D0=BC=D1=81=D1=82=D0=B2=20=D0=9C=D0=B0=D0=BC=D0"
            + "=B1=D0=B0\"?= <new-messages@support-love.com>\n"
            + "reply-to: new-messages@support-love.com\n"
            + "list-unsubscribe: <mailto:unsubscribe@wamba.com?subject=c3new-m"
            + "essages2~karlina.zoya@yandex.ru~214b7960d40fa5fd73c040db53f0bc4"
            + "2>\ndate: Mon, 08 Jul 2013 19:50:18 +0400\n"
            + "content-type: multipart/related; charset=\"utf-8\"; boundary=\""
            + "=_b1ced7adc37dfae81a937bda19868443\"\n"
            + "content-transfer-encoding: quoted-printable"
            + CONTENT_DISPOSITION_INLINE
            + "\nmime-version: 1.0\nmessage-id: "
            + "<20130708155018.92AEE1F290A4E@support-love.com>\n"
            + "return-path: new-messages@support-love.com\n"
            + "x-yandex-forward: 84555f69794d7567a323fc74a33d823f";
        Json json = new Json(headers, MID, SUID);
        String email = "new-messages@support-love.com";
        String from = "\"Сеть знакомств Мамба\" <" + email + '>';
        String fromName = "\"Сеть знакомств Мамба\"\n";
        String toEmail = "karlina.zoya@yandex.ru";
        String toEmailNormalized = "karlina-zoya@yandex.ru";
        String receivedDate = "1373298618";
        String smtpId = "6C3qy5eIKU-oIs0hd7H";
        String subject =
            "У вас 1 непрочитанное сообщение";

        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "\ncontent-type: text/html; charset=\"utf-8\"\n"
            + FieldName.CONTENT_TRANSFER_ENCODING.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR + MimeUtil.ENC_QUOTED_PRINTABLE
            + CONTENT_DISPOSITION_INLINE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.TITLE, "Здравствуйте, Zoya!");
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Zoya! У вас\n1 новое сообщение\n"
            + "1 новое сообщение\nЛоки, 38\n"
            + "Ростов-на-Дону\n"
            + "Пробуй! Команда Mamba.ru\n"
            + "Это сообщение было отправлено на "
            + "адрес karlina.zoya@yandex.ru посредством "
            + "ввода данного e-mail на сайте "
            + "http://mamba.ru/. В случае, если данное "
            + "сообщение попало к вам ошибочно, "
            + "вы можете его проигнорировать или "
            + "пожаловаться в\n"
            + "Службу поддержки пользователей"
            + "\n.\nОбратите внимание, что Сеть "
            + "знакомств Мамба никогда не просит "
            + "вас выслать пароль от вашего "
            + "профайла или почтового ящика.\n"
            + "Если вы не хотите получать "
            + "подобных сообщений, укажите это в\n"
            + "настройках вашей анкеты\n.");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE + '\n' + INLINE);
        String xurls =
            "http://mamba.ru/my/access.phtml?oid=1132437387&secret=jCBKMzDf3O0"
            + "Q4wvO&ts=1373298618&goto=newmessages&token=64ed3336f3c6564827fe"
            + "c6d85a8f5a3d\n"
            + "http://mamba.ru/my/access.phtml?oid=1132437387&secret=jCBKMzDf3"
            + "O0Q4wvO&ts=1373298618&goto=anketa_578509102\nhttp://mamba.ru\n"
            + "http://ad.apps.fm/6Z7SnWAX3iAKTW9DQFu-Ztf00DPwq1qCdxLUIm_ZAze7l"
            + "e-IqQNrhddG0SS8mx-UjXPV0g-nl9q_7Yf5UVZv6zSVJtJLdAERtoLgi2NlNT0"
            + "\nhttp://ad.apps.fm/pH_qig0VWYef6RMng-fT7a914wHrDm-B2krNaaQ_1TM"
            + "yJg1BCm39Jh3A8GX_9k6Iiun0yCLoYE7TUUAsepiw4g\n"
            + "http://ad.apps.fm/gzcvGq3rhA1ceCf-kKwAaSFxyKYo0eFP0jSrQWuWILT2A"
            + "W-ysY-9nsguLD4DOF8238rT4FYLWdynUNc7hvpX3Q2THPdiwubFtAwIEnDLBC65"
            + "rKgHawZACxncgWjQ7N8S7heNA-tEI4hLOQh2pk1toYDqJ_caXxKUFa1ZRpiSoW4"
            + "\nhttp://mamba.ru/my/access.phtml?oid=1132437387"
            + "&secret=jCBKMzDf3O0Q4wvO"
            + "&ts=1373298618&token=64ed3336f3c6564827fec6d85a8f5a3d"
            + "&goto=/support.phtml\n"
            + "http://mamba.ru/my/access.phtml"
            + "?oid=1132437387&secret=jCBKMzDf3O0Q4wvO&ts=1373298618"
            + "&token=64ed3336f3c6564827fec6d85a8f5a3d"
            + "&goto=/my/settings_messages.phtml\n";
        doc.put(MailMetaInfo.X_URLS, xurls);

        String ratio = "120:43";
        doc = json.createDoc(
            ONE_TWO,
            headers + PNG + BASE64
            + "\ncontent-id: <abe0dbd43d02ce1b63edba285f8c8a85>"
            + CONTENT_DISPOSITION_INLINE
            + "; filename=\"abe0dbd43d02ce1b63edba285f8c8a85\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_PNG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, new Json.Headers(PNG_META));
        doc.put(CommonFields.CREATED, CREATED);
        doc.put(CommonFields.HEIGHT, 43L);
        doc.put(CommonFields.WIDTH, 120L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, ratio);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE + '\n' + INLINE);
        doc.put(MailMetaInfo.ATTACHNAME, "abe0dbd43d02ce1b63edba285f8c8a85");
        doc.put(MailMetaInfo.ATTACHSIZE, 3673L);
        doc.put(MailMetaInfo.ATTACHTYPE, PNG_ATTACHTYPE);
        doc.put(MailMetaInfo.MD5, "A7801E05B1643D2C4A4CB5FD04C9E5F8");

        doc = json.createDoc(
            ONE_THREE,
            headers
            + PNG + BASE64
            + "\ncontent-id: <fc372b45f4289f08630236350b4bb592>"
            + CONTENT_DISPOSITION_INLINE
            + "; filename=\"fc372b45f4289f08630236350b4bb592\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_PNG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, new Json.Headers(PNG_META));
        doc.put(CommonFields.CREATED, CREATED);
        doc.put(CommonFields.HEIGHT, 43L);
        doc.put(CommonFields.WIDTH, 120L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, ratio);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE + '\n' + INLINE);
        doc.put(MailMetaInfo.ATTACHNAME, "fc372b45f4289f08630236350b4bb592");
        doc.put(MailMetaInfo.ATTACHSIZE, 2115L);
        doc.put(MailMetaInfo.ATTACHTYPE, PNG_ATTACHTYPE);
        doc.put(MailMetaInfo.MD5, "01373F547B6AB4B0C55CE002A20B9212");

        doc = json.createDoc(
            ONE_FOUR,
            headers
            + PNG + BASE64
            + "\ncontent-id: <502135b652c267e9cd7e4b226c8eaaa8>"
            + CONTENT_DISPOSITION_INLINE
            + "; filename=\"502135b652c267e9cd7e4b226c8eaaa8\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.MIMETYPE, IMAGE_PNG);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.META, new Json.Headers(PNG_META));
        doc.put(CommonFields.CREATED, CREATED);
        doc.put(CommonFields.HEIGHT, 43L);
        doc.put(CommonFields.WIDTH, 135L);
        doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        doc.put(CommonFields.RATIO, "135:43");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE + '\n' + INLINE);
        doc.put(MailMetaInfo.ATTACHNAME, "502135b652c267e9cd7e4b226c8eaaa8");
        doc.put(MailMetaInfo.ATTACHSIZE, 4053L);
        doc.put(MailMetaInfo.ATTACHTYPE, PNG_ATTACHTYPE);
        doc.put(MailMetaInfo.MD5, "F47CBE958BF2CFF6B35C6F9AFFD6A6C7");
        checkMail("mamba.eml", json);
    }

    @Test
    public void testBadZipAttach() throws Exception {
        String headers =
            "received: from smtp3.mail.yandex.net ([77.88.46.103])\t"
            + "by mxback10.mail.yandex.net with LMTP id nJXO5gRc\t"
            + "for <poker1888@yandex.ru>; Thu, 6 Jun 2013 13:49:19 +0400\n"
            + "received: from smtp3.mail.yandex.net (localhost [127.0.0.1])\t"
            + "by smtp3.mail.yandex.net (Yandex) with ESMTP id 06E741BA02EB\t"
            + "for <poker1888@yandex.ru>; "
            + "Thu,  6 Jun 2013 13:49:19 +0400 (MSK)\n"
            + "received: from ip86-69.sowa.com.ua "
            + "(ip86-69.sowa.com.ua [91.195.69.86])\t"
            + "by smtp3.mail.yandex.net (nwsmtp/Yandex) with ESMTP id "
            + "lPGCmn2CNW-kgh85F7D;\tThu,  6 Jun 2013 13:46:42 +0400\n"
            + "x-yandex-front: smtp3.mail.yandex.net\n"
            + "x-yandex-timemark: 1370512002\n"
            + "message-id: <20130606134918.kgh85F7D@smtp3.mail.yandex.net>\n"
            + "dkim-signature: v=1; a=rsa-sha256; c=relaxed/relaxed; "
            + "d=yandex.ru; s=mail; t=1370512158;\t"
            + "bh=909yz76AAzu9VcaX41kxieAIU2A6L9iePfjrm+PdwWU=;\t"
            + "h=From:Subject:To:Content-Type:MIME-Version:Sender:Reply-To:"
            + "Date;\tb=Qy5S80DQZwCr0TUPz1pVK+Lfq6wW3Fss0bLF362JdlK/y7v+9nqDqO"
            + "2/sLcIeofU0\t 2k2yNsxS9+A0iqC8zW32kiHPz35tVMKrUIaGQfIRqDvDvK5M+"
            + "tBaSfvHvvtcrnyXoF\t v78i2qHElsPf+0ISLCyAXu/Rc8exbhRRcg3sLbIo=\n"
            + "authentication-results: smtp3.mail.yandex.net; dkim=pass "
            + "header.i=@yandex.ru\nx-yandex-spam: 1\n"
            + "from: \"poker1888@yandex.ru\" <poker1888@yandex.ru>\n"
            + "subject: =?utf-8?Q?Otvet?=\n"
            + "to: poker1888@yandex.ru\n"
            + "content-type: multipart/mixed; boundary=\"oy8poJKt75BX9vmikJfT6"
            + "HO5ew=_xDaY64\"\nmime-version: 1.0\n"
            + "sender: poker1888@yandex.ru\n"
            + "reply-to: poker1888@yandex.ru\n"
            + "date: Thu, 6 Jun 2013 12:46:43 +0300\n"
            + "return-path: poker1888@yandex.ru\n"
            + "x-yandex-forward: fa19dae7f77270ef2816f49aa5c4c279\n";
        Json json = new Json(headers, MID, SUID);
        String from = "\"poker1888@yandex.ru\" <poker1888@yandex.ru>";
        String email = "poker1888@yandex.ru";
        String gwReceivedDate = "1370512159";
        String smtpId = "lPGCmn2CNW-kgh85F7D";
        String receivedDate = "1370512002";
        String subject = "Otvet";

        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers
            + "content-type: text/html;charset=UTF-8;\n"
            + FieldName.CONTENT_TRANSFER_ENCODING.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR + MimeUtil.ENC_QUOTED_PRINTABLE
            + CONTENT_DISPOSITION_INLINE);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Лог файлы сохранены в zip архиве");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SENDER, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gwReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, INLINE);

        doc = json.createDoc(
            ONE_TWO,
            headers
            + "content-type: application/zip;        "
            + "name=\"????-??-Log_file(2013-06-06_12-46-38).zip\""
            + BASE64
            + "\ncontent-disposition: attachment        filename=\"????-??-"
            + "Log_file(2013-06-06_12-46-38).zip\"");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, new Json.Contains("Заголовок окна:"));
        doc.put(CommonFields.MIMETYPE, ZIP);
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_ZIP);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SENDER, email);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, email);
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gwReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(
            MailMetaInfo.DISPOSITION_TYPE,
            "attachment filename=\"????-??-Log_file.zip\"");
        doc.put(MailMetaInfo.ATTACHTYPE, "zip");
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "????-??-Log_file(2013-06-06_12-46-38).zip");
        doc.put(MailMetaInfo.ATTACHSIZE, 6388661L);
        doc.put(MailMetaInfo.MD5, "401D27F811A7CB46A6784F13654EB590");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://www.odnoklassniki.ru/leond.semenchuk\n"
            + "http://www.odn\n"
            + "http://iki.ru\n"
            + "http://www.odnoklassniki.ru/dk?st.cmd=friendFriend&st.category="
            + "7&st.friendId=oepkpsbgaerjeahyse0qwsviablrbokixlepjv&st._aid="
            + "FriendFriend_ViewRelative\n"
            + "http://www.odnoklassniki.ru/profile/346904075380/friends\n"
            + "http://www.odnoklassniki.ru/profile/346904075380\n");
        checkMail("bad.zip.eml", json);
    }

    @Test
    public void testBadUrls() throws Exception {
        String headers =
            "content-type: multipart/alternative; boundary=047d7b86e424ccb1160"
            + "4e9a92e8e\ncontent-type: text/html; charset=ISO-8859-9\n"
            + FieldName.CONTENT_TRANSFER_ENCODING.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR + MimeUtil.ENC_QUOTED_PRINTABLE;
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers.replaceAll("/html", "/plain"));
        doc.put(CommonFields.PARSED, true);
        String first = "olabilecek herke";
        String second = "Ankara'da bir Kitabevi'nde";
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Not(new Json.Contains(first)),
                new Json.Contains(second)));
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains(first),
                new Json.Not(new Json.Contains(second))));
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.META,
            "Content-Type:text/plain; charset=ISO-8859-9");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://groups.google.com/group/kurumtabipleri\nh"
            + "ttps://groups.google.com/groups/opt_outadresiniz\nh"
            + "ttps://groups.google.com/groups/opt_out\n");

        doc = json.createDoc(ONE_TWO);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Not(new Json.Contains(first)),
                new Json.Contains(second)));
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains("maya devam milletvekili olmasa da duyarl"),
                new Json.Contains(
                    "TestUrl: http://here.we.are:080/wow\n"
                    + "TestUrl2: https://here.we.again:0444"),
                new Json.Contains(first),
                new Json.Not(new Json.Contains(second))));
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.META,
            "Content-Type:text/html; charset=ISO-8859-9");
        doc.put(
            MailMetaInfo.X_URLS,
            "http://groups.google.com/group/kurumtabipleri\n"
            + "https://groups.google.com/groups/opt_out\n"
            + "http://here.we.are/wow\n"
            + "https://here.we.again:444\n");
        checkMail("badurls.eml", json);
    }

    @Test
    public void testCyrillicNoEncoding() throws Exception {
        Json json = new Json(
            "x-yandex-front: mxback6h.mail.yandex.net\n"
            + "x-yandex-timemark: 1385626002\n"
            + "x-yandex-spam: 1\nx-yandex-pop-server: pop.bigmir.net\n"
            + "x-yandex-rpop-id: 2290000000000212620\n"
            + "x-yandex-rpop-info: pgunya@pop.bigmir.net\n"
            + "delivered-to: pgunya@bigmir.net\n"
            + "date: Tue, 7 Oct 2008 13:44:00 GMT\n"
            + "message-id: <200810071344.m97Di0NH076213@grand.ua>\n"
            + "to: pgunya@bigmir.net\n"
            + "subject: ќформление заказа в »нтернет-магазине GRAND.UA\n"
            + "from: Grand Support <support@grand.ua>\n"
            + "return-path: pgunya@yandex.ru\n"
            + "x-yandex-forward: 258778609752f99d0ea8da33dffce9d2\n"
            + "x-yandex-filter: 2290000000000545889\n",
            MID,
            SUID);
        String from = "support@grand.ua\n";
        String to = "pgunya@bigmir.net\n";
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.AllOf(
                new Json.Contains("Доброго времени суток"),
                new Json.Contains("Asus Socket775")));
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "Grand Support <support@grand.ua>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Grand Support\n");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, "pgunya@bigmir.net");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "ќформление заказа в »нтернет-магазине GRAND.UA");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1385626002");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1251);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        checkMail("cyrillic-no-encoding.txt", json);
    }

    private static void testNpePng(final TextExtractOptions.Mode mode)
        throws Exception
    {
        Json json = new Json(
            "content-type: \"image/png; x-mac-hide-extension=yes; "
            + "x-unix-mode=0644; name=\\\"???????????? ???????????? 2014-04-17"
            + " ?? 16.02.08.png\\\"\"; "
            + "\tname=\"=?UTF-8?B?0KHQvdC40LzQvtC6INGN0LrRgNCw0L3QsCAyMDE0LTA0"
            + "LTE3INCyIDE2LjAyLjA4LnBuZw==?=\""
            + "\ncontent-disposition: attachment; \tfilename=\"=?UTF-8?B?0KHQv"
            + "dC40LzQvtC6INGN0LrRgNCw0L3QsCAyMDE0LTA0LTE3INCyIDE2LjAyLjA4LnBu"
            + "Zw==?=\""
            + BASE64,
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        if (mode == TextExtractOptions.Mode.ULTRA_FAST) {
            doc.put(CommonFields.PARSED, false);
        } else {
            doc.put(CommonFields.PARSED, true);
            doc.put(CommonFields.BODY_TEXT, "");
            doc.put(CommonFields.HEIGHT, 686L);
            doc.put(CommonFields.WIDTH, 801L);
            doc.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
            doc.put(CommonFields.RATIO, "801:686");
            doc.put(CommonFields.META, PNG_META.trim());
        }
        doc.put(CommonFields.MIMETYPE, IMAGE_PNG);
        doc.put(MailMetaInfo.CONTENT_TYPE, "\"image/png");
        doc.put(MailMetaInfo.MD5, "9AFE20CC8C8FBB12376B8441D0A6363F");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHSIZE, 172244L);
        doc.put(MailMetaInfo.ATTACHTYPE, PNG_ATTACHTYPE);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "Снимок экрана 2014-04-17 в 16.02.08.png");
        checkMail("npe-png.eml", json, POST_BODY, "", '&' + mode.cgiFlag());
    }

    @Test
    public void testNpePng() throws Exception {
        testNpePng(TextExtractOptions.Mode.NORMAL);
    }

    @Test
    public void testNpePngFast() throws Exception {
        testNpePng(TextExtractOptions.Mode.FAST);
    }

    @Test
    public void testNpePngUltraFast() throws Exception {
        testNpePng(TextExtractOptions.Mode.ULTRA_FAST);
    }

    // CSOFF: MethodLength
    private void testPkpass(final TextExtractOptions.Mode mode)
        throws Exception
    {
        String headers =
            "received: from mxfront32.mail.yandex.net ([127.0.0.1])\t"
            + "by mxfront32.mail.yandex.net with LMTP id sSHSFrsb\t"
            + "for <tabolin@yandex.ru>; Sat, 26 Apr 2014 18:54:28 +0400\n"
            + "received: from mailrelay1.rambler.ru (mailrelay1.rambler.ru [81"
            + ".19.66.239])\tby mxfront32.mail.yandex.net (nwsmtp/Yandex) with"
            + " ESMTPS id XKALPqFpgh-sSQ0xtQc;\t"
            + "Sat, 26 Apr 2014 18:54:28 +0400\t"
            + "(using TLSv1 with cipher CAMELLIA256-SHA (256/256 bits))\t"
            + "(Client certificate not present)\n"
            + "x-yandex-front: mxfront32.mail.yandex.net\n"
            + "x-yandex-timemark: 1398524068\n"
            + "x-yandex-uniq: 547863fe-d627-41ea-a5f7-cf2230ea673e\n"
            + "authentication-results: mxfront32.mail.yandex.net; spf=pass "
            + "(mxfront32.mail.yandex.net: domain of rambler-co.ru designates "
            + "81.19.66.239 as permitted sender) smtp.mail=noreply@rambler-co."
            + "ru; dkim=pass header.i=@rambler-co.ru\nx-yandex-spam: 1\n"
            + "received: from TSK2 (tsk2.mir.afisha.ru [81.19.92.135])\t"
            + "by mailrelay1.rambler.ru (Postfix) with ESMTP id 3gGFdb6fW2zJTM"
            + "\tfor <tabolin@yandex.ru>; Sat, 26 Apr 2014 18:54:27 +0400 (MSK"
            + ")\ndkim-signature: v=1; a=rsa-sha256; c=relaxed/simple; d=rambl"
            + "er-co.ru;\ts=mail; t=1398524068;\t"
            + "bh=IJNZOKBgrT7sBPkrvQNEmC7k4s42+EzgPJejJ2qX3Fg=;\t"
            + "h=From:To:Date:Subject;\tb=oxzogDLViB5zjnXmPMBvermDxK0ALKXrg68r"
            + "1sQS53lJDUxFSHAE0GdoEo6xta+Xh\t pzYtiLywrKveGc9PzhnmX8ZPvWm3wKA"
            + "OeJBZiKR8Gm9CjnDTX0/iGn7k3BrT8eMuwK\t Ty/iaHlqV43l+l2LWX7ex/EnQ"
            + "DRXjXgjalzQKMH0=\nmessage-id: 311c0d91-201a-4b34-acc8-4c57ab181"
            + "9bb@rambler-co.ru\nmime-version: 1.0\n"
            + "from: =?windows-1251?Q?=D0=E0=EC=E1=EB=E5=F0=2D=CA=E0=F1=F1=E0?"
            + "= <noreply@rambler-co.ru>\nto: tabolin@yandex.ru\n"
            + "date: 26 Apr 2014 18:55:04 +0400\n"
            + "subject: =?utf-8?B?0K3Qu9C10LrRgtGA0L7QvdC90YvQuSDQsdC40LvQtdGC"
            + "INCy?= =?utf-8?B?INCa0YDQvtC90LLQtdGA0Log0KHQuNC90LXQvNCwINCb0L"
            + "XRhNC+0YDR?= =?utf-8?B?gtC+0LLQvg==?=\n"
            + "content-type: multipart/mixed; boundary=--boundary_0_d46a31ac-5"
            + "ddf-4bd9-948d-83f364ce0161\nx-rcpt-to: <tabolin@yandex.ru>\n"
            + "return-path: noreply@rambler-co.ru\n"
            + "x-yandex-forward: b3bb7fdcc2ca497e63c6468acac7fd37\n"
            + "x-yandex-filter: 7213360000000004893";
        Json json = new Json(headers, MID, SUID);
        String name = "Рамблер-Касса";
        String from = name + " <noreply@rambler-co.ru>";
        String email = "noreply@rambler-co.ru";
        String to = "tabolin@yandex.ru";
        String receivedDate = "1398524068";
        String smtpId = "XKALPqFpgh-sSQ0xtQc";
        String subject = "Электронный билет в Кронверк Синема Лефортово";

        Map<String, Object> doc =
            json.createDoc(ONE_ONE, headers + HTML + UTF8 + BASE64);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(CommonFields.TITLE, "Электронный билет");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains("Выдача билета осуществляется лицу, первому"));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            name + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://vk.com/ramblerkassa\nhttps://twitter.com/ramblerkassa\n"
            + "http://kassa.rambler.ru/refund\nhttp://www.formulakino.ru\n");

        doc = json.createDoc(
            ONE_TWO,
            headers
            + "\ncontent-type: application/octet-stream;"
            + " name=Ticket.pkpsss\ncontent-disposition: attachment" + BASE64);
        doc.put(CommonFields.PARSED, mode == TextExtractOptions.Mode.NORMAL);
        doc.put(
            CommonFields.BODY_TEXT,
            "{\"organizationName\":\"Rambler Internet Holding OOO\","
            + "\"passTypeIdentifier\":\"pass.rambler.kassa.movies\","
            + "\"formatVersion\":1,\"serialNumber\":\"9819685\","
            + "\"description\":\"Для получения билета предъявите номер "
            + "электронного билета в кассе кинотеатра или введите его в "
            + "терминале по выдаче билетов.\",\"teamIdentifier\":\"X7QD2URS8E"
            + "\",\"logoText\":\"\",\"foregroundColor\":\"#000000\","
            + "\"backgroundColor\":\"#333333\",\"eventTicket\":"
            + "{\"headerFields\":[{\"key\":\"time\",\"label\":\"26 апр\","
            + "\"value\":\"20:00\"}],\"primaryFields\":[{\"key\":\"number\","
            + "\"label\":\"Номер эл. билета\",\"value\":\"211019685\"}],"
            + "\"secondaryFields\":[{\"key\":\"event\",\"label\":"
            + "\"Кронверк Синема Лефортово\",\"value\":"
            + "\"Новый Человек-паук: Высокое напря... (12+)\"}],"
            + "\"auxiliaryFields\":[{\"key\":\"tickets\",\"label\":\"Места\","
            + "\"value\":\"ряд 10, места 13, 14, 15 (3 билета)\"},{\"key\":"
            + "\"price\",\"label\":\"Цена\",\"value\":\"1386 руб\"}],"
            + "\"backFields\":[{\"key\":\"instructions\",\"label\":"
            + "\"ИНСТРУКЦИИ\",\"value\":\"Для получения билета предъявите "
            + "номер электронного билета в кассе кинотеатра или введите его в "
            + "терминале по выдаче билетов.\"},{\"key\":\"help\",\"label\":"
            + "\"ПОМОЩЬ\",\"value\":\"По всем вопросам обращайтесь в службу "
            + "поддержки по телефонам+7 (495) 785-17-03, 8 800 700-29-03\"}]},"
            + "\"barcode\":{\"format\":\"PKBarcodeFormatQR\",\"message\":\""
            + "211019685\",\"messageEncoding\":\"UTF-8\",\"altText\":\"\"}}");
        doc.put(CommonFields.MIMETYPE, "application/vnd.apple.pkpass");
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/octet-stream");
        doc.put(
            CommonFields.META,
            "Content-Type:application/vnd.apple.pkpass");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            email + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            name + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.ATTACHTYPE, "pkpass pkpsss");
        if (mode != TextExtractOptions.Mode.NORMAL) {
            // overwrite extracted info
            doc.remove(CommonFields.BODY_TEXT);
            doc.remove(CommonFields.META);
            doc.put(CommonFields.MIMETYPE, ZIP);
            doc.put(MailMetaInfo.CONTENT_TYPE, "application/octet-stream");
            doc.put(MailMetaInfo.ATTACHTYPE, "zip pkpsss");
        }
        doc.put(MailMetaInfo.ATTACHNAME, "Ticket.pkpsss");
        doc.put(MailMetaInfo.ATTACHSIZE, 61295L);
        doc.put(MailMetaInfo.MD5, "194E78FEC878EDAC87C279ADA05213F5");
        checkMail("pkpass.eml", json, POST_BODY, "", '&' + mode.cgiFlag());
    }
    // CSON: MethodLength

    @Test
    public void testPkpass() throws Exception {
        testPkpass(TextExtractOptions.Mode.NORMAL);
    }

    @Test
    public void testPkpassFastMode() throws Exception {
        testPkpass(TextExtractOptions.Mode.FAST);
    }

    @Test
    public void testPkpassUltraFastMode() throws Exception {
        testPkpass(TextExtractOptions.Mode.ULTRA_FAST);
    }

    @Test
    public void testJsonBody() throws Exception {
        final long suid = 9001L;
        Json json = new Json("100501", suid);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_WINDOWS_1252);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"Pupkin\"<v.pupkin@gmail.com>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            "v.pupkin@gmail.com\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            "vpupkin@gmail.com\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Pupkin\n");
        doc.put(MailMetaInfo.REPLY_TO_FIELD, "\"Evil\" <here.i.am@ya.ru>");
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            "here.i.am@ya.ru\n");
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            "here-i-am@yandex.ru\n");
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.DISPLAY_NAME,
            "Evil\n");
        doc.put(MailMetaInfo.MESSAGE_TYPE, "20 s_zdticket");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1234567891");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, "test json");
        doc.put(MailMetaInfo.THREAD_ID_FIELD, "100502");
        doc.put(MailMetaInfo.STID, "222.333.444");
        checkMail(
            SIMPLE,
            json,
            new StringEntity(
                "{\"mid\":\"100501\",\"uname\":9001,"
                + "\"from\":\"\\\"Pupkin\\\"<v.pupkin@gmail.com>\","
                + "\"cc\":null,\"bcc\":\"\",\"subobject\":[1],"
                + "\"is_mixed\":2621440,\"received_date\":1234567891,"
                + "\"reply_to\":\"\\\"Evil\\\" <here.i.am@ya.ru>\","
                + "\"subject\":\"test json\",\"thread_id\":\"100502\","
                + "\"st_id\":\"222.333.444\"}",
                ContentType.APPLICATION_JSON));
    }

    @Test
    public void testBadInputSimplest() throws Exception {
        ContentWriter writer = new ContentWriter() {
            @Override
            public void writeTo(final Writer writer) throws IOException {
                writer.write("<message>\n</message>\n\r\n");
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
                writer.write("<root>\r\n");
                writer.write("\t<text>Hello, world</text>\r\n");
                final int fillers = 65536;
                for (int i = 0; i < fillers; ++i) {
                    writer.write("\t<filler/>\r\n");
                }
                writer.flush();
                try {
                    Thread.sleep(TIMEOUT << 1);
                } catch (InterruptedException e) {
                    return;
                }
                writer.write("\t<text>Hello again</text>\r\n</root>");
                writer.flush();
            }
        };
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, writer);
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(POST_BODY));
            HttpResponse response = client.execute(post);
            Assert.assertEquals(
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            DiskHandlerTest.assertInvalidJson(response);
        }
    }

    @Test
    public void testMegagroupSpam() throws Exception {
        String headers =
            "received: from mxback24o.mail.yandex.net (localhost [127.0.0.1])"
            + "\tby mxback24o.mail.yandex.net with LMTP id 1oTPrXXzY2-7AXuENCH"
            + "\tfor <nlihouzova@yandex.ru>; Mon, 23 Dec 2019 02:43:36 +0300\n"
            + "received: from rpop6o.mail.yandex.net (rpop6o.mail.yandex.net "
            + "[2a02:6b8:0:1a2d::180])\tby mxback24o.mail.yandex.net (mxback/"
            + "Yandex) with ESMTP id NCafqdKJHC-haMqFJQk;\tMon, 23 Dec 2019 02"
            + ":43:36 +0300\nx-yandex-front: mxback24o.mail.yandex.net\n"
            + "x-yandex-timemark: 1577058216.030\n"
            + "authentication-results: mxback24o.mail.yandex.net; dkim=pass "
            + "header.i=@megagroup.ru\nx-yandex-suid-status: 2 1377367751\n"
            + "x-yandex-spam: 2\nx-yandex-envelope: aGVsbz1teGJhY2tzLm1haWwueW"
            + "FuZGV4Lm5ldAptYWlsX2Zyb209bmxpaG91em92YUB5YW5kZXgucnUKcmNwdF90b"
            + "z1ubGlob3V6b3ZhQHlhbmRleC5ydQpyZW1vdGVfaG9zdD1ycG9wNm8ubWFpbC55"
            + "YW5kZXgubmV0CnJlbW90ZV9pcD0yYTAyOjZiODowOjFhMmQ6OjE4MAo=\n"
            + "x-yandex-pop-server: imap.yandex.ru\nx-yandex-rpop-id: 2636823"
            + "\nx-yandex-rpop-info: natalihouzova@imap.yandex.ru\n"
            + "received: from natalihouzova@imap.yandex.ru ([77.88.21.125])\t"
            + "by mail.yandex.ru with POP3 id NHVkn1qJZ4Y1\t"
            + "for 1377367751@2636823; Mon, 23 Dec 2019 02:43:36 +0300\n"
            + "received: from mxfront8o.mail.yandex.net (localhost [127.0.0.1]"
            + ")\tby mxfront8o.mail.yandex.net with LMTP id BSf4EVRTPA-"
            + "oQF3Tnas\tfor <natalihouzova@yandex.by>; Mon, 23 Dec 2019 02:25"
            + ":45 +0300\nreceived: from relay2.oml.ru (relay2.oml.ru "
            + "[185.32.58.43])\tby mxfront8o.mail.yandex.net (mxfront/Yandex) "
            + "with ESMTPS id mvNDqET8gM-PjtOJOYL;\tMon, 23 Dec 2019 02:25:45 "
            + "+0300\t(using TLSv1.2 with cipher ECDHE-RSA-AES128-GCM-SHA256 "
            + "(128/128 bits))\t(Client certificate not present)\n"
            + "x-yandex-suid-status: 2 1124009362\nx-yandex-envelope: aGVsbz1y"
            + "ZWxheTIub21sLnJ1Cm1haWxfZnJvbT1kZXYtbnVsbEBtZWdhZ3JvdXAucnUKcmN"
            + "wdF90bz1uYXRhbGlob3V6b3ZhQHlhbmRleC5ieQpyZW1vdGVfaG9zdD1yZWxheT"
            + "Iub21sLnJ1CnJlbW90ZV9pcD0xODUuMzIuNTguNDMK\nx-yandex-fwd: MTQzM"
            + "TAwODc2NDExOTk3MjY0OTgsNzQwMTUxODc0NzcwNTE4MDQxNg==\n"
            + "dkim-signature: v=1; a=rsa-sha256; q=dns/txt; c=relaxed/relaxed"
            + ";\td=megagroup.ru; s= main ; h=X-Feedback-ID:Feedback-ID:"
            + "Content-Type:\tX-Megagroup-Service:MIME-Version:Reply-To:From:"
            + "Subject:To:Message-Id:Date;\t bh=WFnxnqJSfvep+Wf791ER5Pe5ZDSPOC"
            + "f7dinKe6ML+jA=; b=i2TDX0xVAvfaiUEp7BJnwQrmq\tUijvYkxz/z7TwANtJv"
            + "ufD2lytlft2T44ZgK0xIcJwMKBU6oTh8fOwKt0k9m3++BkJy9iuA32vzZqW\t"
            + "shN685DP4sEaqI2jKdbjBevaS4vG2lh+gunyMy49Wb1Ua0dlxLQV9wTl4mi4T+"
            + "ihsZi9IRE94Lc41\tjezOQ7LP+HJMsKXgpHQt8pFJKGZQ2AuVIbtNNC2X/z7xP1"
            + "ElO7zBMc/Z9bgj0m7PPkoWMFVYC9Tdr\t92vuDI6c48GbBXtHb+OiVJqlE2cm3I"
            + "8h0Y7TRFiE6AoHjHz3itaqOSGio1TC5GVqgMm2yvl8v7qBS\tGBfJHZg4g==;\n"
            + "received: from s3a.rtn.m ([10.2.1.18]:43910 helo=s3a.cs.m)\t"
            + "by relay2.oml.ru with esmtp (Exim 4.86_2)\t(envelope-from "
            + "<dev-null@megagroup.ru>)\tid 1ijAbZ-0003Am-1v\tfor "
            + "natalihouzova@yandex.by; Mon, 23 Dec 2019 02:25:45 +0300\n"
            + "received: (from apache@localhost)\tby s3a.cs.m (8.14.9/8.14.9/"
            + "Submit) id xBMNPi85024108;\tMon, 23 Dec 2019 02:25:44 +0300\n"
            + "date: Mon, 23 Dec 2019 02:25:44 +0300\n"
            + "message-id: <201912222325.xBMNPi85024108@s3a.cs.m>\n"
            + "to: <natalihouzova@yandex.by>\nsubject: "
            + "=?UTF-8?B?0JLQsNC8INC/0YDQtdC00L7RgdGC0LDQstC70LXQvSDQtA==?= "
            + "=?UTF-8?B?0L7RgdGC0YPQvyDQsiDQmtCw0LHQuNC90LXRgiDQv9C+0LvRjNC30"
            + "L4=?= =?UTF-8?B?0LLQsNGC0LXQu9GPIE1lZ2Fncm91cC5ydSDQtNC70Y8g0YD"
            + "QsNCx0L4=?= =?UTF-8?B?0YLRiyDRgSDRgdC10YDQstC40YHQvtC8INC+0L3Qu"
            + "9Cw0LnQvS3Qug==?= =?UTF-8?B?0L7QvdGB0YPQu9GM0YLQuNGA0L7QstCw0L3"
            + "QuNGPIE9uaWNvbg==?=\n"
            + "from: =?UTF-8?B?0JrQsNCx0LjQvdC10YIg0L/QvtC70YzQt9C+0LLQsNGC0LX"
            + "Quw==?= =?UTF-8?B?0Y8gTWVnYWdyb3VwLnJ1?= <support@megagroup.ru>"
            + "\nreply-to: <no-reply@megagroup.ru>\nmime-version: 1.0\n"
            + "x-megagroup-service: megacabinet\ncontent-type: multipart/"
            + "alternative; boundary=\"91d69f517f401a25b7b23b4717e20d86.altern"
            + "ative\"\nfeedback-id: megacabinet:relay2.oml.ru\nx-feedback-id:"
            + " megacabinet:relay2.oml.ru\nx-yandex-forward: ef133a79c9a677c33"
            + "374540c257f6e87\nreturn-path: nlihouzova@yandex.ru\n"
            + "x-yandex-forward: 5bb7c0befcd954a14b01cf3d49f5eb69";
        Json json = new Json(headers, MID, SUID);
        String fromEmail = "support@megagroup.ru";
        String fromName = "Кабинет пользователя Megagroup.ru";
        String from = fromName + ' ' + '<' + fromEmail + '>';
        String replyTo = "no-reply@megagroup.ru";
        String toEmail = "natalihouzova@yandex.by";
        String toEmailNormalized = "natalihouzova@yandex.ru";
        String receivedDate = "1577058216.030";
        String gatewayReceivedDate = "1577058216";
        String smtpId = "NCafqdKJHC-haMqFJQk";
        String subject =
            "Вам предоставлен доступ в Кабинет пользователя Megagroup.ru для "
            + "работы с сервисом онлайн-консультирования Onicon";
        String xurls =
            "http://www.like100.pw\nhttp://megagroup.ru\n"
            + "https://cabinet.megagroup.ru/user/confirm/473038/t86A1S6Y1Ra323"
            + "8xTp57OWKnYdI36giMEstB3Q37\nhttp://onicon.ru/instructions\n";

        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + PLAIN + "; charset=\"UTF-8\"" + BASE64);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains("Для начала работы Вам необходимо"));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, '<' + replyTo + '>');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyTo + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.X_URLS, xurls);

        doc = json.createDoc(
            ONE_TWO,
            headers + HTML + "; charset=\"UTF-8\"" + BASE64);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            new Json.Contains("Для начала работы Вам необходимо"));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            fromName + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, '<' + toEmail + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            toEmail + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            toEmailNormalized + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, '<' + replyTo + '>');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL,
            replyTo + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            replyTo + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.X_URLS, xurls);

        checkMail("megagroup-spam.eml", json);
    }

    @Test
    public void testDoubleSmtpId() throws Exception {
        Json json = new Json(
            "received: from mxback28o.mail.yandex.net (localhost [127.0.0.1])"
            + "\tby mxback28o.mail.yandex.net with LMTP id TL5fB0GWBK-mMdbMyWX"
            + ";\tMon, 30 Dec 2019 16:57:13 +0300\nreceived: from mxback28o."
            + "mail.yandex.net (localhost [127.0.0.1])\tby "
            + "mxback28o.mail.yandex.net (Yandex) with ESMTP id B8D09632CADD;"
            + "\tMon, 30 Dec 2019 16:57:13 +0300 (MSK)\nx-yandex-internal: 1\n"
            + "received: from myt4-ee976ce519ac.qloud-c.yandex.net "
            + "(myt4-ee976ce519ac.qloud-c.yandex.net [2a02:6b8:c00:1da4:0:640:"
            + "ee97:6ce5])\tby mxback28o.mail.yandex.net (mxback/Yandex) with "
            + "ESMTP id BdVSeZvzQY-vDuWvjT5;\tMon, 30 Dec 2019 16:57:13 +0300"
            + "\nx-yandex-front: mxback28o.mail.yandex.net\nx-yandex-timemark:"
            + " 1577714233.697\ndkim-signature: v=1; a=rsa-sha256; c=relaxed/"
            + "relaxed; d=yandex.ru; s=mail; t=1577714233;\tbh="
            + "lvRTfhnd+hsWdsvzX/Z/ZPfaE6xZsBJeR9O2qZ264rM=;\t"
            + "h=Subject:From:Date:To:Message-ID;\tb=ifLnrDR15JGiY6WjwusS9sYm1"
            + "mOdARxK1FHGs5bg8C3eZ/ohIsls1YPbfbNIqYZsf\t Kx13Mgpaitv9gXJX5//u"
            + "+C3//ohzp5ztb51EV+2djlbYSV9rgjPWgjoOnpBHQEam5T\t j6gWpnHvR+qvAZ"
            + "IfYCJnia7rHtY47d0Gnr/4RJ5o=\n"
            + "authentication-results: mxback28o.mail.yandex.net; dkim=pass "
            + "header.i=@yandex.ru\nx-yandex-envelope: aGVsbz1teXQ0LWVlOTc2Y2U"
            + "1MTlhYy5xbG91ZC1jLnlhbmRleC5uZXQKbWFpbF9mcm9tPXBhbmFzZW5jazAuc0"
            + "B5YW5kZXgucnUKcmNwdF90bz1zYWZvbm92anVyYUB5YW5kZXgucnUKcmVtb3RlX"
            + "2hvc3Q9bXl0NC1lZTk3NmNlNTE5YWMucWxvdWQtYy55YW5kZXgubmV0CnJlbW90"
            + "ZV9pcD0yYTAyOjZiODpjMDA6MWRhNDowOjY0MDplZTk3OjZjZTUK\n"
            + "received: by myt4-ee976ce519ac.qloud-c.yandex.net (smtp/Yandex)"
            + " with ESMTPSA id yC1AktbCQU-vDVelEw9;\t"
            + "Mon, 30 Dec 2019 16:57:13 +0300\t(using TLSv1.2 with cipher "
            + "ECDHE-RSA-AES128-GCM-SHA256 (128/128 bits))\t"
            + "(Client certificate not present)\n"
            + "x-yandex-timemark: 1577714233.176\n"
            + "x-yandex-suid-status: 1 113607240\n"
            + "x-yandex-spam: 1\n"
            + "x-yandex-envelope: aGVsbz1PN1E0V0wyOTgKbWFpbF9mcm9tPXBhbmFzZW5j"
            + "azAuc0B5YW5kZXgucnUKcmNwdF90bz1zYWZvbm92anVyYUB5YW5kZXgucnUKcmV"
            + "tb3RlX2hvc3Q9bWFpbC5yaWNoYXJkaGVzcy5pbmZvCnJlbW90ZV9pcD04MC4yND"
            + "EuMjE1LjE4Nwo=\n"
            + "to: safonovjura@yandex.ru\n"
            + "date: Mon, 30 Dec 2019 14:57:12 +0300\n"
            + "from: \"=?UTF-8?B?0J7RgtC00LXQuyDQv9C+INGE0LjQvdCw0L3RgdCw0Lw=="
            + "?=\" <panasenck0.s@yandex.ru>\n"
            + "subject: =?utf-8?B?0KHQu9GD0LbQsdCwINCx0LXQt9C+0L/QsNGB0L3QvtGB"
            + "0YLQuCA=?= =?utf-8?B?0YDQsNC30YDQtdGI0LjQu9CwINC/0LXRgNC10YfQuN"
            + "GB0LvQtdC9?= =?utf-8?B?0LjQtSDRgdGA0LXQtNGB0YLQsiDQvdCwINCy0LDR"
            + "iNGDINCx?= =?utf-8?B?0LDQvdC60L7QstGB0LrRg9GOINC60LDRgNGC0YMg0L"
            + "Ig0YDQsA==?= =?utf-8?B?0LfQvNC10YDQtSAxMzc2MyDRgA==?=\n"
            + "message-id: <zhwrrrl16xw47x4yi0idg7zp9958x8p07i87sqe657y0x8157t"
            + "w26w1g0xdg78ow2vf0.panasenck0.s@yandex.ru>\n"
            + "return-path: panasenck0.s@yandex.ru\n"
            + "x-yandex-forward: 658ea228f6cd79f1dd468132722ae719",
            MID,
            SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, HELLO_WORLD);
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);
        String from = "panasenck0.s@yandex.ru";
        String to = "safonovjura@yandex.ru";
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "\"Отдел по финансам\" <" + from + '>');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            "panasenck0-s@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Отдел по финансам\n");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, to);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            to + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "Служба безопасности разрешила перечисление средств на вашу "
            + "банковскую карту в размере 13763 р");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1577714233.697");
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, "1577714233");
        String smtpId = "yC1AktbCQU-vDVelEw9";
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, "BdVSeZvzQY-vDuWvjT5\n" + smtpId);
        checkMail("double-smtp-id.eml", json);
    }

    @Test
    public void testContentlessAlternative() throws Exception {
        String headers =
            FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "multipart/alternative; \tboundary=\"bndry\"";
        Json json = new Json(headers, MID, SUID);

        Map<String, Object> doc = json.createDoc(
            ONE_ONE,
            headers + '\n'
            + FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "text/html; charset=UTF-8");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "Html part");
        doc.put(CommonFields.MIMETYPE, "text/html");
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/html");
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);

        doc = json.createDoc(ONE_TWO);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "some text");
        doc.put(CommonFields.MIMETYPE, "text/plain");
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/plain");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(
            ONE_THREE,
            headers + '\n'
            + FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "text/plain; charset=UTF-8");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "text plain part");
        doc.put(CommonFields.MIMETYPE, "text/plain");
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/plain");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_UTF_8);

        doc = json.createDoc(ONE_FOUR);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "another text");
        doc.put(CommonFields.MIMETYPE, "text/plain");
        doc.put(MailMetaInfo.CONTENT_TYPE, "text/plain");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(
            ONE_FIVE,
            headers + '\n'
            + FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "message/feedback-report");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "feedback report");
        doc.put(CommonFields.MIMETYPE, "text/plain");
        doc.put(MailMetaInfo.CONTENT_TYPE, "message/feedback-report");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        doc = json.createDoc(
            "1.6",
            headers + '\n'
            + FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "text/plain\n"
            + FieldName.CONTENT_TYPE.toLowerCase()
            + MailMetaInfo.HEADERS_SEPARATOR
            + "message/feedback-report");
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(MailMetaInfo.PURE_BODY, "feedback report2");
        doc.put(CommonFields.MIMETYPE, "text/plain");
        doc.put(MailMetaInfo.CONTENT_TYPE, "message/feedback-report");
        doc.put(CommonFields.META, CONTENT_TYPE_PLAIN_ISO_8859_1);

        checkMail("contentless-alternative.eml", json);
    }

    @Test
    public void testDoubleContentType2() throws Exception {
        String headers =
            "received: from mxback6j.mail.yandex.net (localhost [127.0.0.1])\t"
            + "by mxback6j.mail.yandex.net with LMTP id Zofvm9Eh7H-SezXXUPI;\t"
            + "Thu, 02 Apr 2020 09:48:52 +0300\n"
            + "received: from mxback6j.mail.yandex.net (localhost.localdomain "
            + "[127.0.0.1])\t"
            + "by mxback6j.mail.yandex.net (Yandex) with ESMTP id AF957543FAF3"
            + ";\tThu,  2 Apr 2020 09:48:52 +0300 (MSK)\n"
            + "x-yandex-internal: 1\n"
            + "received: from sas8-6bf5c5d991b2.qloud-c.yandex.net "
            + "(sas8-6bf5c5d991b2.qloud-c.yandex.net "
            + "[2a02:6b8:c1b:2a1f:0:640:6bf5:c5d9])\t"
            + "by mxback6j.mail.yandex.net (mxback/Yandex) with ESMTP id "
            + "bOECxHvvuh-mqA8EvbT;\tThu, 02 Apr 2020 09:48:52 +0300\n"
            + "x-yandex-front: mxback6j.mail.yandex.net\n"
            + "x-yandex-timemark: 1585810132.658\n"
            + "dkim-signature: v=1; a=rsa-sha256; c=relaxed/relaxed; "
            + "d=yandex.ru; s=mail; t=1585810132;\t"
            + "bh=hc45Y74mF7Qey7WwrVZ0St5K4yDl+LGCH772NuLN49U=;\t"
            + "h=To:Subject:From:Date:References:Message-ID:In-Reply-To;\t"
            + "b=AQoutXp96yvoRKaq0sP39IKZX/0ECE9exjiPLwF80Aqyhe5p0/"
            + "lSsJYPAfuT3fUkl\t 7seylry9B2+pkyEZY5AWZdkSJ1NHWAE0Wtr99A1r6KXFD"
            + "kM/vtdPev47PsUmZsCljF\t ZX6vPNSMthgcErmgzF+2Tp6EOXMaTHj2hjq3uGz"
            + "U=\n"
            + "authentication-results: mxback6j.mail.yandex.net; dkim=pass "
            + "header.i=@yandex.ru\n"
            + "received: by sas8-6bf5c5d991b2.qloud-c.yandex.net (smtp/Yandex)"
            + " with HTTP id bIDJeSzKyt-mpY86OPI;\t"
            + "Thu, 02 Apr 2020 09:48:52 +0300\t"
            + "(using TLSv1.2 with cipher ECDHE-RSA-AES128-GCM-SHA256 "
            + "(128/128 bits))\t(Client certificate not present)\n"
            + "x-yandex-timemark: 1585810132.060\n"
            + "x-yandex-suid-status: 1 64512519\n"
            + "x-yandex-spam: 1\n"
            + "mime-version: 1.0\n"
            + "references: <CADNB+4MWtPPowF-JVtKYFW5DVDmG+rgnzn9SV=oeOVa6pZozL"
            + "g@mail.gmail.com> <1497244066947868@sas6-m7136srcl8r7.qloud-c.y"
            + "andex.net>\n"
            + "in-reply-to: <1497244066947868@sas6-m7136srcl8r7.qloud-c.yandex"
            + ".net>\n"
            + "from: =?UTF-8?B?0KHQuNGB0YLQtdC80LAg0J7Qv9C+0LLQtdGJ0LXQvdC40Y8"
            + "=?= <vanya2281331@yandex.ru>\n"
            + "date: Date: Thu, 02 Apr 2020 09:48:50\n"
            + "message-id: <CADNB+4MWtPPowF-JVtKYFW5DVDmG+rgnzn9SV=oeOVa6pZozL"
            + "g@mail.gmail.com>\n"
            + "subject: =?utf-8?B?0L3QsCDQktCw0YEg0YfQuNGB0LvQuNGC0YzRgdGPINC9"
            + "0LUg?= =?utf-8?B?0LfQsNC60L7QvdGH0LXQvdC90LDRjyDQvtC/0LXRgNCw0Y"
            + "bQuNGP?= =?utf-8?B?LCDQuCDQvdC40YfQtdCz0L4g0L3QtSDRgdC+0L7QsdGJ"
            + "0Lg=?= =?utf-8?B?0LvQuD8hINCh0L7QstC10YLRg9GOINC30LDQsdGA0LDRgt"
            + "GM?= =?utf-8?B?INC/0L7QutCwINC90LUg0YHQs9C+0YDQtdC70Lgg?=\n"
            + "x-mailer: iPhone Mail (17D50)\n"
            + "to: =?UTF-8?B?0J7RgtC00LXQuyDQktGL0L/Qu9Cw0YI=?= "
            + "<tabolin@yandex.ru>\n"
            + "content-type: multipart/mixed;        "
            + "boundary=\"4g8P614a47Y4p6mg74p5\"\n"
            + "content-transfer-encoding: base64\n"
            + "content-type: text/html; charset\n"
            + "return-path: vanya2281331@yandex.ru\n"
            + "x-yandex-forward: b3bb7fdcc2ca497e63c6468acac7fd37";
        Json json = new Json(headers, MID, SUID);
        Map<String, Object> doc = json.createDoc(ONE);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Второй раз не могу Вам передать, у Вас транзакция не обработанная"
            + ", больше чем на сто штук.\nУспейте вывести\nСмотрите, "
            + "потому что пропадут\n;cz");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_HTML.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, CONTENT_TYPE_HTML_UTF_8);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO,
            "Отдел Выплат <tabolin@yandex.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            "tabolin@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            "tabolin@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.DISPLAY_NAME,
            "Отдел Выплат\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM,
            "Система Оповещения <vanya2281331@yandex.ru>");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            "vanya2281331@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            "vanya2281331@yandex.ru\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.DISPLAY_NAME,
            "Система Оповещения\n");
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SUBJECT,
            "на Вас числиться не законченная операция, и ничего не сообщили?! "
            + "Советую забрать пока не сгорели ");
        doc.put(
            MailMetaInfo.X_URLS,
            "https://forms.yandex.ru/u/5e857bf7f5acbb33c7ebb891/\n");
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, "1585810132");
        doc.put(MailMetaInfo.RECEIVED_DATE, "1585810132.658");
        String smtpId = "bIDJeSzKyt-mpY86OPI";
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, "bOECxHvvuh-mqA8EvbT\n" + smtpId);
        checkMail("preamble.eml", json);
    }

    @Test
    public void testSubjectCp1251() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(
                    getClass().getResource("subject-cp1251.eml").toURI()));
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(""));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonSubsetChecker(
                        "{\"docs\":[{\"hdr_subject\":\"Обучение макияжу | "
                        + "Осетинские пироги | Массаж лица\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSubjectKoi8r() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(
                    getClass().getResource("subject-koi8-r.eml").toURI()));
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(""));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonSubsetChecker(
                        "{\"docs\":[{\"hdr_subject\":\"Обучение макияжу | "
                        + "Осетинские пироги | Массаж лица\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQuotedPrintable() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(
                    getClass().getResource("qprint.eml").toURI()));
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(""));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonSubsetChecker(
                        "{\"docs\":[{\"pure_body\":\""
                        + "В БОЛЬНИЦЕ ОПАСНО! ВЗРЫВНОЕ УСТРОЙСТВО С ФОСФОРНЫМ "
                        + "ЗАРЯДОМ НАХОДИТСЯ У ВАС НА ПЕРВОМ ЭТАЖЕ И ПРЕВРАТИТ"
                        + " ЗДАНИЕ В БРАТСКУЮ МОГИЛУ!!! ОЖИДАЕТСЯ МНОГО ЖЕРТВ"
                        + "\"},{\"pure_body\":\"В БОЛЬНИЦЕ ОПАСНО! ВЗРЫВНОЕ "
                        + "УСТРОЙСТВО С ФОСФОРНЫМ ЗАРЯДОМ НАХОДИТСЯ У ВАС НА "
                        + "ПЕРВОМ ЭТАЖЕ И ПРЕВРАТИТ ЗДАНИЕ В БРАТСКУЮ МОГИЛУ"
                        + "!!! ОЖИДАЕТСЯ МНОГО ЖЕРТВ\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQuotedPrintable2() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new File(
                    getClass().getResource("qprint2.eml").toURI()));
            backend.start();
            server.start();
            HttpPost post =
                new HttpPost(LOCALHOST + server.port() + PATH + NAME);
            post.setEntity(new StringEntity(""));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonSubsetChecker(
                        "{\"docs\":[{\"pure_body\":\""
                        + "MЫ ЗA ДРYГYЮ CBOБOДY, ПOTOMKИ РYCCKИX ФАШИКОВ БYДYT"
                        + " YHИЧTOЖАТЬСЯ! MЫ MИHИРOBAЛИ ЗДАНИЕ И ПРOДOЛЖEM "
                        + "ЧИCTКY И ДAЛЬШE."
                        + "\"},{\"pure_body\":\""
                        + "MЫ ЗA ДРYГYЮ CBOБOДY, ПOTOMKИ РYCCKИX ФАШИКОВ БYДYT"
                        + " YHИЧTOЖАТЬСЯ!\nMЫ MИHИРOBAЛИ ЗДАНИЕ И ПРOДOЛЖEM "
                        + "ЧИCTКY И ДAЛЬШE."
                        + "\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

