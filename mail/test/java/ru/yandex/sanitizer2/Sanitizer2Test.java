package ru.yandex.sanitizer2;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.sanitizer2.config.ImmutableSanitizer2Config;
import ru.yandex.sanitizer2.config.PageHeaderConfigBuilder;
import ru.yandex.sanitizer2.config.Sanitizer2ConfigBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.CloseableDeleter;

public class Sanitizer2Test extends TestBase {
    private static final String BEGIN = "<html><body>";
    private static final String END = "</body></html>";
    private static final String NO_RESOLVE = "<base href=\"no_resolve\">";
    private static final String STYLE_BEGIN = "<html><head><style>\n";
    private static final String STYLE_END = "</style>\n</head>\n";
    private static final String SECPROXY = "/?s=mail_secproxy";
    private static final String UNPROXY = "/?s=mail_unproxy";
    private static final String SPAM = "/?s=mail_spam";
    private static final String WEB = "/?s=web";
    private static final String DETEMPL = "/?s=detempl";
    private static final String COMPACT = "/?s=compact";
    private static final String DISABLE_HIDEREFERER =
        "&disable-hidereferer=true&preserve-classes=true";
    private static final String TEXT_PLAIN = "&mimetype=text/plain";
    private static final String SLASH = " />";
    private static final String CLOSE_DIV = "</div>";
    private static final String PATTERN_FILE = "type = regex\ncontent = ";
    private static final String HEADER_FILE = "\nheader-file = ";
    private static final String CONFIG_PATH =
        "mail/library/html/sanitizer/sanitizer2_config/sanitizer2.conf";
    private static final String JSON_MARKUP_YA_RU =
        "[{\"type\":3,\"position\":[3,20,9,13]}]";
    private static final String PHISHING_LINKS_SIGNAL = "phishing-links_ammm";
    private static final ImmutableSanitizer2Config DEFAULT_CONFIG;

    static {
        System.setProperty("BSCONFIG_IDIR", "");
        System.setProperty("BSCONFIG_IPORT", Integer.toString(0));
        System.setProperty("SANITIZER2_PORT", Integer.toString(0));
        System.setProperty("NANNY_SERVICE_ID", "ps_sanitizer");
        System.setProperty("CPU_CORES", "20");
        try {
            DEFAULT_CONFIG = config(false).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sanitizer2Test() {
        super(false, 0L);
    }

    private static Sanitizer2ConfigBuilder config(
        final boolean enablePageHeaders)
        throws Exception
    {
        IniConfig ini =
            new IniConfig(new File(Paths.getSourcePath(CONFIG_PATH)));
        ini.sections().remove("log");
        ini.sections().remove("accesslog");
        ini.sections().remove("stderr");
        ini.sections().get("server")
            .sections().remove("free-space-signals");
        ini.section("stat")
            .section("/stat")
            .put("prefix", "ignore-stat");

        Sanitizer2ConfigBuilder config = new Sanitizer2ConfigBuilder(ini);
        ini.checkUnusedKeys();
        config.port(0);
        config.connections(2);
        if (!enablePageHeaders) {
            config.sanitizers().traverse(
                (pattern, sanitizingConfig) ->
                    sanitizingConfig.pageHeaders().clear());
        }
        return new Sanitizer2ConfigBuilder(config.build());
    }

    private static HttpPost createPost(
        final Sanitizer2 sanitizer2,
        final String body)
        throws Exception
    {
        return createPost(sanitizer2, "", body);
    }

    private static HttpPost createPost(
        final Sanitizer2 sanitizer2,
        final String uri,
        final String body)
        throws Exception
    {
        HttpPost post = new HttpPost(sanitizer2.host() + uri);
        post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        return post;
    }

    @Test
    public void test() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(config(true).build());
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // Test tags balancing
            String request = BEGIN + NO_RESOLVE + "<B>text" + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker("<b>text</b>"));
            }

