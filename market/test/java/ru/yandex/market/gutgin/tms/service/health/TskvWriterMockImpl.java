package ru.yandex.market.gutgin.tms.service.health;

import ru.yandex.market.partner.content.common.service.health.TskvWriter;
import ru.yandex.market.request.trace.TskvRecordBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dergachevfv
 * @since 5/21/20
 */
public class TskvWriterMockImpl implements TskvWriter {

    private final List<TskvRecordBuilder> tskvRecords = new ArrayList<>();

    @Override
    public void writeTskv(TskvRecordBuilder tskvRecordBuilder) {
        tskvRecords.add(tskvRecordBuilder);
    }

    public List<TskvRecordBuilder> getTskvRecords() {
        return tskvRecords;
    }

    public void clear() {
        tskvRecords.clear();
    }
}
