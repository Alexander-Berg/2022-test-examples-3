package ru.yandex.search.mail.tupita;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.server.sync.HttpSession;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.NotFoundException;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.search.mail.tupita.config.TupitaConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.server.Server;
import ru.yandex.tikaite.server.ServerTest;

public class TupitaCluster implements GenericAutoCloseable<IOException> {
    public static final String APE_TVM2_TICKET = "3:serv:TIKAITEn";

    // CSOFF: MultipleStringLiterals
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/mail/search_backend_mail_config/files"
            + "/tupita_search_backend.conf");
    private static final String TUPITA_CONFIG =
        Paths.getSourcePath(
            "mail/search/mail/tupita/files/mail_search_delivery.conf");

    private static final String PORT = "port";

    private final StaticServer tikaiteSrwProxy;
    private final Server tikaiteServer;
    private final StaticServer ljinx;
    private final Tupita tupita;
    private final StaticServer tvm;

    private final GenericAutoCloseableChain<IOException> chain;
    private final LinkedHashMap<String, HttpEntity> stidEntityMap
        = new CommonsLinkedHashMap<>();

    static {
        System.setProperty(
            "LUCENE_FIELDS_CONFIG_DIR",
            Paths.getSourcePath(
                "mail/search/mail/search_backend_mail_config/files"));
    }

    public TupitaCluster(final TestBase testBase) throws Exception {
        this(testBase, "");
    }

    public TupitaCluster(
        final TestBase testBase,
        final String overrides)
        throws Exception
    {
        this(testBase, overrides, false);
    }

    public TupitaCluster(
        final TestBase testBase,
        final String overrides,
        final boolean realTikaite)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            if (!realTikaite) {
                tikaiteSrwProxy = new StaticServer(Configs.baseConfig("Tikaite"));
                chain.get().add(tikaiteSrwProxy);
                tikaiteSrwProxy.start();

                tikaiteServer = null;
            } else {
                tikaiteServer = new Server(ServerTest.getConfig(1));
                chain.get().add(tikaiteServer);
                tikaiteServer.start();
                tikaiteSrwProxy = new StaticServer(Configs.baseConfig());
                chain.get().add(tikaiteSrwProxy);
                tikaiteSrwProxy.start();
                tikaiteSrwProxy.add(
                    "/mail/handler*",
                    new StaticHttpResource(new TikaiteSrwProxyHandler(tikaiteServer.port())));
            }

            tvm = new StaticServer(Configs.baseConfig("Tvm"));
            chain.get().add(tvm);
            tvm.add("/ticket/", HttpStatus.SC_OK);
            tvm.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"" + APE_TVM2_TICKET + "\"}}");
            tvm.start();

            ljinx = new StaticServer(Configs.baseConfig());
            chain.get().add(ljinx);
            ljinx.start();

            System.setProperty("ROBOT_KEY", "");
            System.setProperty("SECRET", "1234567890123456789011");
            System.setProperty("BSCONFIG_INAME", "tupita");
            System.setProperty("BSCONFIG_IDIR", ".");
            System.setProperty("TVM_CLIENT_ID", "4");
            System.setProperty("TIKAITE_HOST", "");
            System.setProperty("LOGS_BASE_DIR", "");
            System.setProperty("PRODUCER_HOST", "localhost");
            System.setProperty("YT_ACCESS_LOG", "");
            System.setProperty("TVM_API_HOST", tvm.host().toString());
            System.setProperty(
                "BSCONFIG_IPORT",
                String.valueOf(0));

            IniConfig tupitaIniConfig = new IniConfig(new File(TUPITA_CONFIG));
            if (!overrides.isEmpty()) {
                IniConfig overrideConfig =
                    new IniConfig(
                        new BufferedReader(new StringReader(overrides)));
                tupitaIniConfig =
                    overrideConfig(tupitaIniConfig, "", overrideConfig);
            }

            TupitaConfigBuilder tupitaConfigBuilder =
                new TupitaConfigBuilder(
                    patchConfig(tupitaIniConfig));

            Path indexPath =
                Files.createTempDirectory(testBase.testName.getMethodName());
            tupitaConfigBuilder.luceneConfig(
                TestSearchBackend.patchConfig(
                    indexPath,
                    new IniConfig(new File(SEARCH_BACKEND_CONFIG))));
            tupitaConfigBuilder.searchMapConfig(new SearchMapConfigBuilder().content(""));

            tupitaConfigBuilder.tvm2ServiceConfig().host(tvm.host());

            tupita = new Tupita(tupitaConfigBuilder.build());
            chain.get().add(tupita);
            tupita.start();

            ljinx.add(
                "/fat-check*",
                new StaticHttpResource(new ProxyHandler(tupita.host())));

            this.chain = chain.release();
        }
    }

    private IniConfig overrideConfig(
        final IniConfig config,
        final String prefix,
        final IniConfig override)
        throws ConfigException
    {
        for (Map.Entry<String, IniConfig> section
            : override.sections().entrySet())
        {
            overrideConfig(
                config,
                prefix + section.getKey() + '.',
                section.getValue());
        }

        for (String key: override.keys()) {
            config.put(prefix + key, override.getString(key));
        }

        System.out.println(
            "Overriden config " + config + " overiders " + override);
        return config;
    }

    private IniConfig patchConfig(
        final IniConfig config)
        throws IOException
    {
        config.sections().remove("accesslog");
        config.sections().remove("log");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        config.sections().remove("searchmap");
        config.sections().remove("mop");
        config.sectionOrNull("server").put(PORT, "0");
        config.sections().remove("lucene");
        IniConfig fatConfig = config.sectionOrNull("fat");
        fatConfig.put("proxy", null);
        fatConfig.put("host", ljinx.host().toString());
        IniConfig tikaiteConfig = config.sectionOrNull("tikaite");
        tikaiteConfig.put("proxy", null);
        tikaiteConfig.put("host", "localhost");
        tikaiteConfig.put(PORT, String.valueOf(tikaiteSrwProxy.port()));

        return config;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public void addStid(final String stid, final String resource) throws IOException {
        String resourceString = IOStreamUtils.consume(
            new InputStreamReader(
                this.getClass().getResourceAsStream(resource),
                StandardCharsets.UTF_8))
            .toString();
        addStid(stid, new StringEntity(resourceString, StandardCharsets.UTF_8));
    }

    public void addStid(final String stid, final HttpEntity entity) {
        stidEntityMap.put(stid, entity);
    }

    public StaticServer tikaite() {
        return tikaiteSrwProxy;
    }

    public Tupita tupita() {
        return tupita;
    }

    private final class TikaiteSrwProxyHandler extends ProxyHandler {
        public TikaiteSrwProxyHandler(int port) {
            super(port);
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            HttpSession session = new HttpSession(request, response, context);
            String stid = session.params().getString("stid");
            HttpEntity entity = stidEntityMap.get(stid);
            if (entity == null) {
                throw new NotFoundException(
                    "Stid not found " + stid + " available: " + stidEntityMap.keySet());
            }

            HttpPost post = new HttpPost(request.getRequestLine().getUri());
            post.setEntity(entity);
            for (Header sourceHeader: request.getAllHeaders()) {
                if (sourceHeader != null) {
                    post.addHeader(sourceHeader);
                }
            }

            super.handle(post, response, context);
        }
    }
    // CSON: MultipleStringLiterals
}
