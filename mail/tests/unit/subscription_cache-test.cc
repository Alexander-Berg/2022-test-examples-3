#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "timer.h"
#include "worker_id.h"
#include "cache_mocks.h"
#include <src/access_impl/subscription_cache.h>
#include <macs_pg/subscription/factory.h>
#include <macs_pg/subscription/subscription_state.h>
#include "shard_mock.h"

namespace macs {

inline bool operator==(const Subscription& lhs, const Subscription& rhs) {
    return lhs.workerId() == rhs.workerId()
            && lhs.uid() == rhs.uid()
            && lhs.subscriptionId() == rhs.subscriptionId()
            && lhs.state() == rhs.state();
}

}

namespace {

using doberman::testing::WorkerId;

using SubscriptionCache = ::doberman::access_impl::SubscriptionCache<ShardMock*, WorkerId, MutexMock, ClockMock>;
using SubscriptionVec = std::vector<macs::Subscription>;
struct SubscriptionCacheTest : public Test {
    ShardMock shard;

    auto subscription(macs::WorkerId wid,
                      macs::Uid uid,
                      macs::SubscriptionId sid,
                      macs::pg::SubscriptionState state = macs::pg::SubscriptionState::init) {
        macs::SubscriptionFactory ret;
        return ret.workerId(wid)
                .uid(uid)
                .subscriptionId(sid)
                .state(state)
                .release();
    }
};

TEST_F(SubscriptionCacheTest, getByWorker_callWithEmptyCache_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache.getReserved(Yield{});
}

TEST_F(SubscriptionCacheTest, getByWorker_secondCallWithCacheWithinTtl_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);

    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());

    cache.getReserved(Yield{});
    cache.getReserved(Yield{});
}

TEST_F(SubscriptionCacheTest, getByWorker_secondCallWithCacheOutOfTtl_callsFetch) {
    ClockMock clock;
    MutexMock mutex;

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    Sequence s;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache.getReserved(Yield{});

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1200));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache.getReserved(Yield{});
}

TEST_F(SubscriptionCacheTest, getById_withNoneEmptyCache_returnsStoredSubscription) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    SubscriptionVec subscriptions{
        subscription("WorkerId", "uid", 1),
        subscription("WorkerId", "uid", 2),
        subscription("WorkerId", "uid", 3),
    };
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache.getReserved(Yield{});

    EXPECT_EQ(cache.getById("uid", 2, Yield{}), subscriptions[1]);
}

TEST_F(SubscriptionCacheTest, getById_withEmptyCache_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    SubscriptionVec subscriptions{
        subscription("WorkerId", "uid", 1),
        subscription("WorkerId", "uid", 2),
        subscription("WorkerId", "uid", 3),
    };
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());

    EXPECT_EQ(cache.getById("uid", 1, Yield{}), subscriptions[0]);
    EXPECT_EQ(cache.getById("uid", 2, Yield{}), subscriptions[1]);
}

TEST_F(SubscriptionCacheTest, getById_whenNotFoundInCache_beforeLeastExpired_callsWait_thenCallsFetch) {
    ClockMock clock;
    MutexMock mutex;
    TimerMock timer;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    Sequence s1, s2;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s1).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s1).WillOnce(Return());

    cache.getReserved(Yield{});

    EXPECT_CALL(timer, wait_(10)).Times(1).InSequence(s1, s2);
    EXPECT_CALL(clock, now_()).InSequence(s2).WillRepeatedly(Return(1020));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s1).WillOnce(Return(MutexMock::LockHandle(1)));
    SubscriptionVec subscriptions{
        subscription("WorkerId", "uid", 1),
        subscription("WorkerId", "uid", 2),
        subscription("WorkerId", "uid", 3),
    };
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s1).WillOnce(Return());

    EXPECT_EQ(cache.getById("uid", 2, Yield{}), subscriptions[1]);
}

TEST_F(SubscriptionCacheTest, getById_whenNotFoundInCache_withLeastExpired_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    Sequence s;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache.getReserved(Yield{});

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1020));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    SubscriptionVec subscriptions{
        subscription("WorkerId", "uid", 1),
        subscription("WorkerId", "uid", 2),
        subscription("WorkerId", "uid", 3),
    };
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    EXPECT_EQ(cache.getById("uid", 2, Yield{}), subscriptions[1]);
}

TEST_F(SubscriptionCacheTest, getById_whenFoundInCache_withCacheExpired_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);

    SubscriptionVec subscriptions{
        subscription("WorkerId", "uid", 1),
        subscription("WorkerId", "uid", 2),
        subscription("WorkerId", "uid", 3),
    };

    Sequence s;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache.getReserved(Yield{});

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1200));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(subscriptions));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    EXPECT_EQ(cache.getById("uid", 2, Yield{}), subscriptions[1]);
}

TEST_F(SubscriptionCacheTest, getById_whenNotFoundAtAll_throwsException) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);
    Sequence s;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).InSequence(s).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache.getReserved(Yield{});

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1020));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).InSequence(s).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    EXPECT_THROW(cache.getById("uid", 2, Yield{}), std::logic_error);
}

TEST_F(SubscriptionCacheTest, transit_withUidSubscriptionId_callsTransitAndModifyCache) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);

    using Action = macs::pg::SubscriptionAction;
    const auto sub = subscription("WorkerId", "uid", 2, macs::pg::SubscriptionState::sync);
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    EXPECT_CALL(shard, transitState("uid", 2, Action{Action::synchronization}, _))
            .WillOnce(Return(sub));

    cache.getReserved(Yield{});

    EXPECT_EQ(cache.transitState("uid", 2, Action{Action::synchronization}, Yield{}), sub);
    EXPECT_EQ(cache.getById("uid", 2, Yield{}), sub);
}

TEST_F(SubscriptionCacheTest, fail_withUidSubscriptionId_callsFailAndModifyCache) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    SubscriptionCache cache({"WorkerId"}, &shard, 100, 10);

    const auto sub = subscription("WorkerId", "uid", 2, macs::pg::SubscriptionState::initFail);
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, getByWorker("WorkerId", _)).WillOnce(Return(SubscriptionVec{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    EXPECT_CALL(shard, markFailed("uid", 2, "failed", _))
            .WillOnce(Return(sub));

    cache.getReserved(Yield{});

    EXPECT_EQ(cache.markFailed("uid", 2, "failed", Yield{}), sub);
    EXPECT_EQ(cache.getById("uid", 2, Yield{}), sub);
}

}
