#include <mail/sharpei/unistat/cpp/sharpei_metrics.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <chrono>
#include <optional>
#include <string>
#include <type_traits>

using namespace pa;
using namespace testing;
using namespace unistat;

using Record = std::map<std::string, std::string>;

TEST(StatusLogAdaptorTests, getUnixtimeTest) {
    const auto now = 100500ull;
    const auto res = StatusLogAdaptor::getUnixtime({{"unixtime", std::to_string(now)}});
    ASSERT_EQ(*res, now);
}

class StatusRecordsCounterTests : public Test {
public:
    const std::string namePrefix = "test";
    const std::string shard = "sharddb";
    const std::string host = "host.mail.yandex.net";
    const std::string state = "alive";
    const bool alive = true;
    const std::string unixtime = "100500";
};

TEST_F(StatusRecordsCounterTests, positive) {
    ShardHostStateMatcher m(shard, host, state);
    ASSERT_TRUE(m.match(shard, host, state));
}

TEST_F(StatusRecordsCounterTests, negative) {
    ShardHostStateMatcher m(shard, host, state);
    ASSERT_FALSE(m.match(shard, host, "xxx"));
}

TEST_F(StatusRecordsCounterTests, getWithoutPreviousUpdate) {
    StatusCounter counter(namePrefix, shard, host, alive);
    ASSERT_FALSE(std::get<1>(counter.get()));
}

TEST_F(StatusRecordsCounterTests, simple) {
    StatusCounter counter(namePrefix, shard, host, alive);
    Record record{{"shard_name", shard}, {"host", host}, {"smooth_state", state}, {"unixtime", unixtime}};
    counter.update(record);
    ASSERT_TRUE(std::get<1>(counter.get()));
}

TEST_F(StatusRecordsCounterTests, returnsTrueIfThereIsMatchingEvenIfNotMatchingIsAlsoPresent) {
    StatusCounter counter(namePrefix, shard, host, alive);
    Record first{{"shard_name", shard}, {"host", host}, {"smooth_state", state}, {"unixtime", unixtime}};
    Record second{{"shard_name", shard}, {"host2", host}, {"smooth_state", state}, {"unixtime", "100501"}};
    counter.update(first);
    counter.update(second);
    ASSERT_TRUE(std::get<1>(counter.get()));
}

TEST_F(StatusRecordsCounterTests, doesntAccountOldEntries) {
    StatusCounter counter(namePrefix, shard, host, alive);
    Record first{{"shard_name", shard}, {"host", host}, {"smooth_state", state}, {"unixtime", unixtime}};
    // not matching line arrives and makes matching line expired: 100504 = 100500 + ttl + 1
    Record second{{"shard_name", shard}, {"host", host}, {"smooth_state", "dead"}, {"unixtime", "100504"}};
    counter.update(first);
    counter.update(second);
    ASSERT_FALSE(std::get<1>(counter.get()));
}

TEST_F(StatusRecordsCounterTests, exceptionIfUnixtimeAbsent) {
    StatusCounter counter(namePrefix, shard, host, alive);
    Record record{{"shard_name", shard}, {"host", host}, {"smooth_state", state}, {"unixteam", unixtime}};
    ASSERT_THROW(counter.update(record), std::bad_optional_access);
}

struct StatusRecordsCounterTests_NegativeCases : StatusRecordsCounterTests, WithParamInterface<Record> {};

TEST_P(StatusRecordsCounterTests_NegativeCases, recordDoesntMatch) {
    StatusCounter counter(namePrefix, shard, host, alive);
    Record record = GetParam();
    counter.update(record);
    ASSERT_FALSE(std::get<1>(counter.get()));
}

INSTANTIATE_TEST_SUITE_P(, StatusRecordsCounterTests_NegativeCases,
                         Values(Record{{"shard_name", "sharddb"},
                                       {"host", "host.mail.yandex.net"},
                                       {"smooth_state", "nealive"},
                                       {"unixtime", "100500"}},
                                Record{{"shard_name", "sharddb"},
                                       {"host", "nehost.mail.yandex.net"},
                                       {"smooth_state", "alive"},
                                       {"unixtime", "100500"}},
                                Record{{"shard_name", "nesharddb"},
                                       {"host", "host.mail.yandex.net"},
                                       {"smooth_state", "alive"},
                                       {"unixtime", "100500"}}));

class PaLogHistogramForEndpointTests : public Test {
public:
    auto getHist() const {
        return PaLogHistogramForEndpoint(borders, handler, namePrefix, host);
    }

    auto createPaRecord(const std::string& host, unsigned spentMs) const {
        return PaRecord(rem_type::sharpei, host, handler, "suid", spentMs, time);
    }

    auto getSignalName() const {
        return namePrefix + "_" + host + "_" + handler + "_hgram";
    }

    const std::vector<long long> borders{0, 5};
    const std::string host = "sharpei01h.mail.yandex.net";

private:
    const std::string handler = "handler";
    const std::string namePrefix = "prefix";
    const unsigned time = 100500;
};

TEST_F(PaLogHistogramForEndpointTests, zerosInAllBucketsIfNoUpdateHappened) {
    auto hist = getHist();
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 0u}, {borders[1], 0u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}

TEST_F(PaLogHistogramForEndpointTests, relevantRecordsIncreaseCounters) {
    auto hist = getHist();
    hist.update(createPaRecord(host, 1));
    hist.update(createPaRecord(host, 2));
    hist.update(createPaRecord(host, 6));
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 2u}, {borders[1], 1u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}

TEST_F(PaLogHistogramForEndpointTests, irrelevantRecordsDoNotAffectCounters) {
    auto hist = getHist();
    hist.update(createPaRecord("irrelevant", 1));
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 0u}, {borders[1], 0u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}

TEST_F(PaLogHistogramForEndpointTests, recordWithEmptyHostDoesNotAffectCounters) {
    auto hist = getHist();
    hist.update(createPaRecord("", 1));
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 0u}, {borders[1], 0u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}

TEST_F(PaLogHistogramForEndpointTests, comparisonIsAwareOfHostLengthInPaRecord) {
    // only the first |maxHostLength| chars from the host name can be stored in PaRecord,
    // so in general we should match |paRecord.host| with only a prefix of |host| of length |maxHostLength|
    static constexpr size_t maxHostLength = std::size(decltype(PaRecord::host){});
    auto hist = getHist();
    hist.update(createPaRecord(host.substr(0, maxHostLength), 1));
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 1u}, {borders[1], 0u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}

TEST_F(PaLogHistogramForEndpointTests, comparisonDoesNotAcceptTooShortPrefixesOfHost) {
    static constexpr size_t maxHostLength = std::size(decltype(PaRecord::host){});
    auto hist = getHist();
    hist.update(createPaRecord(host.substr(0, maxHostLength - 1), 1));
    NamedHist<std::chrono::milliseconds::rep> expected{
        getSignalName(),
        {{borders[0], 0u}, {borders[1], 0u}, {borders[1] * 2, 0u}}
    };
    ASSERT_EQ(hist.get(), expected);
}
