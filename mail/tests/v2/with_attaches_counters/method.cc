#include <utility>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/with_attaches_counters/method.h>
#include "../helpers.h"
#include <macs/hooks.h>

namespace {

using namespace hound::server::handlers::v2::with_attaches_counters;
using namespace hound::testing;
using namespace ::testing;

struct MailboxMock {
    MOCK_METHOD(macs::AttachesCounters, getAttachesCounters, (), (const));
};

struct TestMailbox {
    MailboxMock& mailbox;

    macs::AttachesCounters getAttachesCounters() const {
        return mailbox.getAttachesCounters();
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(std::move(uid)); }
};

struct WithAttachesCountersMethod : Test {
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    Method<MailboxGetter> method{MailboxGetter{getMailbox}};
};

TEST_F(WithAttachesCountersMethod, should_return_invalid_argument_for_empty_uid) {
    const auto res = method(Request{""});
    EXPECT_EQ(res.error(), error_code{error::invalidArgument});
}

TEST_F(WithAttachesCountersMethod, should_return_attaches_count_for_correct_uid) {
    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, getAttachesCounters()).WillOnce(Return(macs::AttachesCounters{10, 3}));

    const auto res = method(Request{"uid"});

    EXPECT_EQ(res.value().messagesCount, 10U);
    EXPECT_EQ(res.value().newMessagesCount, 7U);
}

} // namespace
