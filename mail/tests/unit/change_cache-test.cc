#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "timer.h"
#include "cache_mocks.h"
#include "profiler_mock.h"
#include "log_mock.h"

#include <src/access_impl/change_cache.h>
#include "shard_mock.h"
#include "worker_id.h"
#include <macs_pg/changelog/factory.h>

namespace doberman {
namespace logic {

inline bool operator == (const Change& lhs, const Change& rhs) {
    return lhs.id() == rhs.id();
}

} // namespace logic
} // namespace doberman

namespace macs {
inline bool operator==(const macs::ChangeReference& lhs, const macs::ChangeReference& rhs) {
    return lhs.change == rhs.change
           && lhs.subscriptionId == rhs.subscriptionId
           && lhs.uid == rhs.uid;
}
}

namespace {

using namespace doberman::testing;
using ::doberman::ChangeId;
using ::doberman::logic::Change;

struct MockChangeLogCache {
public:
    struct Change {
        Change() = default;
        Change(std::shared_ptr<::doberman::logic::Change> change) : change(change) {}
        std::shared_ptr<::doberman::logic::Change> change;
        auto get(Yield) { return change; }
        bool resolved() const noexcept { return true; }
    };
    using ChangePtr = std::shared_ptr<Change>;
    MOCK_METHOD(ChangePtr, getChange, (ChangeId), (const));
    MOCK_METHOD(void, update_, (std::vector<ChangeId>, Yield), (const));
    template <typename SortedIdSequence>
    void update(const SortedIdSequence& ids, Yield yield) {
        update_(std::vector<ChangeId>{ids.begin(), ids.end()}, yield);
    }
};

using Profiler = ::doberman::profiling::Profiler<ProfilerMock*>;
using LogMock = NiceMock<doberman::testing::LogMock>;
using ChangeChunk = std::vector<macs::ChangeReference>;
using ChangeCache = ::doberman::access_impl::ChangeCache<StrictMock<ShardMock>*, MockChangeLogCache*, Profiler,
        doberman::testing::WorkerId, ::logdog::none_t, MutexMock, ClockMock>;
using ChangeCachePtr = std::shared_ptr<ChangeCache>;

struct ChangeCacheTest : public Test {
    StrictMock<ShardMock> shard;
    MockChangeLogCache changeLogCache;
    NiceMock<ProfilerMock> profiler;
    TimerMock timer;
    auto change(ChangeId id) {
        return Change{id, {}, {}};
    }
    auto changeptr(ChangeId id) {
        return std::make_shared<MockChangeLogCache::Change>(std::make_shared<Change>(change(id)));
    }
    ChangeCachePtr makeChangeCache(doberman::testing::WorkerId workerId) {
        return std::make_shared<ChangeCache>(workerId, &shard, ttl, flushTimeout, &changeLogCache, &profiler, ::logdog::none);
    }

    const int ttl = 100;
    const int flushTimeout = 2;
};

TEST_F(ChangeCacheTest, get_firstCallWithEmptyCache_callsFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});
    EXPECT_CALL(changeLogCache, update_(_, _)).WillOnce(Return());
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(ChangeChunk{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache->get("uid", 666, Yield{});
}

TEST_F(ChangeCacheTest, get_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId", []{ return false;}});
    EXPECT_THROW(cache->get("uid", 666, Yield{}), doberman::WorkerIdOutdated);
}


TEST_F(ChangeCacheTest, get_withUidSubscriptionId_returnsChangeWithMinimumId) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    const ChangeChunk changes{
        {5, "uid", 666}, {2, "uid", 666}, {7, "uid", 666}, {2, "uid", 777}, {2, "uid2", 666}
    };
    EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(changes));
    EXPECT_CALL(changeLogCache, update_(ElementsAre(2, 5, 7), _)).WillOnce(Return());
    EXPECT_CALL(changeLogCache, getChange(5)).WillOnce(Return(changeptr(5)));
    EXPECT_CALL(changeLogCache, getChange(2)).WillOnce(Return(changeptr(2)));
    EXPECT_CALL(changeLogCache, getChange(7)).WillOnce(Return(changeptr(7)));
    EXPECT_CALL(changeLogCache, getChange(2)).WillOnce(Return(changeptr(2)));
    EXPECT_CALL(changeLogCache, getChange(2)).WillOnce(Return(changeptr(2)));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    EXPECT_EQ(*cache->get("uid", 666, Yield{}), change(2));
}

TEST_F(ChangeCacheTest, get_withUidSubscriptionId_ommitCachedChanges) {
    ClockMock clock;
    MutexMock mutex;

    const ChangeChunk changes{ {5, "uid", 666}, {2, "uid", 666} };
    EXPECT_CALL(shard, get("WorkerId", _)).WillRepeatedly(Return(changes));

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));

    ChangeCachePtr cache = makeChangeCache({"WorkerId"});

    // First call with cache being filled
    {
        InSequence s;

        EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
        EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(changes));
        EXPECT_CALL(changeLogCache, update_(ElementsAre(2, 5), _)).WillOnce(Return());
        EXPECT_CALL(changeLogCache, getChange(5)).WillOnce(Return(changeptr(5)));
        EXPECT_CALL(changeLogCache, getChange(2)).WillOnce(Return(changeptr(2)));
        EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    }
    cache->get("uid", 666, Yield{});

    // Second call with same Changes from shard
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1200));
    {
        InSequence s;

        EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
        EXPECT_CALL(changeLogCache, update_(ElementsAre(), _)).WillOnce(Return());
        EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    }
    EXPECT_TRUE(!cache->get("noncached_uid", 777, Yield{}));
}

