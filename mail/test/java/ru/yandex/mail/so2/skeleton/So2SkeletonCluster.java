package ru.yandex.mail.so2.skeleton;

import java.io.File;
import java.io.IOException;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.so2.skeleton.config.So2SkeletonConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;

public class So2SkeletonCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String TVM_CLIENT_ID = "2001103";
    public static final String TVM_SECRET = "1234567890123456789011";
    public static final String BLACKBOX_TVM_CLIENT_ID = "222";
    public static final String BLACKBOX_TVM_TICKET = "here the ticket";
    public static final String CORP_BLACKBOX_TVM_CLIENT_ID = "223";
    public static final String CORP_BLACKBOX_TVM_TICKET =
        "here the corp ticket";

    private final StaticServer tvm2;
    private final StaticServer blackbox;
    private final StaticServer corpBlackbox;
    private final So2SkeletonTestServer skeleton;

    public So2SkeletonCluster() throws Exception {
        this("mail/so/daemons/so2/skeleton/test/configs/skeleton.conf");
    }

    public So2SkeletonCluster(final String configPath) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);

            blackbox = new StaticServer(Configs.baseConfig("Blackbox"));
            chain.get().add(blackbox);

            corpBlackbox =
                new StaticServer(Configs.baseConfig("CorpBlackbox"));
            chain.get().add(corpBlackbox);

            System.setProperty("NANNY_SERVICE_ID", "spdaemon-in");
            System.setProperty("SKELETON_PORT", Integer.toString(0));
            System.setProperty("ROOT", ".");
            System.setProperty("ROUTE", "in");
            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", TVM_SECRET);
            System.setProperty("BLACKBOX_HOST", blackbox.host().toString());
            System.setProperty(
                "BLACKBOX_TVM_CLIENT_ID",
                BLACKBOX_TVM_CLIENT_ID);
            System.setProperty(
                "CORP_BLACKBOX_HOST",
                corpBlackbox.host().toString());
            System.setProperty(
                "CORP_BLACKBOX_TVM_CLIENT_ID",
                CORP_BLACKBOX_TVM_CLIENT_ID);
            System.setProperty("PANEL_TITLE", "spdaemon-in");
            System.setProperty(
                "PANEL_TAG",
                "itype=spdaemon;prj=so;nanny=spdaemon-in*;ctype=prod");
            System.setProperty("CPU_CORES", "4");
            System.setProperty("TIMER_RESOLUTION", "100ms");
            IniConfig ini =
                new IniConfig(new File(Paths.getSourcePath(configPath)));
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.section("server").sections().remove("free-space-signals");

            IniConfig mainSection =
                ini.sectionOrNull("extract-modules.extract-module.main");
            if (mainSection != null) {
                mainSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/so2/skeleton/test/configs/"
                        + mainSection.getString("dsl-script")));
            }

            So2SkeletonConfigBuilder builder =
                new So2SkeletonConfigBuilder(ini);
            ini.checkUnusedKeys();

            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"" + BLACKBOX_TVM_CLIENT_ID + "\":{\"ticket\":\""
                + BLACKBOX_TVM_TICKET + "\"},\""
                + CORP_BLACKBOX_TVM_CLIENT_ID + "\":{\"ticket\":\""
                + CORP_BLACKBOX_TVM_TICKET + "\"}}");
            tvm2.start();

            skeleton =
                new So2SkeletonTestServer(
                    new So2SkeletonConfigBuilder(builder).build());
            chain.get().add(skeleton);
            reset(chain.release());
        }
    }

    public void start() throws IOException {
        blackbox.start();
        corpBlackbox.start();
        skeleton.start();
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer corpBlackbox() {
        return corpBlackbox;
    }

    public So2SkeletonTestServer skeleton() {
        return skeleton;
    }
}

