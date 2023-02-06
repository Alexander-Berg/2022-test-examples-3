package ru.yandex.autotests.reporting.api.beans;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.autotests.market.stat.date.DatePatterns;
import ru.yandex.autotests.market.stat.util.JsonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.DEFAULT_USER;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.FILES;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.FINISHED_AT;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.JOB_ID;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.PARAMETERS;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.STATUS;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.SUBMITTED_AT;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.USER;

/**
 * Created by kateleb on 17.11.16.
 */
@ToString
@Getter
@Setter
public class BuildReportJob {
    public static final String ERROR = "ERROR";
    private String jobId;
    private String status;
    private String user;
    private List<ReportFileInfo> files;
    private LocalDateTime submittedAt;
    private LocalDateTime finishedAt;
    private ReportingApiProfile parameters;

    public BuildReportJob(JsonObject response) {
        checkResonse(response);
        JsonElement jobValue = response.get(JOB_ID);
        JsonElement statusValue = response.get(STATUS);
        this.jobId = jobValue.getAsString();
        this.status = statusValue.getAsString();
        this.user = response.get(USER) == null ? DEFAULT_USER : response.get(USER).getAsString();
        this.submittedAt = response.get(SUBMITTED_AT) == null ? null : DatePatterns.parseByFirstMatchingPattern(response.get(SUBMITTED_AT).getAsString().replace("Z", ""));
        this.finishedAt = response.get(FINISHED_AT) == null ? null : DatePatterns.parseByFirstMatchingPattern(response.get(FINISHED_AT).getAsString().replace("Z", ""));
        this.files = JsonUtils.getElementsFromJsonArray(response.get(FILES)).stream().map(e -> new ReportFileInfo(e.getAsJsonObject())).collect(toList());
        this.parameters = response.get(PARAMETERS) == null ? new ReportingApiProfile() : new ReportingApiProfile(response.get(PARAMETERS).getAsJsonObject());
    }

    public BuildReportJob(BuildReportTmsJob jobStatus) {
        this.jobId = jobStatus.getName();
        this.user = jobStatus.getSubmittedBy();
        this.submittedAt = jobStatus.getSubmittedAt();
        this.finishedAt = jobStatus.getFinishedAt();
        JsonObject result = JsonUtils.parse(jobStatus.getResult()).getAsJsonObject();
        this.status = result.get(ERROR) == null ? jobStatus.getStatus() : jobStatus.getStatus() + ": " + result.get(ERROR).getAsString();
        this.files = result.get(FILES) == null ? new ArrayList<>() :
            JsonUtils.getElementsFromJsonArray(result.get(FILES)).stream().map(e -> new ReportFileInfo(e.getAsJsonObject())).collect(toList());

        JsonObject params = JsonUtils.parse(jobStatus.getParams()).getAsJsonObject();
        this.parameters = new ReportingApiProfile(params);
    }

    private void checkResonse(JsonObject response) {
        if (response == null || response.get(JOB_ID) == null || response.get(STATUS) == null) {
            throw new IllegalArgumentException("Can't parse job details from response " + response);
        }
    }
}
