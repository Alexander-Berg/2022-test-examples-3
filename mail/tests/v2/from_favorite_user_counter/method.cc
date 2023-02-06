#include <utility>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/from_favorite_user_counter/method.h>
#include "../helpers.h"
#include <macs/folder_set.h>
#include <macs/hooks.h>
#include <macs/label_set.h>

namespace {

using namespace hound::server::handlers::v2::from_favorite_user_counter;
using namespace hound::testing;
using namespace ::testing;

struct MailboxMock {
    MOCK_METHOD(macs::LabelSet, labels, (), (const));
    MOCK_METHOD(macs::FolderSet, folders, (), (const));
    MOCK_METHOD(size_t, countNewMessagesWithLids, (std::list<macs::Lid>, size_t), (const));
};

struct TestMailbox {
    MailboxMock& mailbox;

    macs::LabelSet labels() const {
        return mailbox.labels();
    }

    macs::FolderSet folders() const {
        return mailbox.folders();
    }

    size_t countNewMessagesWithLids(const macs::Lid& lid,
                                    size_t limit) const {
        return mailbox.countNewMessagesWithLids(std::list<macs::Lid>{lid}, limit);
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(uid);}
};

struct FromFavoriteUserCounterMethod : Test {
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    Method<MailboxGetter> method{MailboxGetter{getMailbox}};
};

TEST_F(FromFavoriteUserCounterMethod, should_return_invalid_argument_for_empty_uid) {
    const auto res = method(Request{"", 10, 10});
    EXPECT_EQ(res.error(), error_code{error::invalidArgument});
}

TEST_F(FromFavoriteUserCounterMethod, should_return_too_many_messages_for_big_mailbox) {
    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet({
        std::make_pair("1", macs::FolderFactory().fid("1").messages(2000).product()),
        std::make_pair("2", macs::FolderFactory().fid("2").messages(3001).product())
    })));

    const auto res = method(Request{"uid", 100, 5000});
    EXPECT_EQ(res.error(), error_code{error::tooManyMessages});
}

TEST_F(FromFavoriteUserCounterMethod, should_return_0_if_label_doesnt_exist) {
    macs::LabelSet labels;
    macs::Label label = macs::LabelFactory().lid("5");
    labels.insert(make_pair(label.lid(), label));

    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet({
        std::make_pair("1", macs::FolderFactory().fid("1").messages(2000).product()),
        std::make_pair("2", macs::FolderFactory().fid("2").messages(2000).product())
    })));
    EXPECT_CALL(mailbox, labels()).WillOnce(Return(labels));

    const auto res = method(Request{"uid", 100, 5000});
    EXPECT_EQ(res.value().newMessagesCount, 0U);
}

TEST_F(FromFavoriteUserCounterMethod, should_return_0_if_label_has_0_messages) {
    macs::LabelSet labels;
    macs::Label label = macs::LabelFactory().lid("5")
        .symbol(macs::Label::Symbol::from_favorite_user_label).messages(0);
    labels.insert(make_pair(label.lid(), label));

    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet({
        std::make_pair("1", macs::FolderFactory().fid("1").messages(2000).product()),
        std::make_pair("2", macs::FolderFactory().fid("2").messages(2000).product())
    })));
    EXPECT_CALL(mailbox, labels()).WillOnce(Return(labels));

    const auto res = method(Request{"uid", 100, 5000});
    EXPECT_EQ(res.value().newMessagesCount, 0U);
}

TEST_F(FromFavoriteUserCounterMethod, should_return_count_unread_messages) {
    macs::LabelSet labels;
    macs::Label label = macs::LabelFactory().lid("5")
        .symbol(macs::Label::Symbol::from_favorite_user_label).messages(200);
    labels.insert(make_pair(label.lid(), label));

    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet({
        std::make_pair("1", macs::FolderFactory().fid("1").messages(2000).product()),
        std::make_pair("2", macs::FolderFactory().fid("2").messages(2000).product())
    })));
    EXPECT_CALL(mailbox, labels()).WillOnce(Return(labels));
    EXPECT_CALL(mailbox, countNewMessagesWithLids(std::list<macs::Lid>{"5"}, 100)).WillOnce(Return(12));

    const auto res = method(Request{"uid", 100, 5000});
    EXPECT_EQ(res.value().newMessagesCount, 12U);
}

} // namespace
