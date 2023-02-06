package ru.yandex.market.abo.core.outlet.license;

import java.time.LocalDate;

import org.junit.jupiter.params.provider.Arguments;

/**
 * @author komarovns
 * @date 02.07.19
 */
public class AutoCloseTestData implements Arguments {
    private final Object[] args;

    private AutoCloseTestData(Builder builder) {
        this.args = new Object[]{
                builder.mbiJurAddress,
                builder.fsrarJurAddress,
                builder.mbiFactAddress,
                builder.fsrarFactAddress,
                builder.mbiIssueDate,
                builder.fsrarIssueDate,
                builder.fsrarUpdateDate,
                builder.mbiExpireDate,
                builder.fsrarExpireDate,
                builder.expected
        };
    }

    @Override
    public Object[] get() {
        return args;
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * С этими значениями тест должен проходить
     */
    static Builder initializedBuilder() {
        var now = LocalDate.now();
        return AutoCloseTestData.builder()
                .withMbiJurAddress("jurAddr")
                .withFsrarJurAddress("jurAddr")
                .withMbiFactAddress("factAddr")
                .withFsrarFactAddress("factAddr")
                .withMbiIssueDate(now.minusYears(1))
                .withFsrarIssueDate(now.minusYears(1))
                .withFsrarUpdateDate(now.minusMonths(6))
                .withMbiExpireDate(now.plusYears(1))
                .withFsrarExpireDate(now.plusYears(1))
                .withExpected(true);
    }

    static class Builder {
        private String mbiJurAddress;
        private String fsrarJurAddress;
        private String mbiFactAddress;
        private String fsrarFactAddress;
        private LocalDate mbiIssueDate;
        private LocalDate fsrarIssueDate;
        private LocalDate fsrarUpdateDate;
        private LocalDate mbiExpireDate;
        private LocalDate fsrarExpireDate;
        private boolean expected;

        public Builder withMbiJurAddress(String val) {
            mbiJurAddress = val;
            return this;
        }

        public Builder withFsrarJurAddress(String val) {
            fsrarJurAddress = val;
            return this;
        }

        public Builder withMbiFactAddress(String val) {
            mbiFactAddress = val;
            return this;
        }

        public Builder withFsrarFactAddress(String val) {
            fsrarFactAddress = val;
            return this;
        }

        public Builder withMbiIssueDate(LocalDate val) {
            mbiIssueDate = val;
            return this;
        }

        public Builder withFsrarIssueDate(LocalDate val) {
            fsrarIssueDate = val;
            return this;
        }

        public Builder withFsrarUpdateDate(LocalDate val) {
            fsrarUpdateDate = val;
            return this;
        }

        public Builder withMbiExpireDate(LocalDate val) {
            mbiExpireDate = val;
            return this;
        }

        public Builder withFsrarExpireDate(LocalDate val) {
            fsrarExpireDate = val;
            return this;
        }

        public Builder withExpected(boolean val) {
            expected = val;
            return this;
        }

        public AutoCloseTestData build() {
            return new AutoCloseTestData(this);
        }
    }
}
