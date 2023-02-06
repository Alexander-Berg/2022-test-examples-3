package ru.yandex.market.crm.platform.reader.test;

import ru.yandex.market.crm.platform.reader.export.IncrementalExportResult;

import java.util.List;

public class ExportResultStub extends IncrementalExportResult {

    private final List<String> data;

    public ExportResultStub(long endTime, List<String> data) {
        this.endTime = endTime;
        this.data = data;
    }

    public List<String> getData() {
        return data;
    }
}
