#include <gtest/gtest.h>

#include <mail/notsolitesrv/src/meta_save_op/types/request.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;
using namespace std::literals;

namespace NNotSoLiteSrv::NMetaSaveOp {

bool operator==(const TFolderPath& lhs, const TFolderPath& rhs) {
    return std::tie(lhs.path, lhs.delim) == std::tie(rhs.path, rhs.delim);
}

bool operator==(const TFolder& lhs, const TFolder& rhs) {
    return std::tie(lhs.fid, lhs.path) == std::tie(rhs.fid, rhs.path);
}

std::ostream& operator<<(std::ostream& os, const TFolderPath& v) {
    os << v.path << " (" << v.delim<< ")";
    return os;
}

std::ostream& operator<<(std::ostream& os, const TFolder& v) {
    os << v.fid << ", " << v.path;
    return os;
}

std::ostream& operator<<(std::ostream& os, ENoSuchFolderAction v) {
    os << std::to_string(v);
    return os;
}

}

struct TMetaSaveOpRequestFolderTest: public Test {
    using TExpected = std::pair<TFolder, ENoSuchFolderAction>;

    TXYandexHint Hint;
    std::string DEFAULT_DELIM{TFolderPath::DEFAULT_DELIM};
};

#define CHECK(isSpam, isMailish, expected) \
    EXPECT_EQ(CreateFolder(Hint, isSpam, isMailish), expected)

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromEmpty) {
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Inbox", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"\\Spam", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromSaveToSent) {
    Hint.save_to_sent = true;
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Sent", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"\\Sent", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
    Hint.save_to_sent = false;
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Sent", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"\\Sent", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromFid) {
    Hint.fid = "17";
    CHECK(false, false, TExpected(TFolder{"17"s, boost::none}, ENoSuchFolderAction::FallbackToInbox));
    CHECK(true, false, TExpected(TFolder{"17"s, boost::none}, ENoSuchFolderAction::FallbackToInbox));
    CHECK(false, true, TExpected(TFolder{"17"s, boost::none}, ENoSuchFolderAction::Fail));
    Hint.imap = true;
    CHECK(false, false, TExpected(TFolder{"17"s, boost::none}, ENoSuchFolderAction::Fail));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromIncorrectFid) {
    Hint.fid = "17 and more";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Inbox", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromPath) {
    Hint.folder_path = "\\Inbox";
    Hint.folder_spam_path = "\\Spam";

    // default delim
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Inbox", DEFAULT_DELIM}}, ENoSuchFolderAction::Create));
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"\\Spam", DEFAULT_DELIM}}, ENoSuchFolderAction::Create));

    Hint.folder_path_delim = "#";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Inbox", "#"}}, ENoSuchFolderAction::Create));
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"\\Spam", "#"}}, ENoSuchFolderAction::Create));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderFromFolder) {
    Hint.folder = "some|folder|path";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"some|folder|path", "|"}}, ENoSuchFolderAction::FallbackToInbox));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderForSpamFromFolderIfNoFolderSpamPathPresent) {
    Hint.folder = "some|folder|path";
    Hint.folder_path = "some/path/in/folder_path";
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"some|folder|path", "|"}}, ENoSuchFolderAction::FallbackToInbox));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderIgnoreDelimForFolderHint) {
    Hint.folder = "some|folder|path";
    Hint.folder_path_delim = "#";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"some|folder|path", "|"}}, ENoSuchFolderAction::FallbackToInbox));
}

TEST_F(TMetaSaveOpRequestFolderTest, CreateFolderPriority) {
    // hint params priority: save_to_inbox > fid > folder_path/folder_spam_path > folder > default (\Inbox)

    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Inbox", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));

    Hint.folder = "obsoleted|folder|param";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"obsoleted|folder|param", "|"}}, ENoSuchFolderAction::FallbackToInbox));

    Hint.folder_path = "path/to/folder";
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"path/to/folder", DEFAULT_DELIM}}, ENoSuchFolderAction::Create));

    // spam message and no folder_spam_path and folder is present -> put to folder
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"obsoleted|folder|param", "|"}}, ENoSuchFolderAction::FallbackToInbox));

    Hint.folder_spam_path = "path/to/spam/folder";
    CHECK(true, false, TExpected(TFolder{boost::none, TFolderPath{"path/to/spam/folder", DEFAULT_DELIM}}, ENoSuchFolderAction::Create));

    Hint.fid = "17";
    CHECK(false, false, TExpected(TFolder{"17"s, boost::none}, ENoSuchFolderAction::FallbackToInbox));

    Hint.save_to_sent = false;
    CHECK(false, false, TExpected(TFolder{boost::none, TFolderPath{"\\Sent", DEFAULT_DELIM}}, ENoSuchFolderAction::Fail));
}
