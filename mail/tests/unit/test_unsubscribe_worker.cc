#include "timer_mock.h"
#include "wrap_mock.h"

#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include "error.h"
#include <internal/unsubscribe_worker/worker.h>
#include <macs_pg/subscription/unsubscribe_task_factory.h>
#include <macs/io.h>

using namespace testing;

namespace york {
namespace tests {

using Task = worker::UnsubscribeTask;

struct TestConfig {
    const UnsubscribeWorkerCfg worker;
};

struct ShardGetterMock {
    MOCK_METHOD(void, get, (ShardId), (const));
    void operator()(ShardId id) const {
        get(id);
    }
};

struct UnsubscribeMock {
    MOCK_METHOD(void, run, (Task), (const));
};

struct UnsubscribeWorkerTest: public Test {
    SharpeiClientMock<sync> sharpei;
    ShardMock<sync> shard;
    ShardGetterMock shardGetter;
    UnsubscribeMock unsubscribe;
    LoggerMock logger;

    auto makeWorker(UnsubscribeWorkerCfg cfg = UnsubscribeWorkerCfg{}) {
        return worker::makeUnsubscribeWorker(
                    std::make_shared<TestConfig>(TestConfig{cfg}),
                    &sharpei,
                    [&](ShardId id, auto, auto) { shardGetter(id); return &shard; },
                    [&](worker::UnsubscribeTask task) { unsubscribe.run(task); },
                    log::make_log(log::none, &logger),
                    TimerMock());
    }
};

TEST_F(UnsubscribeWorkerTest, run_getsShardsFromSharpeiClient_ifEmpty_doesNothing) {
    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(ShardMap{}));

    makeWorker().run(int());
}

TEST_F(UnsubscribeWorkerTest, run_getsTasksFromEveryShard_ifEmpty_doesNothing) {
    ShardMap shardMap;
    shardMap["1"] = Shard();
    shardMap["2"] = Shard();
    shardMap["3"] = Shard();

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shardGetter, get("1"));
    EXPECT_CALL(shardGetter, get("2"));
    EXPECT_CALL(shardGetter, get("3"));
    EXPECT_CALL(shard, getUnsubscribeTasks(10, std::chrono::seconds(120), _))
            .Times(3)
            .WillRepeatedly(Return(std::vector<Task>{}));

    makeWorker(UnsubscribeWorkerCfg{60, 10}).run(int());
}

TEST_F(UnsubscribeWorkerTest, run_getsTasks_ifNotEmpty_logsInfo_processTasks) {
    ShardMap shardMap;
    shardMap["1"] = Shard();

    std::vector<Task> tasks {
        macs::UnsubscribeTaskFactory().taskRequestId("id1").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id2").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id3").release()
    };

    InSequence seq;
    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shardGetter, get("1"));
    EXPECT_CALL(shard, getUnsubscribeTasks(10, std::chrono::seconds(120), _))
            .WillOnce(Return(tasks));

    EXPECT_CALL(logger, notice(_)).Times(1);
    for (const Task& t: tasks) {
        EXPECT_CALL(unsubscribe, run(t)).Times(1);
    }

    makeWorker(UnsubscribeWorkerCfg{60, 10}).run(int());
}

TEST_F(UnsubscribeWorkerTest, run_getsTasksFromShardsWhileLessThanLimit_logsInfo_processTasks) {
    ShardMap shardMap;
    shardMap["1"] = Shard();
    shardMap["2"] = Shard();
    shardMap["3"] = Shard();

    std::vector<Task> tasks1 {
        macs::UnsubscribeTaskFactory().taskRequestId("id1").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id2").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id3").release()
    };

    std::vector<Task> tasks2 {
        macs::UnsubscribeTaskFactory().taskRequestId("id4").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id5").release(),
    };

    InSequence seq;
    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shardGetter, get(_));
    EXPECT_CALL(shard, getUnsubscribeTasks(5, std::chrono::seconds(120), _))
            .WillOnce(Return(tasks1));
    EXPECT_CALL(shardGetter, get(_));
    EXPECT_CALL(shard, getUnsubscribeTasks(2, std::chrono::seconds(120), _))
            .WillOnce(Return(tasks2));

    EXPECT_CALL(logger, notice(_)).Times(1);
    for (const Task& t: boost::join(tasks1, tasks2)) {
        EXPECT_CALL(unsubscribe, run(t)).Times(1);
    }

    makeWorker(UnsubscribeWorkerCfg{60, 5}).run(int());
}

TEST_F(UnsubscribeWorkerTest, run_withErrorInSharpeiStat_trowsError) {
    InSequence seq;
    EXPECT_CALL(sharpei, stat(_))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeWorker(UnsubscribeWorkerCfg{60, 10}).run(int()),
                 boost::system::system_error);
}

TEST_F(UnsubscribeWorkerTest, run_withErrorInGetUnsubscribeTasks_trowsError) {
    ShardMap shardMap;
    shardMap["1"] = Shard();

    std::vector<Task> tasks {
        macs::UnsubscribeTaskFactory().taskRequestId("id1").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id2").release(),
        macs::UnsubscribeTaskFactory().taskRequestId("id3").release()
    };

    InSequence seq;
    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shardGetter, get("1"));
    EXPECT_CALL(shard, getUnsubscribeTasks(10, std::chrono::seconds(120), _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeWorker(UnsubscribeWorkerCfg{60, 10}).run(int()),
                 boost::system::system_error);
}

} // namespace tests
} // namespace york
