package ru.yandex.autotests.direct.cmd.data.stat.report;

import com.google.gson.annotations.SerializedName;

public class SaveStatReportResponse {

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("status")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
}
