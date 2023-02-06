package ru.yandex.market.tsum.clients.aqua.testcases;

import java.util.Objects;

public class TmsJobState {
    String jobName;
    String status;

    private TmsJobState(Builder builder) {
        jobName = builder.jobName;
        status = builder.status;
    }

    public String getJobName() {
        return jobName;
    }

    public String getStatus() {
        return status;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private String jobName;
        private String status;

        private Builder() {
        }

        public Builder withJobName(String val) {
            jobName = val;
            return this;
        }

        public Builder withStatus(String val) {
            status = val;
            return this;
        }

        public TmsJobState build() {
            return new TmsJobState(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TmsJobState that = (TmsJobState) o;
        return Objects.equals(jobName, that.jobName) &&
            Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, status);
    }

    @Override
    public String toString() {
        return "TmsJobState{" +
            "jobName='" + jobName + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
