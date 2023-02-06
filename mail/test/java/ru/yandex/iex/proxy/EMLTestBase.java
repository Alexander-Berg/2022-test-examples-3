package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.search.document.mail.FirstlineMailMetaInfo;
import ru.yandex.search.document.mail.JsonFirstlineMailMetaHandler;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public abstract class EMLTestBase extends TestBase {
    private static final String ANY_URL = "/*";
    protected static final String FAKE_STID = "1.632123143.7594801846142779115218810981";
    protected static final String FAKE_MID = "42779115218810981";
    private static final String FACTS = "/facts?mdb=pg";
    private static final String FILTER_SEARCH = "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String TAKSA_WIDGET_TYPE = "taksa_widget_type_1234543456546";
    protected static final String HTTP_LOCALHOST = "http://localhost:";
    protected static final String UID = "&uid=";
    protected static final long UID_VALUE = 1130000013896717L;
    protected static final String URL_PREFIX = "user_gbl_";

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
    }

    public abstract String factname();

    public abstract String configExtra();

    public abstract Set<String> checkEntities();

    protected long getUid(final String to) { return UID_VALUE; }

    @Test
    public void testEMLs() throws Exception {
        final URL factDirUrl = this.getClass().getResource(factname());
        Assert.assertNotNull(factDirUrl);
        final File factDir = new File(factDirUrl.toURI());

        try (IexProxyCluster cluster =
                new IexProxyCluster(
                    this,
                    null,
                    configExtra(),
                    true,
                    true))
        {
            cluster.iexproxy().start();
            File[] files = factDir.listFiles();
            Arrays.sort(files);
            for (File file: files) {
                if (file.getName().endsWith(".eml")) {
                    System.err.println("Testing file: " + file.getName());
                    testEml(cluster, file);
                }
            }
        }
    }

    protected File auxilaryTestFile(final File eml, final String extension, final String subfolder)
        throws Exception
    {
        final String auxName = subfolder + '/'
            + eml.getName().substring(0, eml.getName().lastIndexOf('.'))
            + extension;
        URL url = this.getClass().getResource(auxName);
        if (url == null) {
            return null;
        }
        return new File(url.toURI());
    }

    protected String fileToString(final File file) throws IOException {
        return Files.readString(file.toPath());
    }

    protected String fileToString(final String fileName) throws Exception {
        Path path = Paths.get(getClass().getResource(fileName).toURI());
        return Files.readString(path);
    }

    protected void prepareCluster(
        final IexProxyCluster cluster,
        final File eml,
        final FirstlineMailMetaInfo meta)
            throws Exception
    {
        String to = "work@yandex.ru";
        cluster.blackbox().add(
            IexProxyCluster.blackboxUri(UID + getUid(to)),
            new StaticHttpItem(IexProxyCluster.blackboxResponse(getUid(to), to)));
        cluster.blackbox().add(
            IexProxyCluster.blackboxUri(UID + getUid(to)),
            new StaticHttpItem(IexProxyCluster.blackboxResponse(getUid(meta.get(MailMetaInfo.TO)), to)));
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("[{\"localhost\":-1}]")));
        cluster.testLucene().add(
            new LongPrefix(getUid(to)),
            "\"url\": \"" + URL_PREFIX + getUid(to) + "\",\"yuids\": \"3984446211505941116\n2489070261462964455\"");
        cluster.testLucene().add(
            new LongPrefix(getUid(meta.get(MailMetaInfo.TO))),
            "\"url\": \"" + URL_PREFIX + getUid(meta.get(MailMetaInfo.TO))
                + "\",\"yuids\": \"3984446211505941116\n2489070261462964455\"");
    }

    protected FirstlineMailMetaInfo prepareStorage(
        final IexProxyCluster cluster,
        final File eml,
        final String subfolder,
        final boolean patchStid)
            throws Exception
    {
        final File filterSearchJsonFile = auxilaryTestFile(eml, ".fs.json", subfolder);
        Assert.assertNotNull(filterSearchJsonFile);
        final JsonMap fsRoot =
            TypesafeValueContentHandler.parse(fileToString(filterSearchJsonFile)).asMap();

        final JsonList envelopes = fsRoot.get("envelopes").asList();
        Assert.assertNotNull(envelopes);

        final JsonMap envelope = envelopes.get(0).asMap();
        //patch stid
        if (patchStid) {
            envelope.put(MailMetaInfo.STID, new JsonString(FAKE_STID));
        }

        FirstlineMailMetaInfo meta = new FirstlineMailMetaInfo();
        new JsonFirstlineMailMetaHandler(meta).handle(envelope);
        final String mid = meta.get(MailMetaInfo.MID);
        final String stid = meta.get(MailMetaInfo.STID);

        final JsonMap toMap = envelope.get("to").get(0).asMap();
        final JsonMap fromMap = envelope.get("from").get(0).asMap();
        meta.set(MailMetaInfo.TO, toMap.getString("displayName", ""));
        meta.set(MailMetaInfo.FROM, fromMap.getString("displayName", ""));
        meta.set(MailMetaInfo.SUBJECT, envelope.getString(MailMetaInfo.SUBJECT, ""));
        meta.set(MailMetaInfo.FIRSTLINE, envelope.getString(MailMetaInfo.FIRSTLINE, ""));

        logger.info("TestEML: mid=" + mid + ", stid=" + stid + ", to=" + meta.get(MailMetaInfo.TO)
            + ", received_date=" + meta.get(MailMetaInfo.RECEIVED_DATE) + ", subject="
            + meta.get(MailMetaInfo.SUBJECT) + ", firstline=" + meta.get(MailMetaInfo.FIRSTLINE)
            + ", types=" + meta.messageTypes().toString());

        cluster.storageCluster().put(stid, eml);
        String fsUri = FILTER_SEARCH + getUid(meta.get(MailMetaInfo.TO)) + '*';
        cluster.filterSearch().add(fsUri, JsonType.NORMAL.toString(fsRoot));
        return meta;
    }

    // CSOFF: MethodLength
    private void testEml(final IexProxyCluster cluster, final File eml)
        throws Exception
    {
        final File factsJsonFile = auxilaryTestFile(eml, ".facts.json", factname());
        Assert.assertNotNull(factsJsonFile);

        final FirstlineMailMetaInfo meta = prepareStorage(cluster, eml, factname(), true);

        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            prepareCluster(cluster, eml, meta);

            FileEntity kinoEntity = new FileEntity(
                new File(getClass().getResource("kinopoisk_1.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.kinopoisk().add(
                ANY_URL,
                new StaticHttpItem(HttpStatus.SC_OK, kinoEntity),
                new StaticHttpItem(HttpStatus.SC_OK, kinoEntity),
                new StaticHttpItem(HttpStatus.SC_OK, kinoEntity));

            FileEntity afishaGraphQLResponse = new FileEntity(
                new File(getClass().getResource("afisha_response.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.afisha().add(
                ANY_URL,
                new StaticHttpItem(HttpStatus.SC_OK, afishaGraphQLResponse));

            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + getUid(meta.get(MailMetaInfo.TO))
                    + "&mid=" + meta.get(MailMetaInfo.MID) + "&cokedump&extract"
                    + "&ignore_cache=true&update_cache=false");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString + " for file " + eml);

                final JsonMap factsRoot =
                    TypesafeValueContentHandler.parse(entityString).asMap();

                final JsonMap expectedRoot =
                    TypesafeValueContentHandler.parse(
                        fileToString(factsJsonFile)).asMap();

                checkFacts(
                    expectedRoot.get(meta.get(MailMetaInfo.MID)).asList(),
                    factsRoot.get(meta.get(MailMetaInfo.MID)).asList());
            }
        }
    }
    // CSON: MethodLength

    private void checkFacts(final JsonList expected, final JsonList actual)
        throws Exception
    {
        Set<String> checkingFacts = checkEntities();

        JsonMap expectedMap = new JsonMap(expected.containerFactory());
        for (JsonObject fact: expected) {
            String type = fact.asMap().get(TAKSA_WIDGET_TYPE).asString();
            if (checkingFacts.contains(type)) {
                expectedMap.put(type, fact);
            }
        }

        JsonMap actualMap = new JsonMap(actual.containerFactory());
        for (JsonObject fact: actual) {
            String type = fact.asMap().get(TAKSA_WIDGET_TYPE).asString();
            if (checkingFacts.contains(type)) {
                actualMap.put(type, fact);
            }
        }

        YandexAssert.check(
            new JsonChecker(JsonType.NORMAL.toString(expectedMap)),
            JsonType.NORMAL.toString(actualMap));
    }
}
