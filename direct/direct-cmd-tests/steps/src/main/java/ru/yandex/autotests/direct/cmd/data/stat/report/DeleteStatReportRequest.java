package ru.yandex.autotests.direct.cmd.data.stat.report;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class DeleteStatReportRequest extends BasicDirectRequest {

    @SerializeKey("report_id")
    private String reportId;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public DeleteStatReportRequest withReportId(String reportId) {
        this.reportId = reportId;
        return this;
    }
}
