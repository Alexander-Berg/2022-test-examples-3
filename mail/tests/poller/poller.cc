#include <internal/poller/poller.h>

#include <mail/sharpei/tests/mocks.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <boost/asio/io_context.hpp>

#include <spdlog/details/format.h>

#include <algorithm>
#include <chrono>
#include <future>
#include <memory>
#include <stdexcept>
#include <thread>

namespace {

using namespace sharpei;
using namespace sharpei::cache;
using namespace sharpei::db;
using namespace sharpei::poller;
using namespace ::testing;

// just for convenience
constexpr auto shard = _;
constexpr auto finishHandler = _;
constexpr auto errorHandler = _;

std::vector<ShardWithoutRoles> generateShards(unsigned shards) {
    using Address = Shard::Database::Address;
    std::vector<ShardWithoutRoles> res;
    for (unsigned i = 0; i < shards; ++i) {
        const auto shardName = fmt::format("shard{}", i);
        const auto host = fmt::format("host{}", i);
        res.emplace_back(i, shardName, ShardWithoutRoles::Addresses{Address(host, 42, "dbname", "sas")});
    }
    return res;
}

class TestShardsProvider : public IShardsProvider {
public:
    TestShardsProvider(unsigned shards) : shards(shards) {
    }

    expected<std::vector<ShardWithoutRoles>> getAllShards(const TaskContextPtr&) const override {
        return generateShards(shards);
    }

private:
    const unsigned shards;
};

auto makePoller(const std::string& uniqId, ShardAdaptorPtr shardAdaptor, ShardsProviderPtr shardsProvider) {
    CachePtr cache = std::make_shared<Cache>(1, 2);
    PollerConfig pollerCfg;
    pollerCfg.coroutineStackSize = 1024 * 1024;
    pollerCfg.interval = std::chrono::seconds(1);

    return std::make_shared<Poller>(Poller::Type::meta, uniqId, pollerCfg, shardsProvider, shardAdaptor, cache);
}

class PollerTests : public Test {
    using ShardAdaptor = StrictMock<ShardAdaptorMock>;

public:
    PollerTests() {
        ShardsProviderPtr shardsProvider = std::make_shared<TestShardsProvider>(shards);
        worker = std::make_unique<IoThreadWorker>(uniqId, io);
        shardAdaptor = std::make_shared<ShardAdaptor>();
        poller = makePoller(uniqId, shardAdaptor, shardsProvider);
    }

    ~PollerTests() {
        poller->stop();
        worker->stop();
    }

    static constexpr unsigned shards = 3;
    std::string uniqId = "test";
    std::shared_ptr<ShardAdaptor> shardAdaptor;
    boost::asio::io_context io;
    std::unique_ptr<IoThreadWorker> worker;
    std::shared_ptr<Poller> poller;
};

// clang-format off
TEST_F(PollerTests, waitForTheFirstUpdateCompletes) {
    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .Times(AtLeast(shards))
        .WillRepeatedly(InvokeArgument<1>());

    poller->start(io);
}

TEST_F(PollerTests, waitForTheFirstUpdateCompletesIfErrorOccuredOnSomeShard) {
    InSequence s;
    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .WillOnce(InvokeArgument<2>(ExplainedError(Error::resetError, "reset cache error")));

    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .Times(AtLeast(shards - 1))
        .WillRepeatedly(InvokeArgument<1>());

    poller->start(io);
}

TEST_F(PollerTests, waitForTheFirstUpdateCompletesIfExceptionsOccuredOnTheFirstIterations) {
    auto shardsProvider = std::make_shared<StrictMock<ShardsProviderMock>>();
    poller = makePoller(uniqId, shardAdaptor, shardsProvider);

    {
    InSequence s;
    EXPECT_CALL(*shardsProvider, getAllShards(_))
        .Times(2)
        .WillOnce(Throw(std::runtime_error{"connection timeout for example"}));

    EXPECT_CALL(*shardsProvider, getAllShards(_))
        .WillRepeatedly(Return(generateShards(shards)));
    }

    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .Times(AtLeast(shards))
        .WillRepeatedly(InvokeArgument<1>());

    poller->start(io);
}

TEST_F(PollerTests, waitForTheFirstUpdateCompletesIfErrorOccuredOnTheFirstIterations) {
    auto shardsProvider = std::make_shared<StrictMock<ShardsProviderMock>>();
    poller = makePoller(uniqId, shardAdaptor, shardsProvider);

    {
    InSequence s;
    EXPECT_CALL(*shardsProvider, getAllShards(_))
        .Times(2)
        .WillOnce(Return(make_unexpected(ExplainedError(Error::metaRequestTimeout))));

    EXPECT_CALL(*shardsProvider, getAllShards(_))
        .WillRepeatedly(Return(generateShards(shards)));
    }

    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .Times(AtLeast(shards))
        .WillRepeatedly(InvokeArgument<1>());

    poller->start(io);
}

TEST_F(PollerTests, actuallyWaitsForTheFirstUpdate) {
    std::promise<void> allCallbacksCalledPromise;
    auto allCallbacksCalledFuture = allCallbacksCalledPromise.get_future();

    std::atomic<bool> waits{true};

    InSequence s;
    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .Times(shards - 1)
        .WillRepeatedly(DoAll(Invoke([&]() {
                                  ASSERT_TRUE(waits.load());
                              }),
                              InvokeArgument<1>()));

    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .WillOnce(DoAll(Invoke([&]() {
                            ASSERT_TRUE(waits.load());
                            allCallbacksCalledPromise.set_value();
                        }),
                        InvokeArgument<1>()));

    EXPECT_CALL(*shardAdaptor, resetHostCache(shard, finishHandler, errorHandler))
        .WillRepeatedly(InvokeArgument<1>());

    poller->start(io);
    waits.store(false);
    allCallbacksCalledFuture.wait();
}

}  // namespace
