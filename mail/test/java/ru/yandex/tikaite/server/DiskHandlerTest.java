package ru.yandex.tikaite.server;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.server.sync.ContentWriter;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.server.HttpServerConfigBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

// CSOFF: MagicNumber
public class DiskHandlerTest extends TestBase {
    private static final String TEXT_HTML = "text/html";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_RTF = "application/rtf";
    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_TTF = "application/x-font-ttf";
    private static final String CONTENT_TYPE =
        "application/json; charset=UTF-8";
    private static final String MESSAGE_RFC822 = "message/rfc822";
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String BODY_TEXT = "Hello, world";
    private static final String BODY_TEXT_SHORT = "Hello";
    private static final String BODY_TEXT_LIMIT = "&limit=5";
    private static final String BODY =
        "<html><body>" + BODY_TEXT + "</body></html>";
    private static final String LIMIT_PARAM = "&limit=";
    private static final String GET = "/get/";
    private static final String PATH = GET + "somepath";
    private static final String NAME = "?name=disk";
    private static final String RAW = "?raw";
    private static final String LOCALHOST = "http://localhost:";
    private static final String META = ",\"meta\":\"";
    private static final String PARSED = "\",\"parsed\":true";
    private static final String PRIVET_MIR = "Привет, мир";
    private static final String PPT_HEADER =
        "Здравствуй, мир\n"
        + "Давай протестируем презентацию\n"
        + "Справа график, слева таблица";
    private static final String JSON_END = "\"}";
    private static final String CONTENT_TYPE_PPT =
        "application/vnd.ms-powerpoint";
    private static final String AUDIO_MPEG = "audio/mpeg";
    private static final String AUDIO_OGG = "audio/vorbis";
    private static final String FB2 = "application/x-fictionbook+xml";
    private static final String ALEVTINA = "Alevtina Gamzikova";
    private static final ContentType HTML_UTF8 =
        ContentType.create(TEXT_HTML, "UTF-8");
    private static final int DELAY = 10;
    private static final int TIMEOUT = 20000;

    public DiskHandlerTest() {
        super(false, 0L);
    }

    private static String toJson(final String text) {
        return toJson(text, ContentType.TEXT_HTML);
    }

    private static String toJson(final String text, final ContentType type) {
        StringBuilder sb = new StringBuilder("{\"mimetype\":\"");
        sb.append(type.getMimeType());
        sb.append("\",\"built_date\":\"");
        sb.append(HttpServerConfigBuilder.BUILT_DATE);
        sb.append("\",\"body_text\":");
        sb.append(text);
        sb.append(META);
        sb.append(CONTENT_TYPE_META);
        sb.append(type);
        sb.append(JSON_END);
        return sb.toString();
    }

    public static void assertInvalidJson(final HttpResponse response)
        throws IOException
    {
        try {
            EntityUtils.toString(response.getEntity());
            Assert.fail("Expected to be incomplete stream");
        } catch (IOException e) {
            return;
        }
    }

