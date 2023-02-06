#include <gtest/gtest.h>
#include <macs/folder.h>

namespace {

using namespace testing;

typedef Test FoldersShortNameTest;

TEST_F(FoldersShortNameTest, getFolderDisplayName_severalParents_returnLastName) {
    const std::string path = "top-folder1|top-folder2|child-folder";
    ASSERT_EQ("child-folder", macs::getFolderDisplayName(path));
}

TEST_F(FoldersShortNameTest, getFolderDisplayName_noParents_returnSelf) {
    ASSERT_EQ("folder", macs::getFolderDisplayName("folder"));
}

TEST_F(FoldersShortNameTest, getFolderDisplayName_emptyString_returnEmptyString) {
    ASSERT_EQ("", macs::getFolderDisplayName(""));
}

TEST_F(FoldersShortNameTest, getFolderDisplayName_should_unescape_escaped_folder_name) {
    ASSERT_EQ("foo|bar", macs::getFolderDisplayName("foo||bar"));
}

}
