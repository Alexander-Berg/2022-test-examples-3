package ru.yandex.msearch.proxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HeadersHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.writer.JsonType;
import ru.yandex.logger.FileRotateType;
import ru.yandex.logger.HandlersManager;
import ru.yandex.logger.IdGenerator;
import ru.yandex.logger.ImmutableLoggerFileConfig;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.msearch.proxy.MsearchProxyCluster.MproxyClusterContext;
import ru.yandex.msearch.proxy.api.async.mail.Product;
import ru.yandex.msearch.proxy.api.async.mail.Side;
import ru.yandex.msearch.proxy.api.async.mail.relevance.search.BasicMailSearchRelevanceFactory;
import ru.yandex.msearch.proxy.api.async.mail.relevance.search.LoggingSession;
import ru.yandex.msearch.proxy.api.async.mail.relevance.search.MailSearchRelevanceConfigBuilder;
import ru.yandex.msearch.proxy.api.async.mail.relevance.search.MailSearchRelevanceFactory;
import ru.yandex.msearch.proxy.config.FactorsLogConfig;
import ru.yandex.msearch.proxy.config.ImmutableFactorsLogConfig;
import ru.yandex.msearch.proxy.config.ImmutableMsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.msearch.proxy.config.RankingConfigBuilder;
import ru.yandex.msearch.proxy.config.RelevanceConfig;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.tskv.BasicTskvParser;
import ru.yandex.tskv.MockImmutableLoggerConfig;
import ru.yandex.tskv.MockLogger;
import ru.yandex.tskv.TskvHandler;
import ru.yandex.tskv.TskvException;
import ru.yandex.tskv.TskvRecord;

public class RankingTest extends MsearchProxyTestBase {
    private static final IdGenerator idGenerator = new IdGenerator();

    private abstract class MockLoggerMsearchCluster extends MsearchProxyCluster {
        protected abstract ImmutableMsearchProxyConfig updateConfig(
            final MsearchProxyConfigBuilder config)
            throws Exception;

        public MockLoggerMsearchCluster(
            final TestBase testBase)
            throws Exception
        {
            super(testBase);
        }

        public MockLoggerMsearchCluster(
            final TestBase testBase,
            final MproxyClusterContext clusterContext)
            throws Exception
        {
            super(testBase, clusterContext);
        }

        @Override
        public MockImmutableMsearchProxyConfig config(
            final MproxyClusterContext context,
            final IniConfig prodConfig)
            throws Exception
        {
            MsearchProxyConfigBuilder config =
                new MsearchProxyConfigBuilder(super.config(context, prodConfig));

            return new MockImmutableMsearchProxyConfig(updateConfig(config));
        }
    }

    private MockLoggerMsearchCluster createCluster(
        final MproxyClusterContext context,
        final Set<String> logFactors)
        throws Exception
    {
        return new MockLoggerMsearchCluster(this, context) {
            @Override
            protected ImmutableMsearchProxyConfig updateConfig(
                final MsearchProxyConfigBuilder config) throws Exception
            {
                config.tskvLogConfig().factors(logFactors);
                return config.build();
            }
        };
    }

    private static class MockImmutableFactorsLogConfig
        extends ImmutableFactorsLogConfig
    {
        private final MockImmutableLoggerConfig loggerConfig;
        private final boolean personal;
        private final Set<String> factors;

        public MockImmutableFactorsLogConfig(
            final FactorsLogConfig config) throws ConfigException
        {
            super(config);
            this.loggerConfig = new MockImmutableLoggerConfig(config);
            this.factors = new LinkedHashSet<>(config.factors());
            this.personal = config.personal();
        }

        public MockImmutableLoggerConfig loggerConfig() {
            return loggerConfig;
        }

        @Override
        public Set<String> factors() {
            return factors;
        }

        @Override
        public boolean personal() {
            return true;
        }

        @Override
        public Map<String, ImmutableLoggerFileConfig> files() {
            return super.files();
        }

        @Override
        public String separator() {
            return loggerConfig.separator();
        }

        @Override
        public Level logLevel() {
            return super.logLevel();
        }

        @Override
        public Logger build(
            final HandlersManager handlersManager) throws ConfigException
        {
            return loggerConfig.build(handlersManager);
        }
    }

    private static class MockImmutableMsearchProxyConfig
        extends ImmutableMsearchProxyConfig
    {
        private final MockImmutableFactorsLogConfig tskvConfig;

        public MockImmutableMsearchProxyConfig(
            final MsearchProxyConfig config)
            throws ConfigException
        {
            super(config);
            this.tskvConfig = new MockImmutableFactorsLogConfig(
                config.tskvLogConfig());
        }

        @Override
        public ImmutableFactorsLogConfig tskvLogConfig() {
            return tskvConfig;
        }

        public MockLogger mockLogger() {
            return tskvConfig.loggerConfig.logger();
        }
    }

    private static List<TskvRecord> getRecords(final MockLogger logger)
        throws Exception
    {
        final List<TskvRecord> recordList = new ArrayList<>();

        String output = String.join("\n", logger.output());

        new BasicTskvParser(new TskvHandler<TskvRecord>() {
            @Override
            public boolean onRecord(final TskvRecord r) {
                recordList.add(r);
                return true;
            }

            @Override
            public boolean onError(final TskvException exc) {
                throw new RuntimeException(exc);
            }
        }).parse(new StringReader(output));

        return recordList;
    }

    private static void search(
        final CloseableHttpClient client,
        final HttpGet get,
        final String request,
        final String... envelopes)
        throws Exception
    {
        try (CloseableHttpResponse response = client.execute(get)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            YandexAssert.check(
                new JsonChecker(serp(request, true, envelopes)),
                CharsetUtils.toString(response.getEntity()));
        }
    }

    private static String search(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String request,
        final String... envelopes)
        throws Exception
    {
        QueryConstructor qc = new QueryConstructor(
            cluster.proxy().host()
            + "/api/async/mail/search?mdb=mdb200&suid=0&first=0");
        qc.append("request", request);
        HttpGet httpGet = new HttpGet(qc.toString());

        String requestId = idGenerator.next();

        httpGet.addHeader(
            YandexHeaders.X_REQUEST_ID,
            requestId);

        String jsonRequest = JsonType.NORMAL.toString(request);
        jsonRequest = jsonRequest.substring(1, jsonRequest.length() - 1);
        search(client, httpGet, jsonRequest, envelopes);

        return requestId;
    }


    @Test
    public void testPersonalAndTextFactors() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> factors = new HashSet<>(Arrays.asList(
            "age", "request_email", "total_clicks_n", "serp_clicks_n",
            "mtype_freq", "hdr_subject", "pure_body", "hdr_from", "was_rel"
        ));

