#include "wrap_yield.h"
#include "timer.h"
#include "log_mock.h"
#include "errors.h"

#include <src/access_impl/heartbeat.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::error_code;

using ::doberman::LaunchId;

struct WorkerIdControlMock {
    MOCK_METHOD(::macs::WorkerId, id, (), (const));
    MOCK_METHOD(bool, valid, (), (const));
    MOCK_METHOD(bool, validate, (), ());
    MOCK_METHOD(void, invalidate, (), ());
    MOCK_METHOD(Duration, ttl, (), (const));
    MOCK_METHOD(const LaunchId&, launchId, (), (const));
};

struct RunStatusMock {
    MOCK_METHOD(bool, get, (), (const));
    operator bool () const { return get(); }
    MOCK_METHOD(void, reset, (), (const));
};

struct ShardMock {
    auto& subscriptions() const { return *this; }
    MOCK_METHOD(bool, confirmTheJob, (::macs::WorkerId, std::string, Yield), (const));
};

using doberman::access_impl::makeHeartbeat;

struct HeartbeatTest : Test {
    Duration::Mock ttl;
    Duration::Mock retryTimeout;
    WorkerIdControlMock workerId;
    StrictMock<RunStatusMock> running;
    ShardMock shard;
    Yield yield;

    auto logger() { return ::logdog::none; }
};

#define EXPECT_CLEANUP(workerId, running)\
        EXPECT_CALL(workerId, invalidate()).WillOnce(Return());\
        EXPECT_CALL(running, reset()).WillOnce(Return())

TEST_F(HeartbeatTest, heartbeat_withRunningFalse_returnsWithCleanup) {
    NiceMock<WorkerIdControlMock> workerId;
    NiceMock<RunStatusMock> running;

    ON_CALL(workerId, valid()).WillByDefault(Return(true));
    const LaunchId launchId{};
    ON_CALL(workerId, launchId()).WillByDefault(ReturnRef(launchId));

    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(running, get()).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueNonValidWorkerId_returnsWithCleanup) {
    NiceMock<WorkerIdControlMock> workerId;
    NiceMock<RunStatusMock> running;

    ON_CALL(running, get()).WillByDefault(Return(true));
    const LaunchId launchId{};
    ON_CALL(workerId, launchId()).WillByDefault(ReturnRef(launchId));

    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, valid()).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueValidWorkerIdAndConfirmTheJobSucceded_validatesWorkerIdAndWaitsTimeout) {
    NiceMock<WorkerIdControlMock> workerId;
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    ON_CALL(workerId, id()).WillByDefault(Return("id"));
    const LaunchId launchId{};
    ON_CALL(workerId, launchId()).WillByDefault(ReturnRef(launchId));

    InSequence s;
    EXPECT_CALL(running, get()).WillOnce(Return(true));
    EXPECT_CALL(workerId, valid()).WillOnce(Return(true));
    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Return(true));
    EXPECT_CALL(workerId, validate()).WillOnce(Return(true));
    EXPECT_CALL(workerId, ttl()).WillOnce(Return(Duration(ttl)));
    EXPECT_CALL(ttl, wait(_));
    EXPECT_CALL(running, get()).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueConfirmTheJobSuccededByWorkerIdValidateFalse_returns) {
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, id()).WillOnce(Return("id"));
    const LaunchId launchId{};
    EXPECT_CALL(workerId, launchId()).WillRepeatedly(ReturnRef(launchId));

    InSequence s;
    EXPECT_CALL(running, get()).WillOnce(Return(true));
    EXPECT_CALL(workerId, valid()).WillOnce(Return(true));
    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Return(true));
    EXPECT_CALL(workerId, validate()).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueConfirmTheJobFalse_invalidatesWorkerIdAndResetsRunning) {
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, id()).WillOnce(Return("id"));
    const LaunchId launchId{};
    EXPECT_CALL(workerId, launchId()).WillRepeatedly(ReturnRef(launchId));

    InSequence s;
    EXPECT_CALL(running, get()).WillOnce(Return(true));
    EXPECT_CALL(workerId, valid()).WillOnce(Return(true));
    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueConfirmTheJobThrowsException_invalidatesWorkerIdAndResetsRunning) {
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, id()).WillOnce(Return("id"));
    const LaunchId launchId{};
    EXPECT_CALL(workerId, launchId()).WillRepeatedly(ReturnRef(launchId));

    InSequence s;
    EXPECT_CALL(running, get()).WillOnce(Return(true));
    EXPECT_CALL(workerId, valid()).WillOnce(Return(true));
    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Invoke([](auto,auto,auto){
        throw std::exception();
        return false;
    }));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueConfirmTheJobReturnsNonRetriableError_invalidatesWorkerIdAndResetsRunning) {
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, id()).WillOnce(Return("id"));
    const LaunchId launchId{};
    EXPECT_CALL(workerId, launchId()).WillRepeatedly(ReturnRef(launchId));

    InSequence s;
    EXPECT_CALL(running, get()).WillOnce(Return(true));
    EXPECT_CALL(workerId, valid()).WillOnce(Return(true));
    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Invoke([](auto,auto,Yield yield){
        yield.error(error_code(errors::nonretriable));
        return true;
    }));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

TEST_F(HeartbeatTest, heartbeat_withRunningTrueConfirmTheJobReturnsWithRetriableError_retriesCorfirmTheJobAfterWait) {
    auto heartbeat = makeHeartbeat(&shard, running, &workerId, logger(), Duration{retryTimeout});

    EXPECT_CALL(workerId, id()).WillRepeatedly(Return("id"));
    const LaunchId launchId{};
    EXPECT_CALL(workerId, launchId()).WillRepeatedly(ReturnRef(launchId));
    EXPECT_CALL(running, get()).WillRepeatedly(Return(true));
    EXPECT_CALL(workerId, valid()).WillRepeatedly(Return(true));

    InSequence s;

    EXPECT_CALL(shard, confirmTheJob(_,_,_))
            .WillOnce(Invoke([](auto,auto,Yield yield){
        yield.error(error_code(errors::retriable));
        return true;
    }));

    EXPECT_CALL(retryTimeout, wait(_));

    EXPECT_CALL(shard, confirmTheJob(_,_,_)).WillOnce(Return(false));

    EXPECT_CLEANUP(workerId, running);

    heartbeat(yield);
}

}
