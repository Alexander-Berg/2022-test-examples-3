package ru.yandex.market.tsum.clients.aqua.testcases;

import java.util.Objects;

import ru.yandex.market.tsum.clients.aqua.startek.TestCaseForStartrek;

public class BasicTestCaseReport implements TestCaseReport {

    String name;
    String message;

    private BasicTestCaseReport(Builder builder) {
        name = builder.name;
        message = builder.message;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public TestCaseForStartrek toTestCaseForStartrek() {
        return TestCaseForStartrek.newBuilder()
            .withCaseName(name)
            .withDetails(message)
            .build();
    }

    public static final class Builder {
        private String name;
        private String message;

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

        public BasicTestCaseReport build() {
            return new BasicTestCaseReport(this);
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
        BasicTestCaseReport that = (BasicTestCaseReport) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, message);
    }

    @Override
    public String toString() {
        return "BasicTestCaseReport{" +
            "name='" + name + '\'' +
            ", message='" + message + '\'' +
            '}';
    }
}
