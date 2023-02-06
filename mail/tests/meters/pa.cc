#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/meters/pa.h>

using namespace ::testing;
using namespace ::unistat;
using namespace std::chrono_literals;

struct PaLogRequestTimeHistTest : public Test {
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
    using PaLogRequestTimeHistMocked =
            PaLogRequestTimeHistBase<std::chrono::milliseconds, PaLogRequestTimeHistTest::SharedHistMock>;

    void SetUp() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset(new HistMock<std::chrono::milliseconds>());
    }

    void TearDown() override {
        SharedHistMock<std::chrono::milliseconds>::hist.reset();
    }
};

template<typename T>
std::shared_ptr<PaLogRequestTimeHistTest::HistMock<T>> PaLogRequestTimeHistTest::SharedHistMock<T>::hist;

TEST_F(PaLogRequestTimeHistTest, shouldHasZeroValueJustAfterInit) {
    EXPECT_THROW(PaLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "windows", "name"), std::out_of_range);
}

TEST_F(PaLogRequestTimeHistTest, shouldThrowExceptionWhenServiceNameIsInvalid) {
    PaLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "postgresql", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_postgresql_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    EXPECT_EQ(meter.get(), (HistValue{"name_postgresql_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}


TEST_F(PaLogRequestTimeHistTest, shouldIncrementForMatchedEndpoint) {
    PaLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "postgresql", "name");

    EXPECT_CALL(getHist(), update(3ms)).WillOnce(Return());
    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_postgresql_hgram", {{0ms, 3}, {10ms, 0}, {100ms, 0}}})));

    const PaRecord input{static_cast<pa::rem_type>(PaRecordTypes::Enum::postgresql), "macs_pg",
                         "QueryHandler", "AllLabelsList", 3u, 1574186474u};

    meter.update(input);
    EXPECT_EQ(meter.get(), (HistValue{"name_postgresql_hgram", {{0ms, 3}, {10ms, 0}, {100ms, 0}}}));
}

TEST_F(PaLogRequestTimeHistTest, shouldNotIncrementForUnmatchedEndpoint) {
    PaLogRequestTimeHistMocked meter(std::vector<long>{0, 10, 100}, "postgresql", "name");

    EXPECT_CALL(getHist(), get()).WillOnce(
            Return((HistValue{"name_postgresql_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}})));

    meter.update(PaRecord{static_cast<pa::rem_type>(PaRecordTypes::Enum::mysql), "", "", "", 2u, 974352434u});
    EXPECT_EQ(meter.get(), (HistValue{"name_postgresql_hgram", {{0ms, 0}, {10ms, 0}, {100ms, 0}}}));
}