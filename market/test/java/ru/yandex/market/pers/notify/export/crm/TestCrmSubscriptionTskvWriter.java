package ru.yandex.market.pers.notify.export.crm;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vtarasoff
 * @since 03.08.2021
 */
class TestCrmSubscriptionTskvWriter extends CrmSubscriptionTskvWriterImpl {
    private final List<String> records = new ArrayList<>();

    @Override
    void log(Logger logger, String record) {
        records.add(record);
    }

    List<String> getRecords() {
        return records;
    }
}