TEST_F(ChangeCacheTest, get_secondCallWithEmptyCacheWithinTtl_doesNotCallFetch) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});
    EXPECT_CALL(changeLogCache, update_(_, _)).WillOnce(Return());
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(ChangeChunk{}));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache->get("uid", 666, Yield{});
    cache->get("uid", 666, Yield{});
}

TEST_F(ChangeCacheTest, get_secondCallWithEmptyCacheOutOfTtl_callsFetch) {
    ClockMock clock;
    MutexMock mutex;

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});
    Sequence s;
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, get("WorkerId", _)).InSequence(s).WillOnce(Return(ChangeChunk{}));
    EXPECT_CALL(changeLogCache, update_(_, _)).WillOnce(Return());
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache->get("uid", 666, Yield{});

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1200));
    EXPECT_CALL(mutex, lock_(_, _)).InSequence(s).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, get("WorkerId", _)).InSequence(s).WillOnce(Return(ChangeChunk{}));
    EXPECT_CALL(changeLogCache, update_(_, _)).WillOnce(Return());
    EXPECT_CALL(mutex, unlock_(_)).InSequence(s).WillOnce(Return());

    cache->get("uid", 666, Yield{});
}

TEST_F(ChangeCacheTest, remove_withUidSubscriptionIdAndChangeId_callsRepoRemoveAndRemovesChangeFromCache) {
    ClockMock clock;
    MutexMock mutex;
    SpawnMock spawn;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});

    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    const ChangeChunk changes{ {5, "uid", 666}, {2, "uid", 666} };
    EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(changes));
    EXPECT_CALL(changeLogCache, update_(ElementsAre(2, 5), _)).WillOnce(Return());
    EXPECT_CALL(changeLogCache, getChange(5)).WillOnce(Return(changeptr(5)));
    EXPECT_CALL(changeLogCache, getChange(2)).WillOnce(Return(changeptr(2)));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());

    Yield yield;
    yield.spawnMock_ = &spawn;
    EXPECT_EQ(*cache->get("uid", 666, yield), change(2));

    EXPECT_CALL(spawn, spawn(_)).WillOnce(InvokeArgument<0>());
    EXPECT_CALL(timer, wait_(flushTimeout)).Times(1);
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, remove(Trash{{2, "uid", 666}}, _)).WillOnce(Return());
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache->remove("uid", 666, 2, yield);
    EXPECT_EQ(*cache->get("uid", 666, yield), change(5));
}

TEST_F(ChangeCacheTest, remove_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    ClockMock clock;
    MutexMock mutex;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId", []{ return false;}});
    EXPECT_THROW(cache->remove("uid", 666, 2, Yield{}), doberman::WorkerIdOutdated);
}

TEST_F(ChangeCacheTest, remove_withOutdatedWorkerId_throwsWorkerIdOutdatedInFlushCoroutine) {
    ClockMock clock;
    MutexMock mutex;
    SpawnMock spawn;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));

    auto validatorMock = []() -> bool {
        static int callCounter = 0;
        return callCounter++ < 1;
    };
    ChangeCachePtr cache = makeChangeCache({"WorkerId", validatorMock});
    Yield yield;
    yield.spawnMock_ = &spawn;

    EXPECT_CALL(spawn, spawn(_)).WillOnce(InvokeArgument<0>());
    EXPECT_CALL(timer, wait_(flushTimeout)).Times(1);
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    cache->remove("uid", 666, 2, yield);
}

TEST_F(ChangeCacheTest, remove_manyUidSubscriptionIdAndChangeId_callsBulkRepoRemove) {
    ClockMock clock;
    MutexMock mutex;
    SpawnMock spawn;
    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(1000));
    ChangeCachePtr cache = makeChangeCache({"WorkerId"});

    const ChangeChunk changes{{1, "uid",  666},
                              {2, "uid",  666},
                              {3, "uid2", 666},
                              {4, "uid2", 666},
                              {5, "uid",  777}};
    Yield yield;
    yield.spawnMock_ = &spawn;

    InSequence s;
    EXPECT_CALL(spawn, spawn(_)).WillOnce(Return());
    for (const auto& change : changes) {
        cache->remove(change.uid, change.subscriptionId, change.change, yield);
    }

    EXPECT_CALL(clock, now_()).WillRepeatedly(Return(2000));
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(MutexMock::LockHandle(1)));
    EXPECT_CALL(shard, remove(changes, _)).Times(1);
    EXPECT_CALL(shard, get("WorkerId", _)).WillOnce(Return(ChangeChunk{}));
    EXPECT_CALL(changeLogCache, update_(ElementsAre(), _)).WillOnce(Return());
    EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());

    // trigger for remove from shard.remove
    EXPECT_EQ(cache->get("noncached_uid", 777, yield), nullptr);
}

}
