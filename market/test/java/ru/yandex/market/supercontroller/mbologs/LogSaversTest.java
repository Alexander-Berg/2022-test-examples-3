package ru.yandex.market.supercontroller.mbologs;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsTestConfig;
import ru.yandex.market.supercontroller.mbologs.parallel.BlockingQueueStrategy;
import ru.yandex.market.supercontroller.mbologs.parallel.DisruptorStrategy;
import ru.yandex.market.supercontroller.mbologs.test.TestRowProviderFactory;
import ru.yandex.market.supercontroller.mbologs.test.TestRowSaverFactory;
import ru.yandex.market.supercontroller.mbologs.test.ZOffer;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * @author amaslak
 * @timestamp 6/27/12 7:09 PM
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MboLogsTestConfig.class})
public class LogSaversTest {
    private final Logger log = Logger.getLogger(getClass());

    @Autowired
    private DisruptorStrategy<ZOffer> testDisruptorStrategy;

    @Autowired
    private BlockingQueueStrategy<ZOffer> testQueueLogSaver;

    @Autowired
    private TestRowProviderFactory providerFactory;

    @Autowired
    private TestRowSaverFactory rowSaverFactory;

    private int counsumerCount = 4;

    @Test
    public void testDisruptor() throws Exception {
        log.info("Disruptor test started");
        AtomicInteger counter = new AtomicInteger(0);

        providerFactory.setCounter(counter);
        providerFactory.setConsumerCount(counsumerCount);

        rowSaverFactory.setCounter(counter);
        rowSaverFactory.setConsumerCount(counsumerCount);

        testDisruptorStrategy.copy(providerFactory, rowSaverFactory);

        assertTrue(counter.get() == 0);
        log.info("Disruptor test finished");
    }

    @Test
    public void testQueue() throws Exception {
        log.info("Blocking queue test started");
        AtomicInteger counter = new AtomicInteger(0);

        providerFactory.setCounter(counter);
        providerFactory.setConsumerCount(counsumerCount);

        rowSaverFactory.setCounter(counter);
        rowSaverFactory.setConsumerCount(counsumerCount);

        testQueueLogSaver.copy(providerFactory, rowSaverFactory);

        assertTrue(counter.get() == 0);
        log.info("Blocking queue test finished");
    }
}