    @Test
    public void testPost() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            Server server = new Server(ServerTest.getConfig(1)))
        {
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpPost(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, BODY);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                new Json(
                    TEXT_HTML,
                    BODY_TEXT,
                    true,
                    null,
                    "Content-Type:text/html; charset=ISO-8859-1")
                    .assertEquals(EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNamedHtmlCharacterEntities() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                "<html><body>Привет, мир&lt;"
                + "&copy;&bscr;&acE;&b.alpha;&#128522;<br>"
                + "test: &#55357;&#56842;&#55357;</body></html>");
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());
                YandexAssert.check(
                    new JsonChecker(
                        toJson("\"Привет, мир<©"
                            + "\uD835\uDCB7∾̳&b.alpha;\uD83D\uDE0A\\n"
                            + "test: \uD83D\uDE0A\",\"parsed\":true",
                            HTML_UTF8)),
                    EntityUtils.toString(body));
            }
        }
    }

    public static void testJson(final String name, final Json json)
        throws Exception
    {
        testJson(name, json, "");
    }

    public static void testJson(
        final String name,
        final Json json,
        final String params)
        throws Exception
    {
        testJson(name, json, params, "");
    }

    // CSOFF: ParameterNumber
    public static void testJson(
        final String name,
        final Json json,
        final String params,
        final String configSuffix)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server =
                new Server(ServerTest.getConfig(backend.port(), configSuffix)))
        {
            URL url = DiskHandlerTest.class.getResource(name);
            File file;
            if (url == null) {
                file = new File(Paths.getSandboxResourcesRoot() + '/' + name);
            } else {
                file = new File(url.toURI());
            }
            backend.add(GET + name + RAW, file);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + GET + name + NAME + params)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());
                String bodyText = CharsetUtils.toString(body);
                try {
                    json.assertEquals(bodyText);
                } catch (Throwable e) {
                    System.err.println("Json " + bodyText + " caused:");
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }
    // CSON: ParameterNumber

    @Test
    public void testDocx()
        throws Exception
    {
        String mimetype = "application/"
            + "vnd.openxmlformats-officedocument.wordprocessingml.document";
        Json json = new Json(
            mimetype,
            BODY_TEXT,
            true,
            null,
            new Json.Headers(
                "Revision-Number:0\ncp:revision:0\n"
                + "Application-Name:LibreOffice/3.6$Linux_X86_64"
                + " LibreOffice_project/360m1$Build-3\n"
                + "extended-properties:DocSecurityString:None\n"
                + "extended-properties:Application:LibreOffice/3.6"
                + "$Linux_X86_64 LibreOffice_project/360m1$Build-3\n"
                + CONTENT_TYPE_META + mimetype));
        json.root().put(CommonFields.CREATED, 1362475988L);
        String docx = "hello.docx";
        String ooxml = "hello.ooxml.docx";
        testJson(docx, json);
        testJson(ooxml, json);
        json.root().put(CommonFields.TRUNCATED, 5L);
        json.root().put(CommonFields.BODY_TEXT, BODY_TEXT_SHORT);
        testJson(docx, json, BODY_TEXT_LIMIT);
        testJson(ooxml, json, BODY_TEXT_LIMIT);
    }

    @Test
    public void testOdt() throws Exception {
        Json json = new Json(
            "application/vnd.oasis.opendocument.text",
            BODY_TEXT,
            true,
            null,
            new Json.Headers(
                "Image-Count:0\n"
                + "nbObject:0\nmeta:paragraph-count:1\nWord-Count:2\n"
                + "Object-Count:0\nnbImg:0\nmeta:object-count:0\n"
                + "generator:LibreOffice/3.6$Linux_X86_64 "
                + "LibreOffice_project/360m1$Build-3\nParagraph-Count:1\n"
                + "meta:character-count:12\nnbTab:0\nmeta:word-count:2\n"
                + "meta:table-count:0\nmeta:image-count:0\n"
                + "Table-Count:0\nnbPara:1\nCharacter Count:12\nnbWord:2\nC"
                + "ontent-Type:application/vnd.oasis.opendocument.text\n"
                + "nbCharacter:12"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1362461588L);
        root.put(CommonFields.PAGES, 1L);
        String odt = "hello.odt";
        testJson(odt, json);
        root.put(CommonFields.TRUNCATED, 5L);
        root.put(CommonFields.BODY_TEXT, BODY_TEXT_SHORT);
        // TODO: fix this
        root.put(
            CommonFields.META,
            new Json.Headers(
                "Content-Type:application/vnd.oasis.opendocument.text\n"
                + "nbObject:0\n"
                + "meta:paragraph-count:1\n"
                + "Word-Count:2\n"
                + "Object-Count:0\n"
                + "nbImg:0\n"
                + "meta:object-count:0\n"
                + "generator:LibreOffice/3.6$Linux_X86_64 LibreOffice_project/"
                + "360m1$Build-3\n"
                + "Paragraph-Count:1\n"
                + "meta:character-count:12\n"
                + "nbTab:0\n"
                + "meta:word-count:2\n"
                + "meta:table-count:0\n"
                + "meta:image-count:0\n"
                + "Table-Count:0\n"
                + "nbPara:1\n"
                + "Character Count:12\n"
                + "nbWord:2\nImage-Count:0\n"
                + "nbCharacter:12\n"));
        testJson(odt, json, BODY_TEXT_LIMIT);
    }

    @Test
    public void testBadPpt() throws Exception {
        Json json = new Json(
            CONTENT_TYPE_PPT,
            new Json.AllOf(
                new Json.Contains(
                    "Самая полезная презентация\n"
                    + "Или чушь и баклажанная революция\n%-)\n*\n"
                    + "Когда баклажаны правили миром"),
                new Json.Contains(
                    "Для изменения диапазона данных диаграммы перетащите"),
                new Json.Contains("\nПродолжение следует\n")),
            true,
            null,
            new Json.Headers("meta:slide-count:2\ncp:revision:1\n"
                + "meta:last-author:Alevtina Gamzikova\nSlide-Count:2\n"
                + "Last-Author:Alevtina Gamzikova\n"
                + "Application-Name:Microsoft Office"
                + " PowerPoint\n"
                + "Word-Count:17\n"
                + "Edit-Time:3324240000\n"
                + "extended-"
                + "properties:Application:Microsoft Office PowerPoint\n"
                + "Company:Microsoft\nRevision-Number:1\n"
                + "extended-properties:Company:Microsoft\n"
                + "meta:word-count:17\n"
                + CONTENT_TYPE_META + CONTENT_TYPE_PPT));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, ALEVTINA);
        root.put(CommonFields.CREATED, 1326643106L);
        root.put(CommonFields.MODIFIED, 1326643438L);
        root.put(CommonFields.PAGES, 2L);
        root.put(
            CommonFields.TITLE,
            "Самая полезная презентация");
        String ppt = "ptt-qa.ppt";
        testJson(ppt, json);
        root.put(CommonFields.TRUNCATED, 13L);
        root.put(CommonFields.BODY_TEXT, "Самая полезна");
        testJson(ppt, json, "&limit=13");
    }

    @Test
    public void testOdp() throws Exception {
        String mimetype =
            "application/vnd.oasis.opendocument.presentation";
        Json json = new Json(
            mimetype,
            PPT_HEADER
            + "\nНомер числа\nПростое число\n"
            + "Число Мерсенна\n1\n2\n3\n2\n3\n7\n3\n5\n31\n"
            + "Простое число\nЧисло Фиббоначи\n"
            + "Полупростые числа\n1\n2\n0\n4\n2\n3\n1\n6\n3\n5"
            + "\n1\n9\n4\n7\n2\n10\n5\n11\n3\n14\n6\n13\n5\n15\n7\n17\n6\n21\n"
            + "8\n19\n13\n22\n9\n23\n21\n25",
            true,
            null,
            new Json.Headers(
                "nbObject:30\n"
                + "Object-Count:30\n"
                + "meta:object-count:30\n"
                + "generator:LibreOffice/4.0.2.2$Linux_X86_64 "
                + "LibreOffice_project/400m0$Build-2\n"
                + CONTENT_TYPE_META + mimetype));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1366007806L);
        testJson("test.odp", json);
    }

    @Test
    public void testPpt() throws Exception {
        Json json = new Json(
            CONTENT_TYPE_PPT,
            new Json.AllOf(
                new Json.Contains(
                    PPT_HEADER
                    + "\nНомер числа Простое число "
                    + "Число Мерсенна\n1 2 3\n2 3 7\n3 5 31\n"),
                new Json.Contains(
                    "А здесь вообще только текст\n"
                    + "Да, только текст.\n"
                    + "123456789\n0\n5\n10\n15\n20\n25\n"
                    + "Число Фиббоначи\nПолупростые числа\n"
                    + "Число Фиббоначи\nПростое число\nПолупростые числа")),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + CONTENT_TYPE_PPT
                + "\nRevision-Number:0"
                + "\ncp:revision:0"
                + "\nEdit-Time:20670680000000"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1366007806L);
        testJson("test.ppt", json);
    }

    @Test
    public void testPptx() throws Exception {
        String mimetype =
            "application/vnd.openxmlformats-officedocument."
            + "presentationml.presentation";
        testJson(
            "test.pptx",
            new Json(
                mimetype,
                PPT_HEADER,
                true,
                null,
                new Json.Headers(
                    CONTENT_TYPE_META + mimetype
                    + "\nextended-properties:DocSecurityString:None")));
    }

    @Test
    public void testExtractRfc822() throws Exception {
        Json json = new Json(
            MESSAGE_RFC822,
            "Good day to you all\n"
            + "date/time: 12.01.2011 2118 UTC\n"
            + "cargo: Full- 224 X 20'+ 458 X 40' Weight 12768 Mt\n"
            + "ETA: Singapore 16.01.2010 2100LT\n"
            + "AGENTS: KANOO Intergroup Shipping PTE LTD\n"
            + "120 Robinson Road #05-01\n"
            + "Parakou Building\n"
            + "Singapore 068913\n"
            + "Tel : +65 6508 2071, Mobile +65 9787 8380\n"
            + "E Mail :frank.choo@kanoo-intergroup.com\n"
            + "R.O.B : HFo= 426.9 DO=78.10 FW=196\n"
            + "brgds\nCapt.Minkiewicz Mariusz",
            true,
            null,
            new Json.Headers(
                "Content-Type:message/rfc822\n"
                + "Message-Cc:operating@mlb-sunship.de\n"
                + "Message-Cc:walkiewicz@sunship.de\n"
                + "Message-From:City of Shanghai\n"
                + "Message-From:Owners.City-Shanghai@SkyFile.com\n"
                + "Message:From-Email:Owners.City-Shanghai@SkyFile.com\n"
                + "Message:From-Name:City of Shanghai\n"
                + "Message-To:info@sunship.de"));
        Map<String, Object> root = json.root();
        root.put(
            CommonFields.AUTHOR,
            new Json.Headers(
                "City of Shanghai\nOwners.City-Shanghai@SkyFile.com"));
        root.put(CommonFields.CREATED, 1332335057L);
        root.put(
            CommonFields.SUBJECT,
            "City of Shanghai -departure report -Shekou");
        root.put(CommonFields.TITLE, root.get(CommonFields.SUBJECT));
        testJson("disk.mail.txt", json);
    }

    @Test
    public void testExtractRfc822Html() throws Exception {
        Json json = new Json(
            MESSAGE_RFC822,
            "What about some test?\n©\uD835\uDCB7∾̳&b.alpha;",
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + MESSAGE_RFC822 + '\n'
                + "Message:Raw-Header:Content-Type:text/html; "
                + "charset=\"utf-8\"\n"
                + "Message-From:Dmitry Potapov\n"
                + "Message-From:potapov.d@gmail.com\n"
                + "Message:From-Email:potapov.d@gmail.com\n"
                + "Message:From-Name:Dmitry Potapov\n"
                + "Message-To:analizer@yandex.ru"));
        Map<String, Object> root = json.root();
        root.put(
            CommonFields.AUTHOR,
            new Json.Headers("Dmitry Potapov\npotapov.d@gmail.com"));
        root.put(CommonFields.CREATED, 1332335056L);
        root.put(CommonFields.SUBJECT, "Test named entities");
        root.put(CommonFields.TITLE, root.get(CommonFields.SUBJECT));
        testJson("disk.mail.html.txt", json);
    }

    @Test
    public void testExtractRfc822AuthenticationResult() throws Exception {
        Json json = new Json(
            MESSAGE_RFC822,
            "Пожалуйста,\nне нажимайте \"Reply\"\n"
            + "(\"Ответить\") в Вашей почтовой "
            + "программе! Пользуйтесь ссылками "
            + "в тексте письма.\n"
            + "от\nОбезьянка из анекдота про "
            + "умных и красивых\n23.03.2012\n16:32:57\n"
            + "Тема:\nЖизненная ситуация\n"
            + "Искренне-искренне) Не "
            + "сомневайтесь)\n"
            + "Мне-то какой резон неискренне "
            + "тут чего-то желать)))\n"
            + "Т.ч.полностью я Вас поддерживаю в "
            + "борьбе) Надеюсь, что все у Вас "
            + "получится)\nОтветить в конференции"
            + "\nПредыдущее сообщение\n|\n"
            + "Вся тема\nРЕКЛАМА\n"
            + "ЧТО ЭТО?\n"
            + "Это сообщение из конференции\n"
            + "'Семейные отношения'\nсайта\n7я.ру\n"
            + "А ЗАЧЕМ?\nКто-то Вас сюда подписал..."
            + " Возможно это были Вы сами. :) В "
            + "любом случае, отписаться от "
            + "рассылки можно\nздесь\n.\n"
            + "ЧаВо по подписке\n"
            + "КАК ОТВЕТИТЬ\nВНИМАНИЕ!\n"
            + "Если Вы хотите отправить ответ "
            + "автору сообщения, нужно жать на "
            + "ссылочку \"ответить автору по email\" "
            + "в тексте письма, а\n"
            + "не делать Reply (Ответить) в почтовой "
            + "программе!\nЕсли такой ссылочки "
            + "нет, значит, у автора нет e-mail и "
            + "ответить ему Вы не можете. fromconf@7ya.ru"
            + " - это просто адрес, с которого "
            + "идет рассылка!",
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + MESSAGE_RFC822 + '\n'
                + "Message:Raw-Header:Authentication-Results:mxfront4.mail."
                + "yandex.net; spf=pass (mxfront4.mail.yandex.net: domain of "
                + "7ya.ru designates 195.2.68.158 as permitted sender) smtp."
                + "mail=fromconf@7ya.ru\n"
                + "Message:Raw-Header:Content-Transfer-Encoding:base64\n"
                + "Message:Raw-Header:Content-Type:text/html; charset=utf-8\n"
                + "Message:Raw-Header:MIME-Version:1.0\n"
                + "Message:Raw-Header:Message-ID:<SRV37Mum03yMJiQTyfP001d1590@"
                + "IIS14tmp.ALPMSK.alp.ru>\n"
                + "Message:Raw-Header:Received:from IIS14tmp.ALPMSK.alp.ru ("
                + "int013.ip-zenon.alp.ru [10.1.1.222])\tby mailers.alp.ru ("
                + "Postfix) with ESMTP id 6051F292\tfor <hotsalsa@ya.ru>; "
                + "Fri, 23 Mar 2012 16:39:57 +0400 (MSK)\n"
                + "Message:Raw-Header:Received:from Srv37 ([127.0.0.1]) by "
                + "IIS14tmp.ALPMSK.alp.ru with Microsoft SMTPSVC(6.0.3790.4675"
                + ");\t Fri, 23 Mar 2012 16:32:57 +0400\n"
                + "Message:Raw-Header:Return-Path:fromconf@7ya.ru\n"
                + "Message:Raw-Header:X-OriginalArrivalTime:23 Mar 2012 12:32:"
                + "57.0370 (UTC) FILETIME=[13AA97A0:01CD08F1]\n"
                + "Message-From:fromconf@7ya.ru\n"
                + "Message:From-Email:fromconf@7ya.ru\n"
                + "Message-To:meta.user@ya.ru"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "fromconf@7ya.ru");
        root.put(CommonFields.CREATED, 1332505977L);
        root.put(
            CommonFields.SUBJECT,
            "7я.ру [family]. Жизненная ситуация");
        root.put(CommonFields.TITLE, root.get(CommonFields.SUBJECT));
        testJson("rfc822.auth.result.txt", json);
    }

    @Test
    public void testNotRfc822() throws Exception {
        Json json = new Json(
            TEXT_PLAIN,
            new Json.Contains("Conflict file content"),
            true,
            null,
            CONTENT_TYPE_META + TEXT_PLAIN + "; charset=windows-1252");
        testJson("stackoverflow.txt", json);
    }

    @Test
    public void testDetailPdf() throws Exception {
        Json json =
            new Json(
                APPLICATION_PDF,
                new Json.Contains("Эстакады и каналы технологических "),
                true,
                Json.ANY_VALUE,
                new Json.Headers(
                    CONTENT_TYPE_META + APPLICATION_PDF
                    + "\ndc:format:application/pdf; version=1.6\n"
                    + "pdf:PDFVersion:1.6\n"
                    + "pdf:docinfo:created:2013-06-07T13:11:46Z\n"
                    + "pdf:docinfo:creator:User1\n"
                    + "pdf:docinfo:creator_tool:Adobe Acrobat Pro 9.3.2\n"
                    + "pdf:docinfo:modified:2013-06-07T13:11:46Z\n"
                    + "pdf:docinfo:producer:novaPDF Professional Desktop Ver 7"
                    + ".2 Build 348 (Windows XP Home Edition  (SP 3) - Version"
                    + ": 5.1.2600 (x86))\n"
                    + "pdf:docinfo:title:Деталь а_1\n"
                    + "pdf:encrypted:false\n"
                    + "pdf:hasMarkedContent:true\n"
                    + "pdf:hasXFA:false\n"
                    + "pdf:hasXMP:true\n"
                    + "pdf:producer:novaPDF Professional Desktop Ver 7.2 Build"
                    + " 348 (Windows XP Home Edition  (SP 3) - Version: "
                    + "5.1.2600 (x86))\n"
                    + "xmp:CreateDate:2013-06-07T17:11:46Z\n"
                    + "xmp:MetadataDate:2013-06-07T17:11:46Z\n"
                    + "xmp:ModifyDate:2013-06-07T17:11:46Z\n"
                    + "xmpMM:DocumentID:uuid:b917fa86-83a8-40f7-bc7b-"
                    + "de70850fc341"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "User1");
        root.put(CommonFields.TOOL, "Adobe Acrobat Pro 9.3.2");
        root.put(
            CommonFields.PRODUCER,
            "novaPDF Professional Desktop Ver 7.2 Build 348 (Windows XP Home "
            + "Edition  (SP 3) - Version: 5.1.2600 (x86))");
        root.put(CommonFields.TITLE, "Деталь а_1");
        root.put(CommonFields.PAGES, 247L);
        root.put(CommonFields.CREATED, 1370610706L);
        root.put(CommonFields.MODIFIED, 1370610706L);
        testJson("detail.pdf", json);
    }

    private void testExtractText(
        final String name,
        final String text,
        final String charset)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                GET + name + RAW,
                new File(getClass().getResource(name).toURI()));
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + GET + name + NAME
                    + "&mimetype=text%2Fplain")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());
                YandexAssert.check(
                    new JsonChecker(
                        toJson(
                            '"' + text + PARSED,
                            ContentType.TEXT_PLAIN.withCharset(charset))),
                    EntityUtils.toString(body));
            }
        }
    }

    @Test
    public void testExtractWindows1251Text() throws Exception {
        testExtractText(
            "hello.windows1251.txt",
            PRIVET_MIR,
            "windows-1251");
    }

    @Test
    public void testExtractKoi8rText() throws Exception {
        testExtractText("hello.koi8r.txt", PRIVET_MIR, "koi8-r");
    }

    @Test
    public void testExtractUtf8Text() throws Exception {
        testExtractText("hello.utf8.txt", PRIVET_MIR, "utf-8");
    }

    public void badDataTest(
        final String data,
        final String contentType,
        final String error)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, data);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                JsonChecker expected =
                    new JsonChecker(
                        toJson(
                            "\"\",\"parsed\":false",
                            ContentType.create(contentType)));
                @SuppressWarnings("unchecked")
                Map<String, Object> map =
                    (Map<String, Object>) expected.expected();
                map.put(CommonFields.ERROR, Json.ANY_VALUE);
                YandexAssert.check(
                    expected,
                    EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadXml() throws Exception {
        badDataTest(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><el/><el></root>",
            APPLICATION_XML,
            "org.apache.tika.exception.TikaException: XML parse error");
    }

    @Test
    public void testBadRtf() throws Exception {
        badDataTest(
            "{\\rtf1\\pard\\plain\\ltrpar\\bin10}",
            APPLICATION_RTF,
            "org.apache.tika.exception.TikaException: unexpected end of file: "
            + "need 10 bytes of binary data, found 1");
    }

    private static void testOctetStream(final HttpEntity entity)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, entity);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testOctetStream() throws Exception {
        final int length = 128;
        final int mask = 31;
        byte[] buf = new byte[length];
        for (int i = 0; i < length; ++i) {
            buf[i] = (byte) (i & mask);
        }
        testOctetStream(new ByteArrayEntity(buf));
        testOctetStream(
            new ByteArrayEntity(
                Files.readAllBytes(
                    new File(
                        DiskHandlerTest.class.getResource("MS208a_13C-1.jdf")
                            .toURI())
                        .toPath())));
    }

    @Test
    public void testNotFound() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, HttpStatus.SC_NOT_FOUND, BODY);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_NOT_FOUND,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testFailedBackend() throws Exception {
        int port;
        try (StaticServer fake = new StaticServer(Configs.baseConfig())) {
            port = fake.port();
        }
        try (CloseableHttpClient client = HttpClients.createDefault();
            Server server = new Server(ServerTest.getConfig(port)))
        {
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_GATEWAY,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    private void checkOverridedLimit(
        final CloseableHttpClient client,
        final int port,
        final String limit)
        throws Exception
    {
        try (CloseableHttpResponse response = client.execute(new HttpGet(
            LOCALHOST + port + PATH + NAME + LIMIT_PARAM + limit)))
        {
            Assert.assertEquals(
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            HttpEntity body = response.getEntity();
            Assert.assertEquals(
                CONTENT_TYPE,
                body.getContentType().getValue());

            YandexAssert.check(
                new JsonChecker(toJson('"' + BODY_TEXT + PARSED)),
                EntityUtils.toString(body));
        }
    }

    @Test
    public void testOverrideConfigLimit() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(PATH + RAW, BODY);
            backend.start();
            server.start();
            final int limit = 4;
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                LOCALHOST + server.port() + PATH + NAME + LIMIT_PARAM
                + limit)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());
                YandexAssert.check(
                    new JsonChecker(
                        toJson(
                            "\"Hell\",\"parsed\":true,\"truncated\":"
                            + limit)),
                    EntityUtils.toString(body));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME
                    + BODY_TEXT_LIMIT)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());

                YandexAssert.check(
                    new JsonChecker(
                        toJson("\"Hello\",\"parsed\":true,\"truncated\":5")),
                    EntityUtils.toString(body));
            }

            for (String limitString: new String[] {
                "1M", "1000", Integer.toString(BODY_TEXT.length())})
            {
                checkOverridedLimit(client, server.port(), limitString);
            }
        }
    }

    @Test
    public void testBadStorage() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                PATH + RAW,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_FORBIDDEN), DELAY));
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_GATEWAY,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testStorageTimeout() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(
                ServerTest.getConfig(backend.port(), "\nstorage.timeout = 1")))
        {
            backend.add(
                PATH + RAW,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_OK), DELAY));
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_GATEWAY_TIMEOUT,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testDoubleRtf() throws Exception {
        Json json = new Json(
            APPLICATION_RTF,
            new Json.StartsWith("Форматы файлов:\n"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_RTF
                + "\nmeta:character-count:749\n"
                + "extended-properties:Company:"
                + "Microsoft\nmeta:word-count:131"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, ALEVTINA);
        root.put(CommonFields.CREATED, 1326643020L);
        root.put(CommonFields.PAGES, 1L);
        testJson("double.rtf", json);
    }

    @Test
    public void testOgg() throws Exception {
        Json json =
            new Json(
                AUDIO_OGG,
                "00:00:06.10",
                true,
                null,
                new Json.Headers(
                    "xmpDM:audioCompressor:Vorbis"
                    + "\nvendor:Xiph.Org libVorbis I 20020717\n"
                    + "xmpDM:audioChannelType:Stereo\n"
                    + "xmpDM:audioSampleRate:44100\n"
                    + "Content-Type:audio/vorbis\nversion:Vorbis 0"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.DURATION, 7L);
        root.put(CommonFields.TOOL, "Xiph.Org libVorbis I 20020717");
        testJson("example.ogg", json);
    }

    @Test
    public void testBadOgg() throws Exception {
        testJson(
            "bad.ogg",
            new Json(
                "application/ogg",
                "",
                true,
                null,
                new Json.Headers(
                    "Content-Type:application/ogg\n"
                    + "streams-vorbis:1\nstreams-total:3\nstreams-video:1\n"
                    + "streams-audio:1\nstreams-annodex:1\nstreams-theora:1\n"
                    + "streams-metadata:1")));
    }

    @Test
    public void testBadDocx() throws Exception {
        Json json =
            new Json(
                "application/vnd.openxmlformats-"
                + "officedocument.wordprocessingml.document",
                new Json.AllOf(
                    new Json.Contains("Втулка полурессоры"),
                    new Json.Contains("Время на выполнение работ по сборке")),
                true,
                null,
                new Json.Headers(
                    "Content-Type:application/vnd.openxmlformats"
                    + "-officedocument.wordprocessingml.document\n"
                    + "cp:revision:12\nmeta:last-author:Прохоров\n"
                    + "Last-Author:Прохоров\n"
                    + "Application-Name:Microsoft Office Word\n"
                    + "Application-Version:12.0000\n"
                    + "Character-Count-With-Spaces:62214\nTotal-Time:149\n"
                    + "extended-properties:Template:Normal\n"
                    + "meta:line-count:441\nWord-Count:9304\n"
                    + "meta:paragraph-count:124\n"
                    + "extended-properties:AppVersion:12.0000\n"
                    + "Line-Count:441\n"
                    + "extended-properties:DocSecurityString:None\n"
                    + "extended-properties:Application:Microsoft Office Word\n"
                    + "Paragraph-Count:124\nRevision-Number:12\n"
                    + "Template:Normal\nmeta:character-count:53034\n"
                    + "meta:word-count:9304\n"
                    + "extended-properties:TotalTime:149\n"
                    + "Character Count:53034\n"
                    + "meta:character-count-with-spaces:62214"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1339232100L);
        root.put(CommonFields.MODIFIED, 1366113780L);
        root.put(CommonFields.AUTHOR, "Alexander");
        root.put(CommonFields.TITLE, "Service Vertrag");
        root.put(CommonFields.PRINT_DATE, 1310551020L);
        root.put(CommonFields.PAGES, 33L);
        testJson("bad.docx", json);
    }

    @Test
    public void testOomPpt() throws Exception {
        Json json = new Json(
            CONTENT_TYPE_PPT,
            new Json.StartsWith(
                "ЭЛЕКТРОННЫЕ РЕСУРСЫ ПО ПОДПИСКЕ\nОБЗОР РОССИЙСКОГО "
                + "РЫНКА\nРынок - это …"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + CONTENT_TYPE_PPT
                + "\ncp:revision:99\nmeta:last-author:Margarita\n"
                + "Last-Author:Margarita\n"
                + "Application-Name:Microsoft Office PowerPoint\n"
                + "extended-properties:Template:TS030006921\n"
                + "Word-Count:1121\nEdit-Time:1150266400000\n"
                + "extended-properties:Application:Microsoft "
                + "Office PowerPoint\n"
                + "Revision-Number:99\n"
                + "Template:TS030006921\n"
                + "meta:word-count:1121"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "lib");
        root.put(CommonFields.CREATED, 1286252112L);
        root.put(CommonFields.MODIFIED, 1288336526L);
        root.put(CommonFields.TITLE, "Золотая Осень");
        testJson("oom.ppt", json);
    }

    @Test
    public void testDeminoPpt() throws Exception {
        Json json = new Json(
            CONTENT_TYPE_PPT,
            new Json.StartsWith("ОАО Центр лыжного\nспорта «Демино»"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + CONTENT_TYPE_PPT
                + "\nmeta:slide-count:12\ncp:revision:49\n"
                + "meta:last-author:Админ\nSlide-Count:12\n"
                + "Last-Author:Админ\n"
                + "Word-Count:477\n"
                + "Edit-Time:7420192640000\n"
                + "Revision-Number:49\n"
                + "meta:word-count:477"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "zayceva");
        root.put(CommonFields.CREATED, 1277291775L);
        root.put(CommonFields.MODIFIED, 1372766897L);
        root.put(CommonFields.PAGES, 12L);
        root.put(CommonFields.TITLE, "Слайд 1");
        testJson("demino.ppt", json);
    }

    @Test
    public void testFb2() throws Exception {
        Json json = new Json(
            FB2,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + FB2));
        Map<String, Object> root = json.root();
        root.put(
            CommonFields.BODY_TEXT,
            new Json.AllOf(
                new Json.Contains(
                    "Федор Достоевский\nМАЛЬЧИК У ХРИСТА НА ЕЛКЕ"),
                new Json.Contains(
                    "Набрав копеек, мальчик возвращается с красными, "
                    + "окоченевшими руками")));
        String description = "стало быть, лишь начинал профессию.";
        root.put(CommonFields.AUTHOR, "Федор Михайлович Достоевский");
        root.put(CommonFields.TITLE, "Мальчик у Христа на елке");
        root.put(CommonFields.GENRE, "prose_classic\nprose_rus_classic");
        root.put(CommonFields.DESCRIPTION, new Json.Contains(description));
        String filename = "Dostoevskij_-_Mal_chik_u_KHrista_na_elke.fb2";
        testJson(filename, json);
        testJson(filename, json);
        root.put(CommonFields.TRUNCATED, 3000L);
        testJson(filename, json, "&limit=3000");
    }

    @Test
    public void testBrelMp3() throws Exception {
        Json json = new Json(
            AUDIO_MPEG,
            "",
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + AUDIO_MPEG
                + "\nxmpDM:audioChannelType:Stereo\nchannels:2\nxmp"
                + "DM:audioSampleRate:44100\n"
                + "version:MPEG 3 Layer III Version 1\nxmp"
                + "DM:audioCompressor:MP3\nsamplerate:44100\n"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.ALBUM, "I Put A Spell On You");
        String author = "Nina Simone";
        root.put(CommonFields.ARTIST, author);
        root.put(CommonFields.AUTHOR, author);
        root.put(
            CommonFields.BODY_TEXT,
            "Ne Me Quitte Pas\nNina Simone\nI Put"
            + " A Spell On You, track 3\n1965\nJazz\n220.06917\nrus -\nWritten"
            + " by J.Brel\n\ufffd\ufffd\ufffd -\nLisle Atkinson - Bass; Bob "
            + "Hamilton - Drums; Nina Simone - Piano, Vocals; Rudy Stevenson -"
            + " Flute, Guitar.");
        root.put(
            CommonFields.COMMENT,
            "rus - \nWritten by J.Brel\n"
            + "- \nLisle Atkinson - Bass; Bob Hamilton - Drums; Nina Simone - "
            + "Piano, Vocals; Rudy Stevenson - Flute, Guitar.");
        root.put(CommonFields.COMPOSER, "J.Brel");
        root.put(CommonFields.DURATION, 221L);
        root.put(CommonFields.GENRE, "Jazz");
        root.put(CommonFields.RELEASED, "1965");
        root.put(CommonFields.TITLE, "Ne Me Quitte Pas");
        root.put(CommonFields.TRACK_NUMBER, 3L);
        testJson("brel.mp3", json, "&mimetype=text/html");
    }

    @Test
    public void testNauMp3() throws Exception {
        Json json = new Json(
            AUDIO_MPEG,
            "",
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + AUDIO_MPEG
                + "\nxmpDM:audioChannelType:Stereo\n"
                + "channels:2\nxmpDM:audioSampleRate:44100\n"
                + "version:MPEG 3 Layer III Version 1\n"
                + "xmpDM:audioCompressor:MP3\nsamplerate:44100\n"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.ALBUM, "Разлука");
        String artist = "Наутилус Помпилиус";
        root.put(CommonFields.ARTIST, artist);
        root.put(CommonFields.AUTHOR, artist);
        root.put(
            CommonFields.BODY_TEXT,
            "Разлука (Эпиграф)\nНаутилус Помпилиус\n"
            + "Разлука, track 01/11\n1986\nRussian rock\n140.1528\n"
            + "eng -\nExactAudioCopy v0.95b4");
        root.put(CommonFields.COMMENT, "eng - \nExactAudioCopy v0.95b4");
        root.put(CommonFields.DURATION, 141L);
        root.put(CommonFields.GENRE, "Russian rock");
        root.put(CommonFields.RELEASED, "1986");
        root.put(CommonFields.TITLE, "Разлука (Эпиграф)");
        root.put(CommonFields.TRACK_NUMBER, 1L);
        root.put(CommonFields.ALBUM_TRACKS, 11L);
        testJson("nau.mp3", json, "&mimetype=audio/ogg");
    }

    @Test
    public void testManyEntitiesXml() throws Exception {
        testJson(
            "many-entities.xml",
            new Json(
                TEXT_PLAIN,
                new Json.Contains("text"),
                true,
                null,
                "Content-Type:text/plain; charset=ISO-8859-1"),
            "&mimetype=application/octet-stream");
    }

    @Test
    public void testBadTtf() throws Exception {
        Json json = new Json(
            APPLICATION_TTF,
            "",
            false,
            Json.ANY_VALUE,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_TTF));
        testJson("test.ttf", json);
    }

    @Test
    public void testResetTtf() throws Exception {
        Json json = new Json(
            APPLICATION_TTF,
            "",
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_TTF
                + "\nFontName:Yokawerad Regular"
                + "\nFontSubFamilyName:Regular"
                + "\nFontFamilyName:Yokawerad"
                + "\nDocVersion:1.0\nPSName:Yokawerad"
                + "\nCopyright:Created by gluk (gluksza@wp.pl | "
                + "www.glukfonts.pl) with FontForge"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1358528373L);
        root.put(CommonFields.MODIFIED, 1358528373L);
        root.put(CommonFields.TITLE, "Yokawerad Regular");
        testJson("reset.ttf", json);
    }

    @Test
    public void testNoHtmlScript() throws Exception {
        Json json = new Json(
            TEXT_HTML,
            new Json.AllOf(
                new Json.Contains("ФХР требует наказать немецкого игрока"),
                new Json.Not(new Json.Contains("window.device")),
                new Json.Not(new Json.Contains("authenticate"))),
            true,
            null,
            CONTENT_TYPE_META + HTML_UTF8);
        Map<String, Object> root = json.root();
        root.put(CommonFields.TITLE, "Яндекс");
        testJson("yandex.ru.html", json);
    }

    @Test
    public void testBadInputSimplest() throws Exception {
        ContentWriter writer = new ContentWriter() {
            @Override
            public void writeTo(final Writer writer) throws IOException {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<root>\n\t<text>");
                writer.write("\t<text>Hello, world</text>\n");
                final int fillers = 65536;
                for (int i = 0; i < fillers; ++i) {
                    writer.write("\t<filler></filler>\n");
                }
                writer.flush();
                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    return;
                }
                writer.write("\t<text>Hello again</text>\n</root>");
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
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                assertInvalidJson(response);
            }
        }
    }

    @Test
    public void testBadInputSimplestDetect() throws Exception {
        ContentWriter writer = new ContentWriter() {
            @Override
            public void writeTo(final Writer writer) throws IOException {
                writer.write("<?xml version=\"1.0\"");
                writer.flush();
                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    return;
                }
                writer.write(" encoding=\"UTF-8\"?>\r\n<root>\r\n");
                writer.write("\t<text>Hello, world</text>\r\n</root>");
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
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    LOCALHOST + server.port() + PATH + NAME + "&flag")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_GATEWAY_TIMEOUT,
                    response.getStatusLine().getStatusCode());
            }
        }
    }
}
// CSON: MagicNumber

