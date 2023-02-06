#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/range.hpp>
#include <boost/range/adaptors.hpp>
#include "subscription_mock.h"


namespace {

using namespace ::testing;
using namespace ::doberman::testing;

using ::doberman::logic::SubscriptionData;
using ::doberman::SubscriptionState;

struct SubscriptionTest : public Test {
    SubscriptionAccessMock mock;
    SharedFolderAccessMock src;
    SubscribedFolderAccessMock dst;
    ChangeQueueAccessMock changes;

    auto makeSubscription() {
        ::doberman::LabelFilter labelFilter({{},{}});
        return ::doberman::logic::makeSubscription(
                SubscriptionData{dummySid, {{"OwnerUid"}, "Fid"}, {"SunscriberUid"}},
                &src, &dst, &changes, &mock, labelFilter);
    }
};

TEST_F(SubscriptionTest, state_callsGetStatusWithSid_returnsStatus) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, state(dummySid)).WillOnce(Return(SubscriptionState::init));
    EXPECT_EQ(subscription.state(), SubscriptionState::init);
}

TEST_F(SubscriptionTest, init_callsAccessInit_returnsStatus) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, init(dummySid)).WillOnce(Return(std::make_tuple(SubscriptionState::init, true)));

    EXPECT_EQ(subscription.init(), std::make_tuple(SubscriptionState::init, true));
}

TEST_F(SubscriptionTest, sync_callsAccessSync) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, sync(dummySid)).WillOnce(Return(SubscriptionState::sync));

    EXPECT_EQ(subscription.sync(), SubscriptionState::sync);
}

TEST_F(SubscriptionTest, fail_callsAccessFail) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, fail(dummySid, "fail reason")).WillOnce(Return(SubscriptionState::initFail));

    EXPECT_EQ(subscription.fail("fail reason"), SubscriptionState::initFail);
}

TEST_F(SubscriptionTest, finish_callsAccessFinish) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, finish(dummySid)).WillOnce(Return(SubscriptionState::terminated));

    EXPECT_EQ(subscription.finish(), SubscriptionState::terminated);
}

TEST_F(SubscriptionTest, clear_callsAccessClear) {
    auto subscription = makeSubscription();
    EXPECT_CALL(mock, clear(dummySid)).WillOnce(Return(std::make_tuple(SubscriptionState::clear, true)));

    EXPECT_EQ(subscription.clear(), std::make_tuple(SubscriptionState::clear, true));
}

}
