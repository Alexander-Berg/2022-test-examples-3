package ru.yandex.market.supercontroller.mbologs.dao;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.date.TimerUtils;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.supercontroller.mbologs.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;
import ru.yandex.market.supercontroller.mbologs.dao.savers.ProtoFileRowSaver;
import ru.yandex.market.supercontroller.mbologs.dao.savers.RowSaver;
import ru.yandex.market.supercontroller.mbologs.model.generation_data.GenerationDataOffer;
import ru.yandex.market.supercontroller.mbologs.model.generation_data.GenerationDataToProtoConverter;
import ru.yandex.market.supercontroller.mbologs.parallel.publishers.RowPublisher;
import ru.yandex.market.supercontroller.mbologs.workers.generation_data.FromYtGenerationDataRowProviderFactory;
import ru.yandex.market.supercontroller.mbologs.workers.stat.StatCounter;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MboLogsIntegrationTestConfig.class)
public class YtRowProviderTest {

    private final Logger log = Logger.getLogger(getClass());

    private static final int TIMEOUT_MINS = 10;

    private static final int MIN_ROWS = 200_000;

    private static final int MAX_ROWS = 200_000;

    private static final int MAX_ROWS_TO_FILE = 1_000;

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Autowired
    private FromYtGenerationDataRowProviderFactory factory;

    @Autowired
    private StatCounter<GenerationDataOffer> statCounter;

    @Resource(name = "ytIndexerRelatedHttpApi")
    private Yt yt;

    @Value("${mbo.yt.sc.offers.path}")
    private String scOffersPath;

    @Test
    public void testPublish() throws InterruptedException {
        log.info("[yt test started]");

        long ts = System.currentTimeMillis();

        YPath sourceTable = YtQueueReaderTest.getGenerationTable(yt, scOffersPath, "mbo_offers_mr", MIN_ROWS);
        String tableSessionId = Generations.getTableSessionId(sourceTable.toString());

        log.info("scOffersPath is " + scOffersPath);
        log.info("sourceTableName is " + sourceTable.toString());
        log.info("tableSessionId is " + tableSessionId);

        factory.setBaseSession(null);
        factory.setSessionsToCopy(null);
        factory.setSourceTable(sourceTable.toString());

        final CountDownLatch dataProviderCompletion = new CountDownLatch(factory.getProducerThreadCount());

        final RowSaver<GenerationDataOffer> saver = CollectionUtils.first(CollectionUtils.first(
                statCounter.getAllRowSavers()
        ));

        File offersFile = TEMPORARY_FOLDER.getRoot().toPath().resolve("offers_1000.proto.gz").toFile();

        try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(offersFile))) {

            final ProtoFileRowSaver<GenerationDataOffer, OffersStorage.GenerationDataOffer> fileSaver =
                new ProtoFileRowSaver<>(os, new GenerationDataToProtoConverter(), 100);

            final AtomicInteger ii = new AtomicInteger();
            final Supplier<RowPublisher<GenerationDataOffer>> publishers = () ->
                    (RowPublisher<GenerationDataOffer>) dataRow -> {
                        int i = ii.incrementAndGet();
                        saver.insert(Collections.singleton(dataRow));
                        if (i <= MAX_ROWS_TO_FILE) {
                            fileSaver.insert(Collections.singleton(dataRow));
                        }
                        if (i >= MAX_ROWS) {
                            IntStream.range(0, factory.getProducerThreadCount())
                                    .forEach((s) -> dataProviderCompletion.countDown());
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("Test exception. " + MAX_ROWS + " rows reached.");
                        }
                    };

            ExecutorService e = Executors.newFixedThreadPool(1 + factory.getProducerThreadCount());
            e.execute(statCounter.createLogger());

            List<Runnable> providers = factory.getRowProviders(publishers, dataProviderCompletion);
            providers.forEach(e::execute);

            boolean timeout = !dataProviderCompletion.await(TIMEOUT_MINS, TimeUnit.MINUTES);
            if (timeout) {
                log.error("Process timeouted");
            }

            e.shutdownNow();

            log.debug(ii.get() + " rows processed in " + TimerUtils.pastTimeWithMetric(ts));

            Assert.assertEquals(dataProviderCompletion.getCount(), 0);
            Assert.assertTrue(ii.get() >= MAX_ROWS);
            Assert.assertTrue(ii.get() <= MAX_ROWS + factory.getProducerThreadCount() - 1);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("[yt test finished]");

    }

}
