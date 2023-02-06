#include <utility>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/folders_tree/method.h>
#include "../helpers.h"

namespace {

namespace folders = hound::server::handlers::v2::folders_tree;
using namespace hound::testing;
using namespace ::testing;
using macs::FolderFactory;

struct MailboxMock {
    MOCK_METHOD(macs::FolderSet, getAllFolders, (), (const));
};

struct TestMailbox {
    MailboxMock& mailbox;

    macs::FolderSet folders() const {
        return mailbox.getAllFolders();
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(std::move(uid)); }
};

struct FoldersTreeMethod : Test {
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    folders::Method<MailboxGetter> method{MailboxGetter{getMailbox}};
};

TEST_F(FoldersTreeMethod, should_return_invalid_argument_for_empty_uid) {
    const auto res = method(folders::Request{"", "", boost::none});
    EXPECT_EQ(res.error(), folders::error_code{folders::error::invalidArgument});
}

TEST_F(FoldersTreeMethod, should_return_invalid_argument_for_unknown_sorting_type) {
    const auto res = method(folders::Request{"uid", "unknown", boost::none});
    EXPECT_EQ(res.error(), folders::error_code{folders::error::invalidArgument});
}

TEST_F(FoldersTreeMethod, should_return_invalid_argument_for_empty_sorting_type) {
    const auto res = method(folders::Request{"uid", "", boost::none});
    EXPECT_EQ(res.error(), folders::error_code{folders::error::invalidArgument});
}

TEST_F(FoldersTreeMethod, should_return_invalid_argument_for_sorting_by_path_with_empty_locale) {
    const auto res = method(folders::Request{"uid", "locale", boost::none});
    EXPECT_EQ(res.error(), folders::error_code{folders::error::invalidArgument});
}

TEST_F(FoldersTreeMethod, should_return_invalid_argument_for_sorting_by_path_with_unknown_locale) {
    const auto res = method(folders::Request{"uid", "locale", std::string("whoami")});
    EXPECT_EQ(res.error(), folders::error_code{folders::error::invalidArgument});
}

TEST_F(FoldersTreeMethod, should_return_sorted_folders_by_date_for_date_sorting_type) {
    macs::FolderSet folders({
        std::make_pair("1", FolderFactory().fid("1").creationTime("1600000000")
                .type(macs::Folder::Type::user).product()),
        std::make_pair("3", FolderFactory().fid("3").creationTime("1500000000")
                .type(macs::Folder::Type::user).product())
    });
    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    // WillOnce resets value after call, but `method` returns reference to result of getAllFolders().
    EXPECT_CALL(mailbox, getAllFolders()).WillOnce(Return(folders));

    const auto res = method(folders::Request{"uid", "date", boost::none});
    const std::vector<folders::Folder> actual = res.value().folders_tree;
    EXPECT_THAT(map(actual, idExtractor), ElementsAre("3", "1"));
}

TEST_F(FoldersTreeMethod, should_return_sorted_folders_by_locale_for_locale_sorting_type) {
    macs::FolderSet folders({
        std::make_pair("1", FolderFactory().fid("1").name("чёлка")
                .type(macs::Folder::Type::user).product()),
        std::make_pair("3", FolderFactory().fid("3").name("ёлка")
                .type(macs::Folder::Type::user).product())
    });
    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    // WillOnce resets value after call, but `method` returns reference to result of getAllFolders().
    EXPECT_CALL(mailbox, getAllFolders()).WillOnce(Return(folders));

    auto res = method(folders::Request{"uid", "lang", std::string("ru")});
    const std::vector<folders::Folder> actual = res.value().folders_tree;
    EXPECT_THAT(map(actual, idExtractor), ElementsAre("3", "1"));
}

} // namespace
