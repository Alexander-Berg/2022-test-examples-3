#ifndef DOBERMAN_TESTS_SHARD_MOCK_H_
#define DOBERMAN_TESTS_SHARD_MOCK_H_

#include <gmock/gmock.h>
#include <src/meta/types.h>
#include <macs_pg/changelog/change.h>
#include <macs_pg/subscription/subscription.h>
#include <macs_pg/subscription/subscription_action.h>

#include "macs_change_io.h"
#include "wrap_yield.h"

namespace doberman {
namespace testing {

using ChangeIdVector = std::vector<::macs::ChangeId>;
using SubscriptionAction = ::macs::pg::SubscriptionAction;
using Trash = std::vector<::macs::ChangeReference>;

struct ShardMock {
    const ShardMock* operator()(::doberman::SubscriptionId id) const { return shard(id); }
    MOCK_METHOD(const ShardMock*, shard, (::doberman::SubscriptionId), (const));
    const ShardMock* operator()(::macs::WorkerId id) const { return shard(id); }
    MOCK_METHOD(const ShardMock*, shard, (::macs::WorkerId), (const));

    // changeLog
    auto& changeLog() const {return *this;}
    MOCK_METHOD(boost::optional<::macs::Change>, getChange, (::macs::ChangeId, Yield), (const));
    MOCK_METHOD(std::vector<::macs::Change>, getChanges, (std::vector<::macs::ChangeId>, Yield), (const));

    // changeQueue
    auto& changeQueue() const {return *this;}
    MOCK_METHOD(boost::optional<macs::Change>, get, (Uid, ::macs::SubscriptionId, Yield), (const));
    MOCK_METHOD(ChangeIdVector, get, (Uid, ::macs::SubscriptionId, size_t, Yield), (const));
    MOCK_METHOD(std::vector<macs::ChangeReference>, get, (macs::WorkerId, Yield), (const));
    MOCK_METHOD(void, remove, (Trash, Yield), (const));

    auto& subscriptions() const { return *this;}
    MOCK_METHOD(::macs::Subscription, transitState, (Uid, ::macs::SubscriptionId, SubscriptionAction, Yield), (const));
    MOCK_METHOD(::macs::Subscription, markFailed, (Uid, ::macs::SubscriptionId, std::string, Yield), (const));
    MOCK_METHOD(::macs::Subscription, getById, (Uid, ::macs::SubscriptionId, Yield), (const));
    MOCK_METHOD(std::vector<::macs::Subscription>, getByWorker, (::macs::WorkerId, Yield), (const));
    MOCK_METHOD(std::vector<::macs::Subscription>, getFreeForWorker, (::macs::WorkerId, std::size_t, Yield), (const));
    MOCK_METHOD(void, release, (Uid, ::macs::SubscriptionId, ::macs::WorkerId, Yield), (const));
};

} // namespace testing
} // namespace doberman

#endif /* DOBERMAN_TESTS_SHARD_MOCK_H_ */