        try (MockLoggerMsearchCluster cluster = createCluster(context, factors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();


            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.start();

            cluster.backend().add(
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_from\":\"alesya@gmail.com\""
                        + ",\"hdr_subject\":\""
                        + "Запрос к  ivan@mail.ru от" +
                        " ООО \\\"Какая компания\\\"" +
                        "\",\"clicks_total_count\": 2",
                    "\"pure_body\":\"Иван, почему в почте факторы не " +
                        "логируются\n" +
                        "ООО-ПоискПочты\n" +
                        "С уважением\n" +
                        "Вадим Мидав vadim@yandex.ru\n" +
                        "Алеся alesya@gmail.com alesya midav\""),
                doc(
                    "100520",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_from\":\"spam@gmail.com\""
                        + ",\"hdr_subject\":\"Spam\","
                        + "\"clicks_total_count\": 2",
                    "\"pure_body\":\"Spam is coming\""),
                doc(
                    "100540",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_from\":\"dpotapov@yandex-team.ru\""
                        + ",\"hdr_subject\":\"backslash\","
                        + "\"clicks_total_count\": 5",
                    "\"pure_body\":\"escape it properly\""));

            String envelope1 = "{\"types\":[54,4],"
                + "\"mid\":\"100500\","
                + "\"subject\":\"Запрос\",\"receiveDate\":1468291113}";

            String envelope2 = "{\"types\":[11,4,13],"
                + "\"mid\":\"100520\","
                + "\"subject\":\"Spam\",\"receiveDate\":1468291113}";

            String envelope3 = "{\"mid\":\"100540\","
                + "\"subject\":\"backslash\",\"receiveDate\":1468291113}";

            cluster.filterSearch().add(
                fsURI + "&mids=100500",
                envelopes("", envelope1));

            cluster.filterSearch().add(
                fsURI + "&mids=100520",
                envelopes("", envelope2));

            cluster.filterSearch().add(
                fsURI + "&mids=100540",
                envelopes("", envelope3));

            search(cluster, client, "вадиМ мидав", envelope1);
            List<TskvRecord> records = getRecords(mockLogger);
            for (TskvRecord r: records) {
                System.out.println(r);
            }

            Assert.assertEquals(1, records.size());
            TskvRecord record = records.get(0);
            assertFactor(record, "mtype_freq", 0);
            assertGreater(record, "pure_body_score_e", 0.0);
            assertGreater(record, "pure_body_freq_ne", 0.0);
            assertFactor(record, "pure_body_hits_ne", 2.0);
            assertGreater(record, "pure_body_score_ne", 0.0);
            assertFactor(record, "pure_body_hits_e", 2.0);
            assertGreater(record, "pure_body_freq_e", 0.0);
            assertFactor(record, "hdr_subject_hits_e", 0);
            assertFactor(record, "hdr_subject_freq_e", 0);
            assertFactor(record, "hdr_subject_score_e", 0);
            assertFactor(record, "hdr_subject_freq_ne", 0);
            assertFactor(record, "hdr_subject_hits_ne", 0);
            assertFactor(record, "hdr_subject_score_ne", 0);
            assertFactor(record, "hdr_from_hits_e", 0);
            assertFactor(record, "hdr_from_freq_e", 0);
            assertFactor(record, "hdr_from_score_e", 0);
            assertFactor(record, "hdr_from_freq_ne", 0);
            assertFactor(record, "hdr_from_hits_ne", 0);

            cluster.backend().add(
                "\"url\":\"umtype_0\", "
                    + "\"mtype_show_count\":\"all\t11\n4,46\t3\n4\t5\n4,54\t3\"");
            cluster.backend().add("\"url\":\"reqs_0_alesya\"," +
                                      "\"request_raw\":\"alesya midav\"," +
                                      "\"request_normalized\":\"alesya midav\"," +
                                      "\"request_spaceless\":\"alesyamidav\"," +
                                      "\"request_date\":\"1479917671\"," +
                                      "\"request_count\":\"25\"," +
                                      "\"request_mids\":\"100500\"");

            Thread.sleep(500);
            mockLogger.clear();

            search(cluster, client, "alesya midav", envelope1);
            records = getRecords(mockLogger);
            for (TskvRecord r: records) {
                System.out.println(r);
            }

            Assert.assertEquals(1, records.size());
            record = records.get(0);

            assertFactor(record, "mtype_freq", 0.2727272727272727);
            assertGreater(record, "pure_body_score_e", 0.0);
            assertGreater(record, "pure_body_freq_ne", 0.0);
            assertFactor(record, "pure_body_hits_ne", 2.0);
            assertGreater(record, "pure_body_score_ne", 0.0);
            assertFactor(record, "pure_body_hits_e", 2.0);
            assertGreater(record, "pure_body_freq_e", 0.0);
            assertFactor(record, "hdr_subject_hits_e", 0);
            assertFactor(record, "hdr_subject_freq_e", 0);
            assertFactor(record, "hdr_subject_score_e", 0);
            assertFactor(record, "hdr_subject_freq_ne", 0);
            assertFactor(record, "hdr_subject_hits_ne", 0);
            assertFactor(record, "hdr_subject_score_ne", 0);
            assertFactor(record, "hdr_from_hits_e", 0.0);
            assertFactor(record, "hdr_from_freq_e", 0);
            assertFactor(record, "hdr_from_score_e", 0);
            assertGreater(record, "hdr_from_freq_ne", 0);
            assertFactor(record, "hdr_from_hits_ne", 1.0);
            assertGreater(record, "hdr_from_score_ne", 0);
            assertFactor(record, "hdr_from_hits_ne", 1.0);
            assertFactor(record, "total_clicks_n", 0.18181818181818182);
            assertFactor(record, "was_rel", 1.0);

            mockLogger.clear();

            search(cluster, client, "запрос", envelope1);
            records = getRecords(mockLogger);

            Assert.assertEquals(1, records.size());
            record = records.get(0);

            assertFactor(record, "mtype_freq", 0.2727272727272727);
            assertFactor(record, "hdr_subject_hits_e", 1.0);
            assertGreater(record, "hdr_subject_freq_e", 0);
            assertGreater(record, "hdr_subject_score_e", 0);
            assertGreater(record, "hdr_subject_freq_ne", 0);
            assertFactor(record, "hdr_subject_hits_ne", 1.0);
            assertGreater(record, "hdr_subject_score_ne", 0);
            assertFactor(record, "total_clicks_n", 0.18181818181818182);

            mockLogger.clear();
            search(cluster, client, "Spam", envelope2);
            records = getRecords(mockLogger);

            Assert.assertEquals(1, records.size());
            record = records.get(0);

            assertFactor(record, "mtype_freq", 0);

            mockLogger.clear();
            search(cluster, client, "backslash\\", envelope3);
            records = getRecords(mockLogger);

            Assert.assertEquals(1, records.size());
            record = records.get(0);

            assertFactor(record, "mtype_freq", 0);
        }
    }

    @Test
    public void testEmailFactors() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> factors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "request_email", "mtype", "fid", "req_in_subj",
            "from_email_group", "to_email_group"
        ));

        try (MockLoggerMsearchCluster cluster = createCluster(context, factors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();


            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.start();

            cluster.backend().add(
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_from\":\"alesya@gmail.com\""
                        + ",\"hdr_subject\":\""
                        + "Запрос к  ivan@mail.ru от" +
                        " ООО \\\"Какая компания\\\"" +
                        "\",\"clicks_total_count\": 2",
                    "\"pure_body\":\"Иван, почему в почте факторы не " +
                        "логируются\n" +
                        "ООО-ПоискПочты\n" +
                        "С уважением\n" +
                        "Вадим Мидав vadim@yandex.ru\n" +
                        "Алеся alesya@gmail.com\""));

            String envelope = "{\"specialLabels\":[],\"revision\":9129," +
                "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
                "\"subject\":\"Re: Запрос от ООО " +
                "\\\"Какая компания\\\"\"," +
                "\"attachmentsCount\":0,\"attachments\":[]," +
                "\"threadId\":\"100500\",\"date\":1468291113," +
                "\"hdrLastStatus\":\"\",\"receiveDate\":1468291113,\"cc\":[]," +
                "\"uidl\":\"\",\"types\":[13],\"references\":\"\"," +
                "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
                "\"from\":[{\"displayName\":\"Вадим Мидав\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}," +
                "{\"displayName\":\"Алеся\"," +
                "\"local\":\"alesya\",\"domain\":\"gmail.com\"}]," +
                "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
                "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
                "\"mid\":\"100500\"," +
                "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
                "\"subject\":\"Запрос от ООО " +
                "\\\"Какая компания\\\"\",\"postfix\":\"\"," +
                "\"isSplitted\":true},\"stid\":\"96095.886138058" +
                ".31234568448567823326084184586\"," +
                "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
                "\"local\":\"ivan\",\"domain\":\"mail.ru\"}, " +
                "{\"displayName\":\"Иван\", \"local" +
                "\":\"ivan\",\"domain\":\"mail.ru\"}]," +
                "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
                "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
                "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
                "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

            cluster.filterSearch().add(
                fsURI + "&mids=100500",
                envelopes("", envelope));

            search(cluster, client, "вадиМ мидав", envelope);
            List<TskvRecord> records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            TskvRecord record = records.get(0);
            System.out.println(record);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 0);

            mockLogger.clear();

            search(cluster, client, "ООО-ПоискПочты", envelope);
            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);
            System.out.println(record);
            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 0);

            mockLogger.clear();

            search(cluster, client, "ivan@mail.ru", envelope);
            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);
            System.out.println(record);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 0);

            mockLogger.clear();

            search(cluster, client, "вадим", envelope);
            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);
            System.out.println(record);
            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0.4);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 0);

            mockLogger.clear();

            search(cluster, client, "vadim", envelope);
            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0.5);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 0);
        }
    }

    private static void assertFactor(
        final TskvRecord record,
        final String factor,
        double value)
        throws Exception
    {
        Assert.assertEquals(value, Double.parseDouble(record.get(factor)), 1e-6);
    }

    private static void assertGreater(
        final TskvRecord record,
        final String factor,
        double min)
        throws Exception
    {
        YandexAssert.assertGreater(min, Double.parseDouble(record.get(factor)));
    }

    @Test
    public void testBaseLogFactors() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> factors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "request_email", "mtype", "fid", "req_in_subj",
            "from_email_group", "to_email_group", "weekday", "daytime", "age_p"
        ));

        int factorsSize = 17;

        // common entries like side product request
        int metaLogItemsSize = 15;
        int tskvSize = factorsSize + metaLogItemsSize;

        MailSearchRelevanceConfigBuilder rankingConfig
            = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.factors(factors);
        rankingConfig.content(
            this.getClass().getResourceAsStream("matrixnet.inc"));
        rankingConfig.name("model1");
        ranking.mailSearch(Collections.singletonList(rankingConfig));

        context.matrixnet(ranking);

        try (MockLoggerMsearchCluster cluster = createCluster(context, factors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";
            String fsURIFull = "/filter_search?order=default&mdb=mdb200&suid=0"
                + "&excl_folders=spam&excl_folders=hidden_trash";

            cluster.start();

            cluster.backend().add(
                doc(
                "100500",
                "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                    + ",\"received_date\":\"1234567890\""
                    + ",\"hdr_from\":\"vadim@yandex.ru,alesya@gmail.com\""
                    + ",\"message_type\":\"4 people\""
                    + ",\"hdr_subject\":\""
                    + "Запрос от ООО \\\"Какая компания\\\"" + "\""
                    + ",\"clicks_total_count\": 2",
                "\"pure_body\":\""
                    + "Вадим почему в почте факторы не логируются?" + "\""));

            String envelope = "{\"specialLabels\":[],\"revision\":9129," +
                "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
                "\"subject\":\"Re: Запрос от ООО " +
                "\\\"Какая компания\\\"\"," +
                "\"attachmentsCount\":0,\"attachments\":[]," +
                "\"threadId\":\"100500\",\"date\":1468291113," +
                "\"hdrLastStatus\":\"\",\"receiveDate\":1468291113,\"cc\":[]," +
                "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43],\"references\":\"\"," +
                "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
                "\"from\":[{\"displayName\":\"Вадим\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}," +
                "{\"displayName\":\"Алеся\"," +
                "\"local\":\"alesya\",\"domain\":\"gmail.com\"}]," +
                "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
                "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
                "\"mid\":\"100500\"," +
                "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
                "\"subject\":\"Запрос от ООО " +
                "\\\"Какая компания\\\"\",\"postfix\":\"\"," +
                "\"isSplitted\":true},\"stid\":\"96095.886138058" +
                ".31234568448567823326084184586\"," +
                "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
                "\"local\":\"ivan\",\"domain\":\"mail.ru\"}]," +
                "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
                "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
                "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
                "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

            cluster.filterSearch().add(
                fsURI + "&mids=100500",
                envelopes("", envelope));

            cluster.filterSearch().add(
                fsURIFull + "&mids=100500",
                envelopes("", envelope));

            String request1 = "запрос";
            String reqId1 = search(cluster, client, request1, envelope);
            String request2 = "alesya@gmail.com";
            String reqId2 = search(cluster, client, request2, envelope);

            List<TskvRecord> records = getRecords(mockLogger);
            Assert.assertEquals(2, records.size());
            TskvRecord record = records.get(0);
            System.out.println(record);
            Assert.assertEquals(tskvSize, record.size());
            Assert.assertEquals(record.get(LoggingSession.REQUEST), request1);
            Assert.assertEquals(record.get(LoggingSession.MID), "100500");
            assertFactor(record, BasicMailSearchRelevanceFactory.EMAIL, 0.0);
            assertFactor(record, LoggingSession.POSITION, 1.0);
            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), reqId1);

            assertFactor(record, BasicMailSearchRelevanceFactory.SERP_CLICKS, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TOTAL_CLICKS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 1);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PEOPLE, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_SOCIAL, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_NEWS, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TYPE_PERSONAL, 1);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            int weekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (weekDay == 0) {
                weekDay = 7;
            }

            assertFactor(
                record,
                BasicMailSearchRelevanceFactory.WEEKDAY,
                weekDay);

            Assert.assertEquals(
                Double.parseDouble(record.get(BasicMailSearchRelevanceFactory.DAYTIME)),
                cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0,
                0.2);

            assertFactor(record, LoggingSession.POSITION, 1);
            assertFactor(record, LoggingSession.NON_RANKED_POSITION, 1);
            Assert.assertEquals(
                "model1", record.get(LoggingSession.RANK_MODEL));

            assertFactor(record, BasicMailSearchRelevanceFactory.FID, 1);

            Double age = Double.parseDouble(
                record.get(BasicMailSearchRelevanceFactory.RECEIVED_DATE));

            YandexAssert.assertGreater(6.0, age);
            YandexAssert.assertLess(13.0, age);

            record = records.get(1);
            Assert.assertEquals(tskvSize, record.size());
            Assert.assertEquals(record.get(LoggingSession.REQUEST), request2);
            Assert.assertEquals(record.get(LoggingSession.MID), "100500");
            assertFactor(record, BasicMailSearchRelevanceFactory.EMAIL, 1);
            assertFactor(record, LoggingSession.POSITION, 1);

            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), reqId2);

            assertFactor(record, BasicMailSearchRelevanceFactory.SERP_CLICKS, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TOTAL_CLICKS, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 1);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.TO_NAME_IN_REQUEST, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FID, 1);

            assertFactor(record, LoggingSession.POSITION, 1);
            assertFactor(record, LoggingSession.NON_RANKED_POSITION, 1);
            Assert.assertEquals(
                "model1", record.get(LoggingSession.RANK_MODEL));

            mockLogger.clear();

            HttpGet httpGet = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&tzoffset=780&request="
                    + request1);

            search(client, httpGet, request1, envelope);
            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);
            Assert.assertEquals(tskvSize, record.size());
            Assert.assertEquals(
                record.get(LoggingSession.REQUEST_ID), "100");

            cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+13:00"));

            weekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (weekDay == 0) {
                weekDay = 7;
            }

            assertFactor(
                record,
                BasicMailSearchRelevanceFactory.WEEKDAY,
                weekDay);
            Assert.assertEquals(
                cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE)) / 60.0,
                Double.parseDouble(record.get(BasicMailSearchRelevanceFactory.DAYTIME)),
                0.2);

            //test search-filter
            mockLogger.clear();
            httpGet = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&search-filter=people&reqid=100&request="
                    + request1);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request1, true, envelope)),
                    CharsetUtils.toString(response.getEntity()));
            }

            records = getRecords(mockLogger);
            Assert.assertEquals(0, records.size());

            //test message_type
            mockLogger.clear();
            httpGet = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&message_type=4&reqid=100&request="
                    + request1);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request1, true, envelope)),
                    CharsetUtils.toString(response.getEntity()));
            }

            records = getRecords(mockLogger);
            Assert.assertEquals(0, records.size());

            // test imap
            mockLogger.clear();
            request1 = "hdr_from:*";
            httpGet = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&imap=1&reqid=100&request="
                    + request1);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(request1, false, envelope)),
                    CharsetUtils.toString(response.getEntity()));
            }

            records = getRecords(mockLogger);
            Assert.assertEquals(0, records.size());

            //
            mockLogger.clear();

            request1 = "невозможное";
            httpGet = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&request="
                    + request1);
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request1, false)),
                    CharsetUtils.toString(response.getEntity()));
            }

            records = getRecords(mockLogger);
            Assert.assertEquals(0, records.size());
        }
    }

    protected String experimentsSerp(
        final String request,
        final List<String> experiments,
        final String... envelopes)
    {
        StringBuilder sb = new StringBuilder(
            "\"details\":{\"crc32\":\"0\","
                + SEARCH_LIMITS
                + ",\"total-found\":" + envelopes.length
                + ",\"search-options\":{");

        if (experiments != null) {
            sb.append("\"experiments\":\"");
            sb.append(String.join(",", experiments));
            sb.append("\", ");
        }

        sb.append("\"pure\": true, \"request\":\"");
        sb.append(request);
        sb.append("\"}}, ");
        return envelopes(sb.toString(), envelopes);
    }


    @Test
    public void testExperiments() throws Exception {
        String model1 =
            "    static unsigned short GeneratedCompactIndicesTbl[] = {\n"
                + "        0,7,7,7,7,6\n"
                + " };\n"
                + "    static int GeneratedDataTbl[] = {\n"
                + "        0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,\n"
                + "        20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,\n"
                +"         37,38,39,40\n"
                + " };\n"
                + "i64 resInt = 0;\n"
                + "{\n"
                + "    const int *fFactorInt = reinterpret_cast<const int*>(fFactor);\n"
                + "    bool vars[8];\n"
                + "    vars[0] = fFactorInt[0] > 1059817308 ? 1 : 0; // 5.0\n"
                + "    vars[1] = fFactorInt[1] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[2] = fFactorInt[2] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[3] = fFactorInt[3] > 1059817308 ? 1 : 0; // 10000.0\n"
                + "    vars[4] = fFactorInt[4] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[5] = fFactorInt[5] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[6] = fFactorInt[6] > 1076576646 ? 1 : 0; // 0.9\n"
                + "    vars[7] = fFactorInt[7] > 1079397180 ? 1 : 0; // 0.9\n"
                + "    for (int z = 0; z < 1; ++z) {\n"
                + "        ui32 i0 = (reinterpret_cast<const ui32*>(indices))[0];\n"
                + "        ui32 i1 = (reinterpret_cast<const ui32*>(indices))[1];\n"
                + "        ui32 i2 = (reinterpret_cast<const ui32*>(indices))[2];\n"
                + "        int idx = vars[i2 >> 16];\n" +
                "        idx = idx * 2 + vars[i2 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i1 >> 16];\n" +
                "        idx = idx * 2 + vars[i1 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i0 >> 16];\n" +
                "        idx = idx * 2 + vars[i0 & 0xffff];\n" +
                "        resInt += data[idx];\n" +
                "        indices += 6;\n" +
                "        data += 64;\n" +
                "    }\n" +
                "}\n" +
                "double res = 0.0 + resInt * 1;\n";
        String model2 =
            "    static unsigned short GeneratedCompactIndicesTbl[] = {\n"
                + "        0,7,7,7,7,6\n"
                + " };\n"
                + "    static int GeneratedDataTbl[] = {\n"
                + "        0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,\n"
                + "        20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,\n"
                +"         37,38,39,40\n"
                + " };\n"
                + "i64 resInt = 0;\n"
                + "{\n"
                + "    const int *fFactorInt = reinterpret_cast<const int*>(fFactor);\n"
                + "    bool vars[8];\n"
                + "    vars[0] = fFactorInt[0] > 1059817308 ? 1 : 0; // 5.0\n"
                + "    vars[1] = fFactorInt[1] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[2] = fFactorInt[2] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[3] = fFactorInt[3] > 1059817308 ? 1 : 0; // 10000.0\n"
                + "    vars[4] = fFactorInt[4] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[5] = fFactorInt[5] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[6] = fFactorInt[6] > 1076576646 ? 1 : 0; // 0.4\n"
                + "    vars[7] = fFactorInt[7] > 1079397180 ? 1 : 0; // 0.4\n"
                + "    for (int z = 0; z < 1; ++z) {\n"
                + "        ui32 i0 = (reinterpret_cast<const ui32*>(indices))[0];\n"
                + "        ui32 i1 = (reinterpret_cast<const ui32*>(indices))[1];\n"
                + "        ui32 i2 = (reinterpret_cast<const ui32*>(indices))[2];\n"
                + "        int idx = vars[i2 >> 16];\n" +
                "        idx = idx * 2 + vars[i2 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i1 >> 16];\n" +
                "        idx = idx * 2 + vars[i1 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i0 >> 16];\n" +
                "        idx = idx * 2 + vars[i0 & 0xffff];\n" +
                "        resInt += data[idx];\n" +
                "        indices += 6;\n" +
                "        data += 64;\n" +
                "    }\n" +
                "}\n" +
                "double res = 0.0 + resInt * 1;\n";

        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> currentFactors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "fid", "request_email", "req_in_subj", "from_email_group_binary"
        ));
        Set<String> expFactors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "fid", "request_email", "req_in_subj", "from_email_group"
        ));
        Set<String> logFactors = new HashSet<>(Arrays.asList(
            "age", "fid", "from_email_group_binary", "req_in_subj", "lcn_score"
        ));

        Set<String> logFactorsExp = new HashSet<>(Arrays.asList(
            "age", "fid", "from_email_group", "req_in_subj", "lcn_score"
        ));


        final int tskvPrefixSize = 16;
        ByteArrayInputStream bis =
            new ByteArrayInputStream(model1.getBytes(Charset.defaultCharset()));

        MailSearchRelevanceConfigBuilder curRankingConfig
            = new MailSearchRelevanceConfigBuilder();

        curRankingConfig.usageStatus(
            RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        curRankingConfig.factors(currentFactors);
        curRankingConfig.content(bis);
        curRankingConfig.name("model1");

        MailSearchRelevanceConfigBuilder expRankingConfig
            = new MailSearchRelevanceConfigBuilder();

        expRankingConfig.factors(expFactors);
        expRankingConfig.testId("1");
        expRankingConfig.name("model2");
        bis = new ByteArrayInputStream(
            model2.getBytes(Charset.defaultCharset()));
        expRankingConfig.content(bis);
        expRankingConfig.usageStatus(
            RelevanceConfig.RelevanceUsageStatus.EXPERIMENT);

        ranking.mailSearch(Arrays.asList(curRankingConfig, expRankingConfig));

        MproxyClusterContext context = new MproxyClusterContext();
        context.matrixnet(ranking);
        context.userSplit();

        long fiveDaysOld = System.currentTimeMillis() / 1000 - 5 * 24 * 3600;

        String doc1 = doc(
            "100500",
            "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                + ",\"received_date\":\"0\""
                + ",\"hdr_from\":\"ivan@yandex.ru\""
                + ",\"hdr_subject\":\""
                + "Запрос от ООО \\\"Какая компания\\\"" + "\""
                + ",\"clicks_total_count\": 2",
            "\"pure_body\":\""
                + "Йосим почему в почте факторы не логируются?\"");

        String doc2 = doc(
            "100501",
            "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                + ",\"received_date\":\""
                + String.valueOf(fiveDaysOld)
                + "\",\"hdr_from\":\"ivan@yandex.ru\""
                + ",\"hdr_subject\":\""
                + "Запрос от ООО \\\"Какая компания\\\"" + "\""
                + ",\"clicks_total_count\": 2",
            "\"pure_body\":\""
                + "Йосим Еще почему в почте факторы не логируются?\"");

        String envelope1 = "{\"specialLabels\":[],\"revision\":9129," +
            "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
            "\"subject\":\"Re: Запрос от ООО " +
            "\\\"Какая компания\\\"\"," +
            "\"attachmentsCount\":0,\"attachments\":[]," +
            "\"threadId\":\"100500\",\"date\":0," +
            "\"hdrLastStatus\":\"\",\"receiveDate\":0,\"cc\":[]," +
            "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43],"
            + "\"references\":\"\"," +
            "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
            "\"from\":[{\"displayName\":\"Вадим Иванович\"," +
            "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
            "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
            "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
            "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
            "\"mid\":\"100500\"," +
            "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
            "\"subject\":\"Запрос от ООО " +
            "\\\"Какая компания\\\"\",\"postfix\":\"\"," +
            "\"isSplitted\":true},\"stid\":\"96095.886138058" +
            ".31234568448567823326084184586\"," +
            "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
            "\"local\":\"ivan\",\"domain\":\"mail.ru\"}]," +
            "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
            "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
            "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
            "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

        String envelope2 = "{\"specialLabels\":[],\"revision\":9129," +
            "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
            "\"subject\":\"Re: Запрос от ООО " +
            "\\\"Какая компания\\\"\"," +
            "\"attachmentsCount\":0,\"attachments\":[]," +
            "\"threadId\":\"100501\",\"date\":"
            + String.valueOf(fiveDaysOld) + ","
            + "\"hdrLastStatus\":\"\",\"receiveDate\":"
            + String.valueOf(fiveDaysOld) + ",\"cc\":[]," +
            "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43]," +
            "\"references\":\"\"," +
            "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
            "\"from\":[{\"displayName\":\"Йосим Иванович\"," +
            "\"local\":\"ivan\",\"domain\":\"yandex.ru\"}]," +
            "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
            "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
            "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
            "\"mid\":\"100501\"," +
            "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
            "\"subject\":\"Запрос от ООО " +
            "\\\"Какая компания\\\"\",\"postfix\":\"\"," +
            "\"isSplitted\":true},\"stid\":\"96095.886138058" +
            ".31234568448567823326084184586\"," +
            "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
            "\"local\":\"ivan\",\"domain\":\"mail.ru\"}]," +
            "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
            "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
            "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
            "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

        String fsURI = "/filter_search?order=default&mdb=pg&uid=0" +
            "&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

        try (MockLoggerMsearchCluster cluster =
                 createCluster(context, logFactors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            cluster.start();

            cluster.backend().add(doc1);
            cluster.backend().add(doc2);

            cluster.filterSearch().add(
                fsURI + "&mids=100501&mids=100500",
                envelopes("", envelope1, envelope2));
            String request = "Йосим";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&suid=1&first=0");
            qc.append("request", request);
            HttpGet httpGet = new HttpGet(qc.toString());

            String requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);
            httpGet.setHeader("X-Real-IP", "1.1.1.1");

            cluster.userSplit().add(
                "/mail?&uuid=0&service=mail",
                new StaticHttpResource(
                new HeadersHttpItem(
                    null,
                    "X-Yandex-ExpConfigVersion", "5183",
                    "X-Yandex-ExpBoxes", "",
                    "X-Yandex-ExpFlags", "")));

            String expected = experimentsSerp(
                request,
                Collections.emptyList(),
                envelope1,
                envelope2);

            List<TskvRecord> records;
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r : records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            //ok now check what we logged
            records = getRecords(mockLogger);
            Assert.assertEquals(2, records.size());
            TskvRecord record = records.get(0);
            // first record
            Assert.assertEquals(tskvPrefixSize + logFactors.size(),
                                record.size());
            Assert.assertEquals(
                record.get(LoggingSession.REQUEST), request);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100500");
            Assert.assertEquals(
                "model1",
                record.get(LoggingSession.RANK_MODEL));

            assertFactor(record, LoggingSession.NON_RANKED_POSITION,
                         2.0);

            assertFactor(record, LoggingSession.POSITION, 1.0);
            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), requestId);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
            //YandexAssert.assertGreater(
            //    1e-6,
            //    Double.parseDouble(record.get(BasicMailSearchRelevanceFactory.LUCENE_SCORE)));
            // second one
            record = records.get(1);
            System.out.println(record);
            Assert.assertEquals(tskvPrefixSize + logFactors.size(),
                                record.size());
            Assert.assertEquals(
                record.get(LoggingSession.REQUEST), request);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100501");
            Assert.assertEquals(
                "model1",
                record.get(LoggingSession.RANK_MODEL));

            assertFactor(record, LoggingSession.NON_RANKED_POSITION,
                         1.0);
            assertFactor(record, LoggingSession.POSITION, 2.0);
            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), requestId);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
        }

        ranking = new RankingConfigBuilder(ranking);
        curRankingConfig.content(new ByteArrayInputStream(model1.getBytes(Charset.defaultCharset())));
        expRankingConfig.content(new ByteArrayInputStream(model2.getBytes(Charset.defaultCharset())));

        ranking.mailSearch(Arrays.asList(curRankingConfig, expRankingConfig));

        context.matrixnet(ranking);
        try (MockLoggerMsearchCluster cluster =
                 createCluster(context, logFactorsExp);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            cluster.start();

            cluster.backend().add(doc1);
            cluster.backend().add(doc2);

            cluster.filterSearch().add(
                fsURI + "&mids=100501&mids=100500",
                envelopes("", envelope1, envelope2));
            String request = "Йосим";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&suid=1&first=0");
            qc.append("request", request);
            HttpGet httpGet = new HttpGet(qc.toString());

            String requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);
            httpGet.setHeader("X-Real-IP", "1.1.1.1");

            cluster.userSplit().add(
                "/mail?&uuid=0&service=mail",
                new StaticHttpResource(new HeadersHttpItem(
                    null,
                    "X-Yandex-ExpConfigVersion", "5183",
                    "X-Yandex-ExpBoxes", "1,0,0",
                    "X-Yandex-ExpFlags", ""))
                );

            // now we expecting invert order due to more precise factor
            String expected = experimentsSerp(
                request,
                Collections.singletonList("1"),
                envelope2,
                envelope1);

            List<TskvRecord> records;
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            //ok now check what we logged

            Assert.assertEquals(2, records.size());
            TskvRecord record = records.get(0);

            Assert.assertEquals(
                tskvPrefixSize + logFactors.size(), record.size());
            Assert.assertEquals(
                record.get(LoggingSession.REQUEST), request);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100501");
            Assert.assertEquals(
                "model2",
                record.get(LoggingSession.RANK_MODEL));

            assertFactor(record, LoggingSession.NON_RANKED_POSITION, 1.0);
            assertFactor(record, LoggingSession.POSITION, 1.0);
            Assert.assertEquals(
                "1", record.get(LoggingSession.EXPERIMENTS));
            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), requestId);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0.4);
