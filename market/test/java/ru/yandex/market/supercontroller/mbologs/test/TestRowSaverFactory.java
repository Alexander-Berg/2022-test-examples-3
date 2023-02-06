package ru.yandex.market.supercontroller.mbologs.test;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import ru.yandex.market.supercontroller.mbologs.dao.savers.RowSaver;
import ru.yandex.market.supercontroller.mbologs.model.SessionConfiguration;
import ru.yandex.market.supercontroller.mbologs.workers.RowSaverFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author amaslak
 * @timestamp 8/6/12 8:51 PM
 */
@Component
public class TestRowSaverFactory implements RowSaverFactory<ZOffer> {

    private int consumerCount;
    private AtomicInteger counter;
    private int chunks = 5;
    private int chunkSize = 50;

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    @Override
    public int getThreadCount() {
        return consumerCount * chunks;
    }

    @Override
    public void setConfiguration(SessionConfiguration configuration) {

    }

    public void setCounter(AtomicInteger counter) {
        this.counter = counter;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    @Override
    public List<List<RowSaver<ZOffer>>> getAllRowSavers() {
        List<List<RowSaver<ZOffer>>> result = new ArrayList<List<RowSaver<ZOffer>>>();

        // save logs to oracle
        List<RowSaver<ZOffer>> oracleSavers;
        for (int j = 0; j < consumerCount; j++) {
            oracleSavers = new ArrayList<RowSaver<ZOffer>>(chunks);
            for (int i = 0; i < chunks; i++) {
                oracleSavers.add(getRowSaver());
            }
            result.add(oracleSavers);
        }
        return result;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public RowSaver<ZOffer> getRowSaver() {
        return new RowSaver<ZOffer>() {

            private final Logger log = Logger.getLogger(getClass());

            @Override
            public int getChunkSize() {
                return chunkSize;
            }

            @Override
            public void insert(Collection<ZOffer> rows) {
                for (ZOffer row : rows) {
                    log.trace(row.getNum());
                    counter.decrementAndGet();
                }
            }
        };
    }
}
