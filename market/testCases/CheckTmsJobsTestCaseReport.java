package ru.yandex.market.tsum.clients.aqua.testcases;

import java.util.Objects;

import javax.annotation.Nonnull;

import ru.yandex.market.tsum.clients.aqua.startek.TestCaseForStartrek;

public class CheckTmsJobsTestCaseReport implements TestCaseReport {

    @Nonnull
    private final String name;
    private final String message;
    @Nonnull
    private final TmsJobState jobState;

    private CheckTmsJobsTestCaseReport(Builder builder) {
        name = builder.name;
        message = builder.message;
        jobState = builder.jobState;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public TmsJobState getJobState() {
        return jobState;
    }

    @Override
    public TestCaseForStartrek toTestCaseForStartrek() {
        return TestCaseForStartrek.newBuilder()
            .withCaseName(name)
            .withJobName(jobState.jobName)
            .withDetails(jobState.status)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckTmsJobsTestCaseReport that = (CheckTmsJobsTestCaseReport) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(message, that.message) &&
            Objects.equals(jobState, that.jobState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, message, jobState);
    }

    @Override
    public String toString() {
        return "CheckTmsJobsTestCaseReport{" +
            "name='" + name + '\'' +
            ", message='" + message + '\'' +
            ", jobState=" + jobState +
            '}';
    }

    public static final class Builder {
        private String name;
        private String message;
        private TmsJobState jobState;

        private Builder() {
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withMessage(String val) {
            message = val;
            return this;
        }

        public Builder withJobState(TmsJobState val) {
            jobState = val;
            return this;
        }

        public CheckTmsJobsTestCaseReport build() {
            return new CheckTmsJobsTestCaseReport(this);
        }
    }
}
