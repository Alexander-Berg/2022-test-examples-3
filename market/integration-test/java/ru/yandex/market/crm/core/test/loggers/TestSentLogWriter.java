package ru.yandex.market.crm.core.test.loggers;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.services.logging.SentLogWriter;
import ru.yandex.market.crm.util.tskv.TskvParser;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author vtarasoff
 * @since 01.06.2021
 */
@Component
public class TestSentLogWriter implements SentLogWriter, StatefulHelper {
    private static final String EMAIL = "email";
    private static final String GNC = "gnc";
    private static final String SMS = "sms";

    private final Map<String, BlockingQueue<Map<String, String>>> records = new ConcurrentHashMap<>();

    @Override
    public void logEmail(String record) {
        log(EMAIL, record);
    }

    @Override
    public void logGnc(String record) {
        log(GNC, record);
    }

    @Override
    public void logSms(String record) {
        log(SMS, record);
    }

    private void log(String type, String record) {
        try {
            getLog(type).put(TskvParser.parse(record));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockingQueue<Map<String, String>> getEmailLog() {
        return getLog(EMAIL);
    }

    public Queue<Map<String, String>> getSmsLog() {
        return getLog(SMS);
    }

    private BlockingQueue<Map<String, String>> getLog(String type) {
        return records.computeIfAbsent(type, v -> new ArrayBlockingQueue<>(100));
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        records.clear();
    }
}