//            YandexAssert.assertGreater(
//                1e-6,
//                Double.parseDouble(record.get(BasicMailSearchRelevanceFactory.LUCENE_SCORE)));
            // second one
            record = records.get(1);
            System.out.println(record);
            Assert.assertEquals(
                tskvPrefixSize + logFactors.size(), record.size());
            Assert.assertEquals(
                record.get(LoggingSession.REQUEST), request);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100500");
            Assert.assertEquals(
                "model2",
                record.get(LoggingSession.RANK_MODEL));

            assertFactor(record, LoggingSession.NON_RANKED_POSITION, 2.0);
            assertFactor(record, LoggingSession.POSITION, 2.0);
            Assert.assertEquals(
                record.get(LoggingSession.X_REQUEST_ID), requestId);

            assertFactor(record, BasicMailSearchRelevanceFactory.REQUEST_IN_SUBJECT, 0);

            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_EMAIL_IN_REQUEST, 0);
            assertFactor(record, BasicMailSearchRelevanceFactory.FROM_NAME_IN_REQUEST, 0);
//            YandexAssert.assertGreater(
//                1e-6,
//                Double.parseDouble(record.get(BasicMailSearchRelevanceFactory.LUCENE_SCORE)));

            // test rank-model param, same request plus param
            mockLogger.clear();

            expected = experimentsSerp(
                request,
                Collections.singletonList("1"),
                envelope1,
                envelope2);

            cluster.userSplit().add(
                "/mail?&uuid=0&service=mail",
                new StaticHttpResource(new HeadersHttpItem(
                    null,
                    "X-Yandex-ExpConfigVersion", "5183",
                    "X-Yandex-ExpBoxes", "1,0,0",
                    "X-Yandex-ExpFlags", ""))
            );

            httpGet = new HttpGet(qc.toString() + "&model=model1");
            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);
            httpGet.setHeader("X-Real-IP", "1.1.1.1");

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(2, records.size());
            Assert.assertEquals(
                "model1",
                records.get(0).get(LoggingSession.RANK_MODEL));
            Assert.assertEquals(
                "model1",
                records.get(1).get(LoggingSession.RANK_MODEL));
        }
    }


    @Test
    public void testRankingOPtions() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> factors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "request_email", "mtype", "fid", "req_in_subj",
            "from_email_group", "to_email_group"
        ));

        String envelope = "{\"subject\":\"Тема\",\"receiveDate\":0,\"cc\":[]," +
            "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43],"
            + "\"fid\":\"1\"," +
            "\"from\":[{\"displayName\":\"Вадим Иванович\"," +
            "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
            "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
            "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
            "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
            "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
            "\"local\":\"ivan\",\"domain\":\"mail.ru\"}], ";

        int factorsSize = 14;
        int metaLogItemsSize = 12;

        MailSearchRelevanceConfigBuilder rankingConfig
            = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.rankedPositions(1);
        rankingConfig.factors(factors);
        rankingConfig.content(
            this.getClass().getResourceAsStream("matrixnet.inc"));
        rankingConfig.name("model1");
        ranking.mailSearch(Collections.singletonList(rankingConfig));

        context.matrixnet(ranking);

        try (MockLoggerMsearchCluster cluster = createCluster(context, factors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            String fsURI = "/filter_search?order=default&mdb=pg&uid=0&suid=1" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.start();


            cluster.backend().add(
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"0\""
                        + ",\"hdr_from\":\"ivan@yandex.ru\""
                        + ",\"hdr_subject\":\"Тема\""
                        + ",\"clicks_total_count\": 2",
                    "\"pure_body\":\""
                        + "Тело\""));
            cluster.backend().add(
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"1\""
                        + ",\"hdr_from\":\"ivan@yandex.ru\""
                        + ",\"hdr_subject\":\"Тема\""
                        + ",\"clicks_total_count\": 2",
                    "\"pure_body\":\""
                        + "Тело\""));
            cluster.backend().add(
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                        + ",\"received_date\":\"2\""
                        + ",\"hdr_from\":\"ivan@yandex.ru\""
                        + ",\"hdr_subject\":\"Тема\""
                        + ",\"clicks_total_count\": 2",
                    "\"pure_body\":\""
                        + "Тело\""));

            mockLogger.clear();

            String env1 = envelope + "\"mid\":\"100500\"}";
            String env2 = envelope + "\"mid\":\"100501\"}";
            String env3 = envelope + "\"mid\":\"100502\"}";
            cluster.filterSearch().add(
                fsURI + "&mids=100502&mids=100501&mids=100500",
                envelopes(
                    "", env3, env2, env1));

            //test rankedPositions
            String request = "тело";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=0&count=2");

            qc.append("request", request);
            HttpGet httpGet = new HttpGet(qc.toString());

            String requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            StringBuilder sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\","
                    + "\"total-found\": 3,"
                    + "\"search-limits\":{\"offset\":0,\"length\":2}"
                    + ",\"search-options\":{");

            sb.append("\"pure\": true, \"request\":\"");
            sb.append(request);
            sb.append("\"}}, ");
            String expected = envelopes(sb.toString(), env3, env2);

            List<TskvRecord> records;
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));

                Assert.assertEquals(2, records.size());
                Assert.assertEquals(
                    records.get(0).get(LoggingSession.RANK_MODEL),
                    "model1");
                Assert.assertEquals(
                    records.get(0).get(LoggingSession.MID),
                    "100502");
                Assert.assertEquals(
                    records.get(1).get(LoggingSession.RANK_MODEL),
                    "");
                Assert.assertEquals(
                    records.get(1).get(LoggingSession.MID),
                    "100501");
            }

            mockLogger.clear();
            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=2&count=2");

            qc.append("request", request);
            httpGet = new HttpGet(qc.toString());

            requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\","
                    + "\"total-found\": 3,"
                    + "\"search-limits\":{\"offset\":2,\"length\":2}"
                    + ",\"search-options\":{");

            sb.append("\"pure\": true, \"request\":\"");
            sb.append(request);
            sb.append("\"}}, ");
            expected = envelopes(sb.toString(), env1);


            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                String responseStr =
                    CharsetUtils.toString(response.getEntity());

                YandexAssert.check(
                    new JsonChecker(expected),
                    responseStr);

                Assert.assertEquals(1, records.size());
                Assert.assertEquals(
                    records.get(0).get(LoggingSession.MID),
                    "100500");
                Assert.assertEquals(
                    records.get(0).get(LoggingSession.RANK_MODEL),
                    "");
            }

            //test scope
            request = "тема";
            mockLogger.clear();
            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=0&count=2&scope=hdr_subject");

            qc.append("request", request);
            httpGet = new HttpGet(qc.toString());

            requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\","
                    + "\"total-found\": 3,"
                    + "\"search-limits\":{\"offset\":0,\"length\":2}"
                    + ",\"search-options\":{");

            sb.append("\"request\":\"");
            sb.append(request);
            sb.append("\"}}, ");
            expected = envelopes(sb.toString(), env3, env2);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));

                Assert.assertEquals(0, records.size());
            }
        }
        }

    @Test
    public void testPaging() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        Set<String> factors = new HashSet<>(Arrays.asList(
            "age"
        ));

        // Point is - the most age mail is most relevant according
        // to model, but we are retrieve from lucene only 2 docs per request
        // so we should ignore such good document on second request and place
        // it according to date_order

        String model =
            "    static unsigned short GeneratedCompactIndicesTbl[] = {\n"
                + "        0,0,0,0,0,0\n"
                + " };\n"
                + "    static int GeneratedDataTbl[] = {\n"
                + "        0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,\n"
                + "        20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,\n"
                +"         37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,\n"
                +"         54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70\n"
                + " };\n"
                + "i64 resInt = 0;\n"
                + "{\n"
                + "    const int *fFactorInt = reinterpret_cast<const int*>(fFactor);\n"
                + "    bool vars[1];\n"
                + "    vars[0] = fFactorInt[0] > 1059817308 ? 1 : 0; // 5.0\n"
                + "    for (int z = 0; z < 1; ++z) {\n"
                + "        ui32 i0 = (reinterpret_cast<const ui32*>(indices))[0];\n"
                + "        ui32 i1 = (reinterpret_cast<const ui32*>(indices))[1];\n"
                + "        ui32 i2 = (reinterpret_cast<const ui32*>(indices))[2];\n"
                + "        int idx = vars[i2 >> 16];\n" +
                "        idx = idx * 2 + vars[i2 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i1 >> 16];\n" +
                "        idx = idx * 2 + vars[i1 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i0 >> 16];\n" +
                "        idx = idx * 2 + vars[i0 & 0xffff];\n" +
                "        resInt += data[idx];\n" +
                "        indices += 6;\n" +
                "        data += 64;\n" +
                "    }\n" +
                "}\n" +
                "double res = 0.0 + resInt * 1;\n";

        int factorsSize = 14;
        int metaLogItemsSize = 12;
        int tskvSize = factorsSize + metaLogItemsSize;

        MailSearchRelevanceConfigBuilder rankingConfig
            = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.factors(factors);
        rankingConfig.content(
            new ByteArrayInputStream(
                model.getBytes(Charset.defaultCharset())));
        rankingConfig.name("model");
        ranking.mailSearch(Collections.singletonList(rankingConfig));

        context.matrixnet(ranking);

        try (MockLoggerMsearchCluster cluster = createCluster(context, factors);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            String fsURI = "/filter_search?order=default&mdb=pg&uid=0&suid=1" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.start();

            long now = System.currentTimeMillis() / 1000;
            long fiveDaysOld =  now - 5 * 24 * 3600;

            String doc1 = sampleDoc("100500", "Йосим", now);
            String env1 = sampleEnvelope("100500", "Йосим", now);

            String env2 =
                sampleEnvelope("100501", "Йосим попытка 2", fiveDaysOld);
            String doc2 = sampleDoc("100501", "Йосим", fiveDaysOld);

            long tenDaysOld = fiveDaysOld - 5 * 24 * 3600;
            String env3 =
                sampleEnvelope("100503", "Йосим попытка 3", tenDaysOld);
            String doc3 = sampleDoc("100503", "Йосим попытка 3", tenDaysOld);

            cluster.backend().add(doc1, doc2, doc3);

            mockLogger.clear();
            Thread.sleep(200);

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501",
                envelopes("", env1, env2));

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501&mids=100503",
                envelopes("", env1, env2, env3));

