#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "cache_mocks.h"
#include "profiler_mock.h"

#include <src/access_impl/change_log_cache.h>
#include "shard_mock.h"
#include <macs_pg/changelog/factory.h>
namespace {

using namespace doberman::testing;
using ::doberman::ChangeId;

struct ChangeComposer {
    auto operator()(const ::macs::Change& change, Yield) const {
        return std::make_shared<::doberman::logic::Change>(change.changeId(), change.revision(), nullptr);
    }
};

using Profiler = ::doberman::profiling::Profiler<ProfilerMock*>;
using ChangeLogCache = ::doberman::access_impl::ChangeLogCache<ShardMock*, ChangeComposer, Profiler, Yield>;

struct ChangeLogCacheTest : public Test {
    ShardMock shard;
    NiceMock<ProfilerMock> profiler;
    auto change(ChangeId id) {
        return macs::ChangeFactory{}.changeId(id).release();
    }
};

TEST_F(ChangeLogCacheTest, update_withIds_callsGetChangeWithIds) {
    ChangeLogCache cache(&shard, {}, &profiler);
    EXPECT_CALL(shard, getChanges(ElementsAre(1, 2, 3), _)).WillOnce(
            Return(std::vector<macs::Change>{change(1), change(3)}));
    cache.update(std::vector<ChangeId>{1, 2, 3}, Yield{});
}

TEST_F(ChangeLogCacheTest, update_withIds_cachesNonEmptyChanges) {
    ChangeLogCache cache(&shard, {}, &profiler);
    InSequence s;
    EXPECT_CALL(shard, getChanges(ElementsAre(1, 2, 3), _)).WillOnce(
                Return(std::vector<macs::Change>{change(1), change(3)}));
    cache.update(std::vector<ChangeId>{1, 2, 3}, Yield{});

    EXPECT_TRUE(cache.getChange(1));
    EXPECT_TRUE(cache.getChange(3));
    EXPECT_TRUE(!cache.getChange(2));
}

TEST_F(ChangeLogCacheTest, update_withIds_removesChangesWithAbsentIds) {
    ChangeLogCache cache(&shard, {}, &profiler);
    InSequence s;
    EXPECT_CALL(shard, getChanges(ElementsAre(1, 3), _)).WillOnce(
                Return(std::vector<macs::Change>{change(1), change(3)}));
    cache.update(std::vector<ChangeId>{1, 3}, Yield{});
    cache.update(std::vector<ChangeId>{3}, Yield{});

    EXPECT_TRUE(!cache.getChange(1));
    EXPECT_TRUE(cache.getChange(3));
}

TEST_F(ChangeLogCacheTest, update_withIds_callsGetChangeWithIdsForNonCachedIds) {
    ChangeLogCache cache(&shard, {}, &profiler);
    InSequence s;
    EXPECT_CALL(shard, getChanges(ElementsAre(1, 3), _)).WillOnce(
                Return(std::vector<macs::Change>{change(1), change(3)}));
    cache.update(std::vector<ChangeId>{1, 3}, Yield{});

    EXPECT_CALL(shard, getChanges(ElementsAre(2), _)).WillOnce(
                Return(std::vector<macs::Change>{change(2)}));
    cache.update(std::vector<ChangeId>{1, 2, 3}, Yield{});
}

}
