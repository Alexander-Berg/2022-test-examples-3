#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "wrap_yield.h"
#include "timer.h" // must be included before all - substitute for src/access_impl/timer.h
#include "subscription_repository_mock.h"
#include "subscription_io.h"
#include <src/access_impl/subscription_repository.h>
#include <macs_pg/subscription/factory.h>
#include <macs_pg/subscription/subscription.h>
#include <macs_pg/subscription/subscription_action.h>

namespace macs {

inline bool operator == (const Subscription& lhs, const Subscription& rhs) {
    return lhs.uid() == rhs.uid() && lhs.subscriptionId() == rhs.subscriptionId();
}

}

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using doberman::SubscriptionId;
using doberman::logic::SubscriptionData;
using ::doberman::Uid;
using ::doberman::Fid;
using ::doberman::Mid;
using ::doberman::WorkerId;

using ::macs::pg::SubscriptionAction;

struct IoMock {
    MOCK_METHOD(::macs::Subscription, transitState, (Uid, ::macs::SubscriptionId, SubscriptionAction, Yield), (const));
    MOCK_METHOD(::macs::Subscription, markFailed, (Uid, ::macs::SubscriptionId, std::string, Yield), (const));
    MOCK_METHOD(::macs::Subscription, getById, (Uid, ::macs::SubscriptionId, Yield), (const));
    MOCK_METHOD(std::vector<::macs::Subscription>, getReserved, (Yield), (const));
    MOCK_METHOD(std::vector<::macs::Subscription>, reserve, (std::size_t, Yield), (const));
    MOCK_METHOD(void, release, (Uid, ::macs::SubscriptionId, Yield), (const));
};

struct SubscriptionRepositoryAccessImplTest : public Test {
    ::doberman::access_impl::SubscriptionRepositoryTimes sleepTimes{1, 100};
    StrictMock<IoMock> mock;
    ::doberman::access_impl::SubscriptionRepository<IoMock*> accessImpl{&mock, sleepTimes};

    static auto subscription(Uid owner, macs::SubscriptionId id, Fid fid, Uid subscriber) {
        return ::macs::SubscriptionFactory{}.uid(owner).subscriptionId(id)
                .fid(fid).subscriberUid(subscriber).release();
    }
};

TEST_F(SubscriptionRepositoryAccessImplTest, release_withContextAndSubscriptionId_callsRepoRelease) {
    EXPECT_CALL(mock, release("owner", 999, _)).WillOnce(Return());
    auto ctx = accessImpl.makeContext();
    accessImpl.release(ctx, {"owner", 999}, Yield());
}

TEST_F(SubscriptionRepositoryAccessImplTest, reserve_withCredit_callsGetFreeForWorkerWithCredit) {
    EXPECT_CALL(mock, reserve(100500, _))
        .WillOnce(Return(std::vector<macs::Subscription>()));

    auto ctx = accessImpl.makeContext();
    accessImpl.reserve(ctx, 100500, Yield());
}

TEST_F(SubscriptionRepositoryAccessImplTest, reserve_withZeroCredit_doesNotCallGetFreeForWorkerWithCredit) {
    auto ctx = accessImpl.makeContext();
    accessImpl.reserve(ctx, 0, Yield());
}

TEST_F(SubscriptionRepositoryAccessImplTest, getReserved_withContext_callsGetByWorker) {
    EXPECT_CALL(mock, getReserved(_))
        .WillOnce(Return(std::vector<macs::Subscription>{}));
    auto ctx = accessImpl.makeContext();
    accessImpl.getReserved(ctx, Yield());
}

}