//            cluster.filterSearch().add(
//                fsURI + "&mids=100501&mids=100500",
//                AsyncMailSearchTest.envelopes("", envelope2, envelope1));

            String request = "Йосим";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=0&count=1");

            qc.append("request", request);
            HttpGet httpGet = new HttpGet(qc.toString());
            String requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            StringBuilder sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\""
                    + ",\"total-found\":3"
                    + ",\"search-limits\":{\"offset\":0,\"length\":1}"
                    + ",\"search-options\":{");

            sb.append("\"pure\": true, \"request\":\"");
            sb.append(request);
            sb.append("\"}");
            sb.append("},");
            String expected = envelopes(sb.toString(), env1);

            List<TskvRecord> records;
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(1, records.size());
            TskvRecord record = records.get(0);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100500");
            Assert.assertEquals(
                "model",
                record.get(LoggingSession.RANK_MODEL));

            mockLogger.clear();

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=1&count=1");

            qc.append("request", request);
            httpGet = new HttpGet(qc.toString());
            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\""
                    + ",\"total-found\":3"
                    + ",\"search-limits\":{\"offset\":1,\"length\":1}"
                    + ",\"search-options\":{");

            sb.append("\"pure\": true, \"request\":\"");
            sb.append(request);
            sb.append("\"}");
            sb.append("},");
            expected = envelopes(sb.toString(), env2);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(1, records.size());
            record = records.get(0);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100501");
            Assert.assertEquals(
                "model",
                record.get(LoggingSession.RANK_MODEL));

            mockLogger.clear();

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=2&count=1");

            qc.append("request", request);
            httpGet = new HttpGet(qc.toString());
            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);

            sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\""
                    + ",\"total-found\":3"
                    + ",\"search-limits\":{\"offset\":2,\"length\":1}"
                    + ",\"search-options\":{");

            sb.append("\"pure\": true, \"request\":\"");
            sb.append(request);
            sb.append("\"}");
            sb.append("},");
            expected = envelopes(sb.toString(), env3);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(1, records.size());
            record = records.get(0);
            Assert.assertEquals(
                record.get(LoggingSession.MID), "100503");
            Assert.assertEquals(
                "model",
                record.get(LoggingSession.RANK_MODEL));

            // and then check that on full request, we got another order

            mockLogger.clear();

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search"
                    + "?mdb=pg&uid=0&suid=1&first=0&count=200");
            qc.append("request", request);
            httpGet = new HttpGet(qc.toString());

            requestId = idGenerator.next();

            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                requestId);


            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                records = getRecords(mockLogger);
                for (TskvRecord r: records) {
                    System.out.println(r);
                }

                YandexAssert.check(
                    new JsonChecker(serp(request, true, env3, env1, env2)),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(3, records.size());
            Assert.assertEquals(
                records.get(0).get(LoggingSession.MID), "100503");
            Assert.assertEquals(
                records.get(1).get(LoggingSession.MID), "100500");
            Assert.assertEquals(
                records.get(2).get(LoggingSession.MID), "100501");
        }
    }

    private MailSearchRelevanceConfigBuilder experimentConfig(
        final List<String> factors,
        final List<String> products,
        final List<String> sides,
        final String testId)
    {
        MailSearchRelevanceConfigBuilder expConfig
            = new MailSearchRelevanceConfigBuilder();

        if (testId != null) {
            expConfig.testId(testId);
        }

        expConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.EXPERIMENT);
        expConfig.products(
            products.stream().map(Product::parse).collect(Collectors.toSet()));
        expConfig.sides(
            sides.stream().map(Side::parse).collect(Collectors.toSet()));
        expConfig.factors(new LinkedHashSet<>(factors));
        expConfig.content(
            this.getClass().getResourceAsStream("matrixnet.inc"));

        return expConfig;
    }


    @Test
    public void testProductsAndSides() throws Exception {
        // testing influence web corp touch etc
        MproxyClusterContext context = new MproxyClusterContext();
        RankingConfigBuilder ranking = new RankingConfigBuilder();
        List<String> factors = Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "request_email", "mtype", "fid", "req_in_subj",
            "from_email_group", "to_email_group"
        );

        MailSearchRelevanceConfigBuilder defaultRanking =
            experimentConfig(factors, Collections.emptyList(), Collections.singletonList("web"), null);
        defaultRanking.usageStatus(
            RelevanceConfig.RelevanceUsageStatus.DEFAULT);

        MailSearchRelevanceConfigBuilder exp1 =
            experimentConfig(
                factors,
                Collections.emptyList(),
                Collections.singletonList("touchsm"),
                "1");

        MailSearchRelevanceConfigBuilder exp2 =
            experimentConfig(
                factors,
                Collections.emptyList(),
                Collections.singletonList("web"),
                "1");

        MailSearchRelevanceConfigBuilder exp3 =
            experimentConfig(
                factors,
                Collections.singletonList("corp"),
                Collections.singletonList("web"),
                "1");

        MailSearchRelevanceConfigBuilder exp4 =
            experimentConfig(
                factors,
                Collections.emptyList(),
                Collections.emptyList(),
                "3");

        ranking.mailSearch(
            Arrays.asList(defaultRanking, exp1, exp2, exp3, exp4));

        context.matrixnet(ranking);
        context.userSplit();

        try (MockLoggerMsearchCluster cluster =
                 createCluster(context, new HashSet<>(factors));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MockLogger mockLogger
                = ((MockImmutableMsearchProxyConfig) cluster.proxy().config())
                .mockLogger();

            cluster.start();

            String docCommon = "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                + ",\"received_date\":\"1234567890\""
                + ",\"hdr_from\":\"vadim@yandex.ru,alesya@gmail.com\""
                + ",\"hdr_subject\":\""
                + "Запрос от ООО \\\"Какая компания\\\"" + "\""
                + ",\"clicks_total_count\": 2";

            String docParts = "\"pure_body\":\""
                    + "Вадим почему в почте факторы не логируются?" + "\"";

            String doc = doc("100500", docCommon, docParts);
            cluster.backend().add(doc);
            cluster.backend().add(new LongPrefix(1120000000000001L), doc);

            String envelope = "{\"specialLabels\":[],\"revision\":9129," +
                "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
                "\"subject\":\"Re: Запрос от ООО " +
                "\\\"Какая компания\\\"\"," +
                "\"attachmentsCount\":0,\"attachments\":[]," +
                "\"threadId\":\"100500\",\"date\":1468291113," +
                "\"hdrLastStatus\":\"\",\"receiveDate\":1468291113,\"cc\":[]," +
                "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43]," +
                "\"references\":\"\"," +
                "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
                "\"from\":[{\"displayName\":\"Вадим\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}," +
                "{\"displayName\":\"Алеся\"," +
                "\"local\":\"alesya\",\"domain\":\"gmail.com\"}]," +
                "\"replyTo\":[{\"displayName\":\"vadim@yandex.ru\"," +
                "\"local\":\"vadim\",\"domain\":\"yandex.ru\"}]," +
                "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
                "\"mid\":\"100500\"," +
                "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
                "\"subject\":\"Запрос от ООО " +
                "\\\"Какая компания\\\"\",\"postfix\":\"\"," +
                "\"isSplitted\":true},\"stid\":\"96095.886138058" +
                ".31234568448567823326084184586\"," +
                "\"to\":[{\"displayName\":\"ООО-ПоискПочты\"," +
                "\"local\":\"ivan\",\"domain\":\"mail.ru\"}]," +
                "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
                "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
                "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
                "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1" +
                    "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope));

            String request = "компания";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&suid=1&first=0");
            qc.append("request", request);
            qc.append("side", "touchsm");

            HttpGet httpGet = new HttpGet(qc.toString());
            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                "1");
            httpGet.setHeader("X-Real-IP", "1.1.1.1");

            // testing that no experiments in options
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope)),
                    CharsetUtils.toString(response.getEntity()));
            }

            List<TskvRecord> records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            TskvRecord record = records.get(0);
            Assert.assertEquals(
                record.get(LoggingSession.SIDE), "touchsm");
            Assert.assertEquals(
                record.get(LoggingSession.PRODUCT), "bp");

            mockLogger.clear();

            // testing simple experiment
            cluster.userSplit().add(
                "/mail?&uuid=1120000000000001&service=mail",
                new StaticHttpResource(new HeadersHttpItem(
                    null,
                    "X-Yandex-ExpConfigVersion", "5183",
                    "X-Yandex-ExpBoxes", "10,0,0;1,0,0",
                    "X-Yandex-ExpFlags", ""))
            );

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?"
                    + "mdb=pg&uid=1120000000000001&suid=1&first=0");

            qc.append("request", request);
            qc.append("side", "web");

            httpGet = new HttpGet(qc.toString());
            httpGet.addHeader(
                YandexHeaders.X_REQUEST_ID,
                "2");
            httpGet.setHeader("X-Real-IP", "1.1.1.1");

            String expected = experimentsSerp(
                request,
                Arrays.asList("10", "1"),
                envelope);

            cluster.corpFilterSearch().add(
                "/filter_search?order=default&mdb=pg"
                    + "&uid=1120000000000001&suid=1"
                    + "&folder_set=default&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope));

            // testing that no experiments in options
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            records = getRecords(mockLogger);
            Assert.assertEquals(1, records.size());
            record = records.get(0);
            Assert.assertEquals(
                record.get(LoggingSession.PRODUCT),
                "corp");
            Assert.assertEquals(
                record.get(LoggingSession.SIDE),
                "web");
        }
    }

    protected static String sampleEnvelope(
        final String mid,
        final String subject,
        final long ts)
    {
        return "{\"specialLabels\":[],\"revision\":9129," +
            "\"rfcId\":\"<585071468291113@web2m.yandex.ru>\"," +
            "\"subject\":\"" + subject + "\"," +
            "\"attachmentsCount\":0,\"attachments\":[]," +
            "\"threadId\":\"" + mid + "\",\"date\":"+ ts + "," +
            "\"hdrLastStatus\":\"\",\"receiveDate\":" + ts+ ",\"cc\":[]," +
            "\"uidl\":\"\",\"types\":[1, 2, 3, 4, 22, 43],\"references\":\"\"," +
            "\"newCount\":0,\"hdrStatus\":\"\",\"fid\":\"1\"," +
            "\"from\":[{\"displayName\":\"Test\"," +
            "\"local\":\"test\",\"domain\":\"yandex.ru\"}]," +
            "\"replyTo\":[{\"displayName\":\"vonidu@yandex.ru\"," +
            "\"local\":\"vonidu\",\"domain\":\"yandex.ru\"}]," +
            "\"bcc\":[],\"extraData\":\"ivan@mail.ru\"," +
            "\"mid\":\"" + mid + "\"," +
            "\"subjectInfo\":{\"type\":\"replied\",\"prefix\":\"Re: \"," +
            "\"subject\":\"" + subject +
            "\",\"postfix\":\"\"," +
            "\"isSplitted\":true},\"stid\":\"96095.886138058" +
            ".31234568448567823326084184586\"," +
            "\"to\":[{\"displayName\":\"Ivan\"," +
            "\"local\":\"vonidu\",\"domain\":\"yandex.ru\"}]," +
            "\"threadCount\":0,\"inReplyTo\":\"\",\"firstline\":\"Да\"," +
            "\"attachmentsFullSize\":0,\"ImapModSeq\":\"\"," +
            "\"imapId\":\"1255\",\"size\":3716,\"labels\":[\"24\"," +
            "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}";

    }

    protected static String sampleDoc(
        final String mid,
        final String subject,
        final long ts)
    {
        return doc(
            mid,
            "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
                + ",\"received_date\":\"" + ts + "\""
                + ",\"hdr_from\":\"test@yandex.ru\""
                + ",\"hdr_subject\":\"" + subject + "\"",
            "\"pure_body\":\"" + subject + "\"");
    }

    protected static String topRelevantSerp(
        final String request,
        final List<String> relevant,
        final List<String> other)
    {
        return topRelevantSerp(
            request,
            SEARCH_LIMITS,
            other.size() + relevant.size(),
            relevant.size(),
            relevant,
            other);
    }

    protected static String topRelevantSerp(
        final String request,
        final String limits,
        final int total,
        final int topRelevant,
        final List<String> relevant,
        final List<String> other)
    {
        StringBuilder sb = new StringBuilder(
            "{\"details\":{\"crc32\":\"0\","
                + limits
                + ",\"total-found\":"
                + total);

        sb.append(",\"top-relevant\":" + topRelevant);

        sb.append(",\"search-options\":{");

        sb.append("\"pure\": true, \"request\":\"");
        sb.append(request);
        sb.append("\"}}, ");
        if (relevant.size() > 0) {
            sb.append("\"top-relevant\":[");
            sb.append(String.join(",", relevant));
            sb.append("],");
        }

        sb.append("\"envelopes\":[" + String.join(",", other));
        sb.append("]}");

        return sb.toString();
    }

    @Test
    public void testTopRelevant() throws Exception {
        MproxyClusterContext context =
            new MproxyClusterContext().enableTopRelevant();

        Set<String> factors = new HashSet<>(Arrays.asList(
            "age", "total_clicks", "serp_clicks",
            "request_email", "mtype", "fid", "req_in_subj",
            "from_email_group", "to_email_group"
        ));

        RankingConfigBuilder ranking = new RankingConfigBuilder();

        MailSearchRelevanceConfigBuilder rankingConfig
            = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.factors(factors);
        rankingConfig.content(
            this.getClass().getResourceAsStream("matrixnet.inc"));
        rankingConfig.name("model1");

        ranking.mailSearch(Collections.singletonList(rankingConfig));
        context.matrixnet(ranking);

        long ts = System.currentTimeMillis() / 1000;
        String doc1 = sampleDoc("100500", "Заголовок письма", ts - 16000);
        String env100500
            = sampleEnvelope("100500", "Заголовок письма", ts - 16000);

        String doc2 = sampleDoc("100501", "Заголовок письма", ts - 20000);
        String env100501
            = sampleEnvelope("100501", "Заголовок письма", ts - 20000);

        String doc3 = sampleDoc("100502", "Заголовок письма", ts - 10000);
        String env100502 = sampleEnvelope("100502", "Заголовок письма", ts - 10000);

        String doc4 = sampleDoc("100503", "Заголовок письма", ts - 12000);
        String env100503 = sampleEnvelope("100503", "Заголовок письма", ts - 12000);

        String doc5 = sampleDoc("100504", "Его не нашли поиском", ts - 1000);
        String env100504 = sampleEnvelope("100504", "Его не нашли поиском",
                                     ts - 1000);

        String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
            "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

        // test model ranked same as date_order
        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(doc1, doc2, doc3, doc4, doc5);

            String request = "Заголовок";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&hr");
            qc.append("request", request);

            HttpGet httpGet = new HttpGet(qc.toString());

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501",
                envelopes("", env100500, env100501));
            cluster.filterSearch().add(
                fsURI + "&mids=100502&mids=100503",
                envelopes("", env100502, env100503));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String text = CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            request,
                            true,
                            env100502, env100503, env100500, env100501)),
                    text);
            }
        }

        String model1 =
            "    static unsigned short GeneratedCompactIndicesTbl[] = {\n"
                + "        0,0,0,6,7,6\n"
                + " };\n"
                + "    static int GeneratedDataTbl[] = {\n"
                + "        0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,\n"
                + "        20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,\n"
                + "         37,38,39,40\n"
                + " };\n"
                + "i64 resInt = 0;\n"
                + "{\n"
                + "    const int *fFactorInt = reinterpret_cast<const int*>(fFactor);\n"
                + "    bool vars[8];\n"
                + "    vars[0] = fFactorInt[0] > 1076576646 ? 1 : 0; // 1.5\n"
                + "    vars[1] = fFactorInt[1] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[2] = fFactorInt[2] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[3] = fFactorInt[3] > 1059817308 ? 1 : 0; // 10000.0\n"
                + "    vars[4] = fFactorInt[4] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[5] = fFactorInt[5] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[6] = fFactorInt[6] > 1076576646 ? 1 : 0; // 1000.0\n"
                + "    vars[7] = fFactorInt[7] > 1079397180 ? 1 : 0; // 1000.0\n"
                + "    for (int z = 0; z < 1; ++z) {\n"
                + "        ui32 i0 = (reinterpret_cast<const ui32*>(indices))[0];\n"
                + "        ui32 i1 = (reinterpret_cast<const ui32*>(indices))[1];\n"
                + "        ui32 i2 = (reinterpret_cast<const ui32*>(indices))[2];\n"
                + "        int idx = vars[i2 >> 16];\n" +
                "        idx = idx * 2 + vars[i2 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i1 >> 16];\n" +
                "        idx = idx * 2 + vars[i1 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i0 >> 16];\n" +
                "        idx = idx * 2 + vars[i0 & 0xffff];\n" +
                "        resInt += data[idx];\n" +
                "        indices += 6;\n" +
                "        data += 64;\n" +
                "    }\n" +
                "}\n" +
                "double res = 0.0 + resInt * 1;\n";

        ranking = new RankingConfigBuilder();

        rankingConfig = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.factors(factors);
        rankingConfig.content(
            new ByteArrayInputStream(
                model1.getBytes(Charset.defaultCharset())));
        rankingConfig.name("model1");

        ranking.mailSearch(Collections.singletonList(rankingConfig));
        context.matrixnet(ranking);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {

            cluster.start();

            cluster.backend().add(doc1, doc2, doc3, doc4, doc5);

            String request = "Заголовок";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&hr");
            qc.append("request", request);

            HttpGet httpGet = new HttpGet(qc.toString());

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501",
                envelopes("", env100500, env100501));
            cluster.filterSearch().add(
                fsURI + "&mids=100502&mids=100503",
                envelopes("", env100502, env100503));
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(response.getEntity());

                YandexAssert.check(
                    new JsonChecker(
                        topRelevantSerp(
                            request,
                            Arrays.asList(env100500, env100501),
                            Arrays.asList(env100502, env100503))),
                    responseStr);
            }
        }

        ranking = new RankingConfigBuilder();

        rankingConfig = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.factors(factors);
        rankingConfig.rankedPositions(1);
        rankingConfig.content(
            new ByteArrayInputStream(
                model1.getBytes(Charset.defaultCharset())));
        rankingConfig.name("model1");

        ranking.mailSearch(Collections.singletonList(rankingConfig));
        context.matrixnet(ranking);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {

            cluster.start();

            cluster.backend().add(doc1, doc2, doc3, doc4, doc5);

            String request = "Заголовок";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&count=2&reqid=100&hr");
            qc.append("request", request);

            HttpGet httpGet = new HttpGet(qc.toString());

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501",
                envelopes("", env100500, env100501));
            cluster.filterSearch().add(
                fsURI + "&mids=100502&mids=100503",
                envelopes("", env100502, env100503));
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        topRelevantSerp(
                            request,
                            "\"search-limits\":{\"offset\":0,\"length\":2}",
                            4,
                            1,
                            Arrays.asList(env100500),
                            Arrays.asList(env100502))),
                    responseStr);
            }

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=2"
                    + "&count=2&reqid=100&hr");
            qc.append("request", request);

            httpGet = new HttpGet(qc.toString());

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        topRelevantSerp(
                            request,
                            "\"search-limits\":{\"offset\":2,\"length\":2}",
                            4,
                            1,
                            Collections.emptyList(),
                            Arrays.asList(env100503, env100501))),
                    responseStr);
            }

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&hr");
            qc.append("request", request);

            httpGet = new HttpGet(qc.toString());

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        topRelevantSerp(
                            request,
                            Arrays.asList(env100500),
                            Arrays.asList(env100502, env100503, env100501))),
                    responseStr);
            }
        }

        //test min-serp-size
        ranking = new RankingConfigBuilder();

        rankingConfig = new MailSearchRelevanceConfigBuilder();

        rankingConfig.usageStatus(
            RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        rankingConfig.minSerpSize(5);
        rankingConfig.factors(factors);
        rankingConfig.rankedPositions(1);
        rankingConfig.content(
            new ByteArrayInputStream(
                model1.getBytes(Charset.defaultCharset())));
        rankingConfig.name("model1");

        ranking.mailSearch(Collections.singletonList(rankingConfig));
        context.matrixnet(ranking);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {

            cluster.start();

            cluster.backend().add(doc1, doc2, doc3, doc4, doc5);

            String request = "Заголовок";
            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                    + "&reqid=100&hr");
            qc.append("request", request);

            HttpGet httpGet = new HttpGet(qc.toString());

            cluster.filterSearch().add(
                fsURI + "&mids=100500&mids=100501",
                envelopes("", env100500, env100501));
            cluster.filterSearch().add(
                fsURI + "&mids=100502&mids=100503",
                envelopes("", env100502, env100503));
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(
                    response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        serp(request, true, env100502, env100503, env100500, env100501)),
                    responseStr);
            }
        }
    }
}
