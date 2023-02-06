package ru.yandex.antifraud;

import java.io.File;
import java.io.IOException;

import ru.yandex.antifraud.config.AntiFraudConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.familypay.backend.FamilypayCluster;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.util.TestBase;

public class Cluster extends GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseableChain<IOException>> {
    public static final String TVM_CLIENT_ID = "2019898";
    public static final String TVM_SECRET = "1234567890123456789011";
    public static final String TRUST_TVM_CLIENT_ID = "2001796";
    public static final String TRUST_TVM_TICKET = "here the ticket";
    public static final String BLACKBOX_TVM_CLIENT_ID = "222";
    public static final String BLACKBOX_TVM_TICKET = "here the blackbox ticket";
    public static final String PASSPORT_TVM_CLIENT_ID = "2000078";
    public static final String PASSPORT_TVM_TICKET = "here the passport ticket";
    public static final String PHARMA_TVM_CLIENT_ID = "2024479";
    public static final String PHARMA_TVM_TICKET = "here the pharma ticket";
    public static final String TRUST_API_TVM_CLIENT_ID = "2001798";
    public static final String TRUST_API_TVM_TICKET = "here the trust ticket";
    public static final String FAMILY_API_TVM_CLIENT_ID = "2021718";
    public static final String FAMILY_API_TVM_TICKET = "here the family ticket";
    public static final String BILLING_TVM_CLIENT_ID = "2000497";
    public static final String MARKET_TVM_CLIENT_ID = "2010064";
    public static final String CARGO_CRM_CLIENT_ID = "2029038";

    private final AntiFraudHttpServer server;
    private final StaticServer storageSave;
    private final StaticServer storageAggregation;
    private final StaticServer rbl;
    private final StaticServer blackbox;
    private final StaticServer fury;
    private final StaticServer passport;
    private final StaticServer dedupliactedPassport;
    private final StaticServer pharma;
    private final StaticServer trust;
    private final FamilypayCluster familypayCluster;
    private final StaticServer cache;

