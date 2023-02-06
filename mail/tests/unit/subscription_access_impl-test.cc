#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "wrap_yield.h"
#include "timer.h" // must be included before all - substitute for src/access_impl/timer.h
#include <src/access_impl/subscription.h>
#include <boost/range.hpp>
#include <boost/range/adaptors.hpp>
#include <macs_pg/subscription/subscription.h>

#include "shard_mock.h"

namespace {

using namespace ::testing;
using ::doberman::testing::Yield;
using ::doberman::Uid;
using ::doberman::logic::SubscriptionData;
using ::doberman::SubscriptionId;
using State = ::doberman::SubscriptionState;
using Action = ::macs::pg::SubscriptionAction;
using ShardMock = ::doberman::testing::ShardMock;

struct InitResourceMock {
    MOCK_METHOD(bool, get, (const doberman::logic::Subscriber&, Yield), (const));
};

struct IoMock {
    MOCK_METHOD(::macs::Subscription, transitState, (Uid, ::macs::SubscriptionId, Action, Yield), (const));
    MOCK_METHOD(::macs::Subscription, markFailed, (Uid, ::macs::SubscriptionId, std::string, Yield), (const));
    MOCK_METHOD(::macs::Subscription, getById, (Uid, ::macs::SubscriptionId, Yield), (const));
};
struct SubscriptionAccessImplTest : public Test {
    StrictMock<IoMock> mock;
    InitResourceMock initResourceMock;
    ::doberman::access_impl::Subscription<IoMock*, InitResourceMock*> accessImpl{&mock, &initResourceMock};
};

TEST_F(SubscriptionAccessImplTest, fail_withContext_callsMarkFailedAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, markFailed("OwnerUid", 999, "fail reason", _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::initFail).release()));
    EXPECT_EQ(accessImpl.fail(ctx, "fail reason", Yield{}), State::initFail);
}

TEST_F(SubscriptionAccessImplTest, state_withContext_callsGetByIdAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, getById("OwnerUid", 999, _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::init).release()));
    EXPECT_EQ(accessImpl.state(ctx, Yield{}), State::init);
}

TEST_F(SubscriptionAccessImplTest, init_withContext_callsTransitStateWithActionInitializationAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, transitState("OwnerUid", 999, Action{Action::initialization}, _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::init).release()));
    EXPECT_CALL(initResourceMock, get(_, _)).WillOnce(Return(true));
    EXPECT_EQ(accessImpl.init(ctx, Yield{}), std::make_tuple(State::init, true));
}

TEST_F(SubscriptionAccessImplTest, sync_withContext_callsTransitStateWithActionSynchronizationAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, transitState("OwnerUid", 999, Action{Action::synchronization}, _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::sync).release()));
    EXPECT_EQ(accessImpl.sync(ctx, Yield{}), State::sync);
}

TEST_F(SubscriptionAccessImplTest, finish_withContext_callsTransitStateWithActionTerminationAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, transitState("OwnerUid", 999, Action{Action::termination}, _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::terminated).release()));
    EXPECT_EQ(accessImpl.finish(ctx, Yield{}), State::terminated);
}

TEST_F(SubscriptionAccessImplTest, clear_withContext_callsTransitStateWithActionSynchronizationAndReturnsState) {
    const auto ctx = accessImpl.makeContext({"OwnerUid", 999}, {{"SubscriberUid"}});
    EXPECT_CALL(mock, transitState("OwnerUid", 999, Action{Action::clearing}, _)).WillOnce(
            Return(macs::SubscriptionFactory{}.state(State::clear).release()));
    EXPECT_CALL(initResourceMock, get(_, _)).WillOnce(Return(true));
    EXPECT_EQ(accessImpl.clear(ctx, Yield{}), std::make_tuple(State::clear, true));
}

}
