#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/meters/ymod_httpclient/tskv.h>

using namespace ::testing;
using namespace ::unistat;
using namespace std::chrono_literals;


TEST(HttpClientHttpRequestCountByStatus, shouldHasEmptyValueJustAfterInit) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");
    EXPECT_EQ(meter.get(), std::vector<NamedValue<std::size_t>>{});
}

TEST(HttpClientHttpRequestCountByStatus, shouldNotIncrementForUnmatchedEdnpoint) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");
    meter.update(std::map<std::string, std::string>{
            {"status", "504"},
            {"uri", "/other"}
    });
    EXPECT_EQ(meter.get(), std::vector<NamedValue<std::size_t>>{});
}

struct HttpClientHttpRequestCountByStatusWithVariousStatuses : public ::testing::TestWithParam<std::string> {};

INSTANTIATE_TEST_SUITE_P(shouldCountPassedStatusOnly, HttpClientHttpRequestCountByStatusWithVariousStatuses, Values(
    "100", "200", "303", "499", "505"
));

TEST_P(HttpClientHttpRequestCountByStatusWithVariousStatuses, shouldCountPassedStatusOnly) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");
    std::string status = GetParam();
    meter.update(std::map<std::string, std::string>{
            {"status", status},
            {"uri", "/endpoint"}
    });
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"name_prefix_endpoint_" + status + "_summ", 1}}));
}

TEST(HttpClientHttpRequestCountByStatus, shouldCountDifferentStatuses) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");

    meter.update(std::map<std::string, std::string>{
            {"status", "200"},
            {"uri", "/endpoint"}
    });

    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"name_prefix_endpoint_200_summ", 1}}));

    meter.update(std::map<std::string, std::string>{
            {"status", "504"},
            {"uri", "/endpoint"}
    });

    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"name_prefix_endpoint_200_summ", 1},
        {"name_prefix_endpoint_504_summ", 1}
    }));
}

TEST(HttpClientHttpRequestCountByStatus, shouldThrowExceptionOnUpdateWithoutStatus) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");

    EXPECT_THROW(meter.update(std::map<std::string, std::string>{
            {"http", "200"},
            {"uri", "/endpoint"}
    }), std::invalid_argument);
}

TEST(HttpClientHttpRequestCountByStatus, shouldThrowExceptionOnUpdateWithoutUri) {
    HttpClientHttpRequestCountByStatus meter("endpoint", "name_prefix");

    EXPECT_THROW(meter.update(std::map<std::string, std::string>{
            {"status", "200"},
            {"url", "/endpoint"}
    }), std::invalid_argument);
}


struct HttpClientHttpRequestTotalTimeHistTest : public Test {
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

    static decltype(auto) getHist() {
        return SharedHistMock<std::chrono::milliseconds>::instance();
    }

    using HistValue = NamedHist<std::chrono::milliseconds>;
    using HttpClientHttpRequestTotalTimeHistMocked =
            HttpClientHttpRequestTotalTimeHistBase<std::chrono::milliseconds, HttpClientHttpRequestTotalTimeHistTest::SharedHistMock>;

    void SetUp() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset(new HistMock<std::chrono::milliseconds>());
    }

    void TearDown() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset();
    }
};

template<typename T>
std::shared_ptr<HttpClientHttpRequestTotalTimeHistTest::HistMock<T>> HttpClientHttpRequestTotalTimeHistTest::SharedHistMock<T>::hist;

TEST_F(HttpClientHttpRequestTotalTimeHistTest, shouldHasZeroValueJustAfterInit) {
    HttpClientHttpRequestTotalTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    EXPECT_EQ(meter.get(), (HistValue{"name_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}

TEST_F(HttpClientHttpRequestTotalTimeHistTest, shouldNotIncrementForUnmatchedEdnpoint) {
    HttpClientHttpRequestTotalTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    meter.update(std::map<std::string, std::string>{
            {"total_time", "1.00"},
            {"uri", "/other"}
    });
    EXPECT_EQ(meter.get(), (HistValue{"name_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}

TEST_F(HttpClientHttpRequestTotalTimeHistTest, shouldThrowExceptionOnUpdateWithoutTotalTime) {
    HttpClientHttpRequestTotalTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_THROW(meter.update(std::map<std::string, std::string>{
            {"http", "200"},
            {"uri", "/endpoint"}
    }), std::invalid_argument);
}

TEST_F(HttpClientHttpRequestTotalTimeHistTest, shouldThrowExceptionOnUpdateWithoutUri) {
    HttpClientHttpRequestTotalTimeHistMocked meter(std::vector<long>{0, 10, 100}, "endpoint", "name");

    EXPECT_THROW(meter.update(std::map<std::string, std::string>{
            {"total_time", "2.00"},
            {"url", "/endpoint"}
    }), std::invalid_argument);
}
