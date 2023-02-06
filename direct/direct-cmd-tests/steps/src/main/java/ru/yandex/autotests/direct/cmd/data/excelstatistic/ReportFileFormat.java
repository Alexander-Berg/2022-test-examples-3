package ru.yandex.autotests.direct.cmd.data.excelstatistic;

public enum ReportFileFormat {
    XLS("xls"),
    XLSX("xlsx"),
    CSV("csv");

    private String value;

    ReportFileFormat(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
