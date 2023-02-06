#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "change_queue_mock.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::logic::makeChangeQueue;

struct ChangeQueueTest : public Test {
    ChangeQueueAccessMock mock;
};

const ::doberman::SubscriptionId givenSid{ "uid", 999};
const ::doberman::Fid givenFid("7");

TEST_F(ChangeQueueTest, makeChangeQueue_withGivenSid_returnsQueueWithGivenSid) {
    auto queue = makeChangeQueue(givenSid, &mock);
    ASSERT_EQ(queue.subscriptionId(), givenSid);
}

TEST_F(ChangeQueueTest, pop_callsAccessPop_withGivenSid) {
    auto queue = makeChangeQueue(givenSid, &mock);
    EXPECT_CALL(mock, pop(givenSid)).WillOnce(Return());
    queue.pop();
}

TEST_F(ChangeQueueTest, top_callsAccessTop_withGivenSid) {
    auto queue = makeChangeQueue(givenSid, &mock);
    EXPECT_CALL(mock, top(givenSid)).WillOnce(Return(nullptr));
    queue.top();
}

TEST_F(ChangeQueueTest, top_callsAccessTop_returnsChange) {
    auto queue = makeChangeQueue(givenSid, &mock);
    EXPECT_CALL(mock, top(_)).WillOnce(Return(
            std::make_shared<doberman::logic::Change>(
                    666, 0, [](auto&){return ::doberman::error_code();})));
    EXPECT_EQ(queue.top()->id(), 666);
}

}
