#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "timer.h" // must be included before all - substitute for src/access_impl/timer.h

#include <macs_pg/changelog/factory.h>
#include <src/access_impl/change_queue.h>

#include "change_io.h"
#include "mailbox_mock.h"
#include "shard_mock.h"
#include "wrap_yield.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::Uid;
using ::doberman::SubscriptionId;
using ::macs::ChangeId;

const SubscriptionId someSid{"SomeOwnerUid", 13};
const ::macs::ChangeId someChangedId = 100500;
const ::doberman::access_impl::ChangeQueueTimes changeQueueTimes{5, 2};
using ::doberman::logic::Change;

struct ChangeCacheMock {
    using Change = ::doberman::logic::Change;
    using ChangePtr = std::shared_ptr<Change>;

    MOCK_METHOD(ChangePtr, get, (Uid, ::macs::SubscriptionId, Yield), ());
    MOCK_METHOD(void, remove, (Uid, ::macs::SubscriptionId sid, ChangeId, Yield), ());
};

struct ChangeQueueAccessImplTest : public Test {
    StrictMock<ChangeCacheMock> cacheMock;

    ::doberman::access_impl::ChangeQueue<ChangeCacheMock> accessImpl{
            cacheMock,
            changeQueueTimes};

    auto makeCtx(SubscriptionId sid) const {
        return sid;
    }
    auto change(ChangeId id) {
        return std::make_shared<Change>(id, macs::Revision{}, nullptr);
    }
};

TEST_F(ChangeQueueAccessImplTest, top_CallChangeQueueGetWithSubscriptionId) {
    EXPECT_CALL(cacheMock, get("OwnerUid", 42, _)).
        WillRepeatedly(Return(change(1)));
    accessImpl.top(makeCtx(SubscriptionId{"OwnerUid", 42}), Yield());
}

TEST_F(ChangeQueueAccessImplTest, top_RetunNoneWhenChangeQueueGetReturnsNoChangeIds5Times) {
    EXPECT_CALL(cacheMock, get(_, _, _)).
        Times(5).
        WillRepeatedly(Return(nullptr));
    EXPECT_TRUE(!accessImpl.top(makeCtx(someSid), Yield()));
}

TEST_F(ChangeQueueAccessImplTest, top_RetunChangeWhenChangeQueueReturnChangeIdOnThirdTryThenCallGetChange) {
    Expectation emptyCalls = EXPECT_CALL(cacheMock, get(_, _, _)).
        Times(2).
        WillRepeatedly(Return(nullptr));
    EXPECT_CALL(cacheMock, get(_, _, _)).
        After(emptyCalls).
        WillOnce(Return(change(someChangedId)));

    EXPECT_TRUE(accessImpl.top(makeCtx(someSid), Yield()));
}

TEST_F(ChangeQueueAccessImplTest, top_CallChangeQueueGet) {
    EXPECT_CALL(cacheMock, get("OwnerUid", 42, _)).WillOnce(Return(change(100)));

    accessImpl.top(makeCtx(SubscriptionId{"OwnerUid", 42}), Yield());
}

TEST_F(ChangeQueueAccessImplTest, top_ReturnsChangeWithRightId) {
    EXPECT_CALL(cacheMock, get(_, _, _)).WillOnce(Return(change(100)));

    auto ret = accessImpl.top(makeCtx(someSid), Yield());

    EXPECT_EQ(ret->id(), 100);
}

TEST_F(ChangeQueueAccessImplTest, pop_ThrowWhenQueueCacheIsEmpty) {
    EXPECT_CALL(cacheMock, get(_, _, _)).WillRepeatedly(Return(nullptr));
    EXPECT_THROW(accessImpl.pop(makeCtx(someSid), Yield()),
                 std::logic_error);
}

TEST_F(ChangeQueueAccessImplTest, pop_CallChangeQueueRemoveOnFirstElementFromCache) {
    EXPECT_CALL(cacheMock, get(_, _, _)).WillOnce(Return(change(100)));
    EXPECT_CALL(cacheMock, remove("OwnerUid", 42, 100, _));

    accessImpl.pop(makeCtx({"OwnerUid", 42}), Yield());
}

}
