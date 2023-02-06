package ru.yandex.mail.so.logger;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import ru.yandex.logger.PrefixedLogger;
import ru.yandex.mail.so.logger.config.AbstractRulesStatDatabaseConfig;
import ru.yandex.mail.so.logger.config.RulesStatDatabaseConfig;
import ru.yandex.mail.so.logger.config.RulesStatDatabasesConfig;
import ru.yandex.mail.so.logger.config.RulesStatDatabasesConfigBuilder;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class SpLoggerRulesStatCluster extends SpLoggerCluster
{
    public SpLoggerRulesStatCluster(final TestBase testBase) throws Exception {
        super(
            testBase,
            DEFAULT_BATCH_MIN_SIZE,
            Route.IN,
            LogStorageType.MDS,
            AuxiliaryStorageType.LOGS_CONSUMER,
            RulesStatDatabaseType.MONGODB);
    }

    public SpLoggerRulesStatCluster(final TestBase testBase, final long batchMinSize) throws Exception
    {
        super(
            testBase,
            batchMinSize,
            Route.IN,
            LogStorageType.MDS,
            AuxiliaryStorageType.LOGS_CONSUMER,
            RulesStatDatabaseType.MONGODB);
    }

    public SpLoggerRulesStatCluster(final TestBase testBase, final long batchMinSize, final Route route) throws Exception
    {
        super(
            testBase,
            batchMinSize,
            route,
            LogStorageType.MDS,
            AuxiliaryStorageType.LOGS_CONSUMER,
            RulesStatDatabaseType.MONGODB);
    }

    public SpLoggerRulesStatCluster(
        final TestBase testBase,
        final long batchMinSize,
        final Route route,
        final LogStorageType logStorageType,
        final AuxiliaryStorageType auxiliaryStorageType,
        final RulesStatDatabaseType rulesStatDatabaseType)
        throws Exception
    {
        super(testBase, batchMinSize, route, logStorageType, auxiliaryStorageType, rulesStatDatabaseType);
    }

    @Override
    protected Cluster createDbCluster(final PrefixedLogger logger) throws IOException {
        return new RealMongoDbCluster(logger);
    }

    @Override
    protected RulesStatDatabasesConfig<BasicRoutedLogRecordProducer> createRulesStatDbConfig()
        throws ConfigException, IOException
    {
        logger.info("SpLoggerRulesStatCluster.createRulesStatDbConfig");
        IniConfig rulesStatDatabaseConfig = new IniConfig(new StringReader(
            "[" + rulesStatSectionName + "]"
                + "\ntype = " + this.rulesStatDatabaseType.name().toLowerCase(Locale.ROOT)
                + "\nworkers = 128"
                + "\nbatch-min-size = 1000"
                + "\nbatch-save-period = 10s"
                + "\nbatch-save-retries = 3"
                + "\nsaving-operation-timeout = 10s"
                + "\nsaving-method = sync-requests"
                + "\n[" + rulesStatSectionName + "." + AbstractRulesStatDatabaseConfig.DATABASE + "]"
                + "\nport = " + db.port()
                + "\ndb = " + RealMongoDbCluster.DB_RULES
                + "\nuser = " + RealMongoDbCluster.DB_USER
                + "\nsecret = " + RealMongoDbCluster.DB_PASSWD
                + "\n"));
        RulesStatDatabasesConfigBuilder builder = new RulesStatDatabasesConfigBuilder(rulesStatDatabaseConfig);
        rulesStatDatabaseConfig.checkUnusedKeys();
        return builder;
    }

    public RulesStatDatabaseConfig<BasicRoutedLogRecordProducer> rulesStatDatabaseConfig() {
        return spLogger.rulesStatDatabasesConfig().rulesStatDatabases().get(rulesStatSectionName);
    }
}
