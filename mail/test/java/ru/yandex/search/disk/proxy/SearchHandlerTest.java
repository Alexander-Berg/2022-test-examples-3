package ru.yandex.search.disk.proxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SearchHandlerTest extends TestBase {
    private static final String URI = "/?kps=0&key=/disk/*&visible=1&only=id&";
    private static final String CLUSTERIZE =
        "/clusterize?get=id&uid=0&interval=10";
    private static final String DOC = "doc";
    private static final String BODY = "body";
    private static final String FOLDER = "folder";
    private static final String NAME = "name";
    private static final String TAGS = "tags";
    private static final String CV = "cv";
    private static final String GEO = "geo";
    private static final String DISK = "disk";
    private static final String PHOTOUNLIM = "photounlim";
    private static final String PDF = "pdf";

    private static final String SIZE = "\"size\":";
    private static final String MAX = ",\"max\":";
    private static final String MIN = ",\"min\":";
    private static final String MERGED = ",\"merged_docs\":";

    private static final String GEOCODER_URI =
        "/yandsearch?lang=ru_RU&ms=pb&results=1&origin=disk-search&text=%22";

    private static final long WAIT_INTERVAL = 1000L;
    // CSOFF: MagicNumber
    private static final int CLUSTERIZE_TIMESTAMP = Integer.MAX_VALUE - 100;
    private static final int CLUSTERIZE_TIMESTAMP2 = CLUSTERIZE_TIMESTAMP - 20;
    // CSON: MagicNumber

    private static final String PRODUCER_URI =
        "/_status?service=disk_queue&prefix=0&all&json-type=dollar";
    private static final String PRODUCER_RESPONSE = "[{$localhost\0:100500}]";
    private static final String SUGGEST_PREFIX =
        "{\"retry-suggest-types\":[],\"suggest\":[";
    private static final long BACKEND_POS = 100500L;
    private static final User USER =
        new User("disk_queue", new LongPrefix(0L));

    public static String serp(final String request, final String... docs) {
        return serp(DiskRequestParams.DEFAULT_LENGTH, request, docs);
    }

    public static String serp(
        final int numdoc,
        final String request,
        final String... docs)
    {
        StringBuilder sb = new StringBuilder("{\"request\":\"");
        sb.append(request);
        sb.append(
            "\",\"sortBy\":{\"how\":\"mtime\",\"order\":"
            + "\"descending\",\"priority\":\"no\"},\"groupings\":[{\"attr\":\""
            + "\",\"categ\":\"\",\"docs\":\"1\",\"groups-on-page\":\""
            + numdoc
            + "\",\"mode\":\"flat\"}],\"response\":{\"found\":{\"all\":\"");
        sb.append(docs.length);
        sb.append("\",\"phrase\":\"");
        sb.append(docs.length);
        sb.append("\",\"strict\":\"");
        sb.append(docs.length);
        sb.append(
            "\"},\"results\":[{\"attr\":\"\",\"docs\":\"1\",\"found\":{"
            + "\"all\":\"");
        sb.append(docs.length);
        sb.append("\",\"phrase\":\"0\",\"strict\":\"0\"},\"groups\":[");
        for (int i = 0; i < docs.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(docs[i]);
        }
        sb.append("]}]}}");
        return new String(sb);
    }

    public static String doc(final int id) {
        return "\"id\":\"" + id + '"';
    }

    public static String doc(final String scope, final int id) {
        return "{\"doccount\":\"1\",\"relevance\":\"" + id
            + "\",\"documents\":[{\"docId\":\"" + id
            + "\",\"url\":\"\",\"relevance\":\"" + id
            + "\",\"properties\":{\"scope\":\"" + scope
            + "\",\"id\":\"" + id + "\"}}]}";
    }

    public static String doc(
        final int id,
        final String folder)
    {
        return doc(id, folder, null);
    }

    public static String doc(
        final int id,
        final String folder,
        final String name)
    {
        return doc(id, folder, name, null);
    }

    // CSOFF: ParameterNumber
    public static String doc(
        final int id,
        final String folder,
        final String name,
        final String cvTags)
    {
        return doc(id, folder, name, cvTags, DISK);
    }

    public static String doc(
        final int id,
        final String folder,
        final String name,
        final String cvTags,
        final String auxFolder)
    {
        StringBuilder sb =
            new StringBuilder("\"version\":0,\"aux_folder\":\"");
        sb.append(auxFolder);
        sb.append("\",\"visible\":1,\"id\":\"");
        sb.append(id);
        sb.append("\",\"resource_id\":\"0:");
        sb.append(id);
        sb.append("\",\"mtime\":");
        sb.append(Integer.MAX_VALUE - id);
        sb.append(",\"type\":\"");
        if (name == null) {
            sb.append("dir");
        } else {
            sb.append("file");
        }
        sb.append("\",\"folder\":\"");
        sb.append(folder);
        sb.append("\",\"name\":\"");
        String fileName;
        if (name == null) {
            fileName = folder.substring(folder.lastIndexOf('/') + 1);
        } else {
            fileName = name;
        }
        sb.append(fileName);
        int period = fileName.lastIndexOf('.');
        if (period != -1) {
            sb.append("\",\"ext\":\"");
            sb.append(fileName.substring(period + 1));
        }
        if (name != null) {
            if (cvTags == null) {
                sb.append("\",\"body_text\":\"");
                sb.append(name);
            } else {
                sb.append("\",\"cv_tags\":\"");
                sb.append(cvTags);
            }
        }
        sb.append("\",\"key\":\"/");
        sb.append(auxFolder);
        sb.append('/');
        sb.append(folder);
        if (name != null) {
            sb.append('/');
            sb.append(name);
            if (cvTags != null) {
                sb.append("\",\"mimetype\":\"image/jpeg");
            }
        }
        sb.append('"');
        return new String(sb);
    }
    // CSON: ParameterNumber

    // CSOFF: MagicNumber
    private static void prepareIndex(final ProxyCluster cluster)
        throws IOException
    {
        String oHenry = "books/english/O. Henry";
        String vonnegut = "books/english/Vonnegut";
        String dostoevsky = "books/russian/Достоевский";
        String photos = "Фотокамера";
        String documents = "documents";
        String docdocuments = "doc/documents";
        String docbakdocuments = "doc.bak/documents";
        String annex = "annex.doc";
        String newDocuments = "doc new/new documents";
        cluster.backend().add(
            doc(0, "books"),
            doc(1, "books/english"),
            doc(2, oHenry),
            doc(3, oHenry, "Cabbages and Kings.pdf"),
            doc(4, oHenry, "The Last Leaf.fb2"),
            doc(5, oHenry, "Hearts and crosses.fb2"),
            doc(6, vonnegut),
            doc(7, vonnegut, "Mother Night.txt"),
            doc(8, vonnegut, "Slaughterhouse-Five.pdf"),
            doc(9, "books/russian"),
            doc(10, dostoevsky),
            doc(11, dostoevsky, "Бесы.txt"),
            doc(12, dostoevsky, "Мальчик у Христа на ёлке.pdf"),
            doc(13, photos),
            doc(14, photos, "tishka.jpg", "54 75\n76 70\n47 52\n")
            + ",\"fotki_tags\":\"мартиша\nдом\"",
            doc(15, photos, "marriage.jpg", "44 94\n104 85\n140 65\n")
            + ",\"fotki_tags\":\"мышата\n2015\",\"i2t_keyword\":\"00\"",
            doc(16, photos, "pillow.jpg", "76 119\n"),
            doc(17, photos, "sviborg.jpg", "76 132\n47 64\n"),
            doc(18, photos, "tail.jpg", "44 110\n104 78\n140 46\n"),
            doc(19, photos, "bridge.jpg", "44 75\n140 57\n104 35\n"),
            doc(
                20,
                photos,
                "stas.jpg",
                "98 80\n53 71\n99 45\n\",\"latitude\":61.6,"
                + "\"longitude\":\"86.1"),
            doc(21, documents),
            doc(22, documents, annex),
            doc(23, documents, "document-format.pdf"),
            doc(24, DOC),
            doc(25, docdocuments),
            doc(26, docdocuments, annex),
            doc(27, docdocuments, "cv.doc"),
            doc(28, "doc.bak"),
            doc(29, docbakdocuments),
            doc(30, docbakdocuments, "annex2.doc"),
            doc(31, docbakdocuments, "annex3.doc"),
            doc(32, "doc new"),
            doc(33, newDocuments),
            doc(34, newDocuments, "vision.ppt"),
            doc(35, photos, "beer.jpg", "54 75", PHOTOUNLIM));
    }

    private static void prepareClusterizeIndex(final ProxyCluster cluster)
        throws IOException
    {
        prepareIndex(cluster);
        String photos = "photos";
        String mimetype = ",\"mimetype\":\"image/jpeg\",\"etime\":";
        cluster.backend().add(
            doc(100, photos, "10.jpg") + mimetype + CLUSTERIZE_TIMESTAMP,
            doc(101, photos, "11.jpg") + mimetype + (CLUSTERIZE_TIMESTAMP - 1),
            doc(102, photos, "12.jpg") + mimetype + (CLUSTERIZE_TIMESTAMP - 2),
            doc(200, photos, "20.jpg") + mimetype + CLUSTERIZE_TIMESTAMP2,
            doc(201, photos, "21.jpg") + mimetype
            + (CLUSTERIZE_TIMESTAMP2 - 1),
            doc(202, photos, "22.jpg") + mimetype
            + (CLUSTERIZE_TIMESTAMP2 - 2),
            doc(203, photos, "23.jpg") + mimetype
            + (CLUSTERIZE_TIMESTAMP2 - 15));
    }

    private static String historySuggest(
        final String suggestText,
        final String text)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"history\",\"suggest-text\":\"");
        sb.append(suggestText);
        sb.append("\",\"text\":\"");
        sb.append(text);
        sb.append('"');
        sb.append('}');
        return new String(sb);
    }

    @Test
    public void test() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);
            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);
            prepareIndex(cluster);

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=pdf")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            PDF,
                            doc(NAME, 3),
                            doc(NAME, 8),
                            doc(NAME, 12),
                            doc(NAME, 23))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + "text=txt&asc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "txt",
                            doc(NAME, 11),
                            doc(NAME, 7))
                            .replace("descending", "ascending")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=g")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "g",
                            doc(FOLDER, 1),
                            doc(NAME, 3),
                            doc(NAME, 7),
                            doc(NAME, 8),
                            doc(NAME, 14),
                            doc(NAME, 15),
                            doc(NAME, 16),
                            doc(NAME, 17),
                            doc(NAME, 18),
                            doc(NAME, 19),
                            doc(NAME, 20))),
                    CharsetUtils.toString(response.getEntity()));
            }
            String book = "book";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=book")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(book, doc(FOLDER, 0))),
                    CharsetUtils.toString(response.getEntity()));
            }
            String se = "s/e";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=s/e")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(se, doc(FOLDER, 1))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=herota")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("herota")),
                    CharsetUtils.toString(response.getEntity()));
            }
            Thread.sleep(WAIT_INTERVAL);
            String uri =
                "/suggest?uid=0&length=6&suggest-types=history&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "s+e")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + historySuggest(
                            "<span class=\\\"disk-search-highlight\\\">s"
                            + "</span>/<span class=\\\"disk-search-highlight"
                            + "\\\">e</span>",
                            se)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "boo")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + historySuggest(
                            "<span class=\\\"disk-search-highlight\\\">boo"
                            + "</span>k",
                            book)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPaging() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            for (String params
                : new String[]{"&p=2&numdoc=2", "&amount=2&offset=4"})
            {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(
                            cluster.proxy().host() + URI + "text=i" + params)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new JsonChecker(
                            serp(
                                2,
                                "i",
                                doc(NAME, 8),
                                doc(NAME, 14))),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
            for (String params
                : new String[]{"&p=1&numdoc=4", "&amount=4&offset=4"})
            {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(
                            cluster.proxy().host() + URI + "text=x" + params)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new JsonChecker(
                            serp(
                                4,
                                "x",
                                doc(NAME, 30),
                                doc(NAME, 31))),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testErratum() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.erratum().add(
                "/misspell.json/check?srv=disk-search&options=321&text=малчик",
                "{\"code\":201,\"lang\":\"ru,en\",\"rule\":\"Misspell\","
                + "\"flags\":0,\"r\":8000,\"srcText\":\"малчик\","
                + "\"text\":\"мальчик\"}");
            cluster.erratum().add(
                "/misspell.json/check?srv=disk-search&options=321&text=ёлки",
                "{\"code\":200,\"lang\":\"ru,en\",\"rule\":\"\","
                + "\"flags\":0,\"r\":0}");
            cluster.erratum().add(
                "/misspell.json/check?srv=disk-search&options=321&text=tishka",
                "{\"code\":201,\"lang\":\"ru,en\",\"rule\":\"Volapyuk\","
                + "\"flags\":512,\"r\":8000,\"srcText\":\"tishka\","
                + "\"text\":\"тишка\"}");
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=малчик")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "мальчик\", \"rule\": \"Misspell",
                            doc(NAME, 12))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=ёлки")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("ёлки", doc(BODY, 12))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=tishka")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("tishka", doc(NAME, 14))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGeocoder() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.geocoder().add(
                GEOCODER_URI + "%D0%BB%D0%B8%D1%81%D0%B8%D0%B9%22",
                new File(
                    SearchHandlerTest.class
                        .getResource("geocoder_fox.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            cluster.geocoder().add(
                GEOCODER_URI + "%D1%87%D0%B0%D0%B9%D0%BD%D0%B8%D0%BA%22",
                new File(
                    SearchHandlerTest.class
                        .getResource("geocoder_kettle.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=лисий")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "лисий",
                            doc(GEO, 20))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=чайник")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("чайник")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFoldersMerge() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=doc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            DOC,
                            doc(FOLDER, 24),
                            doc(FOLDER, 32),
                            doc(FOLDER, 28),
                            doc(FOLDER, 21),
                            doc(NAME, 22),
                            doc(NAME, 26),
                            doc(NAME, 27),
                            doc(NAME, 30),
                            doc(NAME, 31),
                            doc(NAME, 23))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            doc(FOLDER, 0),
                            doc(FOLDER, 24),
                            doc(FOLDER, 32),
                            doc(FOLDER, 28),
                            doc(FOLDER, 21),
                            doc(FOLDER, 13),
                            doc(NAME, 3),
                            doc(NAME, 4),
                            doc(NAME, 5),
                            doc(NAME, 7),
                            doc(NAME, 8),
                            doc(NAME, 11),
                            doc(NAME, 12),
                            doc(NAME, 14),
                            doc(NAME, 15),
                            doc(NAME, 16),
                            doc(NAME, 17),
                            doc(NAME, 18),
                            doc(NAME, 19),
                            doc(NAME, 20),
                            doc(NAME, 22),
                            doc(NAME, 23),
                            doc(NAME, 26),
                            doc(NAME, 27),
                            doc(NAME, 30),
                            doc(NAME, 31),
                            doc(NAME, 34))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFoldersTokenizedMerge() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI
                        + "text=new+doc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("new doc", doc(FOLDER, 32))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFoldersNegation() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI
                        + "text=txt+-english")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "txt -english",
                            doc(NAME, 11))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFoldersSpecialChars() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(0, "fol*der/"),
                doc(1, "folloder"),
                doc(2, "der_fol"));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI
                        + "text=fol*der")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("fol*der", doc(FOLDER, 0), doc(FOLDER, 2))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEscapingRequest() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host() + URI + "text=/мальчик")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "/мальчик",
                            doc(NAME, 12))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testI2T() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                doc(51, folder, "i2t2.jpg", "20 33\n")
                + ",\"i2t_keyword\":\"7F7E7D\"",
                doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + URI + "&text=свадьб" + 'а';
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testI2THnsw() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/hnsw-search?kps=0&key=/disk/*&visible=1&only=id&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                    doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                    doc(51, folder, "i2t2.jpg", "20 33\n")
                            + ",\"i2t_keyword\":\"7F7E7D\"",
                    doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                        new JsonChecker(
                                serp(request, doc(CV, 51), doc(CV, 50))),
                        CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testI2THnswTwoAdditionsOneAnswerPart() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/hnsw-search?kps=0&key=/disk/*&visible=1&only=id&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                doc(51, folder, "i2t2.jpg", "20 33\n") + ",\"i2t_keyword\":\"7F7E7D\"");
            cluster.backend().flush();
            cluster.backend().add(doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            cluster.backend().flush();
            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }


    @Test
    public void testI2THnswTwoAdditionsSeparatedAnswerParts() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/hnsw-search?kps=0&key=/disk/*&visible=1&only=id&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            cluster.backend().flush();
            cluster.backend().add(
                doc(51, folder, "i2t2.jpg", "20 33\n") + ",\"i2t_keyword\":\"7F7E7D\"");
            cluster.backend().flush();
            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testI2THnswLang() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/search-search?kps=0&visible=1&only=id&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                doc(51, folder, "i2t2.jpg", "20 33\n")
                    + ",\"i2t_keyword\":\"7F7E7D\"",
                doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            String request = "вжух:свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + "/stat?")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                JsonList stats = TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(response.getEntity())).asList();
                Assert.assertEquals(
                        findMetric(stats, "hnsw-search-requests_ammm").asLong(), 1L);
            }
        }
    }

    @Test
    public void testI2THnswCheckCache() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/hnsw-search?kps=0&key=/disk/*&visible=1&only=id&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                doc(51, folder, "i2t2.jpg", "20 33\n")
                    + ",\"i2t_keyword\":\"7F7E7D\"");

            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, doc(CV, 51), doc(CV, 50))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testI2TCalc() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            HttpPost post = new HttpPost(cluster.proxy().host() + "/i2t");
            post.setEntity(
                new StringEntity(
                    "{\"words\":[\"hello\",\"world\"]}",
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"results\":[{\"word\":\"hello\",\"i2t\":\"010002\"},"
                    + "{\"word\":\"world\",\"i2t\":\"010002\"}]}",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }


    @Test
    public void testI2THnswWithKeyField() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String hnswUri = "/hnsw-search?kps=0&key=/disk/*&visible=1&only=id,key&";
            cluster.start();
            prepareIndex(cluster);
            String folder = "i2t";
            cluster.backend().add(
                    doc(50, folder, "i2t1.jpg") + ",\"i2t_keyword\":\"010203\"",
                    doc(52, folder, "i2t3.jpg") + ",\"i2t_keyword\":\"8081FF\"");
            cluster.backend().flush();
            cluster.backend().add(
                    doc(51, folder, "i2t2.jpg", "20 33\n") + ",\"i2t_keyword\":\"7F7E7D\"");
            cluster.backend().flush();
            String request = "свадьб" + 'а';
            String uri = cluster.proxy().host() + hnswUri + "&text=" + request;

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                                "{\"request\":\"свадьба\",\"sortBy\":{\"how\":\"mtime\"," +
                                        "\"order\":\"descending\",\"priority\":\"no\"},\"groupings\":[" +
                                        "{\"attr\":\"\",\"categ\":\"\",\"docs\":\"1\",\"groups-on-page\":\"40\",\"mode\":\"flat\"}]," +
                                        "\"response\":{\"found\":{\"all\":\"2\",\"phrase\":\"2\",\"strict\":\"2\"}," +
                                        "\"results\":[{\"attr\":\"\",\"docs\":\"1\",\"found\":{\"all\":\"2\",\"phrase\":\"0\",\"strict\":\"0\"}," +
                                        "\"groups\":[{\"doccount\":\"1\",\"relevance\":\"51\",\"documents\":[{\"docId\":\"51\",\"url\":\"\"," +
                                        "\"relevance\":\"51\",\"properties\":{\"id\":\"51\",\"key\":\"/disk/i2t/i2t2.jpg\",\"scope\":\"cv\"}}]}," +
                                        "{\"doccount\":\"1\",\"relevance\":\"50\",\"documents\":[{\"docId\":\"50\",\"url\":\"\",\"relevance\":\"50\"," +
                                        "\"properties\":{\"id\":\"50\",\"key\":\"/disk/i2t/i2t1.jpg\",\"scope\":\"cv\"}}]}]}]}}",
                        CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterize() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareClusterizeIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + CLUSTERIZE)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 7
                            + MAX + (Integer.MAX_VALUE - 14)
                            + MIN + (Integer.MAX_VALUE - 20)
                            + MERGED + TestSearchBackend.concatDocs(
                                doc(14),
                                doc(15),
                                doc(16),
                                doc(17),
                                doc(18),
                                doc(19),
                                doc(20)),
                            SIZE + 1
                            + MAX + (Integer.MAX_VALUE - 35)
                            + MIN + (Integer.MAX_VALUE - 35)
                            + MERGED
                            + TestSearchBackend.concatDocs(doc(35)),
                            SIZE + 3
                            + MAX + CLUSTERIZE_TIMESTAMP
                            + MIN + (CLUSTERIZE_TIMESTAMP - 2)
                            + MERGED + TestSearchBackend.concatDocs(
                                doc(100),
                                doc(101),
                                doc(102)),
                            SIZE + 3
                            + MAX + CLUSTERIZE_TIMESTAMP2
                            + MIN + (CLUSTERIZE_TIMESTAMP2 - 2)
                            + MERGED + TestSearchBackend.concatDocs(
                                doc(200),
                                doc(201),
                                doc(202)),
                            SIZE + 1
                            + MAX + (CLUSTERIZE_TIMESTAMP2 - 15)
                            + MIN + (CLUSTERIZE_TIMESTAMP2 - 15)
                            + MERGED + TestSearchBackend.concatDocs(doc(203)))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testCompressedClusterize(final String charset)
        throws Exception
    {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .contentCompression(false)
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.start();
            prepareClusterizeIndex(cluster);
            HttpGet get = new HttpGet(
                cluster.proxy().host() + CLUSTERIZE
                + "&skip-nulls=false&postfilter=date+%3C%3D+"
                + (Integer.MAX_VALUE - 18)
                + "&postfilter=date+%3E%3D+" + CLUSTERIZE_TIMESTAMP);
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
            get.addHeader(HttpHeaders.ACCEPT_CHARSET, charset);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    "gzip",
                    response);
                HttpEntity entity = response.getEntity();
                Assert.assertNotEquals(-1L, entity.getContentLength());
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 3
                            + MAX + (Integer.MAX_VALUE - 18)
                            + MIN + (Integer.MAX_VALUE - 20)
                            + MERGED + TestSearchBackend.concatDocs(
                                doc(18),
                                doc(19),
                                doc(20)),
                            SIZE + 1
                            + MAX + (Integer.MAX_VALUE - 35)
                            + MIN + (Integer.MAX_VALUE - 35)
                            + MERGED
                            + TestSearchBackend.concatDocs(doc(35)),
                            SIZE + 1
                            + MAX + CLUSTERIZE_TIMESTAMP
                            + MIN + CLUSTERIZE_TIMESTAMP
                            + MERGED
                            + TestSearchBackend.concatDocs(doc(100)))),
                    new String(
                        IOStreamUtils
                            .consume(
                                new GZIPInputStream(
                                    new ByteArrayInputStream(
                                        CharsetUtils.toDecodable(entity)
                                            .toByteArray())))
                            .toByteArray(),
                        charset));
            }
        }
    }
    // CSON: MagicNumber

    @Test
    public void testCompressedClusterize() throws Exception {
        testCompressedClusterize("UTF-8");
        testCompressedClusterize("UTF-16BE");
    }

    @Test
    public void testUnavailableMispell() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this)) {
            cluster.start();
            prepareIndex(cluster);
            cluster.erratum().add("*", HttpStatus.SC_NOT_IMPLEMENTED);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                cluster.proxy().port(),
                URI + "text=some+request+here");
        }
    }

    private JsonObject findMetric(final JsonList root, final String name)
        throws Exception
    {
        for (JsonObject pair: root) {
            if (name.equals(pair.get(0).asString())) {
                return pair.get(1);
            }
        }
        Assert.fail("Failed to find metric '" + name + "' in " + root);
        return null;
    }

    @Test
    public void testLagsStat() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this)) {
            cluster.start();
            String uri = cluster.backend().indexerUri() + "/delete";
            HttpPost post = new HttpPost(uri);
            post.setEntity(
                new StringEntity(
                    TestSearchBackend.concatDocs(new LongPrefix(0L)),
                    ContentType.APPLICATION_JSON));
            String shard = "222";
            long start = System.currentTimeMillis() - WAIT_INTERVAL;
            post.addHeader(YandexHeaders.ZOO_QUEUE, "queue1");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, shard);
            post.addHeader(
                YandexHeaders.X_INDEX_OPERATION_TIMESTAMP,
                Long.toString(start));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            for (int i = 0; i <= 2; ++i) {
                post = new HttpPost(uri);
                post.setEntity(
                    new StringEntity(
                        TestSearchBackend.concatDocs(new LongPrefix(i)),
                        ContentType.APPLICATION_JSON));
                post.addHeader(YandexHeaders.ZOO_QUEUE, "queue2");
                post.addHeader(
                    YandexHeaders.ZOO_SHARD_ID,
                    Integer.toString(i));
                post.addHeader(YandexHeaders.ZOO_CTIME, Long.toString(start));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            }
            long end = System.currentTimeMillis();

            try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + "/lags-stat")))
            {
                JsonList root = TypesafeValueContentHandler.parse(
                    CharsetUtils.toString(response.getEntity())).asList();
                Assert.assertEquals(
                    shard,
                    findMetric(root, "indexation-lag-queue1-worst-shard_axxx")
                        .asString());
                Assert.assertEquals(
                    1L,
                    findMetric(root, "indexation-lag-queue1-shards_axxx")
                        .asLong());
                Assert.assertEquals(
                    2L + 1L,
                    findMetric(root, "indexation-lag-queue2-shards_axxx")
                        .asLong());

                long max1 = findMetric(root, "indexation-lag-queue1-max_avvv")
                    .asLong();
                long max2 = findMetric(root, "indexation-lag-queue2-max_avvv")
                    .asLong();
                YandexAssert.assertNotLess(WAIT_INTERVAL, max1);
                YandexAssert.assertNotLess(WAIT_INTERVAL, max2);
                YandexAssert.assertNotGreater(end - start, max1);
                YandexAssert.assertNotGreater(end - start, max2);
            }
            try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + "/lags-stat?ttl=1")))
            {
                JsonList root = TypesafeValueContentHandler.parse(
                    CharsetUtils.toString(response.getEntity())).asList();
                Assert.assertEquals(
                    "100",
                    findMetric(root, "indexation-lag-queue1-50ms_avvv")
                        .asString());
            }
        }
    }

    // CSOFF: MagicNumber
    @Test
    public void testMultiAuxFolders() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String uri = URI.replace(
                "&key=/disk/*",
                "&aux_folder=disk&aux_folder=photounlim&");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "text=jpg")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "jpg",
                            doc(NAME, 14),
                            doc(NAME, 15),
                            doc(NAME, 16),
                            doc(NAME, 17),
                            doc(NAME, 18),
                            doc(NAME, 19),
                            doc(NAME, 20),
                            doc(NAME, 35))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFotkiTags() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=дом")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("дом", doc(TAGS, 14))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + "text=мышата")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("мышата", doc(TAGS, 15))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearchPostFilter() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String folder = "Фотачке";
            cluster.backend().add(
                doc(300, folder, "IMG_0000.JPG", null, DISK),
                doc(301, folder, "VID_0000.MKV", null, DISK),
                doc(302, folder, "VID_0001.AVI", null, DISK),
                doc(303, folder, "IMG_0001.JPG", null, DISK),
                doc(304, folder, "IMG_0002.JPG", null, PHOTOUNLIM),
                doc(305, folder, "VID_0002.MKV", null, PHOTOUNLIM),
                doc(306, folder, "IMG_0003.JPG", null, PHOTOUNLIM),
                doc(307, folder, "VID_0003.AVI", null, PHOTOUNLIM));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + "/?kps=0&visible=1&only=id"
                        + "&text=&aux_folder=disk&aux_folder=photounlim")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            doc(NAME, 300),
                            doc(NAME, 301),
                            doc(NAME, 302),
                            doc(NAME, 303),
                            doc(NAME, 304),
                            doc(NAME, 306))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testKeyFilter() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?kps=0&visible=1&only=id&text=pdf"
                        + "&key=/disk/books/*")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            PDF,
                            doc(NAME, 3),
                            doc(NAME, 8),
                            doc(NAME, 12))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?kps=0&visible=1&only=id&text=&"
                        + "key=/disk/doc+new/*")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            doc(FOLDER, 33),
                            doc(NAME, 34))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    //CSOFF: MultipleStringLiterals
    @Test
    public void testFastMove() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                "\"id\": "
                    + "\"b35c7f628d16f9ce2dc\",\n"
                    + "\"resource_id\": "
                    + "\"227356512:b35c7f628\",\n"
                    + "\"version\": \"1486321074615556\",\n"
                    + "\"stid\": \"320.yadisk:227356512.E211995"
                    + ":97099719033930346922079766403\",\n"
                    + "\"parent_fid\": \"abcdd12\","
                    + "\"mimetype\": \"image/jpeg\",\n"
                    + "\"tikaite_mimetype\": \"image/jpeg\",\n"
                    + "\"mediatype\": \"9\",\n"
                    + "\"name\": \"Description\",\n"
                    + "\"ctime\": \"1486321071\",\n"
                    + "\"etime\": \"1457684535\",\n"
                    + "\"mtime\": \"1486321071\",\n"
                    + "\"photoslice_time\": \"1457684535\",\n"
                    + "\"size\": \"2050155\",\n"
                    + "\"aux_folder\": \"disk\",\n"
                    + "\"ext\": \"JPG\",\n"
                    + "\"type\": \"file\"",
                "\"id\": "
                    + "\"a35c7a628a16f9ce2dc\",\n"
                    + "\"resource_id\": "
                    + "\"227356512:a35a7a628\",\n"
                    + "\"version\": \"1486321074615556\",\n"
                    + "\"stid\": \"320.yadisk:227356512.E211995"
                    + ":97099719033930346922079766403\",\n"
                    + "\"parent_fid\": \"ccccc12\","
                    + "\"mimetype\": \"image/jpeg\",\n"
                    + "\"tikaite_mimetype\": \"image/jpeg\",\n"
                    + "\"mediatype\": \"9\",\n"
                    + "\"name\": \"Abracadabr\",\n"
                    + "\"ctime\": \"1486321071\",\n"
                    + "\"etime\": \"1457684535\",\n"
                    + "\"mtime\": \"1486321071\",\n"
                    + "\"photoslice_time\": \"1457684535\",\n"
                    + "\"size\": \"124323\",\n"
                    + "\"aux_folder\": \"disk\",\n"
                    + "\"ext\": \"JPG\",\n"
                    + "\"type\": \"file\"",
                //folders
                "\"id\": "
                    + "\"dd9a0dcfc7da6452eab5\",\n"
                    + "\"resource_id\":\"227356512:dd9a0dcfc7da\",\n"
                    + "\"version\": \"1490523916229956\",\n"
                    + "\"name\": \"Волго 2016\",\n"
                    + "\"ctime\": \"1490523916\",\n"
                    + "\"mtime\": \"1490523916\",\n"
                    + "\"folder\": \"Волго 2016\",\n"
                    + "\"fid\":\"abcdd12\","
                    + "\"parent_fid\":\"ccccc12\","
                    + "\"aux_folder\": \"disk\",\n"
                    + "\"type\": \"dir\",\n"
                    + "\"visible\": \"1\",\n"
                    + "\"owner\": \"227356512\"",
                "\"id\": "
                    + "\"aaaa0dcfc7da6452eab5\",\n"
                    + "\"resource_id\":\"227356512:aaaa0dcfc7da\",\n"
                    + "\"version\": \"1490523916229956\",\n"
                    + "\"name\": \"disk\",\n"
                    + "\"ctime\": \"1490523916\",\n"
                    + "\"mtime\": \"1490523916\",\n"
                    + "\"folder\": \"Волго 2016\",\n"
                    + "\"fid\":\"ccccc12\","
                    + "\"aux_folder\": \"disk\",\n"
                    + "\"type\": \"dir\",\n"
                    + "\"visible\": \"1\",\n"
                    + "\"owner\": \"227356512\"");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/folder-size?resource_id="
                        + "227356512:aaaa0dcfc7da&uid=0&fast-moved")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                    "{\"227356512:aaaa0dcfc7da\":{"
                        + "\"size\":2174478,\"count\":2}}"),
                    responseStr);
                System.out.println(responseStr);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/folder-size?resource_id="
                        + "227356512:dd9a0dcfc7da"
                        + "&resource_id="
                        + "227356512:aaaa0dcfc7da&uid=0&fast-moved")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"227356512:aaaa0dcfc7da\":{"
                            + "\"size\":2174478,\"count\":2},"
                            + "\"227356512:dd9a0dcfc7da\":"
                            + "{\"size\": 2050155, \"count\": 1}}"),
                    responseStr);
                System.out.println(responseStr);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?service=disk&kps=0&text=Description&numdoc=40"
                        + "&format=json&only=id,key&aux_folder=disk"
                        + "&aux_folder=photounlim&fast-moved")))
            {
                YandexAssert.check(
                    new JsonChecker("{\"request"
                    + "\":\"Description\",\"sortBy\":{\"how\":\"mtime\","
                    + "\"order\":\"descending\",\"priority\":\"no\"},"
                    + "\"groupings\":[{\"attr\":\"\",\"categ\":\"\","
                    + "\"docs\":\"1\",\"groups-on-page\":\"40\","
                    + "\"mode\":\"flat\"}],"
                    + "\"response\":{\"found\":{\"all\":\"1\","
                    + "\"phrase\":\"1\",\"strict\":\"1\"},"
                    + "\"results\":[{\"attr\":\"\",\"docs\":\"1\","
                    + "\"found\":{\"all\":\"1\",\"phrase\":\"0\","
                    + "\"strict\":\"0\"},\"groups\":[{\"doccount\":\"1\","
                    + "\"relevance\":\"b35c7f628d16f9ce2dc\",\"documents\":"
                    + "[{\"docId\":\"b35c7f628d16f9ce2dc\","
                    + "\"url\":\"\",\"relevance\":\"b35c7f628d16f9ce2dc\","
                    + "\"properties\":{\"id\":\"b35c7f628d16f9ce2dc\","
                    + "\"key\":\"/disk/Волго 2016/Description\","
                    + "\"scope\":\"name\"}}]}]}]}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber

    @Test
    public void testQueryLanguage() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this)) {
            cluster.start();
            cluster.backend().add(
                "\"id\": "
                    + "\"b35c7f628d16f9ce2dc\",\n"
                    + "\"resource_id\": "
                    + "\"227356512:b35c7f628\",\n"
                    + "\"version\": \"1486321074615556\",\n"
                    + "\"stid\": \"320.yadisk:227356512.E211995"
                    + ":97099719033930346922079766403\",\n"
                    + "\"parent_fid\": \"abcdd12\","
                    + "\"mimetype\": \"image/jpeg\",\n"
                    + "\"tikaite_mimetype\": \"image/jpeg\",\n"
                    + "\"mediatype\": \"9\",\n"
                    + "\"name\": \"Description\",\n"
                    + "\"ctime\": \"1486321071\",\n"
                    + "\"etime\": \"1457684535\",\n"
                    + "\"mtime\": \"1486321071\",\n"
                    + "\"photoslice_time\": \"1457684535\",\n"
                    + "\"size\": \"2050155\",\n"
                    + "\"aux_folder\": \"disk\",\n"
                    + "\"ext\": \"JPG\",\n"
                    + "\"type\": \"file\"");

            HttpGet get = new HttpGet(
                cluster.proxy().host()
                    + "/?service=disk&kps=0&text=документ:(паспорт+рф)+ИЛИ+папка:(паспорт+рф)&numdoc=40"
                    + "&format=json&only=id,key&aux_folder=disk"
                    + "&aux_folder=photounlim&fast-moved");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
        }
    }
}

