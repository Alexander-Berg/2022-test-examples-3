package ru.yandex.iex.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.junit.Assert;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.client.so.shingler.TestShinglerServer;
import ru.yandex.client.so.shingler.config.ShinglerClientsConfigBuilder;
import ru.yandex.client.so.shingler.config.ShinglerType;
import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.collection.PatternMap;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.geocoder.GeocoderConfigBuilder;
import ru.yandex.http.config.FilterSearchConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.URIConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.iex.proxy.afisha.config.AfishaConfigBuilder;
import ru.yandex.iex.proxy.afisha.config.AfishaConfigDefauls;
import ru.yandex.iex.proxy.complaints.ComplaintsConfigBuilder;
import ru.yandex.iex.proxy.complaints.Route;
import ru.yandex.iex.proxy.images.ZoraProxyClientConfigBuilder;
import ru.yandex.iex.proxy.xiva.XivaConfigBuilder;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.search.MailSearchDefaults;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.mail.received.ReceivedChainParserConfigBuilder;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.stater.StaterConfigBuilder;
import ru.yandex.stater.StatersConfigBuilder;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class IexProxyCluster implements GenericAutoCloseable<IOException> {
    public static final String IPORT = "_IPORT_";
    public static final String QUEUE_NAME = "change_log";
    // Max number of signals for IexProxy System:
    // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/CONF/agent.iexproxy.conf#6
    public static final int MAX_UNISTAT_SIGNALS = 3000;

    private static final long TVM_RENEWAL_INTERVAL = 60000L;
    private static final String FACTEXTRACT = "factextract/";
    private static final String FOLDERS_URI = "/folders";
    private static final String LABELS_URI = "/labels";
    private static final String ATTACH_SID_URI = "/attach_sid";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String HOST = "host = ";
    private static final String IEX = "iex";
    private static final long ALMOST_TIMEOUT = 90L;
    private static final long MAX_RETRIES = 10L;
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;
    private static final int CONNECTIONS = 10;

    private final StaticServer blackbox;
    private final StaticServer corpBlackbox;
    private final StaticServer blackboxDirect;
    private final StaticServer tikaite;
    private final StaticServer tikaiteMl;
    private final StaticServer tvm2;
    private final StaticServer filterSearch;
    private final StaticServer corpFilterSearch;
    private final StaticServer folders;
    private final StaticServer labels;
    private final StaticServer corpFolders;
    private final StaticServer corpLabels;
    private final StaticServer attachSid;
    private final StaticServer corpAttachSid;
    private final StaticServer iex;
    private final StaticServer postProcess;
    private final StaticServer cokemulatorIexlib;
    private final StaticServer mulcaGate;
    private final Map<ShinglerType, TestShinglerServer> shinglers;
    private final StaticServer complaints;
    private final StaticServer complaintsCoworkersSelection;
    private final StaticServer sologger;
    private final StaticServer freemail;
    private final StaticServer msal;
    private final StaticServer corpMsal;
    private final StaticServer settingsApi;
    private final StaticServer corpSettingsApi;
    private final StaticServer rasp;
    private final StaticServer refund;
    private final StaticServer rca;
    private final StaticServer media;
    private final StaticServer market;
    private final StaticServer bk;
    private final StaticServer gettext;
    private final StaticServer gatemail;
    private final StaticServer msearch;
    private final StaticServer onlineDB;
    private final StaticServer reminder;
    private final StaticServer geosearch;
    private final StaticServer afisha;
    private final StaticServer calendar;
    private final StaticServer calendarTools;
    private final StaticServer corovaneer;
    private final StaticServer kinopoiskQl;
    private final StaticServer producer;
    private final StaticServer producerAsyncClient;
    private final StaticServer zoraProxyServer;
    private final TestSearchBackend testLucene;
    private final MailStorageCluster storageCluster;
    private final StaticServer axis;
    private final StaticServer factsExtractor;
    private final StaticServer xiva;
    private final StaticServer taksa;
    private final StaticServer taksaTesting;
    private final StaticServer knn;
    private final StaticServer mops;
    private final StaticServer corpMops;
    private final StaticServer neuroHards;
    private final IexProxy iexproxy;
    private final GenericAutoCloseableChain<IOException> chain;

    public IexProxyCluster(final TestBase testBase) throws Exception {
        this(testBase, null, "", false);
    }

    public IexProxyCluster(
            final TestBase testBase,
            final boolean useTestSearchBackend)
        throws Exception
    {
        this(testBase, null, "", useTestSearchBackend);
    }

    public IexProxyCluster(
            final TestBase testBase,
            final boolean useTestSearchBackend,
            final boolean storageEmulator)
        throws Exception
    {
        this(testBase, null, "", useTestSearchBackend, storageEmulator);
    }

    public IexProxyCluster(
            final TestBase testBase,
            final File logPath,
            final String configExtra)
        throws Exception
    {
        this(testBase, logPath, configExtra, false);
    }

    //CSOFF: ParameterNumber
    //CSOFF: MethodLength
    public IexProxyCluster(
            final TestBase testBase,
            final File logPath,
            final String configExtra,
            final boolean useTestSearchBackend)
        throws Exception
    {
        this(testBase, logPath, configExtra, useTestSearchBackend, false);
    }

    public IexProxyCluster(
        final TestBase testBase,
        final File logPath,
        final String configExtra,
        final boolean useTestSearchBackend,
        final boolean storageEmulator)
        throws Exception
    {
        this(
            testBase,
            logPath,
            configExtra,
            UnaryOperator.identity(),
            useTestSearchBackend,
            storageEmulator);
    }

    public IexProxyCluster(
        final TestBase testBase,
        final File logPath,
        final String configExtra,
        final UnaryOperator<String> configPostProcessor,
        final boolean useTestSearchBackend,
        final boolean storageEmulator)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<IOException>()))
        {
            calendar = new StaticServer(Configs.baseConfig("calendar"));
            chain.get().add(calendar);
            calendarTools =
                new StaticServer(Configs.baseConfig("calendarTools"));
            chain.get().add(calendarTools);
            corovaneer = new StaticServer(Configs.baseConfig("corovaneer"));
            chain.get().add(corovaneer);
            kinopoiskQl = new StaticServer(Configs.baseConfig("kinopoisk"));
            chain.get().add(kinopoiskQl);
            producer = new StaticServer(Configs.baseConfig("producer"));
            chain.get().add(producer);
            producerAsyncClient =
                new StaticServer(Configs.baseConfig("producer-server-mock"));
            chain.get().add(producerAsyncClient);
            testLucene = new TestMailSearchBackend(testBase);
            chain.get().add(testLucene);
            blackbox = new StaticServer(Configs.baseConfig("Blackbox"));
            chain.get().add(blackbox);
            corpBlackbox = new StaticServer(Configs.baseConfig("CorpBlackbox"));
            chain.get().add(corpBlackbox);
            blackboxDirect = new StaticServer(
                new BaseServerConfigBuilder()
                    .port(0)
                    .name("BlackboxDirect")
                    .connections(100)
                    .workers(4)
                    .timeout(TIMEOUT)
                    .build());
            chain.get().add(blackboxDirect);

            if (storageEmulator) {
                storageCluster = new MailStorageCluster(testBase);
                chain.get().add(storageCluster);
                tikaite = null;
                tikaiteMl = null;
                cokemulatorIexlib = null;
                mulcaGate = null;
            } else {
                storageCluster = null;
                mulcaGate = new StaticServer(Configs.baseConfig("MulcaGate"));
                chain.get().add(mulcaGate);
                tikaite = new StaticServer(Configs.baseConfig("Tikaite"));
                chain.get().add(tikaite);
                tikaiteMl = new StaticServer(Configs.baseConfig("TikaiteMl"));
                chain.get().add(tikaiteMl);
                cokemulatorIexlib =
                    new StaticServer(Configs.baseConfig("cokemulatorIexlib"));
                chain.get().add(cokemulatorIexlib);
            }

            shinglers = new HashMap<>();
            for (final ShinglerType shinglerType : ShinglerType.values()) {
                TestShinglerServer testShinglerServer =
                    new TestShinglerServer(
                        Configs.baseConfig(shinglerType.name() + "-Shingler"),
                        shinglerType.dataType());
                if (shinglerType == ShinglerType.COMPL || shinglerType == ShinglerType.FREEMAIL
                        || shinglerType == ShinglerType.MASS_IN || shinglerType == ShinglerType.SENDER)
                {
                    testShinglerServer.setVerbose(true);
                }
                shinglers.put(shinglerType, testShinglerServer);
                chain.get().add(shinglers.get(shinglerType));
            }

            complaints = new StaticServer(Configs.baseConfig("ComplaintsSrv"));
            chain.get().add(complaints);

            complaintsCoworkersSelection = new StaticServer(Configs.baseConfig("ComplaintsCoworkersSelectionSrv"));
            chain.get().add(complaintsCoworkersSelection);

            sologger = new StaticServer(Configs.baseConfig("SologgerSrv"));
            chain.get().add(sologger);

            freemail = new StaticServer(Configs.baseConfig("FreemailSrv"));
            chain.get().add(freemail);

            settingsApi = new StaticServer(Configs.baseConfig("SettingsApi"));
            chain.get().add(settingsApi);
            corpSettingsApi = new StaticServer(Configs.baseConfig("CorpSettingsApi"));
            chain.get().add(corpSettingsApi);

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            filterSearch = new StaticServer(Configs.baseConfig("WMI"));
            chain.get().add(filterSearch);
            corpFilterSearch = new StaticServer(Configs.baseConfig("corp-WMI"));
            chain.get().add(corpFilterSearch);
            folders = new StaticServer(Configs.baseConfig("folders"));
            chain.get().add(folders);
            corpFolders = new StaticServer(Configs.baseConfig("corp-folders"));
            chain.get().add(corpFolders);
            labels = new StaticServer(Configs.baseConfig("labels"));
            chain.get().add(labels);
            corpLabels = new StaticServer(Configs.baseConfig("corp-labels"));
            chain.get().add(corpLabels);
            attachSid = new StaticServer(Configs.baseConfig("attach-sid"));
            chain.get().add(attachSid);
            corpAttachSid =
                new StaticServer(Configs.baseConfig("corp-attach-sid"));
            chain.get().add(corpAttachSid);
            iex = new StaticServer(Configs.baseConfig(IEX));
            chain.get().add(iex);
            postProcess = new StaticServer(Configs.baseConfig("postProcess"));
            chain.get().add(postProcess);
            msal = new StaticServer(Configs.baseConfig("msal"));
            chain.get().add(msal);
            corpMsal = new StaticServer(Configs.baseConfig("corp-msal"));
            chain.get().add(corpMsal);
            rasp = new StaticServer(Configs.baseConfig("rasp"));
            chain.get().add(rasp);
            refund = new StaticServer(Configs.baseConfig("refund"));
            chain.get().add(refund);
            rca = new StaticServer(Configs.baseConfig("rca"));
            chain.get().add(rca);
            media = new StaticServer(Configs.baseConfig("media"));
            chain.get().add(media);
            market = new StaticServer(Configs.baseConfig("market"));
            chain.get().add(market);
            bk = new StaticServer(Configs.baseConfig("bk"));
            chain.get().add(bk);
            gettext = new StaticServer(Configs.baseConfig("gettext"));
            chain.get().add(gettext);
            gatemail = new StaticServer(Configs.baseConfig("gatemail"));
            chain.get().add(gatemail);
            msearch = new StaticServer(Configs.baseConfig("msearch"));
            chain.get().add(msearch);
            onlineDB = new StaticServer(Configs.baseConfig("onlineDB"));
            chain.get().add(onlineDB);
            reminder = new StaticServer(Configs.baseConfig("reminder"));
            chain.get().add(reminder);
            geosearch = new StaticServer(Configs.baseConfig("geosearch"));
            chain.get().add(geosearch);
            afisha = new StaticServer(Configs.baseConfig("afisha"));
            chain.get().add(afisha);
            axis = new StaticServer(Configs.baseConfig("Axis"));
            chain.get().add(axis);
            zoraProxyServer = new StaticServer(Configs.baseConfig("ZoraProxy"));
            chain.get().add(zoraProxyServer);
            xiva = new StaticServer(Configs.baseConfig("xiva"));
            chain.get().add(xiva);
            taksa = new StaticServer(Configs.baseConfig("taksa"));
            chain.get().add(taksa);
            taksaTesting = new StaticServer(Configs.baseConfig("taksaTesting"));
            chain.get().add(taksaTesting);
            knn = new StaticServer(Configs.baseConfig("knn"));
            chain.get().add(knn);
            mops = new StaticServer(Configs.baseConfig("mops"));
            chain.get().add(mops);
            corpMops = new StaticServer(Configs.baseConfig("corp-mops"));
            chain.get().add(corpMops);
            neuroHards = new StaticServer(Configs.baseConfig("NeuroHards"));
            chain.get().add(neuroHards);

            factsExtractor =
                new StaticServer(Configs.baseConfig("FactsExtractor"));
            chain.get().add(factsExtractor);
            IexProxyConfigBuilder config = new IexProxyConfigBuilder(
                new IniConfig(
                    new StringReader(
                        iexProxyConfig(configExtra, configPostProcessor, 1))));
            config.port(0);
            config.connections(2);
            config.workers(2);
            config.blackboxConfig(
                new HttpHostConfigBuilder(Configs.hostConfig(blackbox))
                    .connections(100)
                    .statersConfig(
                        new StatersConfigBuilder().staters(
                            new PatternMap<>(
                                new StaterConfigBuilder()
                                    .prefix("blackbox")))));
            config.corpBlackboxConfig(
                new HttpHostConfigBuilder(Configs.hostConfig(corpBlackbox))
                    .statersConfig(
                        new StatersConfigBuilder().staters(
                            new PatternMap<>(
                                new StaterConfigBuilder()
                                    .prefix("corp-blackbox")))));
            config.blackboxDirectConfig(
                new HttpHostConfigBuilder(Configs.hostConfig(blackboxDirect))
                    .connections(100)
                    .statersConfig(new StatersConfigBuilder().staters(
                        new PatternMap<>(
                            new StaterConfigBuilder()
                                .prefix("blackbox-direct")))));
            config.enlarge(true);

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"logbroker ticket\"},"
                + "\"5\":{\"ticket\":\"blackbox ticket\"},"
                + "\"6\":{\"ticket\":\"corp blackbox ticket\"},"
                + "\"7\":{\"ticket\":\"tikaite ticket\"},"
                + "\"8\":{\"ticket\":\"unistorage ticket\"},"
                + "\"9\":{\"ticket\":\"geocoder ticket\"},"
                + "\"10\":{\"ticket\":\"fs ticket\"},"
                + "\"11\":{\"ticket\":\"corp fs ticket\"},"
                + "\"12\":{\"ticket\":\"folders ticket\"},"
                + "\"13\":{\"ticket\":\"corp folders ticket\"},"
                + "\"14\":{\"ticket\":\"labels ticket\"},"
                + "\"15\":{\"ticket\":\"corp labels ticket\"},"
                + "\"16\":{\"ticket\":\"afisha ticket\"},"
                + "\"17\":{\"ticket\":\"attach sid ticket\"},"
                + "\"18\":{\"ticket\":\"corp attach sid ticket\"},"
                + "\"19\":{\"ticket\":\"taksa ticket\"},"
                + "\"20\":{\"ticket\":\"calendar ticket\"},"
                + "\"21\":{\"ticket\":\"calendar tools ticket\"},"
                + "\"22\":{\"ticket\":\"corovaneer ticket\"},"
                + "\"23\":{\"ticket\":\"kinopoisk ticket\"}}");
            tvm2.start();

            Tvm2ClientConfigBuilder tvm2ClientConfig =
                new Tvm2ClientConfigBuilder();
            tvm2ClientConfig.destinationClientId(
                "4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23");
            tvm2ClientConfig.renewalInterval(TVM_RENEWAL_INTERVAL);

            config.tvm2ServiceConfig(tvm2ServiceConfig);
            config.tvm2ClientConfig(tvm2ClientConfig);
            config.blackboxTvmClientId("5");
            config.corpBlackboxTvmClientId("6");
            config.tikaiteTvmClientId("7");
            config.unistorageTvmClientId("8");
            config.geoTvmClientId("9");
            config.filterSearchTvmClientId("10");
            config.corpFilterSearchTvmClientId("11");
            config.foldersTvmClientId("12");
            config.corpFoldersTvmClientId("13");
            config.labelsTvmClientId("14");
            config.corpLabelsTvmClientId("15");
            config.afishaTvmClientId("16");
            config.attachSidTvmClientId("17");
            config.corpAttachSidTvmClientId("18");
            config.taksaTvmClientId("19");
            config.calendarTvmClientId("20");
            config.calendarToolsTvmClientId("21");
            config.corovaneerTvmClientId("22");
            config.kinopoiskTvmClientId("23");
            config.unpersonConfig()
                .libraryName(
                    ru.yandex.devtools.test.Paths.getBuildPath(
                        "mail/so/libs/unperson/jniwrapper"
                        + "/libunperson-jniwrapper.so"))
                .main16Name("JniWrapperUnpersonText")
                .ctorName("JniWrapperCreateUnperson")
                .dtorName("JniWrapperDestroyUnperson")
                .freeName("JniWrapperFree");

            if (storageEmulator) {
                config.tikaiteConfig(
                    Configs.hostConfig(storageCluster.tikaite()));
                config.tikaiteMlConfig(
                    Configs.hostConfig(storageCluster.tikaite()));
            } else {
                config.tikaiteConfig(Configs.hostConfig(tikaite));
                config.tikaiteMlConfig(Configs.hostConfig(tikaiteMl));
            }

            config.afishaConfig(AfishaConfigDefauls.INSTANCE);

            config.rcaURIConfig(
                Configs.uriConfig(
                    rca,
                    "/urls?account=test_iex"));
            config.rcaStaterConfig(new StaterConfigBuilder().prefix("rca-"));
            config.axisConfig(
                Configs.uriConfig(
                    axis,
                    "/v1/facts/store_batch?client_id=extractors"));
            config.factsExtractURIConfig(
                Configs.uriConfig(
                    factsExtractor,
                    "/facts-extract"));
            IniConfig iniComplaintsConfig = new IniConfig(new StringReader(
                "\nuri = " + HTTP_LOCALHOST + complaints.port() + "/fbl-out"
                + "\nconnections = 100"
                + "\nstat.prefix = complaints"
                + "\nstat.metrics = httpcodes, requesthist"));
            final ComplaintsConfigBuilder complaintsConfig = new ComplaintsConfigBuilder(iniComplaintsConfig);
            complaintsConfig.rulesDictFiles(Map.of(
                Route.IN,
                new File(getClass().getResource("complaints/rules_dict_in.txt").toURI()),
                Route.OUT,
                new File(getClass().getResource("complaints/rules_dict_out.txt").toURI()),
                Route.CORP,
                new File(getClass().getResource("complaints/rules_dict_corp.txt").toURI())
            )).dailyComplaintsLimit(1000).useSolog(true).useSologger(true).messageExpirationPeriod(30 * 86400000L)
                .shinglersAddStatByActions(true);
            final ShinglerClientsConfigBuilder shinglerClientsConfig =
                (ShinglerClientsConfigBuilder) config.complaintsConfig().shinglersConfig();
            for (final ShinglerType shinglerType : ShinglerType.values()) {
                if (shinglerType == ShinglerType.DICT) {
                    continue;
                }
                shinglerClientsConfig.shinglerClientConfig(
                    shinglerType,
                    Configs.hostConfig(shinglers.get(shinglerType)));
            }
            complaintsConfig.shinglersConfig(shinglerClientsConfig);
            complaintsConfig.soSearchRequestsStaterConfig(new StaterConfigBuilder().prefix("complaints-search"));
            config.complaintsConfig(Configs.uriConfig(complaints, "/fbl-out/"), complaintsConfig);
            config.complaintsCoworkersSelectionConfig(
                Configs.uriConfig(complaintsCoworkersSelection,"/coworkers-selection"));
            config.sologgerConfig(Configs.uriConfig(sologger, "/search"));
            config.freemailConfig(Configs.uriConfig(freemail, "/api/v1"));
            config.settingsApiConfig(Configs.uriConfig(settingsApi, "/get"));
            config.corpSettingsApiConfig(Configs.uriConfig(corpSettingsApi, "/get"));

            if (storageEmulator) {
                config.iexConfig(
                    new URIConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .uri(new URI(MailStorageCluster.iexUrl() + FACTEXTRACT))
                        .build());
            } else {
                config.iexConfig(
                    Configs.uriConfig(
                        iex,
                        '/' + FACTEXTRACT));
            }
            config.foldersConfig(Configs.uriConfig(folders, FOLDERS_URI));
            config.corpFoldersConfig(
                Configs.uriConfig(corpFolders, FOLDERS_URI));
            config.labelsConfig(Configs.uriConfig(labels, LABELS_URI));
            config.corpLabelsConfig(
                Configs.uriConfig(corpLabels, LABELS_URI));
            config.attachSidConfig(
                Configs.uriConfig(attachSid, ATTACH_SID_URI));
            config.corpAttachSidConfig(
                Configs.uriConfig(corpAttachSid, ATTACH_SID_URI));
            config.msalConfig(Configs.hostConfig(msal));
            config.corpMsalConfig(Configs.hostConfig(corpMsal));
            config.raspConfig(Configs.hostConfig(rasp));
            config.raspStaterConfig(new StaterConfigBuilder().prefix("rasp-"));
            config.refundConfig(Configs.hostConfig(refund));
            config.marketConfig(Configs.hostConfig(market));
            config.bkConfig(Configs.hostConfig(bk));
            config.mediaConfig(Configs.hostConfig(media));
            config.gettextConfig(Configs.hostConfig(gettext));
            config.gatemailConfig(Configs.hostConfig(gatemail));
            config.calendarConfig(Configs.hostConfig(calendar));
            config.calendarToolsConfig(Configs.hostConfig(calendarTools));
            config.corovaneerConfig(Configs.hostConfig(corovaneer));
            config.kinopoiskQlConfig(Configs.hostConfig(kinopoiskQl));
            config.msearchConfig(Configs.hostConfig(msearch));
            config.onlineDBConfig(Configs.hostConfig(onlineDB));
            config.reminderConfig(Configs.hostConfig(reminder));
            config.taksaConfig(Configs.hostConfig(taksa));
            config.taksaTestingConfig(Configs.hostConfig(taksaTesting));
            config.knnConfig(Configs.hostConfig(knn));
            config.mopsConfig(Configs.hostConfig(mops));
            config.corpMopsConfig(Configs.hostConfig(corpMops));
            config.neuroHardsConfig(Configs.hostConfig(neuroHards));
            config.receivedChainParserConfig(
                new ReceivedChainParserConfigBuilder(
                new IniConfig(new StringReader(
                    "[yandex-nets]\nfile="
                        + ru.yandex.devtools.test.Paths.getSandboxResourcesRoot()
                        + "/yandex-nets.txt"
                ))).build());
            IniConfig geoConfig = new IniConfig(new StringReader(
                "results = " + 1 + "\norigin = test"
                 + "\nhost = localhost\nport = " + geosearch.port()
                 + "\nconnections = 100"));
            config.geoSearchConfig(
                new GeocoderConfigBuilder(geoConfig).build());
            config.geoStaterConfig(new StaterConfigBuilder().prefix("geo-"));
            IniConfig afishaConfig = new IniConfig(new StringReader(
                 "limit" + " = " + 1 + "\norigin = test-afisha"
                 + "\nhost = 127.0.0.1\n" + "port = " + afisha.port()
                 + "\nconnections = 102"));
            config.afishaConfig(
                new AfishaConfigBuilder(afishaConfig).build());
            IniConfig xivaConfig = new IniConfig(new StringReader(
                "token = 123\nhost = 127.0.0.1\nport = " + xiva.port()
                + "\nconnections = 50"));
            config.xivaConfig(new XivaConfigBuilder(xivaConfig).build());
            IniConfig xivaCorpConfig = new IniConfig(new StringReader(
                "token = 223\nhost = " + xiva.host().toString()
                    + "\nconnections = 55"));
            config.xivaCorpConfig(
                new XivaConfigBuilder(xivaCorpConfig).build());
            IniConfig producerConfig = new IniConfig(new StringReader(
                HOST + HTTP_LOCALHOST + producer.port()
                     + "\nconnections = 11"
                     + "\ncache-ttl = 50"
                     + "\ncache-update-interval = 25"));
            ProducerClientConfigBuilder producerClientConfigBuilder =
                new ProducerClientConfigBuilder(producerConfig);
            config.producerClientConfig(producerClientConfigBuilder.build());
            IniConfig producerAsyncClientConfig =
                new IniConfig(new StringReader(
                    HOST + HTTP_LOCALHOST + producerAsyncClient.port()
                        + "\nconnections = 122"));
            HttpHostConfigBuilder producerAsyncClientConfigBuilder =
                new HttpHostConfigBuilder(producerAsyncClientConfig);
            config.producerAsyncClientConfig(
                    producerAsyncClientConfigBuilder.build());
            IniConfig luceneConfig = new IniConfig(new StringReader(
                 HOST + "\nconnections = 10"));
            HttpTargetConfigBuilder testLuceneConfigBuilder =
                new HttpTargetConfigBuilder(luceneConfig);
            config.searchConfig(testLuceneConfigBuilder.build());
            config.postProcessConfig(Configs.hostConfig(postProcess));
            if (storageEmulator) {
                config.mulcagateConfig(
                    Configs.hostConfig(storageCluster.mulcaGate()));
                config.cokemulatorIexlibConfig(
                    Configs.hostConfig(storageCluster.cokemulator()));
            } else {
                config.mulcagateConfig(
                    Configs.hostConfig(mulcaGate));
                config.cokemulatorIexlibConfig(
                    Configs.hostConfig(cokemulatorIexlib));
            }
            String zoraLocalhost =
                zoraProxyServer.host().getHostName()
                    + ':'
                    + zoraProxyServer.host().getPort();

            config.zoraProxyConfig(
                new ZoraProxyClientConfigBuilder()
                    .connections(2)
                    .partToDownload(1)
                    .zoraStringHosts(Collections.singletonList(zoraLocalhost)));

            config.corpFilterSearchConfig(
                new FilterSearchConfigBuilder()
                    .uri(
                        URI.create(
                            HTTP_LOCALHOST + corpFilterSearch.port()
                            + "/filter_search"))
                    .connections(2)
                    .batchSize(2));
            config.filterSearchConfig(
                new FilterSearchConfigBuilder()
                    .uri(
                        URI.create(
                            HTTP_LOCALHOST + filterSearch.port()
                            + "/filter_search"))
                    .connections(2)
                    .batchSize(2));
            config.mdbs(Pattern.compile("(mdb\\d+|pg)"));
            config.maxRetries(MAX_RETRIES);
            config.almostAllFactsTimeout(ALMOST_TIMEOUT);
            config.xIndexOperationQueueNameFacts("iex_facts");
            config.xIndexOperationQueueNameBacklog("iex_backlog");
            config.xIndexOperationQueueNameBacklog("iex_update");
            config.slowQueueName("iex-backlog");
            config.factsIndexingQueueName(QUEUE_NAME);
            config.axisQueueName("axis");

            int indexPort;
            if (tikaite != null) {
                indexPort = tikaite.port();
            } else {
                indexPort = storageCluster.tikaite().port();
            }

            String staticSearchMap =
                " shards:0-65533,host:localhost,search_port:"
                + "55327,search_port_ng:50945,json_indexer_port:"
                + indexPort + '\n';
            String luceneSearchmap =
                " shards:0-65534,host:localhost,search_port:"
                    + testLucene.searchPort()
                    + ",search_port_ng:" + testLucene.searchPort()
                    + ",json_indexer_port:"
                    + indexPort + '\n';
            if (useTestSearchBackend) {
                config.searchMapConfig(
                    new SearchMapConfigBuilder()
                        .content(
                            MailSearchDefaults.IEX
                                + luceneSearchmap
                                + MailSearchDefaults.BP_CHANGE_LOG
                                + luceneSearchmap));
            } else {
                config.searchMapConfig(
                    new SearchMapConfigBuilder()
                        .content(
                            MailSearchDefaults.IEX
                                + staticSearchMap
                                + MailSearchDefaults.BP_CHANGE_LOG
                                + staticSearchMap));
            }

            config.smartObjectConfig().uri(new URI("http://localhost:80/smart/object")).connections(2);
            final IexProxy iexProxyBackend = new IexProxy(config.build());
            iexProxyBackend.start();
            chain.get().add(iexProxyBackend);
            config =
                new IexProxyConfigBuilder(
                    new IniConfig(
                        new StringReader(
                            iexProxyConfig(
                                configExtra,
                                configPostProcessor,
                                iexProxyBackend.port()))),
                    config);

            blackbox.start();
            corpBlackbox.start();
            blackboxDirect.start();
            calendar.start();
            calendarTools.start();
            corovaneer.start();
            kinopoiskQl.start();
            producer.start();
            producerAsyncClient.start();
            filterSearch.start();
            corpFilterSearch.start();
            folders.start();
            corpFolders.start();
            labels.start();
            corpLabels.start();
            attachSid.start();
            corpAttachSid.start();
            msearch.start();
            onlineDB.start();
            axis.start();
            zoraProxyServer.start();
            factsExtractor.start();
            for (final ShinglerType shinglerType : ShinglerType.values()) {
                shinglers.get(shinglerType).start();
            }
            complaints.start();
            complaintsCoworkersSelection.start();
            sologger.start();
            freemail.start();
            msal.start();
            corpMsal.start();
            settingsApi.start();
            corpSettingsApi.start();
            iex.start();
            afisha.start();
            market.start();
            rca.start();
            media.start();
            rasp.start();
            refund.start();
            bk.start();
            xiva.start();
            taksa.start();
            taksaTesting.start();
            if (storageEmulator) {
                storageCluster.start();
            } else {
                tikaite.start();
                tikaiteMl.start();
                cokemulatorIexlib.start();
                mulcaGate.start();
            }
            mops.start();
            corpMops.start();
            neuroHards.start();
            if (logPath != null) {
                config.reqresLog().single().file(
                    new File(logPath, "tomita.log"));
                config.sobbYtLog().single().file(
                    new File(logPath, "sobb.log"));
            }

            if (useTestSearchBackend) {
                config.searchMapConfig(
                    new SearchMapConfigBuilder()
                        .content(
                            MailSearchDefaults.IEX
                                + luceneSearchmap
                                + MailSearchDefaults.BP_CHANGE_LOG
                                + luceneSearchmap));
            } else {
                config.searchMapConfig(
                    new SearchMapConfigBuilder()
                        .content(
                            MailSearchDefaults.IEX
                                + staticSearchMap
                                + MailSearchDefaults.BP_CHANGE_LOG
                                + staticSearchMap));
            }

            iexproxy = new IexProxy(config.build());
            chain.get().add(iexproxy);
            this.chain = chain.release();
        }

        System.out.println(
            "SEARCHMAP "
                + iexproxy.config().searchMapConfig().content());
    }
    //CSON: ParameterNumber

    private String replacePort(final String configExtra, final int port) {
        return configExtra.replaceAll(
            IPORT,
            Integer.toString(port));
    }

    //CSOFF: MultipleStringLiterals
    private String iexProxyConfig(
        final String configExtra,
        final UnaryOperator<String> configPostProcessor,
        final int port)
    {
        String config =
            replacePort(configExtra, port) + '\n'
            + "extrasettings.msal_ignore = true\n"
            + "extrasettings.axis-facts = *\n"
            + "extrasettings.ticket_mail = yndx.ticket@yandex.ru\n"
            + "extrasettings.ticket_travel_mail"
            + "= yndx.ticket.travel@yandex.ru\n"
            + "extrasettings.hotels_mail = yndx.hotels@yandex.ru\n"
            + "extrasettings.eshop_mail = yndx.eshop@yandex.ru\n"
            + "extrasettings.fines_mail = yndx.fines@yandex.ru\n"
            + "extrasettings.contentline_mail = yndx.contentline@yandex.ru\n"
            + "extrasettings.event_ticket_mail = yndx.event.ticket@yandex.ru\n"
            + "extrasettings.invite_mail = yndx.cal@yandex.ru\n"
            + "extrasettings.news_mail = yndx.newss@yandex.ru\n"
            + "extrasettings.news_cat_dog_mail = news.cat.dog@yandex.ru\n"
            + "extrasettings.pre_start_email_time = 130059\n"
            + "extrasettings.start_email_time = 120000\n"
            + "extrasettings.end_email_time = 120001\n"
            + "extrasettings.email_limit = 1\n"
            + "entities.default = contentline\n"
            + "entities.message-type-13 = news\n"
            + "entities.message-type-46 = unsubscribe\n"
            + "entities.message-type-27 = micro, microhtml\n"
            + "entities.message-type-2 = registration\n"
            + "entities.message-type-8 = bounce\n"
            + "entities.message-type-43 = taxi\n"
            + "entities.message-type-48 = movie, timepad\n"
            + "entities.message-type-5-48 = pkpass, micro, movie\n"
            + "entities.message-type-5-30 = pkpass, micro\n"
            + "entities.message-type-5-16 = ticket, micro, microhtml\n"
            + "entities.message-type-5-19 = ticket, micro, microhtml\n"
            + "entities.message-type-16-28 = ticket, micro, microhtml\n"
            + "entities.message-type-19-28 = ticket, micro, microhtml\n"
            + "entities.message-type-16-63 = ticket, micro, microhtml\n"
            + "entities.message-type-16-60 = ticket, micro, microhtml\n"
            + "entities.message-type-6-23 = eshop, eshop_regexp, eshop_xpath\n"
            + "entities.message-type-6-24 = eshop, eshop_regexp\n"
            + "entities.message-type-6-26 = eshop, eshop_regexp\n"
            + "entities.message-type-6-58 = eshop, eshop_regexp\n"
            + "entities.message-type-35 = hotels, micro, microhtml\n"
            + "entities.message-type-13-46 = list_unsubscribe\n"
            + "entities.message-type-62 = discount\n"
            + "entities.message-type-4 = qa-signature, snippet\n"
            + "entities.exclude-message-type-2 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-4 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-5 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-7 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-8 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-13 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-22 = eshop, eshop_regexp\n"
            + "entities.exclude-message-type-3 = events\n"
            + "entities.exclude-message-type-8 = events\n"
            + "entities.exclude-message-type-11 = events\n"
            + "entities.exclude-message-type-12 = events\n"
            + "entities.exclude-message-type-15 = events\n"
            + "entities.exclude-message-type-16 = events\n"
            + "entities.exclude-message-type-20 = events\n"
            + "entities.exclude-message-type-21 = events\n"
            + "entities.exclude-message-type-35 = ticket\n"
            + "postprocess.connections = 100\n"
            + "postprocess.default1 = _VOID:http://localhost:"
                + port + "/contentline\n"
            + "postprocess.message-type-4 = urls_info:http://localhost:"
                + port + "/people-urls, snippet:http://localhost:"
                + port + "/snippet, snippet-text:http://localhost:"
                + port + "/snippet-text\n"
            + "postprocess.message-type-13 = news:http://localhost:"
                + port + "/news, news-allimgs:http://localhost:"
                + port + "/news-allimgs, action:http://localhost:"
                + port + "/action\n"
            + "postprocess.message-type-46 = action:http://localhost:"
                + port + "/action\n"
            + "postprocess.message-type-27 = event-ticket:http://localhost:"
                + port + "/event-ticket, action:http://localhost:"
                + port + "/action, bigimage:http://localhost:"
                + port + "/bigimage\n"
            + "postprocess.message-type-42 = event-ticket:http://localhost:"
                + port + "/event-ticket\n"
            + "postprocess.message-type-48 = event-ticket:http://localhost:"
                + port + "/event-ticket\n"
            + "postprocess.message-type-5-48 = event-ticket:http://localhost:"
                + port + "/event-ticket\n"
            + "postprocess.message-type-5-30 = event-ticket:http://localhost:"
                + port + "/event-ticket\n"
            + "postprocess.message-type-2 = action:http://localhost:"
                + port + "/action\n"
            + "postprocess.message-type-7 = action:http://localhost:"
                + port + "/action\n"
            + "postprocess.message-type-6-23 = eshop:http://localhost:"
                + port + "/eshop\n"
            + "postprocess.message-type-6-24 = eshop:http://localhost:"
                + port + "/eshop\n"
            + "postprocess.message-type-6-26 = eshop:http://localhost:"
                + port + "/eshop\n"
            + "postprocess.message-type-6-58 = eshop:http://localhost:"
                + port + "/eshop\n"
            + "postprocess.message-type-8 = bounce:http://localhost:"
                + port + "/bounce\n"
            + "postprocess.message-type-5-16 = "
                + "ticket:http://localhost:" + port + "/ticket\n"
            + "postprocess.message-type-5-19 = ticket:http://localhost:"
                + port + "/ticket\n"
            + "postprocess.message-type-16-63 = ticket:http://localhost:"
                + port + "/ticket\n"
            + "postprocess.message-type-16-60 = ticket:http://localhost:"
                + port + "/ticket\n"
            + "postprocess.message-type-19-28 = ticket:http://localhost:"
                + port + "/ticket\n"
            + "postprocess.message-type-16-28 = ticket:http://localhost:"
                + port + "/ticket\n"
            + "postprocess.message-type-35 = hotels:http://localhost:"
                + port + "/hotels\n"
            + "postprocess.message-type-43 = taxi:http://localhost:"
                + port + "/taxi\n"
            + "postprocess.message-type-62 = _VOID:http://localhost:"
                + port + "/discount\n"
            + "postprocess.exclude-message-type-3 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-8 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-11 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-12 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-15 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-16 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-20 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-21 = "
                + "events:http://localhost:" + port + "/events\n"
            + "postprocess.exclude-message-type-13 = "
                + "eshop:http://localhost:" + port + "/eshop\n"
            + "postprocess.exclude-message-type-22 = "
                + "eshop:http://localhost:" + port + "/eshop\n"
            + "postprocess.exclude-message-type-35 = "
                + "ticket:http://localhost:" + port + "/ticket\n"
            + "[search]\n"
                + "connections = 10\n"
            + "[indexer]\n"
                + "connections = 10\n"
            + "[urls_whitelilst_regexp]\nfile = "
                + ru.yandex.devtools.test.Paths.getSourcePath(
                    "mail/iex/iex_proxy/test/resources/ru/yandex/iex/proxy"
                    + "/urlsWhitelistRegexp\n")
            + "[media-fiscal-rules]\nfile = "
                + ru.yandex.devtools.test.Paths.getSourcePath(
                    "mail/iex/iex_proxy/test/resources/ru/yandex/iex/proxy"
                    + "/mediaFiscal.properties") + "\n"
            + "[refund-senders-rules]\nfile = "
                + ru.yandex.devtools.test.Paths.getSourcePath(
                    "mail/iex/iex_proxy/test/resources/ru/yandex/iex/proxy"
                    + "/refundSenders.properties") + "\n"
            + "\n[iex-proxy]\n"
                + "fact-names-to-erase-fact-data = "
                + "contentline, snippet, snippet-text\n";
        return configPostProcessor.apply(config);
    }
    //CSON: MultipleStringLiterals
    //CSON: MethodLength

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer corpBlackbox() {
        return corpBlackbox;
    }

    public StaticServer blackboxDirect() {
        return blackboxDirect;
    }

    public StaticServer tikaite() {
        return tikaite;
    }

    public StaticServer tikaiteMl() {
        return tikaiteMl;
    }

    public StaticServer market() {
        return market;
    }

    public StaticServer rca() {
        return rca;
    }

    public StaticServer media() {
        return media;
    }

    public StaticServer rasp() {
        return rasp;
    }

    public StaticServer refund() {
        return refund;
    }

    public StaticServer bk() {
        return bk;
    }

    public StaticServer gatemail() {
        return gatemail;
    }

    public StaticServer gettext() {
        return gettext;
    }

    public StaticServer msearch() {
        return msearch;
    }

    public StaticServer onlineDB() {
        return onlineDB;
    }

    public StaticServer msal() {
        return msal;
    }

    public StaticServer corpMsal() {
        return corpMsal;
    }

    public StaticServer settingsApi() {
        return settingsApi;
    }

    public StaticServer corpSettingsApi() {
        return corpSettingsApi;
    }

    public StaticServer mulcaGate() {
        return mulcaGate;
    }

    public final Map<ShinglerType, TestShinglerServer> shinglers() {
        return shinglers;
    }

    public final TestShinglerServer shingler(final ShinglerType shinglerType) {
        return shinglers.get(shinglerType);
    }

    public StaticServer complaints() {
        return complaints;
    }

    public StaticServer complaintsCoworkersSelection() {
        return complaintsCoworkersSelection;
    }

    public StaticServer sologger() {
        return sologger;
    }

    public StaticServer freemail() {
        return freemail;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer producerAsyncClient() {
        return producerAsyncClient;
    }

    public TestSearchBackend testLucene() {
        return testLucene;
    }

    public StaticServer filterSearch() {
        return filterSearch;
    }

    public StaticServer corpFilterSearch() {
        return corpFilterSearch;
    }

    public StaticServer folders() {
        return folders;
    }

    public StaticServer corpFolders() {
        return corpFolders;
    }

    public StaticServer labels() {
        return labels;
    }

    public StaticServer corpLabels() {
        return corpLabels;
    }

    public StaticServer attachSid() {
        return attachSid;
    }

    public StaticServer corpAttachSid() {
        return corpAttachSid;
    }

    public IexProxy iexproxy() {
        return iexproxy;
    }

    public StaticServer cokemulatorIexlib() {
        return cokemulatorIexlib;
    }

    public StaticServer axis() {
        return axis;
    }

    public StaticServer factsExtractor() {
        return factsExtractor;
    }

    public StaticServer reminder() {
        return reminder;
    }

    public StaticServer kinopoisk() {
        return kinopoiskQl;
    }

    public StaticServer afisha() {
        return afisha;
    }

    public StaticServer xiva() {
        return xiva;
    }

    public StaticServer taksa() {
        return taksa;
    }

    public StaticServer taksaTesting() {
        return taksaTesting;
    }

    public StaticServer calendar() {
        return calendar;
    }

    public StaticServer calendarTools() {
        return calendarTools;
    }

    public StaticServer corovaneer() {
        return corovaneer;
    }

    public StaticServer kinopoiskQl() {
        return kinopoiskQl;
    }

    public StaticServer zoraProxy() {
        return zoraProxyServer;
    }

    public MailStorageCluster storageCluster() {
        return storageCluster;
    }

    public StaticServer mops() {
        return mops;
    }

    public StaticServer corpMops() {
        return corpMops;
    }

    public StaticServer neuroHards() {
        return neuroHards;
    }

    @Override
    public void close() throws IOException {
        try {
            // Wait for postactions to complete
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            // ignore
        }
        chain.close();
    }

    //test rewriting
    public boolean compareJson(
        final String outputExpectedSolution,
        final String returned,
        final boolean rewrite)
        throws Exception
    {
        String expected =
            readExpectedJson(outputExpectedSolution);
        System.out.println("/ticket returned = " + returned);
        System.out.println("/ticket expected = " + expected);
        String result = new JsonChecker(expected)
            .check(returned);
        if (result != null) {
            if (rewrite) {
                rewriteExpectedJson(outputExpectedSolution, returned);
            }
            Assert.fail(result);
            return false;
        }
        return true;
    }

    private String readExpectedJson(final String file) throws Exception {
        Path path = Paths.get(getClass().getResource(file).toURI());
        return java.nio.file.Files.readString(path);
    }

    private void rewriteExpectedJson(
        final String file,
        final String newAnswer)
        throws Exception
    {
        Path path = Paths.get(getClass().getResource(file).toURI());
        String absolutePath = path.toAbsolutePath().toString();
        System.out.println("Change answer for file " + absolutePath);
        try (FileOutputStream stream = new FileOutputStream(absolutePath)) {
            stream.write(newAnswer.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String blackboxUri(final String filter) {
        return "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
            + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall&sid=2"
            + filter;
    }

    public static String blackboxResponse(final long uid, final String addr) {
        return blackboxResponse(uid, addr, "90000");
    }

    public static String blackboxResponse(
        final long uid,
        final String addr,
        final String suid)
    {
        return blackboxResponse(uid, addr, suid, "pg");
    }

    // CSOFF: ParameterNumber
    public static String blackboxResponse(
        final long uid,
        final String addr,
        final String suid,
        final String mdb)
    {
        return "{\"users\":[{\"id\":\"" + uid + "\",\"uid\":{\"value\":\""
            + uid + "\",\"lite\":false,\"hosted\":false},\"login\":\"vp"
            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
            + "\"value\":0},\"karma_status\":{\"value\":6000},"
            + "\"address-list\":[{\"address\":\"" + addr
            + "\",\"validated\":true,\"default\":true,\"prohibit-restore\":"
            + "false,\"rpop\":false,\"unsafe\":false,\"native\":true,"
            + "\"born-date\":\"2003-09-04 21:34:25\"}],"
            + "\"dbfields\":{\"subscription.suid.2\":\"" + suid
            + "\",\"hosts.db_id.2\":\"" + mdb + "\"}}]}";
    }
    // CSON: ParameterNumber

    public void waitProducerRequests(
        final StaticServer producer,
        final String uri,
        final int count)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (producer.accessCount(uri) != count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new TimeoutException(
                    "Expecting " + count + " requests to " + uri
                    + " but got " + producer.accessCount(uri));
            }
        }
    }
}

