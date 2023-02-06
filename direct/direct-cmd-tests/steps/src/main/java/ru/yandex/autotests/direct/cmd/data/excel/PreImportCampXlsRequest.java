package ru.yandex.autotests.direct.cmd.data.excel;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToFileSerializer;

/*
* todo javadoc
*/
public class PreImportCampXlsRequest extends BasicDirectRequest {
    @SerializeKey("import_format")
    private ImportFormat importFormat;

    @SerializeKey("json")
    private Boolean json;

    @SerializeKey("xls")
    @SerializeBy(ValueToFileSerializer.class)
    private String xls;

    public ImportFormat getImportFormat() {
        return importFormat;
    }

    public PreImportCampXlsRequest withImportFormat(ImportFormat importFormat) {
        this.importFormat = importFormat;
        return this;
    }

    public Boolean getJson() {
        return json;
    }

    public PreImportCampXlsRequest withJson(Boolean json) {
        this.json = json;
        return this;
    }

    public String getXls() {
        return xls;
    }

    public PreImportCampXlsRequest withXls(String xls) {
        this.xls = xls;
        return this;
    }

    public enum ImportFormat {
        XLS,
        XLSX,
        CSV;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
