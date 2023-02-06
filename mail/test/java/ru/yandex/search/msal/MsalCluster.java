package ru.yandex.search.msal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.msal.mock.DbMock;
import ru.yandex.search.msal.mock.MockDriver;
import ru.yandex.search.msal.mock.TestSuiteResolver;

public class MsalCluster implements GenericAutoCloseable<IOException> {
    // CSOFF: MultipleStringLiterals
    private static final String CONFIG = Paths.getSourcePath(
        "mail/search/mail/msal/files/msal_prod.conf");

    private static final String DRIVER_FIELD = "driver";

    private final StaticServer sharpei;
    private final StaticServer sharpeiConnect;
    private final Server msal;
    private final GenericAutoCloseableChain<IOException> chain;
    private final DbMock dbMock;
    private volatile TestSuiteResolver resolver = null;

    @SuppressWarnings("JdkObsolete")
    public MsalCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            this.dbMock = new DbMock();
            System.setProperty("BSCONFIG_IDIR", "./");
            System.setProperty("SHARPEI_ORG_HOST", "localhost");
            System.setProperty("SHARPEI_HOST", "localhost");
            sharpei = new StaticServer(Configs.baseConfig("Sharpei"));
            chain.get().add(sharpei);
            sharpeiConnect = new StaticServer(Configs.baseConfig("SharpeiConnect"));
            chain.get().add(sharpeiConnect);

            IniConfig config = new IniConfig(new File(CONFIG));
            config.section("server").put("port", "0");
            config.sections().remove("accesslog");
            config.sections().remove("log");
            config.section("sharpei").put("host", sharpei.host().toString());
            config.section("sharpei-org").put(
                "host",
                sharpeiConnect.host().toString());
            IniConfig avConfig = config.section("database.aceventura");
            IniConfig pgConfig = config.section("database.postgresql");
            DriverManager.deregisterDriver(
                Class.forName(pgConfig.getString(DRIVER_FIELD))
                    .asSubclass(Driver.class).newInstance());
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                DriverManager.deregisterDriver(drivers.nextElement());
            }
            pgConfig.put(DRIVER_FIELD, MockDriver.class.getName());
            pgConfig.put("pgpass", null);
            pgConfig.put("password", "strictPass");

            avConfig.put(DRIVER_FIELD, MockDriver.class.getName());
            avConfig.put("pgpass", null);
            avConfig.put("password", "strictPass");

            msal = new ServerFactory().create(config);
            chain.get().add(msal);

            this.chain = chain.release();
        }
    }

    public synchronized void start() throws Exception {
        sharpei.start();
        sharpeiConnect.start();
        msal.start();

        if (resolver == null) {
            resolver = buildResolver();
            MockDriver.addResolver(resolver);
        }
    }

    private TestSuiteResolver buildResolver() throws Exception {
        Field tpField =
            BaseHttpServer.class.getDeclaredField("threadPool");
        tpField.setAccessible(true);
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) tpField.get(msal);
        Field workersField =
            threadPool.getClass().getDeclaredField("workers");
        workersField.setAccessible(true);

        Set<Thread> threads = new LinkedHashSet<>();
        HashSet<?> workersSet = (HashSet<?>) workersField.get(threadPool);
        for (Object worker: workersSet) {
            Field threadField =
                worker.getClass().getDeclaredField("thread");
            threadField.setAccessible(true);
            Thread thread = (Thread) threadField.get(worker);
            threads.add(thread);
        }

        return new TestSuiteResolver(dbMock, threads);
    }

    public DbMock db() {
        return dbMock;
    }

    public StaticServer sharpei() {
        return sharpei;
    }

    public Server msal() {
        return msal;
    }

    public StaticServer sharpeiConnect() {
        return sharpeiConnect;
    }

    @Override
    public void close() throws IOException {
        this.chain.close();
        MockDriver.removeResolver(resolver);
    }
    // CSON: MultipleStringLiterals
}
