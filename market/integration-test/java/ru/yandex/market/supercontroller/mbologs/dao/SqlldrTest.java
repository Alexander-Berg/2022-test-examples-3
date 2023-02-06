package ru.yandex.market.supercontroller.mbologs.dao;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.supercontroller.mbologs.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;
import ru.yandex.market.supercontroller.mbologs.dao.oracle.OracleTestData;
import ru.yandex.market.supercontroller.mbologs.dao.providers.ProtoFileRowProvider;
import ru.yandex.market.supercontroller.mbologs.model.SessionConfiguration;
import ru.yandex.market.supercontroller.mbologs.model.generation_data.GenerationDataFromProtoConverter;
import ru.yandex.market.supercontroller.mbologs.model.generation_data.GenerationDataOffer;
import ru.yandex.market.supercontroller.mbologs.parallel.LogCopyStrategy;
import ru.yandex.market.supercontroller.mbologs.parallel.publishers.RowPublisher;
import ru.yandex.market.supercontroller.mbologs.workers.RowProviderFactory;
import ru.yandex.market.supercontroller.mbologs.workers.generation_data.ToOracleGenerationDataSqlldrFileRowSaverFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;


/**
 * This test requires sqlldr_11_2_0_4 binary from paysys-oracle-instant-client-11204 deb package.
 *
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MboLogsIntegrationTestConfig.class)
public class SqlldrTest {

    private final Logger log = Logger.getLogger(getClass());

    private final GenerationDataFromProtoConverter fromProtoConverter = new GenerationDataFromProtoConverter();

    private static final int JOB_CHECK_INTERVAL = 5;

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Autowired
    private OracleTestData oracleTestData;

    @Autowired
    private ToOracleGenerationDataSqlldrFileRowSaverFactory sqlldrTestSaverFactory;

    @Resource(name = "blockingQueueStrategy")
    private LogCopyStrategy<GenerationDataOffer> logCopyStrategy;

    @Autowired
    private OraclePartitionManager oraclePartitionManager;

    private static Path tmpDir;
    private static Path dataFile;
    private static Path sqlldrScript;

    @BeforeClass
    public static void setUp() throws Exception {
        File binDir = TEMPORARY_FOLDER.newFolder("bin");

        sqlldrScript = binDir.toPath().resolve("mbo-logs-sqlldr.sh");
        copyResourceContent("/mbo-logs-sqlldr.sh", sqlldrScript, true);

        File confDir = TEMPORARY_FOLDER.newFolder("conf");

        Path tnsNames = confDir.toPath().resolve("tnsnames.ora");
        copyResourceContent("/mbo-logs/integration/tnsnames.ora", tnsNames, false);
        System.setProperty("oracle.net.tns_admin", tnsNames.getParent().toString());

        File dataDir = TEMPORARY_FOLDER.newFolder("data");
        dataFile = dataDir.toPath().resolve("offers.proto.gz");
        copyResourceContent("/mbo-logs/integration/data/offers_1000.proto.gz", dataFile, false);

        tmpDir = TEMPORARY_FOLDER.newFolder("tmp").toPath();
    }


    @Before
    public void init() {
        sqlldrTestSaverFactory.setTmpDir(tmpDir.toString());

        oraclePartitionManager.setOracleJobCheckInterval(JOB_CHECK_INTERVAL);
        oraclePartitionManager.setIndexCheckInterval(JOB_CHECK_INTERVAL);

        oracleTestData.createTestTable();
        oracleTestData.fillScLogPartitions();
    }

    public static void copyResourceContent(String resourceName, Path destination,
                                           boolean executable) throws IOException {
        try (InputStream is = SqlldrTest.class.getResourceAsStream(resourceName)) {
            IOUtils.copy(is, new FileOutputStream(destination.toFile()));
        }
        if (executable) {
            boolean isExecutable = destination.toFile().setExecutable(true);
            Assert.assertTrue(isExecutable);
        }
    }

    @After
    public void onTearDown() throws Exception {
        oracleTestData.cleanScLogPartitions();
        oracleTestData.deleteTestTable();
    }

    @Test
    public void empty() {

    }

    public Set<String> getSessions(RowProviderFactory<GenerationDataOffer> provider) {
        final Set<String> set = new HashSet<>();
        List<Runnable> runnables = provider.getRowProviders(
            () -> (dataRow -> set.add(dataRow.getSessionId())),
            new CountDownLatch(provider.getProducerThreadCount())
        );
        runnables.forEach(Runnable::run);
        return set;
    }

    @Test
    public void doTest2() throws InterruptedException, IOException {
        RowProviderFactory<GenerationDataOffer> provider = getProviderFactory(
            dataFile.toString(), 0, fromProtoConverter
        );

        Set<String> sessions = getSessions(provider);

        SessionConfiguration config = oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(),
            Collections.min(sessions),
            Collections.max(sessions),
            sessions,
            false,
            true
        );

        sqlldrTestSaverFactory.setConfiguration(config);
        sqlldrTestSaverFactory.prepareGenerationFile();

        boolean isOk = logCopyStrategy.copy(provider, sqlldrTestSaverFactory);
        if (!isOk) {
            throw new RuntimeException();
        }

        sqlldrTestSaverFactory.doSqlldr();

        sqlldrTestSaverFactory.removeGenerationFile();
    }

    private static <T> RowProviderFactory<T> getProviderFactory(
        String fileName, int skipOffers,
        final Converter<OffersStorage.GenerationDataOffer, T> converter) {

        RowProviderFactory<T> provider = new RowProviderFactory<T>() {
            @Override
            public Runnable getRowProvider(RowPublisher<T> publisher, CountDownLatch finished) {

                return new ProtoFileRowProvider<>(
                    fileName,
                    OffersStorage.GenerationDataOffer.PARSER,
                    converter,
                    finished, publisher,
                    skipOffers, 0, 0
                );
            }
        };
        return provider;
    }

}