    public Cluster(TestBase env, String rulesConf) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            final StaticServer tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);

            storageSave = new StaticServer(Configs.baseConfig("StorageSaveServer"));
            chain.get().add(storageSave);
            System.setProperty("STORAGE_SAVE_HOST", storageSave.host().toString());

            storageAggregation = new StaticServer(Configs.baseConfig("StorageAggregationServer"));
            chain.get().add(storageAggregation);
            System.setProperty("STORAGE_AGGREGATION_HOST", storageAggregation.host().toString());

            rbl = new StaticServer(Configs.baseConfig("RblServer"));
            chain.get().add(rbl);
            System.setProperty("RBL_HOST", rbl.host().toString());

            blackbox = new StaticServer(Configs.baseConfig("Blackbox"));
            chain.get().add(blackbox);

            fury = new StaticServer(Configs.baseConfig("Fury"));
            chain.get().add(fury);

            passport = new StaticServer(Configs.baseConfig("Passport"));
            chain.get().add(passport);

            dedupliactedPassport = new StaticServer(Configs.baseConfig("DeduplicatedPassport"));
            chain.get().add(dedupliactedPassport);

            pharma = new StaticServer(Configs.baseConfig("Pharma"));
            chain.get().add(pharma);

            chain.get().add(trust = new StaticServer(Configs.baseConfig("Trust")));

            chain.get().add(familypayCluster = new FamilypayCluster(env));

            chain.get().add(cache = new StaticServer(Configs.baseConfig("Cache")));

            System.setProperty("NANNY_SERVICE_ID", "so_fraud");
            System.setProperty("BINBASE_PATH", env.resource("binbase.txt").toString());
            System.setProperty("BLACKBOX_HOST", blackbox.host().toString());
            System.setProperty("BLACKBOX_TVM_CLIENT_ID", BLACKBOX_TVM_CLIENT_ID);
            System.setProperty("FURY_HOST", fury.host().toString());
            System.setProperty("PASSPORT_HOST", passport.host().toString());
            System.setProperty("DEDUPLCATED_PASSPORT_HOST", dedupliactedPassport.host().toString());
            System.setProperty("PASSPORT_TVM_CLIENT_ID", PASSPORT_TVM_CLIENT_ID);

            System.setProperty("PHARMA_HOST", pharma.host().toString());
            System.setProperty("PHARMA_TVM_CLIENT_ID", PHARMA_TVM_CLIENT_ID);

            System.setProperty("TRUST_HOST", trust.host().toString());
            System.setProperty("TRUST_TVM_CLIENT_ID", TRUST_API_TVM_CLIENT_ID);

            System.setProperty("FAMILY_HOST", familypayCluster.familypayBackend().httpHost().toString());
            System.setProperty("FAMILY_TVM_CLIENT_ID", FAMILY_API_TVM_CLIENT_ID);

            System.setProperty("CACHE_HOST", cache.host().toString());

            System.setProperty("BILLING_TVM_CLIENT_ID", BILLING_TVM_CLIENT_ID);

            System.setProperty("MARKET_TVM_CLIENT_ID", MARKET_TVM_CLIENT_ID);

            System.setProperty("CARGO_CRM_CLIENT_ID", CARGO_CRM_CLIENT_ID);

            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("TVM_SECRET", TVM_SECRET);

            System.setProperty("ANTIFRAUD_PORT", Integer.toString(0));
            System.setProperty("ANTIFRAUD_HTTPS_PORT", Integer.toString(0));
            System.setProperty("SERVER_NAME", "localhost");
            System.setProperty("JKS_PASSWORD", "password");

            System.setProperty("CARD_FILTER_TVM_ID", "2021243");

            System.setProperty("PUSHCLIENT_DEFAULT_LOG_NAME", "/logs/push.log");

            System.setProperty("NANNY_SERVICE_ID", "so_fraud");

            System.setProperty("LPM_TVM_CLIENT", "");

            System.setProperty("ALLOWED_SRCS", "");
            System.setProperty("LIST_ALLOWED_SRCS", "");

            System.setProperty("RULES_CONFIG_PATH", rulesConf);

            System.setProperty("SERVICES_MAPPING_PATH", env.resource("services_mapping.json/0_upload_file").toAbsolutePath().toString());
            System.setProperty("FPAY_PUSH_NOTIFICATION_TEMPLATE_PATH", env.resource("PushNotification.json/new_data").toAbsolutePath().toString());

            final IniConfig ini =
                    new IniConfig(
                            new File(Paths.getSourcePath("mail/so/daemons/antifraud/antifraud_config/files/antifraud" +
                                    ".conf")));
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.sections().remove("delivery-log");
            ini.section("pharma").sections().remove("https");
            ini.section("fury").sections().remove("https");
            ini.section("trust").sections().remove("https");
            ini.section("family").sections().remove("https");
            ini.section("server").sections().remove("free-space-signals");
            ini.section("server").sections().remove("https");

            ini.sections().remove("models");
            ini.section("models").section("frodo").put("model",
                    env.resource("tcp_model.bin").toAbsolutePath().toString());
            ini.section("models").section("frodo").put("dict", env.resource("cd.txt").toAbsolutePath().toString());

            ini.sections().remove("auth");

            ini.section("uatraits").put("path",
                    env.resource("metrika/uatraits/data/browser.xml").toAbsolutePath().toString());

            ini.section("currencies").put("rates-path",
                    env.resource("currencies_rate.json.txt/0_upload_file").toAbsolutePath().toString());

            AntiFraudConfigBuilder builder = new AntiFraudConfigBuilder(ini);
            ini.checkUnusedKeys();

            tvm2.add(
                    "/2/keys/?lib_version=" + Version.get(),
                    IOStreamUtils.consume(
                                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                            .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                    "/2/ticket/",
                    "{\"" + BLACKBOX_TVM_CLIENT_ID + "\":{\"ticket\":\"" + BLACKBOX_TVM_TICKET + "\"}," +
                            "\"" + TRUST_TVM_CLIENT_ID + "\":{\"ticket\":\"" + TRUST_TVM_TICKET + "\"}," +
                            "\"" + PASSPORT_TVM_CLIENT_ID + "\":{\"ticket\":\"" + PASSPORT_TVM_TICKET + "\"}," +
                            "\"" + TRUST_API_TVM_CLIENT_ID + "\":{\"ticket\":\"" + TRUST_API_TVM_TICKET + "\"}," +
                            "\"" + PHARMA_TVM_CLIENT_ID + "\":{\"ticket\":\"" + PHARMA_TVM_TICKET + "\"}," +
                            "\"" + FAMILY_API_TVM_CLIENT_ID + "\":{\"ticket\":\"" + FAMILY_API_TVM_TICKET + "\"}" +
                            "}");
            tvm2.start();

            server = new AntiFraudHttpServer(builder.build());
            chain.get().add(server);
            reset(chain.release());
        }
    }

    public void start() throws Exception {
        storageSave.start();
        storageAggregation.start();
        rbl.start();
        blackbox.start();
        fury.start();
        passport.start();
        pharma.start();
        trust.start();
        familypayCluster.start();
        cache.start();
        dedupliactedPassport.start();

        server.start();
    }

    @Override
    public void close() throws IOException {
        try {
            // Wait for postactions to complete
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            // ignore
        }
        super.close();
    }

    public AntiFraudHttpServer server() {
        return server;
    }

    public StaticServer getStorageSave() {
        return storageSave;
    }

    public StaticServer getStorageAggregation() {
        return storageAggregation;
    }

    public StaticServer getRbl() {
        return rbl;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer dedupliactedPassport() {
        return dedupliactedPassport;
    }

    public StaticServer passport() {
        return passport;
    }

    public StaticServer pharma() {
        return pharma;
    }

    public StaticServer trust() {
        return trust;
    }

    public FamilypayCluster familypayCluster() {
        return familypayCluster;
    }

    public StaticServer cache() {
        return cache;
    }
}

