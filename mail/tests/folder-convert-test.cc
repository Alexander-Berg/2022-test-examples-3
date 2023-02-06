#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/query/ids.h>
#include <algorithm>
#include <map>
#include <iterator>
#include <yamail/data/serialization/json_writer.h>
#include <pgg/cast.h>
#include <internal/folder/factory.h>
#include "base-convert-test.h"

namespace {

using namespace testing;

class ConvertFolderTest : public tests::BaseConvertTest<macs::pg::reflection::Folder> {
protected:
    static void fill(Reflection& data) {
        data.fid = 42;
        data.parent_fid = boost::optional<int32_t>();
        data.folder_path = {"NaMe"};
        data.type = "user";
        data.created = 1419580017;
        data.message_count = 100500;
        data.message_seen = 100000;
        data.message_recent = 7;
        data.unvisited = true;
        data.message_size = 4294967296;
        data.position = 0;

        data.revision = 10;
        data.next_imap_id = 846;
        data.uidvalidity = 1395247811;
        data.first_unseen = 13;
    }

    ConvertFolderTest() {
        modifyData(fill);
    }

    macs::Folder convert() const {
        return macs::pg::FolderFactory().fromReflection(data()).product();
    }
};

TEST_F(ConvertFolderTest, FolderConverter) {
    macs::Folder f = convert();
    EXPECT_EQ(f.fid(), "42");
    EXPECT_EQ(f.parentId(), macs::Folder::noParent);
    EXPECT_EQ(f.name(), "NaMe");
    EXPECT_EQ(f.creationTime(), "1419580017");
    EXPECT_EQ(f.revision(), macs::Revision(10));
    EXPECT_EQ(f.messagesCount(), 100500ul);
    EXPECT_EQ(f.newMessagesCount(), 500ul);
    EXPECT_EQ(f.recentMessagesCount(), 7ul);
    EXPECT_EQ(f.unvisited(), true);
    EXPECT_EQ(f.position(), 0ul);

    EXPECT_FALSE(f.isSystem());
}


TEST_F(ConvertFolderTest, rowWithInboxType_set_inbox_type) {
    modifyData([] (Reflection& data) {
        data.type = "inbox";
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.symbolicName().code(), macs::Folder::Symbol::inbox.code());
}

TEST_F(ConvertFolderTest, rowWithTrashType_set_trash_type) {
    modifyData([] (Reflection& data) {
        data.type = "trash";
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.symbolicName().code(), macs::Folder::Symbol::trash.code());
}

TEST_F(ConvertFolderTest, rowWithZombieType_set_zombie_type) {
    modifyData([] (Reflection& data) {
        data.type = "zombie";
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.symbolicName().code(), macs::Folder::Symbol::zombie_folder.code());
}

TEST_F(ConvertFolderTest, rowWithUnkwnonType_set_unknown_type) {
    modifyData([] (Reflection& data) {
        data.type = "OMG";
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.symbolicName().code(), macs::Folder::Symbol::none.code());
}

TEST_F(ConvertFolderTest, rowWithHierarchicalPath_set_last_name) {
    modifyData([] (Reflection& data) {
        data.folder_path = {"foo", "bar", "baz"};
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.name(), "baz");
}

TEST_F(ConvertFolderTest, nameWithHierarchicalPath_set_last_name) {
    modifyData([] (Reflection& data) {
        data.folder_path = {"foo|bar|baz"};
    });
    macs::Folder f = convert();
    EXPECT_EQ(f.name(), "baz");
}

TEST_F(ConvertFolderTest, macsParentToQuery_macsZero_queryNull) {
    auto zero = boost::optional<std::string>("0");
    EXPECT_EQ(macs::pg::query::ParentFolderId(zero).value.get_ptr() == nullptr, true);
}

} // namespace

