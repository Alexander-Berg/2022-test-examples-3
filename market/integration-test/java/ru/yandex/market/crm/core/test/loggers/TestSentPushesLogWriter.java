package ru.yandex.market.crm.core.test.loggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.services.logging.SentPushesLogWriter;
import ru.yandex.market.crm.util.tskv.TskvParser;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@Component
public class TestSentPushesLogWriter implements SentPushesLogWriter, StatefulHelper {

    private final BlockingQueue<Map<String, String>> records = new ArrayBlockingQueue<>(100);

    @Override
    public void log(String record) {
        try {
            records.put(TskvParser.parse(record));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockingQueue<Map<String, String>> getRecords() {
        return records;
    }

    public List<Map<String, String>> getRecordsAsList() {
        List<Map<String, String>> list = new ArrayList<>();
        records.drainTo(list);
        return list;
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        records.clear();
    }
}
