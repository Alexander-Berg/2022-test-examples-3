package ru.yandex.market.supercontroller.mbologs.dao;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.common.util.date.TimerUtils;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.supercontroller.mbologs.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;
import ru.yandex.market.supercontroller.mbologs.dao.savers.RowSaver;
import ru.yandex.market.supercontroller.mbologs.model.generation_data.GenerationDataOffer;
import ru.yandex.market.supercontroller.mbologs.parallel.BlockingQueueStrategy;
import ru.yandex.market.supercontroller.mbologs.workers.RowSaverFactory;
import ru.yandex.market.supercontroller.mbologs.workers.generation_data.FromYtGenerationDataRowProviderFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MboLogsIntegrationTestConfig.class)
public class YtQueueReaderTest {

    private final Logger log = Logger.getLogger(getClass());

    private static final int SAVER_DELAY = 1000;

    private static final int MIN_ROWS = 2_000;

    private static final int MAX_ROWS = 20_000;

    @Autowired
    private FromYtGenerationDataRowProviderFactory factory;

    @Autowired
    private BlockingQueueStrategy<GenerationDataOffer> queueStrategy;

    @Resource(name = "ytIndexerRelatedHttpApi")
    private Yt yt;

    @Value("${mbo.yt.sc.offers.path}")
    private String scOffersPath;

    @Value("${mbo-logs.ytChunkSize}")
    private int ytChunkSize;

    @Value("${mbo-logs.yt.writeThreads}")
    private int writeThreads;

    @Test
    public void testPublish() throws InterruptedException {
        log.info("[yt test started]");

        long ts = System.currentTimeMillis();
        YPath sourceTableName = getGenerationTable(yt, scOffersPath, "mbo_offers_mr", MIN_ROWS);
        String tableSessionId = Generations.getTableSessionId(sourceTableName.toString());

        log.info("scOffersPath is " + scOffersPath);
        log.info("sourceTableName is " + sourceTableName);
        log.info("tableSessionId is " + tableSessionId);

        factory.setBaseSession(null);
        factory.setSessionsToCopy(null);
        factory.setSourceTable(sourceTableName.toString());

        SaverFactory saverFactory = new SaverFactory();

        queueStrategy.copy(factory, saverFactory);

        int rowsSaved = saverFactory.getCount();
        log.debug(rowsSaved + " rows processed in " + TimerUtils.pastTimeWithMetric(ts));

        Assert.assertTrue(rowsSaved >= MAX_ROWS);
        log.info("[yt test finished]");
    }

    public static YPath getGenerationTable(Yt yt, String scOffersPath, String tableName, int minRows) {
        Cypress cypress = yt.cypress();
        ListF<YTreeStringNode> sessionDirs = cypress.list(YPath.simple(scOffersPath));

        final Map<String, YPath> goodSessionIds = new HashMap<>();
        for (YTreeStringNode yTreeStringNode : sessionDirs) {
            String sessionId = yTreeStringNode.getValue();
            String offersTablePathStr = String.format("%s/%s/%s", scOffersPath, sessionId, tableName);
            YPath offersTablePath = YPath.simple(offersTablePathStr);
            if (!offersTablePathStr.contains("recent") && cypress.exists(offersTablePath)) {
                YTreeNode rowCntNode = cypress.get(YPath.simple(offersTablePathStr + "/@row_count"));
                if (rowCntNode.longValue() > minRows) {
                    goodSessionIds.put(sessionId, offersTablePath);
                }
            }
        }

        String lastSessionId = Collections.max(goodSessionIds.keySet());
        return goodSessionIds.get(lastSessionId);
    }

    private class GenerationDataOfferRowSaver implements RowSaver<GenerationDataOffer> {

        private final AtomicInteger rowCounter;

        private GenerationDataOfferRowSaver(AtomicInteger rowCounter) {
            this.rowCounter = rowCounter;
        }

        @Override
        public int getChunkSize() {
            return ytChunkSize;
        }

        @Override
        public void insert(Collection<GenerationDataOffer> rows) {
            try {
                if (SAVER_DELAY > 0) {
                    List<GenerationDataOffer> r = new ArrayList<>(rows);
                    int delay = ThreadLocalRandom.current().nextInt(SAVER_DELAY);
                    Thread.sleep(delay);
                    r.size();
                }

                int globalCount = rowCounter.addAndGet(rows.size());
                if (globalCount > MAX_ROWS) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("Test exception. " + MAX_ROWS + " rows reached.");
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class SaverFactory implements RowSaverFactory<GenerationDataOffer> {

        private final AtomicInteger rowCounter = new AtomicInteger();

        @Override
        public List<List<RowSaver<GenerationDataOffer>>> getAllRowSavers() {

            List<RowSaver<GenerationDataOffer>> rowSavers =
                    Stream.generate(() -> new GenerationDataOfferRowSaver(rowCounter))
                            .limit(getThreadCount())
                            .collect(Collectors.toList());

            return Collections.singletonList(rowSavers);
        }

        @Override
        public int getThreadCount() {
            return writeThreads;
        }

        public int getCount() {
            return rowCounter.get();
        }
    }
}
