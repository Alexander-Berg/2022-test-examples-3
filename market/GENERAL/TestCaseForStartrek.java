package ru.yandex.market.tsum.clients.aqua.startek;

public class TestCaseForStartrek {

    String testCaseName;
    String jobName;
    String ticketName;
    String details;

    private TestCaseForStartrek(Builder builder) {
        testCaseName = builder.testCaseName;
        jobName = builder.jobName;
        ticketName = builder.ticketName;
        details = builder.details;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String testCaseName;
        private String jobName;
        private String ticketName;
        private String details;

        private Builder() {
        }

        public Builder withCaseName(String val) {
            testCaseName = val;
            return this;
        }

        public Builder withJobName(String val) {
            jobName = val;
            return this;
        }

        public Builder withTicketName(String val) {
            ticketName = val;
            return this;
        }

        public Builder withDetails(String val) {
            details = val;
            return this;
        }

        public TestCaseForStartrek build() {
            return new TestCaseForStartrek(this);
        }
    }

    public String getJobName() {
        return jobName;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public String getTicketName() {
        return ticketName;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "StartrekFailedTestCases{" +
            "testCaseName='" + testCaseName + '\'' +
            ", jobName='" + jobName + '\'' +
            ", ticketName='" + ticketName + '\'' +
            ", details='" + details + '\'' +
            '}';
    }
}
