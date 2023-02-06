package ru.yandex.market.supercontroller.mbologs.test;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import ru.yandex.market.supercontroller.mbologs.parallel.publishers.RowPublisher;
import ru.yandex.market.supercontroller.mbologs.workers.RowProviderFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author amaslak
 * @timestamp 8/6/12 8:57 PM
 */
@Component
public class TestRowProviderFactory extends RowProviderFactory<ZOffer> {

    private final Logger log = Logger.getLogger(getClass());

    private final int totalRowsToPass = 50;

    private int consumerCount;
    private static final int LOGGING_INTERVAL = 10_000_000;

    private AtomicInteger counter;

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    public void setCounter(AtomicInteger counter) {
        this.counter = counter;
    }

    @Override
    public Runnable getRowProvider(final RowPublisher<ZOffer> zOfferRowPublisher, final CountDownLatch finished) {
        return new Runnable() {

            final List<String> sessions = Arrays.asList(
                    "20120506_0708", "20120506_0709", "20120506_0710", "20120506_0711"
            );

            @Override
            public void run() {

                Thread.currentThread().setName("RowProviderThread");

                for (int i = 0; i < totalRowsToPass; i++) {
                    ZOffer dataRow = new ZOffer();
                    dataRow.setNum(i);
                    dataRow.setTitle("title");
                    dataRow.setSessionId(sessions.get(i % sessions.size()));

                    try {
                        zOfferRowPublisher.publish(dataRow);
                        counter.addAndGet(consumerCount);
                    } catch (InterruptedException e) {
                        log.error(e, e);
                        break;
                    }

                    if (i % LOGGING_INTERVAL == 0) {
                        log.trace("Obtained " + i + " rows");
                    }
                }

                finished.countDown();
            }
        };
    }
}
