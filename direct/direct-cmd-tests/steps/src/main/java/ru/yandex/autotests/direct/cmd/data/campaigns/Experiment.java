package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

public class Experiment {
    @SerializedName("percent")
    private Integer percent;
    @SerializedName("date_from")
    private String dateFrom;
    @SerializedName("primary_cid")
    private Long primaryCid;
    @SerializedName("create_time")
    private String createTime;
    @SerializedName("date_to")
    private String dateTo;
    @SerializedName("experiment_id")
    private Long experimentId;
    @SerializedName("status")
    private ExperimentStatusEnum status;
    @SerializedName("ClientID")
    private Long clientId;

    @SerializedName("secondary_cid")
    private Long secondaryCId;

    public Long getSecondaryCId() {
        return secondaryCId;
    }

    public Experiment withSecondaryCId(Long secondaryCId) {
        this.secondaryCId = secondaryCId;
        return this;
    }

    public Long getClientId() {
        return clientId;
    }

    public Experiment withClientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public ExperimentStatusEnum getStatus() {
        return status;
    }

    public Experiment withStatus(ExperimentStatusEnum status) {
        this.status = status;
        return this;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public Experiment withExperimentId(Long experimentId) {
        this.experimentId = experimentId;
        return this;
    }

    public String getDateTo() {
        return dateTo;
    }

    public Experiment withDateTo(String dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public String getCreateTime() {
        return createTime;
    }

    public Experiment withCreateTime(String createTime) {
        this.createTime = createTime;
        return this;
    }

    public Long getPrimaryCid() {
        return primaryCid;
    }

    public Experiment withPrimaryCid(Long primaryCid) {
        this.primaryCid = primaryCid;
        return this;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public Experiment withDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public Integer getPercent() {
        return percent;
    }

    public Experiment withPercent(Integer percent) {
        this.percent = percent;
        return this;
    }
}
