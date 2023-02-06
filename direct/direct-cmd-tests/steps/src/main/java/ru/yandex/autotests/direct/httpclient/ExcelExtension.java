package ru.yandex.autotests.direct.httpclient;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 01.08.14
 */
public enum ExcelExtension {

    XLS("xls"),
    XLSX("xlsx");

    private String extension;

    ExcelExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
