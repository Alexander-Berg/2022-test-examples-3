#include <utility>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/folders/method.h>
#include "../helpers.h"

namespace {

using namespace hound::server::handlers::v2::folders;
using namespace hound::testing;
using namespace ::testing;
using macs::FolderFactory;

struct MailboxMock {
    MOCK_METHOD(macs::FolderSet, getAllFolders, (), (const));
    MOCK_METHOD(macs::FolderSet, getAllFoldersWithHidden, (), (const));
};

struct TestMailbox {
    MailboxMock& mailbox;

    macs::FolderSet folders(bool withHidden) const {
        if (withHidden) {
            return mailbox.getAllFoldersWithHidden();
        } else {
            return mailbox.getAllFolders();
        }
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(std::move(uid)); }
};

struct FoldersMethod : Test {
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    Method<MailboxGetter> method{MailboxGetter{getMailbox}};
};

TEST_F(FoldersMethod, should_return_invalid_argument_for_empty_uid) {
    const auto res = method(Request{"", false});
    EXPECT_EQ(res.error(), error_code{error::invalidArgument});
}

TEST_F(FoldersMethod, should_return_folders_for_correct_uid) {
    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, getAllFolders()).WillOnce(Return(macs::FolderSet({
        std::make_pair("1", FolderFactory().fid("1").creationTime("1600000000")
                .type(macs::Folder::Type::user).product()),
        std::make_pair("3", FolderFactory().fid("3").creationTime("1500000000")
                .type(macs::Folder::Type::user).product())
    })));

    const auto res = method(Request{"uid", false});
    const std::vector<Folder> actual = res.value().folders;
    EXPECT_THAT(map(actual, fidExtractor), Contains("1"));
    EXPECT_THAT(map(actual, fidExtractor), Contains("3"));

}

} // namespace

