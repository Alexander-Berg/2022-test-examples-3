package ru.yandex.autotests.direct.cmd.data.stat.report;

import com.google.gson.annotations.SerializedName;

public class StatReports {
    @SerializedName("report_id")
    private String reportId;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

}