            // Test javascript erasure
            request =
                BEGIN + NO_RESOLVE
                + "<a href=\"&#32;javascript:alert(1)\">ссылка</a><br>"
                + "<a href=\"&#32;javascript&colon;alert(1)\">link</a>"
                + "<a href=\"HTTP://YA.RU/test?a=b&amp;txt=23&param=1\">link2"
                + "</a>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[25,51,31,44]}]"),
                    new StringChecker(
                        "ссылка<br />link<a href=\"http://ya.ru/test?a=b"
                        + "&amp;txt=23&amp;param=1\">link2</a>"));
            }
        }
    }

    @Test
    public void testTableBorder() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table border=10 bordercolor=\"fff\" width=\"100%\"><tbody>"
                + "<tr valign=\"top\"><td width=185 align=middle>cell1</td>"
                + "<td width=\"200%\">cell2</td></tr></tbody></table>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<table border=\"10\" bordercolor=\"fff\" width=\"100%"
                        + "\"><tbody>"
                        + "<tr valign=\"top\"><td align=\"middle\" width=\"185"
                        + "\">cell1</td><td>cell2</td></tr></tbody></table>"));
            }
        }
    }

    @Test
    public void testMailto() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div dir=\"ltr\"><a href=\"mailto:potapov.d@gmail.com"
                + "?Subject=hello\">Dmirty</a><br><a href=\"mailto:1C_EDO@"
                + "abcdef.ru?subject=subject#fragment##inner#end\">link</a>"
                + CLOSE_DIV;
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[18,47,24,40]},"
                        + "{\"type\":3,\"position\":[85,72,91,65]}]"),
                    new StringChecker(
                        "<div dir=\"ltr\">"
                        + "<a href=\"mailto:potapov.d@gmail.com"
                        + "?Subject=hello\">Dmirty</a><br /><a href=\"mailto:"
                        + "1C_EDO@abcdef.ru?subject=subject#fragment%23%23"
                        + "inner%23end\">link</a></div>"));
            }
        }
    }

    @Test
    public void testMalformedMailto() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"mailto:hotline@auto.ru?subject=Авто.Выполнено. "
                + "Автоответ.  [0-00033398] Переименовать пользователя в базе "
                + "Финансы ВВ&amp;body=Обращение отработано Хорошо. "
                + "Комментарий: &amp;\" target=\"_blank\" "
                + "rel=\" noopener  NOFOLLOW  \"><font color=\"blue\" "
                + "size=\"1\" face=\"Arial\">Обращение отработано Хорошо <br>"
                + "</font></a>"
                + "<a href=\"mailto:helpdesk@it.mygroup.ru?subject=Update_:0 "
                + "Inc.#0000695569 &amp;body=\" target=\"_self\">"
                + "Обратная связь</a>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<a href=\"mailto:hotline@auto.ru?subject=%D0%90%D0%B2"
                        + "%D1%82%D0%BE.%D0%92%D1%8B%D0%BF%D0%BE%D0%BB%D0%BD"
                        + "%D0%B5%D0%BD%D0%BE.%20%D0%90%D0%B2%D1%82%D0%BE%D0"
                        + "%BE%D1%82%D0%B2%D0%B5%D1%82.%20%20[0-00033398]%20"
                        + "%D0%9F%D0%B5%D1%80%D0%B5%D0%B8%D0%BC%D0%B5%D0%BD%D0"
                        + "%BE%D0%B2%D0%B0%D1%82%D1%8C%20%D0%BF%D0%BE%D0%BB%D1"
                        + "%8C%D0%B7%D0%BE%D0%B2%D0%B0%D1%82%D0%B5%D0%BB%D1%8F"
                        + "%20%D0%B2%20%D0%B1%D0%B0%D0%B7%D0%B5%20%D0%A4%D0%B8"
                        + "%D0%BD%D0%B0%D0%BD%D1%81%D1%8B%20%D0%92%D0%92&amp;"
                        + "body=%D0%9E%D0%B1%D1%80%D0%B0%D1%89%D0%B5%D0%BD%D0"
                        + "%B8%D0%B5%20%D0%BE%D1%82%D1%80%D0%B0%D0%B1%D0%BE%D1"
                        + "%82%D0%B0%D0%BD%D0%BE%20%D0%A5%D0%BE%D1%80%D0%BE%D1"
                        + "%88%D0%BE.%20%D0%9A%D0%BE%D0%BC%D0%BC%D0%B5%D0%BD"
                        + "%D1%82%D0%B0%D1%80%D0%B8%D0%B9:%20&amp;\" "
                        + "rel=\"noopener nofollow\" target=\"_blank\">"
                        + "<font color=\"blue\" face=\"Arial\" size=\"1\">"
                        + "Обращение отработано Хорошо <br /></font></a>"
                        + "<a href=\"mailto:helpdesk@it.mygroup.ru?subject="
                        + "Update_:0%20Inc.#0000695569%20&amp;body=\" "
                        + "target=\"_self\">Обратная связь</a>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testRelativeLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<a href=\"www.ulmart.ru?some_tag\">text</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,29,9,22]}]"),
                    new StringChecker(body));
            }

            post = createPost(sanitizer2, SECPROXY, request);
            String jsonMarkup = "[{\"type\":3,\"position\":[3,37,9,30]}]";
            String result =
                "<a href=\"https://www.ulmart.ru?some_tag\">text</a>";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(jsonMarkup),
                    new StringChecker(result));
            }

            body = "<a href=\"//www.ulmart.ru?some_tag\">text</a>";
            request = BEGIN + NO_RESOLVE + body + END;
            post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(jsonMarkup),
                    new StringChecker(result));
            }

            body = "<a href=\"https:/www.ulmart.ru?some_tag\">text</a>";
            request = BEGIN + NO_RESOLVE + body + END;
            post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(jsonMarkup),
                    new StringChecker(result));
            }
        }
    }

    @Test
    public void testNamedEntitiesEncoding() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "&lt;&nbsp;no\u00a0break\u00a0here&gt;&amp;"
                + "<img alt=\"A&nbsp;B&quot;&amp;&lt;&gt; \">";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "&lt;\u00a0no\u00a0break\u00a0here&gt;&amp;"
                        + "<img alt=\"A\u00a0B&quot;&amp;&lt;&gt; \" />"));
            }
        }
    }

    @Test
    public void testEmbed() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String embed = "<embed";
            String src =
                " id=\"myid\" src=\"http://www.youtube.com/v/Rm3i_OVsraA\"";
            String hiderefSrc =
                " id=\"f990fc29d77ebb9myid\" src=\"https://h.yandex-team.ru/?"
                + "http://www.youtube.com/v/Rm3i_OVsraA\"";
            String type = " type=\"application/x-shockwave-flash\" />";
            String body =
                embed + " src=\"my.swf\"" + type
                + 1 + embed + src + type
                + 2 + embed + " src=\"http://wrongtube.com/v/Rm3ijOVsraA\""
                + type
                + (2 + 1) + embed + src + " type=\"text/html\"/>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        embed + type
                        + 1 + embed + hiderefSrc + type
                        + 2 + embed + type
                        + (2 + 1) + embed + hiderefSrc + SLASH),
                    CharsetUtils.toString(response.getEntity()));
            }
            post = createPost(
                sanitizer2,
                WEB + DISABLE_HIDEREFERER,
                request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        embed + type
                        + 1 + embed + src + type
                        + 2 + embed + type
                        + (2 + 1) + embed + src + SLASH),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTextarea() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<textarea rows=\"0\" cols=\"10\" readonly>"
                + "some text here</textarea>"
                + "<a href=\"http://yandex.ru/link\">text</a>"
                + "<a href=\"http://narod.yandex.ru/?vi_ros=da\">viros</a>"
                + "<a href=\"http://u_rod.ru/?vi_ros=da\">viros</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<textarea cols=\"10\" readonly=\"readonly\">some text"
                        + " here</textarea><a href=\"http://yandex.ru/link\">"
                        + "text</a><a href=\"https://h.yandex-team.ru/?"
                        + "http://narod.yandex.ru/?vi_ros%3Dda\">viros</a>"
                        + "<a href=\"https://h.yandex-team.ru/?"
                        + "http://u_rod.ru/?vi_ros%3Dda\">viros</a>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadInput() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            HttpPost post = new HttpPost(sanitizer2.host() + WEB);
            // CSOFF: MagicNumber
            post.setEntity(
                new ByteArrayEntity(new byte[] {-1, 0, 0x74, 0x78, 0x74}));
            // CSON: MagicNumber
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker("&#xfffd;txt"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEmailsInPlainText() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "Dmitry Potapov <potapov.d@gmail.com> wrote on Tue:";
            // Default config doesn't wrap links
            HttpPost post = createPost(sanitizer2, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "Dmitry Potapov &lt;potapov.d@gmail.com&gt; "
                        + "wrote on Tue:"));
            }
            post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[22,33,28,26]}]"),
                    new StringChecker(
                        "Dmitry Potapov &lt;"
                        + "<a href=\"mailto:potapov.d@gmail.com\">"
                        + "potapov.d@gmail.com</a>&gt; wrote on Tue:"));
            }
        }
    }

    @Test
    public void testTd() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // Should not be extended with <table><tbody><tr> as this will
            // break markup
            // Dimensions are ignored by browsers, so width will be cleared
            String body = "<td width=\"30em\" height=\"100%\">";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<td height=\"100%\" width=\"30\"></td>"));
            }
        }
    }

    @Test
    public void testGlavclubHref() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"http://glavclub.com/mike-portnoy-shattered-fortress"
                + "/?utm_source=email&utm_medium=digest&utm_term=mike-portnoy-"
                + "shattered-fortress&utm_campaign=email|senddate:10072017&utm"
                + "_content=event:mike-portnoy-shattered-fortress|host:moscow|"
                + "eventdate:11072017|\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,278,9,271]}]"),
                    new StringChecker(
                        "<a href=\"http://glavclub.com/mike-portnoy-shattered-"
                        + "fortress/?utm_source=email&amp;utm_medium="
                        + "digest&amp;utm_term=mike-portnoy-shattered-"
                        + "fortress&amp;utm_campaign=email%7Csenddate:"
                        + "10072017&amp;utm_content=event:mike-portnoy-"
                        + "shattered-fortress%7Chost:moscow%7C"
                        + "eventdate:11072017%7C\">link</a>"));
            }
        }
    }

    @Test
    public void testHashlessColor() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<td bgcolor=\"ffffff\"></td>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testTrHeight() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<tr height=\"10\"></tr>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testPre() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<pre width=\"40\">text</pre>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testLocalHref() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<p id=\"topofthisdoc\">top of the world</p>"
                + "<a href=\"#topofthisdoc\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[60,36,66,29]}]"),
                    new StringChecker(
                        "<p id=\"355699bdeeaf3d42topofthisdoc"
                        + "\">top of the world</p><a href=\""
                        + "#355699bdeeaf3d42topofthisdoc\">"
                        + "link</a>"));
            }
        }
    }

    @Test
    public void testParagraphWithTable() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table><tbody><tr><td><p><table><tbody><tr><td>here you are</"
                + "td></tr></tbody></table></p></td></tr></tbody></table>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testNoImpliedLi() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<ul><ul><li></li></ul></ul>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testTableAImg() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table align=\"center\"><tr><td ><a href=\"https://ya.ru\">"
                + "<img src=\"https://ya.ru/pic.jpg\" "
                + "alt=\"\" /></td></tr></table>";
            String request = BEGIN + NO_RESOLVE + body + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[33,20,39,13]},"
                        + "{\"type\":1,\"position\":[59,27,64,21]},"
                        + "{\"type\":3,\"position\":[144,20,150,13]},"
                        + "{\"type\":1,\"position\":[170,27,175,21]}]"),
                    new StringChecker(
                        "<table align=\"center\"><tr><td><a href=\""
                        + "https://ya.ru\"><img src=\"https://ya.ru/pic.jpg"
                        + "\" /></a></td></tr></table><table align="
                        + "\"center\"><tr><td><a href=\"https://ya.ru\""
                        + "><img src=\"https://ya.ru/pic.jpg\" /></a></td>"
                        + "</tr></table>"));
            }
        }
    }

    @Test
    public void testInvalidHtmlEntity() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "text &order_id text";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker("text &amp;order_id text"));
            }
        }
    }

    @Test
    public void testStrongBalancing() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<p><strong><a href=\"https://ya.ru\"><font color=\"fff\">link"
                + "</font></a></p><h2>h2</h2>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[14,20,20,13]}]"),
                    new StringChecker(
                        "<p><strong><a href=\"https://ya.ru\"><font color=\""
                        + "fff\">link</font></a></strong></p><h2>h2</h2>"));
            }
        }
    }

    @Test
    public void testYandexTeamImg() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img src=\"http://user@static.yandex-team.ru/i/smiles/small/"
                + "smile_21.gif?yandex_class=yandex_smile_21#fragment\" />";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"classValue\":\"yandex_smile_21\",\"position\":"
                        + "[5,106,10,100],\"type\":4}]"),
                    new StringChecker(body.replace("http://", "https://")));
            }
        }
    }

    @Test
    public void testImgAltSanitizer() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img src=x alt=\"&#x22;&gt;&lt;script&gt;alert(1)&lt;"
                + "/script&gt;\">";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[59,7,64,1]}]"),
                    new StringChecker(
                        "<img alt=\"&quot;&gt;&lt;script&gt;alert(1)"
                        + "&lt;/script&gt;\" src=\"x\" />"));
            }
        }
    }

    @Test
    public void testMap() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img border=\"0\" usemap=\"#Map\" alt=\"\" src=\"https://"
                + "cache.mail.yandex.net/mail/d79e4922624498d8d7d187eaa00d917b"
                + "/content.boutique.ru/media/newsletter/images/0fd3733adf.gif"
                + "?rev=48894\">\n"
                + "<map name=\"Map\">\n"
                + "<area onclick='javascript:location.href=http://ya.ru' href="
                + "\"http://boutique.us4.list-manage.com/track/click?u="
                + "736c7f8507fdc5841f0f3fd78&amp;id=81494af13a&amp;e="
                + "6fd44d23e2\" title=\"\" alt=\"\" coords=\"-15,-378,120,400"
                + "\" shape=\"rect\">\n"
                + "<area shape=\"poly\" coords=\"210,24,233,0,329,0,307,24\" "
                + "href=\"activity.html\" alt=\"Мероприятия\"><area href=\""
                + "http://boutique.us4.list-manage.com/track/click?u=736c7f850"
                + "7fdc5841f0f3fd78&amp;id=e7018669d0&amp;e=6fd44d23e2\" title"
                + "=\"\" alt=\"\" coords=\"14,438,160,460\" shape=\"rect\">"
                + "<area shape=Polygon coords=\"210 , 24,234,0,329,0,307,24 \""
                + " href=\"activity2.html\" alt=\"Мероприятия2\">\n</map>";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[16,142,21,136]},"
                        + "{\"type\":3,\"position\":[225,117,231,110]},"
                        + "{\"type\":3,\"position\":[429,28,435,21]},"
                        + "{\"type\":3,\"position\":[503,117,509,110]},"
                        + "{\"type\":3,\"position\":[707,29,713,22]}]"),
                    new StringChecker(
                        "<img border=\"0\" src=\"https://cache"
                        + ".mail.yandex.net/mail/d79e4922624498d8d7d187eaa00d9"
                        + "17b/content.boutique.ru/media/newsletter/images/"
                        + "0fd3733adf.gif?rev=48894\" usemap=\"#Map\" />\n<m"
                        + "ap name=\"Map\">\n<area coords=\"-15,-378,120,400\""
                        + " href=\"http://boutique.us4.list-manage.com/track/"
                        + "click?u=736c7f8507fdc5841f0f3fd78&amp;id=81494af13a"
                        + "&amp;e=6fd44d23e2\" shape=\"rect\" />\n<area alt=\""
                        + "Мероприятия\" coords=\"210,24,233,0,329,0,307,24\" "
                        + "href=\"https://activity.html\" shape=\"poly\" />"
                        + "<area coords=\"14,438,160,460\" href=\"http://"
                        + "boutique.us4.list-manage.com/track/click?u="
                        + "736c7f8507fdc5841f0f3fd78&amp;id=e7018669d0&amp;e="
                        + "6fd44d23e2\" shape=\"rect\" />"
                        + "<area alt=\"Мероприятия2\" "
                        + "coords=\"210,24,234,0,329,0,307,24\" "
                        + "href=\"https://activity2.html\" shape=\"polygon\" "
                        + "/>\n</map>"));
            }
        }
    }

    @Test
    public void testNamedAnchors() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body =
                "<A name=\"naverh\">Top</A>"
                + "<A href=\"  #naverh  \">to the top</A>"
                + "<a href=\"#\">to the top</a>"
                + "<a href=\" #top\">to the top</a>"
                + "<div id=\" slozhno&amp;kory&#x61;vo\">mark</div>"
                + "<a href=\"#%20slozhno%26kor%79avo\">link to mark</a>"
                + "<a name=\"якорь\">якорь</a>"
                + "<div id=\"тыкорь\">тыкорь</div>"
                + "<a href=\"#якорь\">на якорь</a>"
                + "<a href=\" #тыкорь \">на тыкорь</a>";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(JsonChecker.ANY_VALUE),
                    new StringChecker(
                        "<a name=\"9fa9143ae5561770naverh\">"
                        + "Top</a><a href=\""
                        + "#9fa9143ae5561770naverh\">to the "
                        + "top</a><a href=\"#\">to the top</a><a href=\"#top\""
                        + ">to the top</a><div id=\""
                        + "838e230f1b3bbf3c slozhno&amp;"
                        + "koryavo\">mark</div><a href=\""
                        + "#838e230f1b3bbf3c%20slozhno&amp;"
                        + "koryavo\">link to mark</a>"
                        + "<a name=\"68f582b9e4e0fe6eякорь\">"
                        + "якорь</a><div id=\"3915dcc2a162e2b9тыкорь\">тыкорь"
                        + "</div><a href=\""
                        + "#68f582b9e4e0fe6e%D1%8F%D0%BA%D0%BE%D1%80%D1%8C\">"
                        + "на якорь</a><a href=\"#3915dcc2a162e2b9"
                        + "%D1%82%D1%8B%D0%BA%D0%BE%D1%80%D1%8C"
                        + "\">на тыкорь</a>"));
            }
        }
    }

    @Test
    public void testDoubleBraces() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body = "<img alt=\"{{name}}\" />{{{{text}}}}";
            String responseBody =
                "<img alt=\"{\u200b{name}}\" />"
                + "{<!-- -->{<!-- -->{<!-- -->{text}}}}";
            HttpPost post = createPost(sanitizer2, DETEMPL, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(responseBody),
                    CharsetUtils.toString(response.getEntity()));
            }
            post = createPost(sanitizer2, "/?s=crm", body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(body),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTextUrlencoded() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body = "text=%3cdiv%3e--%c2%a0%3c%2fdiv%3e";
            HttpPost post = createPost(sanitizer2, "/pr_http", body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker("<div>--\u00a0</div>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInlineImage() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "text<inline-image width=\"200\" height=\"200\">"
                + "ii_jg7yhi440_162e31e1c48f21c2<\\/inline-image>text";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "text<inline-image height=\"200\" width=\"200\">"
                        + "ii_jg7yhi440_162e31e1c48f21c2&lt;\\/inline-image&gt"
                        + ";text</inline-image>"));
            }
        }
    }

    @Test
    public void testUnderscoreInHref() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"http://svux_leit.link.seny.ru/svex_leit/\">"
                + "I hate this sender</a>"
                + "<img src=\"http://mydomain.com/pic_name.jpg\">"
                + "<a href=\"http://i_am_really_broken.cc/\">link<a/>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,47,9,40]},"
                        + "{\"type\":1,\"position\":[78,143,83,137]},"
                        + "{\"type\":3,\"position\":[227,36,233,29]}]"),
                    new StringChecker(
                        "<a href=\"http://svux_leit.link.seny.ru/svex_leit/\""
                        + ">I hate this sender</a>"
                        + "<img src=\"https://resize.yandex.net/mailservice?"
                        + "url=http%3A%2F%2Fmydomain.com%2Fpic_name.jpg&amp;"
                        + "proxy=yes&amp;key=8df14ca0b0485fbd0ddf7814505f8"
                        + "b16\" /><a href=\"http://i_am_really_broken.cc/"
                        + "\">link</a>"));
            }
        }
    }

    @Test
    public void testTableClosure() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div><table><div><a href=\"https://ya.ru\"><img src=\"https:"
                + "//ya.ru/pic.jpg\" /></a></div></table></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, COMPACT, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(BEGIN + body + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBypassSchemes() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String requestTail =
                "<a href=\"kodeks://link/\">kodeks link</a>"
                + "<a href=\"ftp://me.ru/file.jpg\">ftp link</a>"
                + "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhE\" />"
                + "<a href=\"conisio://SDC_PDM/history?projectid=4997&amp;"
                + "documentid=30817&amp;objecttype=1\">conisio link</a>";
            String request =
                "<a href=\"tel:+7(995)1989705\">+7(995)1989705</a>"
                + requestTail;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,25,9,18]},"
                        + "{\"type\":3,\"position\":[50,21,56,14]},"
                        + "{\"type\":3,\"position\":[90,27,96,20]},"
                        + "{\"type\":1,\"position\":[135,48,140,42]},"
                        + "{\"type\":3,\"position\":[189,85,195,78]}]"),
                    new StringChecker(
                        "<a href=\"tel:+7(995)1989705\">+7(995)1989705</a>"
                        + requestTail));
            }
        }
    }

    @Test
    public void testGopherUrl() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"gopher://gopher.floodgap.com/1/world\">gopher</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":3,\"position\":[3,43,9,36]}]"),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testIdn() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"https://ουτοπία.δπθ.gr/link/link2\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,53,9,46]}]"),
                    new StringChecker(
                        "<a href=\"https://xn--kxae4bafwg.xn--pxaix.gr"
                        + "/link/link2\">link</a>"));
            }
        }
    }

    @Test
    public void testUrlParsing() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"https://127.0.0.1\">link</a>"
                + "<a href=\"https://dpotapov@127.0.0.1\">link</a>"
                + "<a href=\"https://[ffff::127.0.0.1]\">link</a>"
                + "<a href=\"https://[a:b:c:d:e:f:127.0.0.1]\">link</a>"
                + "<a href=\"https://[fe80:3438:7667:c77::ce27%18]\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, COMPACT, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(BEGIN + body + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBrokenUrl() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"https://127.0.0.1/?my=dа%09ta%4\r\n0her\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, COMPACT, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<a href=\"https://127.0.0.1/?my=d%D0%B0%09ta%40her"
                        + "\">link</a>"
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEmptyNameAnchors() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<a name=\"realslamshady\"></a><b></b>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, DETEMPL, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<a name=\"realslamshady\"></a>"
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInteractive() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String ending =
                "<div data-interactive-transaction-src=\""
                + "https://pay-test.mail.yandex.ru/?somequery\">good</div>"
                + "<div data-interactive-transaction-src=\"https:/"
                + "/pay.yandex.ru/?somequery\">also good</div>"
                + "<div data-interactive-transaction-src=\"https://"
                + "pay-test2.mail.yandex.ru/?somequery\">bad</div>"
                + "<div data-interactive-without-stub=true>a</div>"
                + "<div data-interactive-cart-host=\"se2_say.host.рф\">b</div>"
                + "<div data-interactive-cart-host=\"http://ya.ru\">c</div>"
                + "<div data-interactive-form-id=\"100500\">d</div>"
                + "<div data-interactive-form-id=\"a00500\">e</div>"
                + "<div data-interactive-form-title=\"true\">f</div>"
                + "<div data-interactive-form-title=\"teue\">g</div>";
            String sanitizedEnding =
                "<div data-interactive-transaction-src=\"https:"
                + "//pay-test.mail.yandex.ru/?somequery\">good</div>"
                + "<div data-interactive-transaction-src=\"https"
                + "://pay.yandex.ru/?somequery\">also good</div>"
                + "<div>bad</div><div data-interactive-without-stub=\""
                + "true\">a</div>"
                + "<div data-interactive-cart-host=\"se2_say.host.рф\">b</div>"
                + "<div>c</div>"
                + "<div data-interactive-form-id=\"100500\">d</div>"
                + "<div>e</div>"
                + "<div data-interactive-form-title=\"true\">f</div>"
                + "<div>g</div>";
            String mainBody =
                "<div class=\"yandex-interactive-element\" "
                + "data-interactive-slide-height=\"600\" "
                + "data-interactive-slide-width=\"400\">element</div>"
                + "<div class=\"yandex-interactive-element-item\" "
                + "data-interactive-name=\"yandex-pay-popup\" "
                + "data-interactive-stub-height=\"300\" "
                + "data-interactive-stub-width=\"200\">item</div>"
                + "<div class=\"yandex-interactive-element "
                + "yandex-interactive-element-item\" "
                + "data-interactive-name=\"name\" "
                + "data-interactive-src=\"https://forms.yandex.ru/u/5d0a530619"
                + "621d11f540e8dd/?iframe=1\">combined</div>"
                + "<div data-interactive-src=\"https://forms.yandex.ru/surveys"
                + "/10014640/?iframe=1\">another one</div>"
                + "<div data-interactive-video-play-visible=\"false\">b</div>"
                + "<div data-interactive-height=\"200\">c</div>"
                + "<div data-interactive-width=\"0\">d</div>"
                + "<div data-interactive-src=\"https://forms.yandex-team.ru/"
                + "surveys/13090/?ticket_id=12345&amp;product=mail\">e</div>"
                + "<div data-interactive-src=\"https://forms.yandex-team.ru/"
                + "surveys/36802/?iframe=1/answer_choices_280714=cerbro&amp;"
                + "answer_url_280715=https://st.yandex-team.ru/TESTLA-1&amp;"
                + "answer_short_text_280717=7.0\">f</div>";
            String request = BEGIN + NO_RESOLVE + mainBody + ending + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div class=\"14b7be7dbed72bb6yandex-"
                        + "interactive-element\">element</div><div class=\""
                        + "f8fe1761d7ef7520yandex-interactive-ele"
                        + "ment-item\" data-interactive-name=\"yandex-pay"
                        + "-popup\">item</div><div class=\"14b7be7dbed72bb6"
                        + "yandex-interactive-element f8fe1761d7ef7520yandex-"
                        + "interactive-element-"
                        + "item\">combined</div><div>another one</div>"
                        + "<div>b</div><div>c</div><div>d</div><div>e</div>"
                        + "<div>f</div>"
                        + sanitizedEnding));
            }
            post = createPost(sanitizer2, SECPROXY, request);
            post.setHeader(
                YandexHeaders.X_ENABLED_BOXES,
                "140619,0,42");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(mainBody + sanitizedEnding));
            }
        }
    }

    @Test
    public void testSenderImage() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img src=\"https://click.sender.yandex.ru/px/95159/96899/L/"
                + "SHdVUkdDUTNOeDQvUERFdFQxYzdNQlFoU1RZL2NBQk5lVTF0UzM1dlVrMTl"
                + "mRXgzYVVoZlJ3eENDbEpiQVVSVFJWZDlYSGwvQjExVwpjMGwrVm1FYldnY0"
                + "1BbjF0UUJBaFBnMW5RQWRxRldKQVJsVTFFUVIwZTBjVUJnNFdKVWt5YWpnc"
                + "0F4VlZBRlFXSVNWZDoyNTQzOjA%3D&amp;proxy=yes&amp;key=c0ad734"
                + "0db808b59146b6febcadb50fd\" />";
            String request = BEGIN + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,315,10,309]}]"),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testPageHeader() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(
                    Files.createTempDirectory("page-header")))
        {
            Path header = deleter.path().resolve("header.html");
            String warning = "<h1>Do not open this link!</h1>";
            Files.write(
                header,
                warning.getBytes(StandardCharsets.UTF_8));
            Path footer = deleter.path().resolve("footer.html");
            String question = "<h1>You opened it, don't you?</h1>";
            Files.write(
                footer,
                question.getBytes(StandardCharsets.UTF_8));
            IniConfig ini =
                new IniConfig(
                    new StringReader(
                        "type = substring\ncontent = enlarge your pen"
                        + HEADER_FILE + header + "\nfooter-file = " + footer));
            PageHeaderConfigBuilder pageHeader =
                new PageHeaderConfigBuilder(ini);
            Sanitizer2ConfigBuilder config = config(true);
            config.sanitizers().traverse(
                (pattern, sanitizingConfig) -> sanitizingConfig.pageHeaders(
                    Collections.singletonMap("header", pageHeader)));
            config = new Sanitizer2ConfigBuilder(config);
            String signal = "page-header-header_ammm";
            try (Sanitizer2 sanitizer2 = new Sanitizer2(config.build());
                CloseableHttpClient client = Configs.createDefaultClient())
            {
                sanitizer2.start();
                HttpAssert.stats(sanitizer2.host());

                String body =
                    "<div>Hello, dear friend, enlarge your pearl!</div>";
                String request = BEGIN + body + END;
                HttpPost post = createPost(sanitizer2, SECPROXY, request);
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker(body));
                }
                HttpAssert.assertStat(
                    signal,
                    Integer.toString(0),
                    sanitizer2.port());

                body = "<div>Hello, dear friend, enlarge your penis!</div>";
                request = BEGIN + body + END;
                post = createPost(sanitizer2, SECPROXY, request);
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker(warning + body + question));
                }
                HttpAssert.assertStat(
                    signal,
                    Integer.toString(1),
                    sanitizer2.port());
            }
        }
    }

    @Test
    public void testPageHeaders() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(
                    Files.createTempDirectory("page-headers")))
        {
            Path header1 = deleter.path().resolve("header1.html");
            String warning1 = "<h1>This is spam</h1>";
            Files.write(
                header1,
                warning1.getBytes(StandardCharsets.UTF_8));
            Path footer1 = deleter.path().resolve("footer1.html");
            String footerText1 = "<h1>This was spam</h1>";
            Files.write(
                footer1,
                footerText1.getBytes(StandardCharsets.UTF_8));

            Path header2 = deleter.path().resolve("header2.html");
            String warning2 = "<h1>This is malic</h1>";
            Files.write(
                header2,
                warning2.getBytes(StandardCharsets.UTF_8));

            Path header3 = deleter.path().resolve("header3.html");
            String warning3 = "<h1>This is fishing</h1>";
            Files.write(
                header3,
                warning3.getBytes(StandardCharsets.UTF_8));

            Map<String, PageHeaderConfigBuilder> pageHeaders =
                new LinkedHashMap<>();
            pageHeaders.put(
                "spam",
                new PageHeaderConfigBuilder(
                    new IniConfig(
                        new StringReader(
                            PATTERN_FILE + "sp.m"
                            + HEADER_FILE + header1
                            + "\nfooter-file = " + footer1))));
            pageHeaders.put(
                "malic",
                new PageHeaderConfigBuilder(
                    new IniConfig(
                        new StringReader(
                            PATTERN_FILE + "m.*c"
                            + HEADER_FILE + header2))));
            pageHeaders.put(
                "fishing",
                new PageHeaderConfigBuilder(
                    new IniConfig(
                        new StringReader(
                            PATTERN_FILE + "f.sh.ng"
                            + HEADER_FILE + header3))));
            Sanitizer2ConfigBuilder config = config(true);
            config.sanitizers().traverse(
                (pattern, sanitizingConfig) ->
                    sanitizingConfig.pageHeaders(pageHeaders));
            config = new Sanitizer2ConfigBuilder(config);
            String spamSignal = "page-header-spam_ammm";
            String malicSignal = "page-header-malic_ammm";
            String totalSignal = "page-header-total_ammm";
            try (Sanitizer2 sanitizer2 = new Sanitizer2(config.build());
                CloseableHttpClient client = Configs.createDefaultClient())
            {
                sanitizer2.start();
                HttpAssert.stats(sanitizer2.host());

                String body =
                    "<div>spAmAlicfIshing</div>";
                String request = BEGIN + body + END;
                HttpPost post = createPost(sanitizer2, SECPROXY, request);
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker(warning1 + body + footerText1));
                }
                HttpAssert.assertStat(
                    spamSignal,
                    Integer.toString(1),
                    sanitizer2.port());
                HttpAssert.assertStat(
                    totalSignal,
                    Integer.toString(1),
                    sanitizer2.port());

                body = "<div>spamaliCfIsh1ng</div>";
                request = BEGIN + body + END;
                post = createPost(sanitizer2, SECPROXY, request);
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker(warning1 + body + footerText1));
                }
                HttpAssert.assertStat(
                    spamSignal,
                    Integer.toString(2),
                    sanitizer2.port());
                HttpAssert.assertStat(
                    malicSignal,
                    Integer.toString(0),
                    sanitizer2.port());

                body = "<div>malic</div>";
                request = BEGIN + body + END;
                post = createPost(sanitizer2, SECPROXY, request);
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker(warning2 + body));
                }
                HttpAssert.assertStat(
                    spamSignal,
                    Integer.toString(2),
                    sanitizer2.port());
                HttpAssert.assertStat(
                    malicSignal,
                    Integer.toString(1),
                    sanitizer2.port());
                HttpAssert.assertStat(
                    totalSignal,
                    Integer.toString(3),
                    sanitizer2.port());
            }
        }
    }

    @Test
    public void testPaypalButton() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"https://www.paypal.com/myaccount/claim-money?em=a%4"
                + "0yandex.ru&txn_id=1VH64446ZZ667973J&utm_source=unp&utm_medi"
                + "um=email&utm_campaign=PPC022272&utm_unptid=05401aaa-8888-11"
                + "e9-8aaa-44bbbbb478e4c&ppid=PPC0555572&cnac=RU&rsta=ru_EE&cu"
                + "st=&unptid=0540bbbb-8888-11e9-8aaa-441ea1478e4c&calc=f2c455"
                + "ddddf4b&unp_tpcid=email-standard-transaction-reminder-unila"
                + "teral&g=null&unilat=null&trid=null&errc=null&emsub=Напомина"
                + "ние: Вы получили 4,30 USD&encrem=null&ennm=null&tems=2018-0"
                + "7-25 11:35:36.666&page=main:consumer:email:unilateral:open:"
                + "::&pgrp=main:consumer:email:unilateral&e=cl&mchn=em&s=ci&ma"
                + "il=sys\">Запросите свои деньги</a>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<a href=\"https://h.yandex-team.ru/?https://www.paypa"
                        + "l.com/myaccount/claim-money?em%3Da%2540yandex.ru%26"
                        + "txn_id%3D1VH64446ZZ667973J%26utm_source%3Dunp%26utm"
                        + "_medium%3Demail%26utm_campaign%3DPPC022272%26utm_un"
                        + "ptid%3D05401aaa-8888-11e9-8aaa-44bbbbb478e4c%26ppid"
                        + "%3DPPC0555572%26cnac%3DRU%26rsta%3Dru_EE%26cust%3D%"
                        + "26unptid%3D0540bbbb-8888-11e9-8aaa-441ea1478e4c%26c"
                        + "alc%3Df2c455ddddf4b%26unp_tpcid%3Demail-standard-tr"
                        + "ansaction-reminder-unilateral%26g%3Dnull%26unilat%3"
                        + "Dnull%26trid%3Dnull%26errc%3Dnull%26emsub%3D%25D0%2"
                        + "59D%25D0%25B0%25D0%25BF%25D0%25BE%25D0%25BC%25D0%25"
                        + "B8%25D0%25BD%25D0%25B0%25D0%25BD%25D0%25B8%25D0%25B"
                        + "5:%2520%25D0%2592%25D1%258B%2520%25D0%25BF%25D0%25B"
                        + "E%25D0%25BB%25D1%2583%25D1%2587%25D0%25B8%25D0%25BB"
                        + "%25D0%25B8%25204,30%25C2%25A0USD%26encrem%3Dnull%26"
                        + "ennm%3Dnull%26tems%3D2018-07-25%252011:35:36.666%26"
                        + "page%3Dmain:consumer:email:unilateral:open:::%26pgr"
                        + "p%3Dmain:consumer:email:unilateral%26e%3Dcl%26mchn%"
                        + "3Dem%26s%3Dci%26mail%3Dsys\">"
                        + "Запросите свои деньги</a>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGalyaLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"https:///galya.ru\">link</a>"
                + "<a href=\"http://////galya.ru \">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, COMPACT, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<a href=\"https://galya.ru\">link</a>"
                        + "<a href=\"http://galya.ru\">link</a>"
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTrimAttrs() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table title=\" \">"
                + "<tr><td bgcolor=\"#ffa \">t</td></tr></table>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, DETEMPL, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<table title=\" \"><tr>"
                        + "<td bgcolor=\"#ffa\">t</td></tr></table>"
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNobrWbr() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<p>verylong<wbr>word</p><nobr>text without breaks</nobr>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p>verylong<wbr />word</p>"
                        + "<nobr>text without breaks</nobr>"));
            }
        }
    }

    @Test
    public void testDataLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"data:text/html,<html><h1>HELLO</h1><script>"
                + "alert(1);</script></html>\" name=\"fake\">text</a>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<a name=\"b06822ad11b2a29ffake\">text</a>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAudio() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<audio src=\"https://jing.yandex-team.ru/files/nglaz/"
                + "alisa_next_track.wav\" controls=\"controls\" / >"
                + "<p>text</p>"
                + "<audio controls>"
                + "<source src=\"https://jing.yandex-team.ru/horse.ogg\" "
                + "type=\"audio/ogg\">Does not compute</audio>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<audio controls=\"controls\" src=\"https://jing.yande"
                        + "x-team.ru/files/nglaz/alisa_next_track.wav\">"
                        + "</audio><p>text</p><audio controls=\"controls\">"
                        + "<source src=\"https://jing.yandex-team.ru/horse.ogg"
                        + "\" type=\"audio/ogg\" />Does not compute</audio>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testVideo() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<video src=\"https://s3.mds.yandex.net/files/nglaz/"
                + "alisa_next_track.mp4\" width=\"320\" height=\"240\" "
                + "controls />"
                + "<video><source src=\"https://sub.s3.mds.yandex.net/h.mpg\" "
                + "type=\"video/mp4\"/>Does not compute</video>"
                + "<video src=\"https://storage.mds.yandex.net/get-imgmturk/"
                + "15952/de91fed2-8fea-4d4d-96b4-7b7483b29356\" width=\"640\" "
                + "height=\"480\" controls=\"controls\"/>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<video controls=\"controls\" height=\"240\" src=\""
                        + "https://s3.mds.yandex.net/files/nglaz/"
                        + "alisa_next_track.mp4\" width=\"320\"></video>"
                        + "<video>"
                        + "<source src=\"https://sub.s3.mds.yandex.net/h.mpg\""
                        + " type=\"video/mp4\" />Does not compute</video>"
                        + "<video controls=\"controls\" height=\"480\" "
                        + "src=\"https://storage.mds.yandex.net/"
                        + "get-imgmturk/15952/de91fed2-8fea-4d4d-96b4-"
                        + "7b7483b29356\" width=\"640\"></video>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMultilineImgSrc() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<img src=\"data:image/png;base64,iVBOR0\r\n PUBG\">";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<img src=\"data:image/png;base64,iVBOR0%20PUBG\" />"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBaseHref() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"/my/path\">link</a>"
                + "<div style=\"background:url(a.png)\">div</div>"
                + "<base href=\"https://ya.ru/some/path/here#there\">"
                + "<a href=\"/my/path\">link2</a>"
                + "<a href=\"my/path\">link3</a>"
                + "<a href=\"#fragment\">link4</a>"
                + "<a href=\"#there\">link5</a>"
                + "<base href=\"https://ya.ru\">"
                + "<div style=\"background:url(a.png)\">div2</div>"
                + "<a href=\"my/path\">link6</a>";
            HttpPost post = createPost(
                sanitizer2,
                WEB + DISABLE_HIDEREFERER,
                request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<a href=\"/my/path\">link</a>"
                        + "<div style=\"background:url('a.png')\">div</div>"
                        + "<a href=\"https://ya.ru/my/path\">link2</a>"
                        + "<a href=\"https://ya.ru/some/path/my/path\">"
                        + "link3</a>"
                        + "<a href=\"https://ya.ru/some/path/here#fragment\">"
                        + "link4</a>"
                        + "<a href=\"https://ya.ru/some/path/here#there\">"
                        + "link5</a>"
                        + "<div style=\"background:url('https://ya.ru/a.png')"
                        + "\">div2</div>"
                        + "<a href=\"https://ya.ru/my/path\">link6</a>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTrimImgSrc() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request = "<img src=\" http://ya.ru/pic.png \">";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<img src=\"https://h.yandex-team.ru/?"
                        + "http://ya.ru/pic.png\" />"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadTagClose() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // </ i> close tag will be ignored my most browsers
            String request = "a<b><i>ti</ i>b</b>a";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker("a<b><i>tib</i></b>a"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUnclosedCloseTag() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // </i… will capture everything until >, and then will be ignored
            String request = "a<b><i>ti</i b</b>a";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker("a<b><i>ti</i>a</b>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDoubleDoubleQuote() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<table width=\"100\"\"d><tr><td>text</td></tr></table>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<table width=\"100\"><tr><td>text</td></tr></table>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCss() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // Test spans and empty spans erasure
            String request =
                BEGIN + NO_RESOLVE
                + "<span style=\" color: red\"><span>text</span></span>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<span style=\"color:red\">text</span>"));
            }

            // Test css urls markup
            request =
                BEGIN + NO_RESOLVE
                + "<div style=\"background: url(image.png?hash=0123"
                + "&user=100500);background-size:300px 400px;"
                + "background-position:25% 30%;background-color:#fffff0\">"
                + "text</div><img src=\"image2.png?hash=1234"
                + "&user=100501\" width=\"33,3333%\" height=\"100,1%\""
                + " valign=\" middle \">"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,89,28,35]},"
                        + "{\"type\":1,\"position\":[110,42,115,36]}]"),
                    new StringChecker(
                        "<div style=\"background:url('"
                        + "image.png?hash=0123&amp;user=100500') #fffff0 "
                        + "25% 30%/300px 400px\">text</div>"
                        + "<img src=\"image2.png?hash=1234&amp;user="
                        + "100501\" valign=\"middle\" width=\"33,3333%\" />"));
            }

            request =
                BEGIN + NO_RESOLVE
                + "<div id=\"test\" style=\"color: \u0027red&quot;;"
                + "css-injection: here;//\u0027\">text</div>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div id=\"15c6e385787f9aeetest\">text</div>"));
            }
        }
    }

    @Test
    public void testStyleBlock() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "#pred {\n  color: red;\n}\n"
                + STYLE_END
                + "<body><p id=\"pred\">red text</p>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p id=\"9af8d130c043d829pred\" style="
                        + "\"color:red\">red text</p>"));
            }

            request =
                STYLE_BEGIN
                + "p {\n  color: red;\n  font-size: 20px !important;\n}\n"
                + STYLE_END
                + "<body><p style=\"color:green;font-size:12px\">red text</p>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p style=\"color:green;font-size:20px !important\">"
                        + "red text</p>"));
            }
        }
    }

    @Test
    public void testFont() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<font face=\"verdana\">verdana</font><br clear=\"left\">"
                + "<font size=5>size 5</font><br style=\"clear:both\">"
                + "<font face=\"serif\"><font size=144>"
                + "<font color=\"#ccddee\">mixed</font></font></font>"
                + "<font size=\"+1\">bigger</font>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<font face=\"verdana\">verdana</font>"
                        + "<br clear=\"left\" />"
                        + "<font size=\"5\">size 5</font>"
                        + "<br style=\"clear:both\" />"
                        + "<font color=\"#ccddee\" face=\"serif\">"
                        + "mixed</font><font size=\"+1\">bigger</font>"));
            }
        }
    }

    @Test
    public void testFontSizeRestrictions() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "p.norm {font: italic 1em/2em Georgia, serif;}\n"
                + "p.micro {font: 3px arial;}\n"
                + STYLE_END
                + "<p class=norm>normal text</p><br>\n"
                + "<p class=micro>micro text</p><br>\n"
                + "<p style=\"font-weight: bold; font-size: 2pc\">"
                + "bold text</p><br>\n"
                + "<p style=\"font-style: italic; font-size: 240pc\">"
                + "italic text</p><br>\n"
                + "<p style=\"font: 120% verdana\">alco text</p><br>\n"
                + "<p style=\"font: 5000% &quot;Times New Roman&quot;, "
                + "sans-serif\">huge text</p><br>\n"
                + "<span style=\"font:bold 22px/87px Arial,Helvetica,"
                + "sans-serif;text-align:center\">Domino's pizza</span>"
                + "<div style=\"  \">no style</div>"
                + "<div style=\"font: 5000%\">huge text again</div>"
                + END;
            HttpPost post = createPost(sanitizer2, SPAM, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p class=\"dca5213dc7e63b85norm\" "
                        + "style=\"font:italic 1em/2em 'georgia' , serif\">"
                        + "normal text</p><br />\n"
                        + "<p class=\"771f6128f40801d3micro\" "
                        + "style=\"font-family:'arial';font-style:normal;font-"
                        + "variant:normal;font-weight:normal;line-height:"
                        + "normal\">micro text</p><br />\n<p style=\"font-size"
                        + ":2pc;font-weight:bold\">bold text</p><br />\n"
                        + "<p style=\"font-style:italic\">"
                        + "italic text</p><br />\n<p style=\"font:120% "
                        + "'verdana'\">alco text</p><br />\n"
                        + "<p style=\"font-family:'times new roman' , "
                        + "sans-serif;font-style:normal;font-variant:normal;"
                        + "font-weight:normal;line-height:normal\">huge text"
                        + "</p><br />\n<span style=\"font:bold 22px/87px "
                        + "'arial' , 'helvetica' , sans-serif;"
                        + "text-align:center\">Domino's pizza</span><div>"
                        + "no style</div><div>huge text again</div>"));
            }
        }
    }

    @Test
    public void testImageResizer() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"background:url(http://reinventedcode.com"
                + "/shimmie/index.php?q=image/99.jpg);\"><br /><img src=\"http"
                + "://reinventedcode.com/shimmie/index.php?q=+image 99.jpg\">"
                + "<img src=\"https://avatars.mdst.yandex.net/dick.pic\">"
                + "<img src=\"https://feedback.fan-test.mail.yandex.net"
                + "/px/227/150/\">"
                + CLOSE_DIV;
            String request = BEGIN + body + END;
            String out =
                "<div style=\"background:url('https://resize"
                + ".yandex.net/mailservice?url=http%3A%2F%2Freinventedcode"
                + ".com%2Fshimmie%2Findex.php%3Fq%3Dimage%2F99.jpg&amp;proxy"
                + "=yes&amp;key=59f5c365d8eaa2435bb76eadc82ba758')"
                + "\"><br /><img src=\"https://resize.yandex.net/mailservice?"
                + "url=http%3A%2F%2Freinventedcode.com%2Fshimmie%2Findex."
                + "php%3Fq%3D%2Bimage%252099.jpg&amp;proxy=yes&amp;key="
                + "ccf675583999213791ff107f88d3fbaa\" />"
                + "<img src=\"https://avatars.mdst.yandex.net/dick.pic\" />"
                + "<img src=\"https://feedback.fan-test.mail.yandex.net"
                + "/px/227/150/\" />"
                + CLOSE_DIV;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,197,28,171]},"
                        + "{\"type\":1,\"position\":[214,182,219,176]},"
                        + "{\"type\":1,\"position\":[404,46,409,40]},"
                        + "{\"type\":1,\"position\":[458,59,463,53]}]"),
                    new StringChecker(out));
            }
            post = createPost(sanitizer2, UNPROXY, out);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,84,28,58]},"
                        + "{\"type\":1,\"position\":[101,67,106,61]},"
                        + "{\"type\":1,\"position\":[176,46,181,40]},"
                        + "{\"type\":1,\"position\":[230,59,235,53]}]"),
                    new StringChecker(
                        "<div style=\"background:url('http://"
                        + "reinventedcode.com/shimmie/index.php?q=image/99"
                        + ".jpg')\"><br /><img src=\"http://reinventedcode"
                        + ".com/shimmie/index.php?q=+"
                        + "image%2099.jpg\" />"
                        + "<img src=\"https://avatars.mdst.yandex.net/dick.pic"
                        + "\" /><img src=\"https://"
                        + "feedback.fan-test.mail.yandex.net/px/227/150/\" />"
                        + CLOSE_DIV));
            }
        }
    }

    @Test
    public void testCompacting() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<b><b><i><b><i>hello</i></b><a><a> </a></a></i><u></u></b>"
                + "<span style=\"font-size: 12px\"><b><i></i></b>"
                + "<span style=\"color: red\">text</span></span></b>"
                + "<span>footer</span>"
                + "<b><font><u>underline</u> </font><b>bold</b></b>"
                + "<font size=\"-1\"><font face=\"verdana\">verdana</font>"
                + " size 12</font>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<b><i>hello </i>"
                        + "<span style=\"color:red;font-size:12px\">text"
                        + "</span></b>footer<b><u>underline</u> "
                        + "bold</b>"
                        + "<font face=\"verdana\" size=\"-1\">verdana</font>"
                        + "<font size=\"-1\"> size 12</font>"));
            }
        }
    }

    @Test
    public void testBodyStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<html><body style=\"color:#000000; font-family:Arial, "
                + "Helvetica, sans-serif, -apple-system; font-size:12px;\">"
                + "<a href=\"http://YA.RU\">ya.ru</a></body></html>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[110,19,116,12]}]"),
                    new StringChecker(
                        "<div style=\"color:#000000;font-family:'arial' ,"
                        + " 'helvetica' , sans-serif , '-apple-system';"
                        + "font-size:12px\"><a href=\"http://ya.ru\">ya.ru</a>"
                        + "</div>"));
            }
        }
    }

    @Test
    public void testBadStyleTag() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<style>..badThing{\n}</style><a><b>bold text</b></b>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker("<b>bold text</b>"));
            }
        }
    }

    @Test
    public void testBadStyleAttr() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<span style=\"color:!important\">text</span>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker("text"));
            }
        }
    }

    @Test
    public void testNoneUrl() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<span style=\"background-image:url(none)\">none text</span>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker("none text"));
            }
        }
    }

    @Test
    public void testCidImage() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img src=\"cid:fc372b45f4289f08630236350b4bb592\"/>"
                + "<div style=\"background-image:url(cid:ddccde)\"/>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":2,\"position\":[5,42,10,36]},"
                        + "{\"type\":2,\"position\":[55,42,84,10]}]"),
                    new StringChecker(
                        "<img src=\"cid:fc372b45f4289f08630236350b4bb592\""
                        + SLASH
                        + "<div style=\"background-image:url('cid:ddccde"
                        + "')\"></div>"));
            }
        }
    }

    @Test
    public void testOracleBorder() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table><colgroup align=char><col char=, charoff=5></colgroup"
                + "><tr><td style=\"BORDER-RIGHT: #b3bcbf 1px solid;\">"
                + "cell</td></tr></table>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<table><colgroup align=\"char\"><col char=\",\" "
                        + "charoff=\"5\" /></colgroup><tr><td style=\""
                        + "border-right-color:#b3bcbf;"
                        + "border-right-style:solid;border-right-width:1px"
                        + "\">cell</td></tr></table>"));
            }
        }
    }

    @Test
    public void testEmptyDiv() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // This shouldn't be converted to <div ... />
            // because browsers will autobalance non-void tag and put </div> at
            // the end of page
            String body = "<div style=\"padding-bottom:510px\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testTextDecoration() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a style=\"text-decoration:none;line-height: 1.25\" "
                + "href=\"http://ya.ru\">ya</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":3,\"position\":[3,19,9,12]}]"),
                    new StringChecker(
                        "<a href=\"http://ya.ru\" style=\"line-height:1.25;"
                        + "text-decoration:none\">ya</a>"));
            }
        }
    }

    @Test
    public void testDisplay() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<div style=\"display:block;float:left\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testBorderRadius() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"border-radius:25%\"></div>"
                + "<div style=\"border-radius:2em 5em\"></div>"
                + "<div style=\"border-radius:50px 50px 0 0\"></div>"
                + "<div style=\"border-radius:2em 1em 4em / 0.5em 3em\"></div>"
                + "<div style=\"border-top-left-radius:40%\"></div>"
                + "<div style=\"border-top-right-radius:50px 20px\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testPerekrestokBorderRadius() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<td bgcolor=\"#ffffff\" style=\"border-radius:15px 15px 0 0;"
                + "overflow:hidden !important\"></td>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testPerekrestokBackground() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"background:#006331 url(http://image.sendsay.ru/"
                + "image/perekrestor/16.02.20/160220_ft.png) top right "
                + "no-repeat;\"></div>"
                + "<div style=\"background:#724837 "
                + "url(http://i.imgur.com/93xngTf.jpg) no-repeat center top;"
                + "background-size:cover;\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,118,28,64]},"
                        + "{\"type\":1,\"position\":[135,91,158,30]}]"),
                    new StringChecker(
                        "<div style=\"background:"
                        + "url('http://image.sendsay.ru/"
                        + "image/perekrestor/16.02.20/160220_ft.png') #006331 "
                        + "top right no-repeat\"></div>"
                        + "<div style=\"background:url('"
                        + "http://i.imgur.com/93xngTf.jpg') #724837 center top"
                        + "/cover no-repeat\"></div>"));
            }
        }
    }

    @Test
    public void testDominosPizzaBackground() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"width: 100%; background: url(http://dominospizza"
                + ".ru/staticont/emails/img/bg.jpg) #0F0F0F no-repeat center "
                + "top; background-size: 100% auto; background-color: #0F0F0F;"
                + "\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,126,28,50]}]"),
                    new StringChecker(
                        "<div style=\"background:url('"
                        + "http://dominospizza.ru/staticont/emails/img/bg.jpg"
                        + "') #0f0f0f center top/100% auto no-repeat;"
                        + "width:100%\">"
                        + CLOSE_DIV));
            }
        }
    }

    @Test
    public void testBackgroundSizeCover() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<div style=\"background-size:cover\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testNestedStyleSelectors() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<style>\n.outer {\n\tdisplay:block;\n}\n"
                + "@media only screen and (max-width:500px) {\n"
                + ".nested {\n\tdisplay:none;\n}\n}\n"
                + "@media only screen { .nested{font-size:20px}}</style>"
                + "<div class=\"outer \"></div><div class=\" nested\">a</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div class=\"86558836272072e8outer\""
                        + " style=\"display:block\"></div><div class=\""
                        + "475b4bc3f4ffaff4nested\" style=\""
                        + "font-size:20px\">a</div>"));
            }
        }
    }

    @Test
    public void testMediaStyleSelectors() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<style media=\"only screen and (max-width:500px)\">"
                + ".nested {\n\tdisplay:none;\n}</style>"
                + "<style media=\"all\">\n.outer {\ndisplay:block;\n}\n</style"
                + "><div class=\"outer\"></div><div class=\"nested\">b</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div class=\"86558836272072e8outer"
                        + "\" style=\"display:block\"></div><div class=\""
                        + "475b4bc3f4ffaff4nested\">b</div>"));
            }
        }
    }

    @Test
    public void testTableLineHeigth() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<table style=\"border-spacing: 0;table-layout:auto;"
                + "margin:0 auto;\"><tbody>"
                + "<tr><td style=\"font-size:  2px; line-height: 2px;\">cell"
                + "</td></tr></tbody></table>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<table style=\"border-spacing:0;"
                        + "margin:0 auto 0 auto;table-layout:auto\"><tbody>"
                        + "<tr><td style=\"font-size:2px;line-height:2px\">"
                        + "cell</td></tr></tbody></table>"));
            }
        }
    }

    @Test
    public void testBorderColor() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"border-spacing:0;table-layout:auto;margin:0 auto"
                + ";border:1px solid;\tborder-color: rgba(230, 230, 230, 0.85)"
                + " red;\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"border-color:rgba( 230 , 230 , "
                        + "230 , 0.85 ) red rgba( 230 , 230 , 230 , 0.85 ) red"
                        + ";border-spacing:0;border-style:solid;"
                        + "border-width:1px;margin:0 auto 0 auto;"
                        + "table-layout:auto\"></div>"));
            }
        }
    }

    @Test
    public void testHr() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<hr noshade border=\"0\" style=\"border:0;height:1px;"
                + "background:#7c8b91;\"/><hr color=\"red\" size=\"1\">"
                + "<hr size=\"100500\">";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<hr border=\"0\" noshade=\"noshade\" style=\""
                        + "background:#7c8b91;border:0;height:1px\" />"
                        + "<hr color=\"red\" size=\"1\" /><hr />"));
            }
        }
    }

    @Test
    public void testMalformedLink() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=http://is7.dev.inision.ru/Task/ChangeStatusSetMark/"
                + "BWwXnB{}<&gt;gQsS5Jeb3|EqKo{I9T2R|nZVESN{8CDtEVM=>link</a>"
                + "<a href=\"http://www.bolena.com.ua/calc{}[]/calc.php?memory"
                + "_textarea=shows(\\'1\\');Foo1.Next{};&height=126&weight=96"
                + " &quot;^`[]\">link2</a><a href=\"http:/"
                + "veryveryveryveryveryveryveryveryveryveryveryveryveryvery"
                + "veryveryveryveryveryveryveryveryveryveryverybad\">link</a>"
                + "<a href=\"https://amateur.ru/sp/confirmation?email="
                + "din%40frt.ru&appId=lkp&isChange=false& target=\"_blank\" "
                + "style=\"color: #66c1cc;\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,120,9,113]},"
                        + "{\"type\":3,\"position\":[135,150,141,143]},"
                        + "{\"type\":3,\"position\":[302,108,308,101]}]"),
                    new StringChecker(
                        "<a href=\"http://is7.dev.inision.ru/Task/"
                        + "ChangeStatusSetMark/BWwXnB%7B%7D%3C%3EgQsS5Jeb3%7CE"
                        + "qKo%7BI9T2R%7CnZVESN%7B8CDtEVM=\">link</a>"
                        + "<a href=\"http://www.bolena.com.ua/calc%7B%7D%5B%5D"
                        + "/calc.php?memory_textarea=shows(%5C'1%5C');Foo1"
                        + ".Next%7B%7D;&amp;height=126&amp;weight=96%20%22%5E"
                        + "%60[]\">link2</a>link"
                        + "<a href=\"https://amateur.ru/sp/confirmation?email="
                        + "din%40frt.ru&amp;appId=lkp&amp;isChange=false&amp;"
                        + "%20target=\" style=\"color:#66c1cc\">link</a>"));
            }
        }
    }

    @Test
    public void testColorGrey() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<span style=\"color:grey\">sasha</span>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testWordDecorations() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"word-break:break-all;word-spacing:30px\">text1"
                + "</div><div style=\"word-break:break-word;word-spacing:200px"
                + ";word-wrap:break-word\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"word-break:break-all;word-spacing:30px\""
                        + ">text1</div><div style=\"word-break:break-word;"
                        + "word-wrap:break-word\"></div>"));
            }
        }
    }

    @Test
    public void testTypeStyleSelectors() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<style type=\\\"text/css\\\">.nested {\n\tdisplay:none;\n}"
                + "</style><style type=\"text/css\">\n.outer {\ndisplay:block;"
                + "\n}\n</style><div class=\"outer\"></div><div "
                + "class=\"nested\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div class=\"86558836272072e8outer\""
                        + " style=\"display:block\"></div><div class=\""
                        + "475b4bc3f4ffaff4nested\"></div>"));
            }
        }
    }

    @Test
    public void testNegativeMargin() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<td style=\"margin-top:-10\"></td>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testCapitalTagsInStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<style>A {\ntext-decoration:none\n}</style>"
                + "<a href=\"https://ya.ru\">link</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(JSON_MARKUP_YA_RU),
                    new StringChecker(
                        "<a href=\"https://ya.ru\" style=\""
                        + "text-decoration:none\">link</a>"));
            }
        }
    }

    @Test
    public void testOutlookStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            HttpPost post = new HttpPost(sanitizer2.host().toString());
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource("outlook.html").toURI())));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "\r\n\r\n<div class=\"2e7c62cffe3b3ed5Section1\">\r\n"
                        + "\r\n<p class=\"228bf8a64b8551e1MsoNormal\" style="
                        + "\"font-family:'times new roman';font-size:12.4pt;"
                        + "margin:0cm 0cm 0.0001pt 0cm\"><font face=\"Arial\""
                        + " size=\"2\"><span style=\"font-family:'arial';"
                        + "font-size:10pt\">Привет Дима!"
                        + "</span></font></p>\r\n\r\n</div>\r\n\r\n\r\n"));
            }
        }
    }

    @Test
    public void testLineHeightValidation() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // assumed line-height: 20, which is not the absolute size, but the
            // multiplier, which exceed multiplier max-value
            String body =
                "<style>tr {font: 12px / 20 serif;}</style><tr><td>c</td></tr>"
                + "<div style=\"line-height: 400\">big</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<tr style=\"font-family:serif;font-size:12px;"
                        + "font-style:normal;font-variant:normal;"
                        + "font-weight:normal\">"
                        + "<td>c</td></tr><div>big</div>"));
            }
        }
    }

    @Test
    public void testExoticLengthUnits() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<tr style=\"margin:5ex 6ch 2rem auto\"></tr>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testFontCombine() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            // Should not be combined, as the resulting color will be red
            String body =
                "<font style=\"color:red\"><font color=\"black\">"
                + "rb</font></font>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testListStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"list-style:circle\"></div>"
                + "<div style=\"list-style:inherit\"></div>"
                + "<div style=\"list-style:disc\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"list-style:circle\""
                        + "></div><div style=\"list-style-image:inherit;"
                        + "list-style-position:inherit;list-style-type:inherit"
                        + "\"></div><div style=\"list-style:disc\">"
                        + CLOSE_DIV));
            }
        }
    }

    @Test
    public void testBorderShorthands() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"border-width: thin thick;border-style:dotted "
                + "dashed solid;border-color: red green blue white\">text"
                + "</div><div style=\"border: 5px red solid\">hext</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"border-color:red green blue white;"
                        + "border-style:dotted dashed solid dashed;"
                        + "border-width:thin thick thin thick\">text</div><div"
                        + " style=\"border:5px solid red\">hext</div>"));
            }
        }
    }

    @Test
    public void testFontKerning() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<div style=\"font-kerning:auto\">text</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testBoxProperties() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"box-sizing:border-box\">text</div>"
                + "<div style=\"box-sizing:content-box\">text</div>"
                + "<div style=\"box-decoration-break:clone\">text</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testWhitelistHostsInjection() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body =
                "<a href=\"http://buglloc.com\" style=\"list-style-image:url"
                + "(&apos;http://a.yandex-team.ru/\\&apos;);position:fixed;"
                + "width:10000px;z-index:110000000;height:10000px;background:"
                + "red;margin-left:-2000px;margin-top:-2000px;\">aa</div>";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,25,9,18]}]"),
                    new StringChecker(
                        "<a href=\"http://buglloc.com\">aa</a>"));
            }
        }
    }

    @Test
    public void testMultiClass() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + ".greentext {color:green}\n"
                + ".redtext {color:red}\n"
                + ".redborder {border-color:red}\n"
                + ".greenborder {border-color:green}\n"
                + ".thickborder {border:5px}\n"
                + ".thinborder {border:1px}\n"
                + ".solidborder {border-style:solid}\n"
                + STYLE_END
                + "<body><p class=\"redtext greenborder solidborder thinborder"
                + " greentext redborder  thickborder \">text</p>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p class="
                        + "\"5a471d3c14f348e7redtext "
                        + "6e16a56d17a17f42greenborder "
                        + "716dad1cc715c31asolidborder "
                        + "d36812acaa2343eethinborder "
                        + "51e0bcf25278af53greentext "
                        + "db7105a2d95d2596redborder "
                        + "8961b94f187fa0a4thickborder\" "
                        + "style=\"border:1px solid green;"
                        + "color:red\">text</p>"));
            }
        }
    }

    @Test
    public void testMultiClassDeclaration() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "* {font-style:italic}\n"
                + "#red {color:red}\n"
                + ".greentext {color:green;font-size:20px;line-height:24px}\n"
                + ".greentext {font-size:12px}\n"
                + STYLE_END
                + "<body><p id=\"red\" class=\"greentext\">green text</p>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p class=\"51e0bcf25278af53greentext"
                        + "\" id=\"519648bd3d6fa731red\" "
                        + "style=\"color:red;font-size:12px;font-style:italic;"
                        + "line-height:24px\">green text</p>"));
            }
        }
    }

    @Test
    public void testConsequentStyles() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                // Define style which will match any paragraph
                + "P {font-style:italic}\n"
                + STYLE_END
                + "<body><p id=\"red\">italiano</p><style>\n"
                // Now add red color to all "red" paragraphs, it will be
                // backpropagated to the paragraph above
                + "p#red {color:red}\n"
                // Set color and font size for all "red" "big" elements
                + "#red.big {color:orange;font-size:20px}\n"
                // ... and even bigger size for such paragraps
                + "p#red.big {font-size:24px}\n"
                // ... and check that id and class order doesn't matter
                + "p.big#red {line-height:36px}\n"
                + "</style><p id=\"red\">redaliano</p>"
                + "<p class=\"big\" id=\"red\">big red paragraph</p>"
                + "<div class=\"big\" id=\"red\">big red redemption</div>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p id=\"519648bd3d6fa731red\" style="
                        + "\"color:red;font-style:italic\">italiano</p>"
                        + "<p id=\"519648bd3d6fa731red\" "
                        + "style=\"color:red;font-style:italic\">redaliano</p>"
                        + "<p class=\"5d5022bc2e352a55big\" id"
                        + "=\"519648bd3d6fa731red\" style=\""
                        + "color:orange;font-size:24px;font-style:italic;"
                        + "line-height:36px\">big red paragraph</p>"
                        + "<div class=\"5d5022bc2e352a55big\" "
                        + "id=\"519648bd3d6fa731red\" style=\""
                        + "color:orange;font-size:20px\">big"
                        + " red redemption</div>"));
            }
        }
    }

    @Test
    public void testNonObfuscation() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body =
                STYLE_BEGIN + ".bdy {background-color: #ffff00;}" + STYLE_END
                + "<body align=\"center\" class=\"bdy\"><A name=\"named\">"
                + "named</a><a href=\"#named\">to the named</A>"
                + "<div id=\"someid\">someid</div></html>";
            String responseBody =
                "<a name=\"named\">named</a>"
                + "<a href=\"#named\">to the named</a>"
                + "<div id=\"someid\">someid</div>";
            HttpPost post = createPost(sanitizer2, DETEMPL, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<html><body class=\"bdy\" align=\"center\" "
                        + "style=\"background-color:#ffff00\">"
                        + responseBody + END),
                    CharsetUtils.toString(response.getEntity()));
            }
            post =
                createPost(sanitizer2, WEB + "&preserve-classes=true", body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(responseBody),
                    CharsetUtils.toString(response.getEntity()));
            }
            post = createPost(sanitizer2, COMPACT, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<html><body class=\"bdy\" "
                        + "style=\"background-color:#ffff00\">"
                        + responseBody + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLinearGradient() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"background-image:-webkit-linear-gradient(bottom"
                + ", blue,red )\">link</div>"
                + "<div style=\"background-image:linear-gradient(to top"
                + ", #00ff00,red )\">link</div>"
                + "<div style=\"background:linear-gradient(-290deg"
                + "  ,#00ff00, red)\">link</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, DETEMPL, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<div style=\"background-image:"
                        + "-webkit-linear-gradient( bottom , blue , red )\">"
                        + "link</div><div style=\"background-image:"
                        + "linear-gradient( to top , #00ff00 , red )\">link"
                        + "</div><div style=\"background:linear-gradient"
                        + "( -290deg , #00ff00 , red )\">link</div>"
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCommentedStyleBlock() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "#myid {\n  background-image:url(image.jpg?h{}a)\n}\n"
                + "<!--p {font-size: 20px}\n "
                + STYLE_END
                + "<body><p id=\"myid\">txt</p>"
                + END;
            HttpPost post = createPost(sanitizer2, "/?s=market", request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p id=\"myid\" styl"
                        + "e=\"background-image:url('image.jpg?h%7b%7da');"
                        + "font-size:20px\">txt</p>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDoubleCopyTable() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                BEGIN
                + "<div style=\"border:solid windowtext 1.0pt\">text</div>"
                + END;
            HttpPost post = createPost(sanitizer2, DETEMPL, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        BEGIN
                        + "<div style=\"border:1pt solid windowtext\">text"
                        + CLOSE_DIV
                        + END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testComplexStyleBlock() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "div #id#id {\n  color: #000001;\n}\n"
                + "div #id {\n  color: #000002;\n}\n"
                + "div > * > #id {\n  color: #000003;font-size:8px;\n}\n"
                + "div > #id {\n  color: #000004;font-size:9px\n}\n"
                + "div > .class {\n  color: #000005;\n}\n"
                + "* * * * * * #id {\n  font-weight:bold;\n}\n"
                + "* > * * > * > * * > #id {\ntext-decoration:overline;\n}"
                + STYLE_END
                + "<body><div><u><u><p id=\"id\">1</p></u></u></div>"
                + "<div><b id=\"id\">2</b></div>"
                + "<div><i><i id=\"id\">3</i></i></div>"
                + "<div><u><u><u><u><u><u id=\"id\">4</u></u></u></u>"
                + "</u></u></div>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div><u><p id=\"60eb3ce4da24457bid\" "
                        + "style=\"color:#000001\">1</p></u></div>"
                        + "<div><b id=\"60eb3ce4da24457bid\" "
                        + "style=\"color:#000001;font-size:9px\">2</b></div>"
                        + "<div><i><i id=\"60eb3ce4da24457bid"
                        + "\" style=\"color:#000001;font-size:8px\">3</i></i>"
                        + "</div><div><u><u id=\"60eb3ce4da24457bid"
                        + "\" style=\"color:#000001;font-weight:bold;"
                        + "text-decoration:overline\">4</u></u></div>"));
            }

            request =
                STYLE_BEGIN
                + ".cls1#id {\n  color: #000001;font-size:10px\n}\n"
                + "p.cls1.cls2#id {\n  color: #000002;\n}\n"
                + "* > #id {\n  color: #000003;\n}\n"
                + ".cls1.cls2.norm#id#id#id {\n color:#000004;\n}\n"
                + STYLE_END
                + "<p id=\"id\">1</p>"
                + "<div><p id=\"id\">2</p></div>"
                + "<p id=\"id\" class=\"cls1 cls2\">3</p>"
                + "<p id=\"pred\" class=\"cls1 cls2\">4</p>"
                + "<b id=\"id\" class=\"cls1 cls2 norm\">5</b>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p id=\"60eb3ce4da24457bid\">1</p>"
                        + "<div><p id=\"60eb3ce4da24457bid\" "
                        + "style=\"color:#000003\">2</p></div>"
                        + "<p class=\"6efc9b015459e6b8cls1 "
                        + "6247649ca9387d21cls2\" id=\""
                        + "60eb3ce4da24457bid\" style="
                        + "\"color:#000002;font-size:10px\">3</p>"
                        + "<p class=\"6efc9b015459e6b8cls1"
                        + " 6247649ca9387d21cls2\" id=\""
                        + "9af8d130c043d829pred\">4</p>"
                        + "<b class=\"6efc9b015459e6b8cls1"
                        + " 6247649ca9387d21cls2 "
                        + "dca5213dc7e63b85norm\" "
                        + "id=\"60eb3ce4da24457bid\" style="
                        + "\"color:#000004;font-size:10px\">5</b>"
                        ));
            }

            request =
                STYLE_BEGIN
                + "#pred#pred {\n  color: #000001\n}\n"
                + STYLE_END
                + "<p id=\"pred\">p</p>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<p id=\"9af8d130c043d829pred\" "
                        + "style=\"color:#000001\">p</p>"));
            }
        }
    }

    @Test
    public void testPadding() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<div style=\"padding:10px 5px 15px\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"padding:10px 5px 15px 5px\"></div>"));
            }
        }
    }

    @Test
    public void testFold() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "div {text-decoration-color: red;font:caption}"
                + STYLE_END
                + "<div data-bem=\"``foo=bar\" "
                + "style=\"text-decoration:overline !important;"
                + "background-color:red;background-size:cover;font-size:10px"
                + ";font-family:'arial'\" class=atas>"
                + "<span style=\"background-position:center;background-size:"
                + "cover\"></span>"
                + "<span style=\"background-size:cover\"></span>"
                + "<span style=\"background-position:-5em 30%\"></span>"
                + END;
            HttpPost post = createPost(sanitizer2, "/?s=market", request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<div class=\"atas\" data-bem=\"``foo=bar \" style=\""
                        + "background-color:red;background-size:cover;"
                        + "font:caption;font-family:'arial';font-size:10px;"
                        + "text-decoration-color:red;"
                        + "text-decoration-line:overline !important\">"
                        + "<span style=\"background-position:center;"
                        + "background-size:cover\"></span>"
                        + "<span style=\"background-size:cover\"></span><"
                        + "span style=\"background-position:-5em 30%\"></span>"
                        + CLOSE_DIV),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDisabledFolding() throws Exception {
        Sanitizer2ConfigBuilder config = config(false);
        config.sanitizers().traverse(
            (pattern, sanitizingConfig) ->
                sanitizingConfig
                    .complexCssMatching(false)
                    .foldProperties(false));

        try (Sanitizer2 sanitizer2 = new Sanitizer2(config.build());
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "div div {color:red}"
                + STYLE_END
                + "<div style=\"text-decoration:underline\"><div>text"
                + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"text-decoration-line:underline\">"
                        + "<div>text</div></div>"));
            }
        }
    }

    @Test
    public void testTagInsideClassStyleBlock() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + ".article-title a {text-decoration: none;color: #000000;}"
                + "a {color: #FFFFFF;text-decoration: underline;}"
                + STYLE_END + "<body>"
                + "<div class=\"article-sector\">"
                + "<span class=\"article-title\">"
                + "<div class=\"section-panel\">"
                + "<span id=\"rpt_ctrl0_rptS_ctl00_rptN_ctl00_aut_name\" "
                + "class=\"info-name color-blue\"></span>"
                + "</div><div class=\"item-text\">"
                + "<span class=\"info-date\">29.05.2019</span>"
                + "<a href=\"http://investcab.ru/ru/sobs/articles/details.aspx"
                + "?id=15714&utm_source=email&utm_medium=analitics"
                + "&utm_campaign=all\" "
                + "id=\"rpt_ctrl0_rptS_ctl00_rptN_ctl00_nlink\">"
                + "<p>Индексам США не удалось удержаться в плюсе</p></a></div>"
                + CLOSE_DIV + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[428,132,434,125]}]"),
                    new StringChecker(
                        "<div class=\"2656f268740105a8article-sector\">"
                        + "<span class=\"1980c758429824baarticle-title\">"
                        + "<div class=\"270c8004a11314bsection-panel\">"
                        + "<span class=\"5a89fc9221ec1binfo-name "
                        + "aacc7c33b72fd41dcolor-blue\" "
                        + "id=\"cf7c79b21c208294rpt_ctrl0_rptS_ctl00_rptN_"
                        + "ctl00_aut_name\"></span></div>"
                        + "<div class=\"3e6c7be7abf1e846item-text\">"
                        + "<span class=\"4c4143c0332b5bc8info-date\">"
                        + "29.05.2019</span><a id=\"93ff68bc025e31derpt_ctrl0_"
                        + "rptS_ctl00_rptN_ctl00_nlink\" "
                        + "href=\"http://investcab.ru/ru/sobs/articles/detai"
                        + "ls.aspx?id=15714&amp;utm_source=email&amp;utm_mediu"
                        + "m=analitics&amp;utm_campaign=all\" "
                        + "style=\"color:#000000;text-decoration:non"
                        + "e\"><p>Индексам США не удалось удержаться в плюсе</"
                        + "p></a></div></span></div>"));
            }
        }
    }

    @Test
    public void testMalformedFontFamily() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"font-family:Arial sans-serif;font-size:12px;"
                + "line-height:20px\"></div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"font-family:'arial sans-serif';"
                        + "font-size:12px;line-height:20px\"></div>"));
            }
        }
    }

    @Test
    public void testMultiBackground() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"background-image:url('https://ya.ru/pic.jpg') ,"
                + " url('https://ya.ru/pic2.jpg') , linear-gradient( to bottom"
                + " , blue , red );background-position:left top , right top , "
                + "right bottom;background-repeat:repeat-y , repeat-x , "
                + "no-repeat;background-size:300px 100px , 800px 800px , "
                + "500px 500px\"></div>";
            HttpPost post = createPost(sanitizer2, COMPACT, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(body),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFixFontFamily() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"font-family:courier new,monospace,"
                + "'times new roman',lucida sans\">monospace text</div>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"font-family:'courier new' , monospace "
                        + ", 'times new roman' , 'lucida sans'\">"
                        + "monospace text</div>"));
            }
        }
    }

    @Test
    public void testErasedImgHeigth() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                " <img class=\"space\" "
                + "src=\"http://dpd.ru/ru/email-tmpl/images/space.gif\" "
                + "alt=\"\" title=\"\" border=\"0\" width=\"100%\" height="
                + "\"0\" style=\"display: block;\" />";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[57,161,62,155]}]"),
                    new StringChecker(
                        " <img class=\"bf498c9b6d7b39dspace\""
                        + " border=\"0\" height=\"0\" "
                        + "src=\"https://resize.yandex.net/"
                        + "mailservice?url=http%3A%2F%2Fdpd.ru%2Fru%2Femail-"
                        + "tmpl%2Fimages%2Fspace.gif&amp;proxy=yes&amp;key="
                        + "9da1a96e97c4d370d6360eacfc28935e\" width=\"100%\" "
                        + "style=\"display:block\" />"));
            }
        }
    }

    @Test
    public void testInput() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<select name=\"my-select\"><option value=\"v1\">V1</option>"
                + "<option value=\"v2\" disabled=\"disabled\">v2</option>"
                + "<option value=\"v-3\" selected=\"selected\">v3</option>"
                + "</select><datalist id=\"my-datalist\">"
                + "<optgroup label=\"my label\"><option value=\"v2\">v2"
                + "</option></datalist><input list=\"my-datalist\">";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<select name=\"my-select\"><option value=\"v1\">V1"
                        + "</option><option disabled=\"disabled\" value=\"v2\""
                        + ">v2</option><option selected=\"selected\" "
                        + "value=\"v-3\">v3</option></select><datalist id="
                        + "\"86a0aa37834d64ffmy-datalist\">"
                        + "<optgroup label=\"my label\">"
                        + "<option value=\"v2\">v2</option></optgroup>"
                        + "</datalist><input list=\""
                        + "86a0aa37834d64ffmy-datalist\" />"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGrid() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"display:grid;grid-template-columns: 220px 230px;"
                + "grid-gap:1000px 100px;grid-area: 2 / 1 / span 2 / span 3\">"
                + CLOSE_DIV
                + "<div style=\"display:grid;grid-template-columns: "
                + "220px 10000px 230px;grid-gap: 110px 120px\">"
                + CLOSE_DIV
                + "<div style=\"display:grid;grid-template-columns:  10000px;"
                + "grid-template-rows:100px\">"
                + CLOSE_DIV
                + "<div style=\"display:grid;grid-template-columns: 100px "
                + "10000px;grid-template-rows:100px 100px 100px\">"
                + CLOSE_DIV
                + "<div style=\"grid-gap: 100px;grid-column: 3\">"
                + CLOSE_DIV;
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<div style=\"display:grid;grid-column:1/span 3;"
                        + "grid-column-gap:100px;grid-row:2/span 2;"
                        + "grid-template-columns:220px 230px\">"
                        + CLOSE_DIV
                        + "<div style=\"display:grid;grid-gap:110px 120px\">"
                        + CLOSE_DIV
                        + "<div style=\"display:grid;grid-template-rows:100px"
                        + "\"></div><div style=\"display:grid;"
                        + "grid-template-rows:100px 100px 100px\"></div>"
                        + "<div style=\"grid-column:3;grid-gap:100px\">"
                        + CLOSE_DIV),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testVisibility() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"color:green;visibility:hidden\"></div>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPageBreak() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"page-break-before:always\"></div>"
                + "<div style=\"page-break-inside:avoid\"></div>"
                + "<div style=\"page-break-after:left\"></div>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLeftRightTopBottom() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"left:auto;right:-47px\"></div>"
                + "<div style=\"bottom:100px;top:-20px\"></div>"
                + "<div style=\"left:80%;right:100%\"></div>"
                + "<div style=\"left:-30px;position:relative\"></div>";
            HttpPost post = createPost(
                sanitizer2,
                WEB,
                request + "<div style=\"left:150%\">a</div>");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request + "<div>a</div>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTextIndent() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"text-indent:-3em\">idented text</div>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTransform() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div style=\"transform:perspective( 0 );transform-origin:"
                + "20% 30px 0;transform-style:preserve-3d\">a</div>"
                + "<div style=\"transform:perspective( 50px )\">b</div>"
                + "<div style=\"transform:skew( 50deg , -20deg )\">c</div>"
                + "<div style=\"transform:skew( 30deg )\">d</div>"
                + "<div style=\"transform:skewY( 30deg )\">e</div>"
                + "<div style=\"transform:skewx( 10deg )\">f</div>"
                + "<div style=\"transform:translate( 20px )\">g</div>"
                + "<div style=\"transform:translateX( -100% )\">h</div>"
                + "<div style=\"transform:matrix( 1.2 , 1.2 , 2.2 , 4.2 ,"
                + " 1 , 0 );transform-origin:left top\">i</div>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request.toLowerCase(Locale.ROOT)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAmp4Email() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<html amp4email><head><meta charset=\"utf-8\"><script "
                + "async src=\"https://cdn.ampproject.org/v0.js\"></script>"
                + "<style>body {color: red}p{font-size:12px}</style><style "
                + "amp4email-boilerplate>body{visibility:hidden}\n"
                + "p{color:blue}</style>"
                + "<style>body {border-color: green}</style></head>"
                + "<body><p>text</p></body></html>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div style=\"border-color:green;color:red\">"
                        + "<p style=\"font-size:12px\">text</p></div>"));
            }
        }
    }

    @Test
    public void testInheritFolding() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<p style=\"font-family:'arial';font-size:inherit\">text</p>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(request),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testComplexNestedStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "@media only screen and (max-width:640px) and screen and "
                + "(-ms-high-contrast:active),only screen and (max-width:640px"
                + ") and (-ms-high-contrast:none){td,th{float:left;width:100%;"
                + "clear:both}.content-cell img,img:not(.p100_img){width:auto;"
                + "height:auto;max-width:269px!important;margin-right:auto;"
                + "display:block!important;margin-left:auto}}"
                + ".content-cell *{-webkit-box-sizing:border-box;"
                + "box-sizing:border-box} "
                + STYLE_END
                + "<table><tbody><tr><td class=\"content-cell\"><p>text</p>"
                + "</td></tr></tbody></table>"
                + END;
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<table><tbody><tr><td class=\""
                        + "20a04a0a3b3f28a7content-cell\">"
                        + "<p style=\"box-sizing:border-box\">text</p>"
                        + "</td></tr></tbody></table>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInheritColor() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<p style=\"color:inherit\">1</p>"
                + "<font color=\"inherit\">2</font>"
                + "<p style=\"color:#0f0f0a\">3</p>"
                + "<font color=\"0f0f0a\">4</font>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p style=\"color:inherit\">1</p>"
                        + "2<p style=\"color:#0f0f0a\">3</p>"
                        + "<font color=\"0f0f0a\">4</font>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBackgroundFolding() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<p style=\"background:#ff0000;background-size:cover\">1</p>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p style=\"background:#ff0000 0% 0%/cover\">1</p>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDetectPhishingHosts() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<a href=\"https://yа.ru\">link</a>";
            String out = "<a href=\"https://xn--y-8sb.ru\">link</a>";
            // Unproxy should not catch anything
            HttpPost post = createPost(sanitizer2, UNPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":3,\"position\":[3,27,9,20]}]"),
                    new StringChecker(out));
            }
            String stats = HttpAssert.stats(client, sanitizer2);
            HttpAssert.assertStat(
                PHISHING_LINKS_SIGNAL,
                Integer.toString(0),
                stats);
            // Secproxy will detect scam
            post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":5,\"position\":[3,27,9,20]}]"),
                    new StringChecker(out));
            }
            stats = HttpAssert.stats(client, sanitizer2);
            HttpAssert.assertStat(
                PHISHING_LINKS_SIGNAL,
                Integer.toString(1),
                stats);
        }
    }

    @Test
    public void testWrapPlainLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"https://ya.ru\">link http://google.com</a>"
                + "<br />some text here www.yandex.ru ya.ru<br />"
                + "<a href=\"https://ya.ru\">b<a href=\"https://bit.ly\">"
                + "c https://google.com</a>d</a>http://t.co";
            // Unproxy should not wrap anything
            HttpPost post = createPost(sanitizer2, UNPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,20,9,13]},"
                        + "{\"type\":3,\"position\":[99,20,105,13]},"
                        + "{\"type\":3,\"position\":[124,21,130,14]}]"),
                    new StringChecker(body));
            }
            // Secproxy will detect urls
            post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,20,9,13]},"
                        + "{\"type\":3,\"position\":[74,28,80,21]},"
                        + "{\"type\":3,\"position\":[135,20,141,13]},"
                        + "{\"type\":3,\"position\":[160,21,166,14]},"
                        + "{\"type\":3,\"position\":[214,18,220,11]}]"),
                    new StringChecker(
                        "<a href=\"https://ya.ru\">link http://google.com</a>"
                        + "<br />some text here "
                        + "<a href=\"https://www.yandex.ru\">www.yandex.ru</a>"
                        + " ya.ru<br /><a href=\"https://ya.ru\">b"
                        + "<a href=\"https://bit.ly\">c https://google.com</a>"
                        + "d</a><a href=\"http://t.co\">http://t.co</a>"));
            }
        }
    }

    @Test
    public void testWrapBrokenPlainLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "https://<unknworntag>ya.ru<br>"
                + "www.<a>ya.ru</a><br>www<b></b>.yndx.ru";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,20,9,13]},"
                        + "{\"type\":3,\"position\":[50,24,56,17]},"
                        + "{\"type\":3,\"position\":[97,26,103,19]}]"),
                    new StringChecker(
                        "<a href=\"https://ya.ru\">https://ya.ru</a><br />"
                        + "<a href=\"https://www.ya.ru\">www.ya.ru</a><br />"
                        + "<a href=\"https://www.yndx.ru\">www.yndx.ru</a>"));
            }
        }
    }

    @Test
    public void testCompactorNpe() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<nothing></nothing>";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(""));
            }
        }
    }

    @Test
    public void testComplexBackground() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"background: #010720;"
                + "background-image:"
                + "linear-gradient(to bottom, rgba(1,7,32,0.2),"
                + "rgba(1,7,32,0.4) 35%, #010720 80%),"
                + "url(https://money.yandex.ru/i/shop/moneyland_newseason_back"
                + ".jpg),"
                + "linear-gradient(.25turn, #ffffff, 5%, #ff00ff),"
                + "linear-gradient(#ffffff 0 10% 20.4%, #ff00ff);"
                + "background-position: center;\">text</div>";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            String result =
                "<div style=\"background-attachment:scroll;"
                + "background-color:#010720;background-image:"
                + "linear-gradient( to bottom , "
                + "rgba( 1 , 7 , 32 , 0.2 ) , "
                + "rgba( 1 , 7 , 32 , 0.4 ) 35% , #010720 80% ) , "
                + "url('https://resize.yandex.net/mailservice?url="
                + "https%3A%2F%2Fmoney.yandex.ru%2Fi%2Fshop%2F"
                +"moneyland_newseason_back.jpg&amp;proxy=yes&amp;"
                + "key=e706bba436341ee1934e1916e55d879c') , "
                + "linear-gradient( 0.25turn , #ffffff , 5% , #ff00ff ) , "
                +"linear-gradient( #ffffff 0 10% 20.4% , #ff00ff );"
                + "background-position:center;background-repeat:repeat"
                + ";background-size:auto\">text</div>";
            String compactResult =
                "<div style=\"background-attachment:scroll;"
                + "background-color:#010720;background-image:"
                + "linear-gradient( to bottom , "
                + "rgba( 1 , 7 , 32 , 0.2 ) , "
                + "rgba( 1 , 7 , 32 , 0.4 ) 35% , #010720 80% ) , "
                + "url('https://money.yandex.ru/i/shop/"
                + "moneyland_newseason_back.jpg') , "
                + "linear-gradient( 0.25turn , #ffffff , 5% , #ff00ff ) , "
                +"linear-gradient( #ffffff 0 10% 20.4% , #ff00ff );"
                + "background-position:center;background-repeat:repeat"
                + ";background-size:auto\">text</div>";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[5,536,191,168]}]"),
                    new StringChecker(result));
            }
            post = createPost(sanitizer2, COMPACT, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(compactResult),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTextPlain() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "before quot\r\n"
                + "> quot 1\r\n"
                + "> quot 2\r\n"
                + "after quot, before block\r\n\r\n\r\n"
                + "after block";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p>before quot<br /></p>"
                        + "<blockquote class=\"wmi-quote\">\u00a0quot 1<br />"
                        + "\u00a0quot 2<br /></blockquote>"
                        + "<p>after quot, before block<br /><br /><br />"
                        + "after block<br /></p>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPgpSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "before quot\r\n"
                + "> quot 1\r\n"
                + "> quot 2\r\n"
                + "after quot, before pgp\r\n"
                + "-----BEGIN PGP SIGNED MESSAGE-----\r\n"
                + "pgp header to be removed\r\n"
                + "\r\n"
                + "pgped message body\r\n"
                + "-----BEGIN PGP SIGNATURE-----\r\n"
                + "signatire header\r\n"
                + "same thing\r\n"
                + "\r\n"
                + "the signature itself\r\n"
                + "-----END PGP SIGNATURE-----\r\n"
                + "after pgp";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p>before quot<br /></p>"
                        + "<blockquote class=\"wmi-quote\">\u00a0quot 1<br />"
                        + "\u00a0quot 2<br /></blockquote>"
                        + "<p>after quot, before pgp<br />"
                        + "pgped message body<br />"
                        + "<blockquote class=\"wmi-pgp\">-----BEGIN PGP SIGNATURE-----"
                        + "<br />signatire header<br />"
                        + "same thing<br />"
                        + "<br />"
                        + "the signature itself<br />"
                        + "-----END PGP SIGNATURE-----<br /></blockquote>"
                        + "after pgp<br /></p>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQuotedPgpSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "wertyertyety\r\nцукенукенкен\r\nцукнукенке\r\n\r\n"
                + "16.10.2013 12:01, Alexandr пишет:\r\n"
                + "> -----BEGIN PGP SIGNED MESSAGE-----\r\n"
                + "> Hash: SHA1 856856785678678\r\n"
                + ">\r\n"
                + "> С уважением, АлександрSign only -----BEGIN PGP SIGNATURE----- Version:\r\n"
                + "> GnuPG v1.4.12 (GNU/Linux)\r\n"
                + "> iQEcBAEBAgAGBQJSXivJAAoJEASLuvLLVaCkFIMH/1li2lQhgeggFYrfO8AuiAav\r\n"
                + "> 2eSVeAnNqgSK/Sg3FQxdW+C8z0r4JzE0+EZhToeCKOlLF0akT0nLUj3+YGbY+9U5\r\n"
                + "> Uzn+NC2/SgXbqz6sQLMT5JeKaLi4KKAENwPm5Pmk8u+ijIk3lKvmjM/sCQqwIryr\r\n"
                + "> qeHhqcqZ8Oa/VRPP9li8/FSNeGOfw/Rvl9c24SQJRInQ6h/M5lQvm4ZayyEyz9pk\r\n"
                + "> +DRI2dNlkmaUPDYwiMzBhLGbWHzJy4X2IKhN+h7qTtjWcUUiLzrKEpwwcMnhz27+\r\n"
                + "> WSe14aLBiukwj4fLs+EiO+hLpS8GRurBL1Ff/jnVSc8kIr2INKbpiEZAP2qkua8= =y5eo\r\n"
                + "> -----END PGP SIGNATURE-----\r\n";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p>wertyertyety<br />цукенукенкен<br />цукнукенке<br /><br />"
                        + "16.10.2013 12:01, Alexandr пишет:<br /></p>"
                        + "<blockquote class=\"wmi-quote\">"
                        + "\u00a0С уважением, АлександрSign only "
                        + "<blockquote class=\"wmi-pgp\">"
                        + "-----BEGIN PGP SIGNATURE----- Version:<br />"
                        + "\u00a0GnuPG v1.4.12 (GNU/Linux)<br />"
                        + "\u00a0iQEcBAEBAgAGBQJSXivJAAoJEASLuvLLVaCkFIMH/1li2lQhgeggFYrfO8AuiAav<br />"
                        + "\u00a02eSVeAnNqgSK/Sg3FQxdW+C8z0r4JzE0+EZhToeCKOlLF0akT0nLUj3+YGbY+9U5<br />"
                        + "\u00a0Uzn+NC2/SgXbqz6sQLMT5JeKaLi4KKAENwPm5Pmk8u+ijIk3lKvmjM/sCQqwIryr<br />"
                        + "\u00a0qeHhqcqZ8Oa/VRPP9li8/FSNeGOfw/Rvl9c24SQJRInQ6h/M5lQvm4ZayyEyz9pk<br />"
                        + "\u00a0+DRI2dNlkmaUPDYwiMzBhLGbWHzJy4X2IKhN+h7qTtjWcUUiLzrKEpwwcMnhz27+<br />"
                        + "\u00a0WSe14aLBiukwj4fLs+EiO+hLpS8GRurBL1Ff/jnVSc8kIr2INKbpiEZAP2qkua8= =y5eo<br />"
                        + "\u00a0-----END PGP SIGNATURE-----<br /></blockquote></blockquote>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBlockQuoteBrokenPgpSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "> -----BEGIN PGP SIGNED MESSAGE-----\r\n"
                + ">\r\n"
                + ">> subquote\r\n"
                + "> -----BEGIN PGP SIGNATURE-----\r\n"
                + "> signa\r\n"
                + "> -----END PGP SIGNATURE-----\r\n";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<blockquote class=\"wmi-quote\">"
                        + "<blockquote class=\"wmi-quote\">\u00a0subquote<br />"
                        + "</blockquote><blockquote class=\"wmi-pgp\">"
                        + "\u00a0-----BEGIN PGP SIGNATURE-----<br />"
                        + "\u00a0signa<br />"
                        + "\u00a0-----END PGP SIGNATURE-----<br />"
                        + "</blockquote></blockquote>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDoublePgpSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "-----BEGIN PGP SIGNED MESSAGE-----\n"
                + '\n'
                + "hello\nworld\n"
                + "!-----BEGIN PGP SIGNATURE----- suffix\n"
                + "signa -----END PGP SIGNATURE-----\n"
                + "text\n"
                + "-----BEGIN PGP SIGNED MESSAGE----- suffix\n"
                + "-----BEGIN PGP SIGNED MESSAGE----- suffix\n"
                + "header\nanother header\n\n"
                + "message\nbody\nhere\n"
                + "-----BEGIN PGP SIGNATURE-----\n"
                + "-----END PGP SIGNATURE-----\n";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p>hello<br />world<br />!"
                        + "<blockquote class=\"wmi-pgp\">"
                        + "-----BEGIN PGP SIGNATURE----- suffix<br />signa "
                        + "-----END PGP SIGNATURE-----<br /></blockquote>"
                        + "text<br />message<br />body<br />here<br />"
                        + "<blockquote class=\"wmi-pgp\">-----BEGIN PGP SIGNATURE-----"
                        + "<br />-----END PGP SIGNATURE-----<br /></blockquote></p>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPlainSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request = "helo\n-- \ncheers\n";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<p>helo<br /></p><span class=\"wmi-sign\">-- <br />"
                        + "cheers<br /></span>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBlockquoteInSignature() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "--\n\tcheers\n"
                + ">>  quotation\n"
                + ">>  here\n";
            HttpPost post = createPost(sanitizer2, WEB + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<span class=\"wmi-sign\">-- <br />"
                        + "\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0cheers"
                        + "<br /></span>"
                        + "<blockquote class=\"wmi-quote\">"
                        + "<blockquote class=\"wmi-quote\">"
                        + "\u00a0\u00a0quotation<br />\u00a0\u00a0here<br />"
                        + "</blockquote></blockquote>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTextPlainUrls() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "https://ya.ru word www.ya.ru\r\n"
                + "line start https://google.com line end\r\n"
                + "> quot www.driver.yandex/query endofquot\r\n";
            HttpPost post =
                createPost(sanitizer2, SECPROXY + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[6,20,12,13]},"
                        + "{\"type\":3,\"position\":[53,24,59,17]},"
                        + "{\"type\":3,\"position\":[111,25,117,18]},"
                        + "{\"type\":3,\"position\":[218,38,224,31]}]"),
                    new StringChecker(
                        "<p><a href=\"https://ya.ru\">https://ya.ru</a> word "
                        + "<a href=\"https://www.ya.ru\">www.ya.ru</a><br />"
                        + "line start <a href=\"https://google.com\">"
                        + "https://google.com</a> line end<br /></p>"
                        + "<blockquote class=\"wmi-quote\">\u00a0quot "
                        + "<a href=\"https://www.driver.yandex/query\">"
                        + "www.driver.yandex/query</a> endofquot<br />"
                        + "</blockquote>"));
            }
        }
    }

    @Test
    public void testTextPlainTrailingUrl() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request = "aaa bbb\r\nhttps://ya.ru";
            HttpPost post =
                createPost(sanitizer2, SECPROXY + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[19,20,25,13]}]"),
                    new StringChecker(
                        "<p>aaa bbb<br />"
                        + "<a href=\"https://ya.ru\">https://ya.ru</a>"
                        + "<br /></p>"));
            }
        }
    }

    @Test
    public void testTextPlainMailiciousLinks() throws Exception {
        try (Sanitizer2 sanitizer2 =
                new Sanitizer2(new ImmutableSanitizer2Config(DEFAULT_CONFIG));
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request = "https://yа.ru";
            String out =
                "<p><a href=\"https://xn--y-8sb.ru\">https://yа.ru"
                + "</a><br /></p>";
            // Unproxy should not catch anything
            HttpPost post =
                createPost(sanitizer2, UNPROXY + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[6,27,12,20]}]"),
                    new StringChecker(out));
            }
            String stats = HttpAssert.stats(client, sanitizer2);
            HttpAssert.assertStat(
                PHISHING_LINKS_SIGNAL,
                Integer.toString(0),
                stats);
            HttpAssert.assertStat(
                "total-total_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "mail-unproxy-total_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "mail-secproxy-total_ammm",
                Integer.toString(0),
                stats);
            // Secproxy will detect scam
            post = createPost(sanitizer2, SECPROXY + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":5,\"position\":[6,27,12,20]}]"),
                    new StringChecker(out));
            }
            // /stat is ignored and won't be accounted in totals
            stats = HttpAssert.stats(client, sanitizer2);
            HttpAssert.assertStat(
                PHISHING_LINKS_SIGNAL,
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "total-total_ammm",
                Integer.toString(2),
                stats);
            HttpAssert.assertStat(
                "mail-unproxy-total_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "mail-secproxy-total_ammm",
                Integer.toString(1),
                stats);
        }
    }

    @Test
    public void testTextPlainVariousLocalesLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "https://Магілёў.бел\n"
                + "http://тұщы.қаз\n"
                + "http://kıloýatt.kz\n"
                + "http://išplėstinė.lt\n"
                + "http://Кәндмајакы.az\n"
                + "http://münasibətdə.az\n"
                + "http://җавап.тат\n"
                + "http://ğәcәp.tat\n"
                + "http://założeńa.pl\n"
                + "[http://italìano.it]\n"
                + "http://zåtterström.se)\n"
                + "http://nøgne.no...";
            String out =
                "<p><a href=\"https://xn--80agyb2i2a2d.xn--90ais\">"
                + "https://Магілёў.бел</a><br />"
                + "<a href=\"http://xn--r1aog02b.xn--80ao21a\">"
                + "http://тұщы.қаз</a><br />"
                + "<a href=\"http://xn--kloatt-dza72b.kz\">"
                + "http://kıloýatt.kz</a><br />"
                + "<a href=\"http://xn--iplstin-v8ae64e.lt\">"
                + "http://išplėstinė.lt</a><br />"
                + "<a href=\"http://xn--80aakxdjf1j0e51d.az\">"
                + "http://Кәндмајакы.az</a><br />"
                + "<a href=\"http://xn--mnasibtd-65a906aca.az\">"
                + "http://münasibətdə.az</a><br />"
                + "<a href=\"http://xn--80aaf7cu0a.xn--80a9ab\">"
                + "http://җавап.тат</a><br />"
                + "<a href=\"http://xn--cp-wna039aba.tat\">"
                + "http://ğәcәp.tat</a><br />"
                + "<a href=\"http://xn--zaoea-l7aq06b.pl\">"
                + "http://założeńa.pl</a><br />"
                + "[<a href=\"http://xn--italano-2ya.it\">"
                + "http://italìano.it</a>]<br />"
                + "<a href=\"http://xn--ztterstrm-52a8q.se\">"
                + "http://zåtterström.se</a>)<br />"
                + "<a href=\"http://xn--ngne-gra.no\">"
                + "http://nøgne.no</a>...<br />"
                + "</p>";
            HttpPost post =
                createPost(sanitizer2, SECPROXY + TEXT_PLAIN, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[6,41,12,34]},"
                        + "{\"type\":3,\"position\":[90,38,96,31]},"
                        + "{\"type\":3,\"position\":[164,34,170,27]},"
                        + "{\"type\":3,\"position\":[232,36,238,29]},"
                        + "{\"type\":3,\"position\":[305,37,311,30]},"
                        + "{\"type\":3,\"position\":[386,39,392,32]},"
                        + "{\"type\":3,\"position\":[463,39,469,32]},"
                        + "{\"type\":3,\"position\":[540,34,546,27]},"
                        + "{\"type\":3,\"position\":[607,34,613,27]},"
                        + "{\"type\":3,\"position\":[677,32,683,25]},"
                        + "{\"type\":3,\"position\":[743,36,749,29]},"
                        + "{\"type\":3,\"position\":[817,29,823,22]}]"),
                    new StringChecker(out));
            }
        }
    }

    @Test
    public void testGolovanPanel() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(config(true).build())) {
            sanitizer2.start();

            String panel = HttpAssert.golovanPanel(sanitizer2.host());
            logger.info("Panel:\n" + panel);
            YandexAssert.check(
                new JsonChecker(
                    Files.readString(
                        resource("sanitizer2-golovan-panel.json"))),
                panel);
        }
    }

    @Test
    public void testCustomPanel() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            sanitizer2.host()
                            + "/custom-golovan-panel?abc=def"
                            + "&tag=itype=sanitizer2;prj=mail-sanitizer;"
                            + "ctype=prod;nanny=ps_sanitizer*"
                            + "&editors=dpotapov&title=Custom+panel&max-cols=4"
                            + "&split-by=nanny&split-values=a,b,c")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String panel = CharsetUtils.toString(response.getEntity());
                logger.info("Custom panel:\n" + panel);
                YandexAssert.check(
                    new JsonChecker(
                        Files.readString(
                            resource("sanitizer2-custom-panel.json"))),
                    panel);
            }
        }
    }

    @Test
    public void testOverrideGolovanPanel() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            sanitizer2.host()
                            + "/generate-golovan-panel?abc=def"
                            + "&categories-order=timings")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String panel = CharsetUtils.toString(response.getEntity());
                logger.info("Overriden golovan panel:\n" + panel);
                YandexAssert.check(
                    new JsonChecker(
                        Files.readString(
                            resource("sanitizer2-overriden-panel.json"))),
                    panel);
            }
        }
    }

    @Test
    public void testBorderWidth() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<div style=\"border-color:#2da1ea;border-radius:32px;"
                + "border-style:solid;border-width:10px 90px 10px 95px\">"
                + "</div>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testCenter() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<center><b>hello</b>, world</center>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(body));
            }
        }
    }

    @Test
    public void testClosedAnchor() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<a href=\"https://yandex.ru\"/>yandex</a><b/>b</b>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<a href=\"https://yandex.ru\">yandex</a><b>b</b>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadTitle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<html><head><title><%= @title></%=>\n</title>\n</head>"
                + "<body>Hello, <%= @p></%=> world</body></html>";
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "Hello, &lt;%= @p&gt; world"));
            }
        }
    }

    @Test
    public void testMailtoNbsp() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "test@example.com|\u00a0";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":3,\"position\":[3,30,9,23]}]"),
                    new StringChecker(
                        "<a href=\"mailto:test@example.com\">test@example.com"
                        + "</a>|\u00a0"));
            }
        }
    }

    @Test
    public void testCssComplexityBurst() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            HttpPost post = new HttpPost(sanitizer2.host().toString());
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource("slow.html").toURI())));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }

    @Test
    public void testCssAndInsideAny() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "div#id div#id2 {\n  color: #000001;\n}\n"
                + STYLE_END
                + "<div>"
                + "<div><div id=\"id2\">a</div></div>"
                + "<div id=\"id\"><div id=\"id2\">b</div></div>"
                + "</div>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div>"
                        + "<div><div id=\"c6f0eeab3b510fc3id2\">a</div></div>"
                        + "<div id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\" "
                        + "style=\"color:#000001\">b</div></div>"
                        + "</div>"));
            }

            request =
                STYLE_BEGIN
                + "div.cls1#id div#id2 {\n  color: #000001;\n}\n"
                + STYLE_END
                + "<div>"
                + "<div><div id=\"id2\">a</div></div>"
                + "<div id=\"id\"><div id=\"id2\">b</div></div>"
                + "<div class=\"cls1\" id=\"id\"><div id=\"id2\">c</div></div>"
                + "</div>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div>"
                        + "<div><div id=\"c6f0eeab3b510fc3id2\">a</div></div>"
                        + "<div id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\">b</div></div>"
                        + "<div class=\"6efc9b015459e6b8cls1\" "
                        + "id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\" "
                        + "style=\"color:#000001\">c</div></div>"
                        + "</div>"));
            }

            request =
                STYLE_BEGIN
                + "div.cls1.cls2#id div#id2 {\n  color: #000001;\n}\n"
                + "div.cls1.cls2#id div#id {\n  font-size:12px;\n}\n"
                + STYLE_END
                + "<div>"
                + "<div><div id=\"id2\">a</div></div>"
                + "<div id=\"id\"><div id=\"id2\">b</div></div>"
                + "<div class=\"cls1\" id=\"id\"><div id=\"id2\">c</div></div>"
                + "<div class=\"cls1 cls2\"><div id=\"id\">d</div></div>"
                + "<div class=\"cls1 cls2\" id=\"id\"><div id=\"id2\">e</div>"
                + "</div>"
                + "</div>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div>"
                        + "<div><div id=\"c6f0eeab3b510fc3id2\">a</div></div>"
                        + "<div id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\">b</div></div>"
                        + "<div class=\"6efc9b015459e6b8cls1\" "
                        + "id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\">c</div></div>"
                        + "<div class=\"6efc9b015459e6b8cls1 "
                        + "6247649ca9387d21cls2\">"
                        + "<div id=\"60eb3ce4da24457bid\">d</div></div>"
                        + "<div class=\"6efc9b015459e6b8cls1 "
                        + "6247649ca9387d21cls2\" id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\" "
                        + "style=\"color:#000001\">e</div></div>"
                        + "</div>"));
            }

            request =
                STYLE_BEGIN
                + "div div.cls1#id div#id {\n  color: #000001;\n}\n"
                + STYLE_END
                + "<div>"
                + "<div><div id=\"id2\">a</div></div>"
                + "<div id=\"id\"><div id=\"id2\">b</div></div>"
                + "<div class=\"cls1\" id=\"id\"><div id=\"id\">c</div></div>"
                + "</div>"
                + END;
            post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div>"
                        + "<div><div id=\"c6f0eeab3b510fc3id2\">a</div></div>"
                        + "<div id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\">b</div></div>"
                        + "<div class=\"6efc9b015459e6b8cls1\" "
                        + "id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"60eb3ce4da24457bid\" "
                        + "style=\"color:#000001\">c</div></div>"
                        + "</div>"));
            }
        }
    }

    @Test
    public void testComplexConsequentStyles() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "div#id2 {\n  color: #0000ff;\n}\n"
                + STYLE_END
                + "<div>"
                + "<div><div id=\"id2\">a</div></div>"
                + "<style>div div div {font-size:8px}</style>"
                + "<div id=\"id\"><div id=\"id2\">b</div></div>"
                + "</div>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div>"
                        + "<div><div id=\"c6f0eeab3b510fc3id2\" "
                        + "style=\"color:#0000ff;font-size:8px\">a</div></div>"
                        + "<div id=\"60eb3ce4da24457bid\">"
                        + "<div id=\"c6f0eeab3b510fc3id2\" "
                        + "style=\"color:#0000ff;font-size:8px\">b</div></div>"
                        + "</div>"));
            }
        }
    }

    @Test
    public void testComplexStyleWithImmediateTagClose() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                "<div><div>"
                + "<style>\n"
                + "* {line-height: 14px}\n"
                + "a {font-size: 8px}\n"
                + "a b {color: #ff0000}\n"
                + "</style>"
                + "</div>"
                + "<A HREF=\"https://ya.ru\">hello, <b>world</A>"
                + "</div>";
            HttpPost post = createPost(sanitizer2, WEB, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        "<div style=\"line-height:14px\">"
                        + "<div style=\"line-height:14px\"></div>"
                        + "<a href=\"https://h.yandex-team.ru/?https://ya.ru\""
                        + " style=\"font-size:8px;line-height:14px\">"
                        + "hello, <b style=\"color:#ff0000;line-height:14px\">"
                        + "world</b></a>"
                        + "</div>"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLfInClass() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request = "<div class=\"a_1\tb-2\nc\r\nd e\">";
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker((Object) null),
                    new StringChecker(
                        "<div class=\"5c1584c8da33e678a_1 11348e90d6df99aab-2 "
                        + "297949818c329f26c 616c5b1d07c5d4f7d "
                        + "fb9be559186a4ff4e\"></div>"));
            }
        }
    }

    @Test
    public void testBadStyle() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String request =
                STYLE_BEGIN
                + "#pred {\n  color:;\n}\na {\n  color:#ff00dd;\n}\n"
                + STYLE_END
                + "<a href=\"https://yandex.ru\">test</a>"
                + END;
            HttpPost post = createPost(sanitizer2, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker("[{\"type\":3,\"position\":[3,24,9,17]}]"),
                    new StringChecker(
                        "<a href=\"https://yandex.ru\" "
                        + "style=\"color:#ff00dd\">test</a>"));
            }
        }
    }

    @Test
    public void testDropPathAndQuery() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = BEGIN
                + "<a class=\"link\" href=\"http://st.pes.spb.ru/clicks.php"
                    + "?q=a5557f4&amp;e=b1e2ef&amp;u=5652\" id=\"lb\"></a>"
                + "<a href='mailto:aleksandr.ryb@vseinstrumenti.ru'></a>"
                + "<a href='callto:8%20800%20775%2075%2000'></a>"
                + "<a href = '#fd_sect_2'></a>"
                + "<a href='fd_sect_2'></a>"
                + "<a href='mailto://support@joom.com?mailId=16452590089'></a>"
                + "<a href='mailto:a@b.com?subject=User'></a>"
                + "<a href='../3_8/http://unsub.ate.su/for/rus/9/?_sc=1s'></a>"
                + "<a href='/compose?To=diler8@ckbel.ru'></a>"
                + "<a href='file://e.mail.ru/com/%3f=mailto%25315@ela.ru'></a>"
                + "<a href='//octavius.mail.ru/com/?mailto=mailto%3ac@9d'></a>"
                + "<img src='//www.cian.ru/sale/suburban/257214767'>"
                + "<a href='www.nalog.ru/'></a>"
                + "<a href='/youtrack/issue/TULAS21-265'></a>"
                + END;
            HttpPost post = createPost(sanitizer2, DETEMPL, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(BEGIN +
                    "<a class=\"link\" id=\"lb\" "
                        + "href=\"http://st.pes.spb.ru/\"></a>"
                    + "<a href=\"mailto:aleksandr.ryb@vseinstrumenti.ru\"></a>"
                    + "<a href=\"callto:8%20800%20775%2075%2000\"></a>"
                    + "<a href=\"#fd_sect_2\"></a>"
                    + "<a href=\"fd_sect_2\"></a>"
                    + "<a href=\"mailto://support@joom.com?\"></a>"
                    + "<a href=\"mailto:a@b.com?\"></a>"
                    + "<a href=\"../\"></a>"
                    + "<a href=\"/\"></a>"
                    + "<a href=\"file://e.mail.ru/\"></a>"
                    + "<a href=\"//octavius.mail.ru/\"></a>"
                    + "<img src=\"//www.cian.ru/\" />"
                    + "<a href=\"www.nalog.ru/\"></a>"
                    + "<a href=\"/\"></a>"
                    +END,
                    HttpAssert.body(response));
            }
        }
    }

    @Test
    public void testValuelessAttrAfterSrc() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<img src=https://ya.ru/pic.png alt width=36 "
                + "longdesc=http://ya.ru/?key=value>";
            String result =
                "<img alt=\"alt\" longdesc=\"http://ya.ru/?key=value\" "
                + "src=\"https://resize.yandex.net/mailservice?url="
                + "https%3A%2F%2Fya.ru%2Fpic.png&amp;proxy=yes&amp;key="
                + "df539310da5a8d2af4bda46a665d3061\" width=\"36\" />";
            HttpPost post = createPost(sanitizer2, SECPROXY, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[15,34,25,23]},"
                        + "{\"type\":1,\"position\":[50,132,55,126]}]"),
                    new StringChecker(result));
            }
        }
    }

    @Test
    public void testInlineBackgroundImagePng() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            HttpPost post =
                createPost(
                    sanitizer2,
                    SECPROXY,
                    loadResourceAsString("inline-background-image.html"));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":1,\"position\":[3,3063,32,3006]},"
                        + "{\"type\":1,\"position\":[3076,1383,3105,1326]},"
                        + "{\"type\":1,\"position\":[4469,195,4498,138]}]"),
                    new StringChecker(
                        loadResourceAsString("inline-background-image.html")));
            }
        }
    }

    @Test
    public void testOpacity() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body = "<div style=\"opacity:0.03\"></div>";
            HttpPost post = createPost(sanitizer2, WEB, body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(body),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCrmLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();
            String body =
                "<a href=\"https://yandex.ru/hello.html\">a</a>"
                + "<a href=\"../hello.html\">a</a>";
            HttpPost post = createPost(sanitizer2, "/?s=crm", body);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(body),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTgAndE1cLinks() throws Exception {
        try (Sanitizer2 sanitizer2 = new Sanitizer2(DEFAULT_CONFIG);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            sanitizer2.start();

            String body =
                "<a href=\"tg://resolve?domain=hirthwork\">Me</a>\n"
                + "<a href=\"e1c://srv/abc/def#cib/p/%D0%94?ref=rer\">e1c</a>";
            String request = BEGIN + NO_RESOLVE + body + END;
            HttpPost post = createPost(sanitizer2, SECPROXY, request);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertMultipart(
                    response.getEntity(),
                    new JsonChecker(
                        "[{\"type\":3,\"position\":[3,36,9,29]},"
                        + "{\"type\":3,\"position\":[50,45,56,38]}]"),
                    new StringChecker(body));
            }
        }
    }
}

