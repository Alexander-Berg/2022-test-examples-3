package ru.yandex.autotests.direct.cmd.data.excel;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/*
* todo javadoc
*/
public class ExportCampXlsRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private String cid;

    @SerializeKey("skip_arch")
    private Boolean skipArch;

    @SerializeKey("xls_format")
    private ExcelFormat xlsFormat;

    @SerializeKey("release_camp_lock")
    private Boolean releaseCampLock;

    public String getCid() {
        return cid;
    }

    public ExportCampXlsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public Boolean getSkipArch() {
        return skipArch;
    }

    public ExportCampXlsRequest withSkipArch(Boolean skipArch) {
        this.skipArch = skipArch;
        return this;
    }

    public ExcelFormat getXlsFormat() {
        return xlsFormat;
    }

    public ExportCampXlsRequest withXlsFormat(ExcelFormat xlsFormat) {
        this.xlsFormat = xlsFormat;
        return this;
    }

    public Boolean getReleaseCampLock() {
        return releaseCampLock;
    }

    public ExportCampXlsRequest withReleaseCampLock(Boolean releaseCampLock) {
        this.releaseCampLock = releaseCampLock;
        return this;
    }

    public enum ExcelFormat {
        XLS,
        XLSX;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
