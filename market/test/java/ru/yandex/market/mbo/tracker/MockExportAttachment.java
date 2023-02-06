package ru.yandex.market.mbo.tracker;

import java.time.Instant;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.models.ExportAttachment;

/**
 * @author s-ermakov
 */
public class MockExportAttachment extends ExportAttachment {

    private final String name;
    private final Instant createdAt;
    private final ExcelFile excelFile;
    private final String createdBy;

    public MockExportAttachment(String name, Instant createdAt, String createdBy, ExcelFile excelFile) {
        super(null, null);
        this.name = name;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.excelFile = excelFile;
    }

    @Override
    public String getFileName() {
        return name;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public ExcelFile getExcelFile() {
        return new ExcelFile(excelFile);
    }

    @Override
    public String toString() {
        return "MockExportAttachment{" +
            "name='" + name + '\'' +
            ", createdAt=" + createdAt +
            ", excelFile=" + excelFile +
            ", createdBy='" + createdBy + '\'' +
            '}';
    }
}
