#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/meters/ymod_webserver/access_log.h>

using namespace ::testing;
using namespace ::unistat;
using namespace std::chrono_literals;

TEST(AccessLogCount, shouldHasZeroValueJustAfterInit) {
    AccessLogCount meter("name");
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"name_summ", 0}));
}


TEST(AccessTskvCount, shouldNotIncrementForUnmatchedEndpoint) {
    AccessLogCountByPath meter("endpoint", "name");

    const std::map<std::string, std::string> in {{"request", "/labels?uid=1234"}};

    meter.update(in);
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"name_endpoint_summ", 0}));
}

struct AccessTskvCountWithVariousRecords : public ::testing::TestWithParam<std::map<std::string, std::string>> {};

INSTANTIATE_TEST_SUITE_P(shouldIgnoreHttpStatusAndTotalTime, AccessTskvCountWithVariousRecords, Values(
        std::map<std::string, std::string>{{"request", "/endpoint?uid=1234"}, {"profiler_total", "0.003"}, {"status_code", "200"}},
        std::map<std::string, std::string>{{"request", "/endpoint?uid=65343"}, {"profiler_total", "0.003"}, {"status_code", "300"}},
        std::map<std::string, std::string>{{"request", "/endpoint?uid=65343"}, {"profiler_total", "-0.003"}, {"status_code", "65539"}}
));

TEST_P(AccessTskvCountWithVariousRecords, shouldIgnoreHttpStatusAndTotalTime) {
    AccessLogCount meter("name");
    meter.update(GetParam());
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"name_summ", 1}));
}


TEST(AccessLogCountByPathAndFirstStatusDigit, shouldHasZeroValueJustAfterInit) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{}));
}


TEST(AccessTskvCountByFirstStatusDigit, shouldNotIncrementForUnmatchedEndpoint) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    const std::map<std::string, std::string> in {{"request", "/labels?uid=1234"}, {"status_code", "200"}};

    meter.update(in);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{}));
}

struct AccessTskvCountByFirstStatusDigitWithVariousStatuses : public ::testing::TestWithParam<long> {};

INSTANTIATE_TEST_SUITE_P(shouldCountPassedStatusOnly, AccessTskvCountByFirstStatusDigitWithVariousStatuses, Values(
        100, 101, 200, 202, 303, 313, 404, 499, 500, 505
));

TEST_P(AccessTskvCountByFirstStatusDigitWithVariousStatuses, shouldCountPassedStatusOnly) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    const long status = GetParam();
    const std::map<std::string, std::string> in {{"request", "/endpoint?uid=1234"}, {"status_code", std::to_string(status)}};

    meter.update(in);

    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"name_endpoint" + std::to_string(status / 100) + "xx_summ", 1}
    }));
}

TEST(AccessTskvCountByFirstStatusDigitWithVariousStatuses, shouldCountDifferentStatusesAtTheSameTime) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    auto makeRecord = [] (long status) {
        return std::map<std::string, std::string> {
            {"request", "/endpoint?uid=1234"},
            {"status_code", std::to_string(status)},
            {"profiler_total", "0.003"}
        };
    };

    meter.update(makeRecord(200));
    meter.update(makeRecord(303));
    meter.update(makeRecord(404));

    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"name_endpoint2xx_summ", 1},
        {"name_endpoint3xx_summ", 1},
        {"name_endpoint4xx_summ", 1}
    }));
}

TEST(AccessTskvCountByFirstStatusDigitWithVariousStatuses, shouldCountStatusesByFirstDigit) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    auto makeRecord = [] (long status) {
        return std::map<std::string, std::string> {
                {"request", "/endpoint?uid=1234"},
                {"status_code", std::to_string(status)},
                {"profiler_total", "0.003"}
        };
    };

    meter.update(makeRecord(200));
    meter.update(makeRecord(203));
    meter.update(makeRecord(204));

    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"name_endpoint2xx_summ", 3}
    }));
}

TEST(AccessTskvCountByFirstStatusDigit, shouldThrowExceptionWhenRequestDoesNotExist) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    const std::map<std::string, std::string> in {};

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}

TEST(AccessTskvCountByFirstStatusDigit, shouldThrowExceptionWhenStatusCodeDoesNotExist) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    const std::map<std::string, std::string> in {
        {"request", "/endpoint?uid=1234"},
        {"profiler_total", "0.003"}
    };

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}

TEST(AccessTskvCountByFirstStatusDigit, shouldThrowExceptionWhenStatusCodeIsNotLong) {
    AccessLogCountByPathAndFirstStatusDigit meter("endpoint", "name");

    const std::map<std::string, std::string> in {
        {"request", "/endpoint?uid=1234"},
        {"profiler_total", "0.003"},
        {"status_code", "СС"}
    };

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}


struct AccessLogRequestTimeHistTest : public Test {
    template <typename BucketBound>
    struct HistMock {
        template <typename ... Args>
        HistMock(Args...) {};
        MOCK_METHOD1_T(update, void(const BucketBound&));
        MOCK_CONST_METHOD0_T(get, NamedHist<BucketBound>(void));
    };

    template <typename BucketBound>
    struct SharedHistMock {
        static std::shared_ptr<HistMock<BucketBound>> hist;

        template <typename ... Args>
        SharedHistMock(Args...) {};

        void update(const BucketBound& b) {
            hist->update(b);
        }

        NamedHist<BucketBound> get() const {
            return hist->get();
        }

        static auto& instance() {
            return *hist;
        }
    };

    static auto& getHist() {
        return SharedHistMock<std::chrono::milliseconds>::instance();
    }

    using HistValue = NamedHist<std::chrono::milliseconds>;
    using AccessLogRequestTimeHistMocked =
            detail::AccessLogRequestTimeHistBase<PathMatcher, std::chrono::milliseconds, AccessLogRequestTimeHistTest::SharedHistMock>;

    void SetUp() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset(new HistMock<std::chrono::milliseconds>());
    }

    void TearDown() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset();
    }
};

template<typename T>
std::shared_ptr<AccessLogRequestTimeHistTest::HistMock<T>> AccessLogRequestTimeHistTest::SharedHistMock<T>::hist;

TEST_F(AccessLogRequestTimeHistTest, shouldHasZeroValueJustAfterInit) {
    AccessLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_endpoint_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    EXPECT_EQ(meter.get(), (HistValue{"name_endpoint_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}

TEST_F(AccessLogRequestTimeHistTest, shouldNotIncrementForUnmatchedEndpoint) {
    AccessLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_endpoint_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    meter.update(std::map<std::string, std::string> {{"request", "/labels?uid=1234"}, {"profiler_total", "0.003"}});
    EXPECT_EQ(meter.get(), (HistValue{"name_endpoint_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}

TEST_F(AccessLogRequestTimeHistTest, shouldThrowExceptionWhenRequestDoesNotExist) {
    AccessLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");
    const std::map<std::string, std::string> in{};

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}

TEST_F(AccessLogRequestTimeHistTest, shouldThrowExceptionWhenProfilerTotalDoesNotExist) {
    AccessLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");
    const std::map<std::string, std::string> in{{"request", "/endpoint?uid=1234"}};

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}

TEST_F(AccessLogRequestTimeHistTest, shouldThrowExceptionWhenProfilerTotalIsNotDouble) {
    AccessLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");
    const std::map<std::string, std::string> in{
        {"request", "/endpoint?uid=1234"}
        , {"profiler_total", "fatal"}
    };

    EXPECT_THROW(meter.update(in), std::invalid_argument);
}
