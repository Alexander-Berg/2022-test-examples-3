#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/folder/folders_converter.h>

namespace {

using namespace testing;

class FoldersConverterTest : public testing::Test {
protected:
    using Type = macs::Folder::Type;
    macs::Folder getFolder(const std::string& fid, const std::string& parentFid,
            const std::string& name, Type type = Type::user) const {
        return macs::FolderFactory().fid(fid).parentId(parentFid).name(name).type(type).product();
    }
};

TEST_F(FoldersConverterTest, normalize_forFolderTreeWithCycles_removesIt) {
    std::vector<macs::Folder> fs = {
        getFolder("111", "113", "cicle1"),
        getFolder("112", "111", "cicle2"),
        getFolder("113", "112", "cicle3"),
        getFolder("114", "113", "cicle4")
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 0ul);
}

TEST_F(FoldersConverterTest, normalize_forFolderTreeWithUnknownParent_removesIt) {
    std::vector<macs::Folder> fs = {
        getFolder("112", "111", "name1"),
        getFolder("113", "112", "name2"),
        getFolder("213", "211", "name1"),
        getFolder("212", "213", "name2")
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 0ul);
}

TEST_F(FoldersConverterTest, normalize_forRootFolder_saveFolderName) {
    std::vector<macs::Folder> fs = { getFolder("111", macs::Folder::noParent, "name") };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 1ul);
    EXPECT_EQ(folders.at("111").name(), "name");
}

TEST_F(FoldersConverterTest, normalize_forFoldersWithParent_setsHierarchicalName) {
    std::vector<macs::Folder> fs = {
        getFolder("111", macs::Folder::noParent, "name1"),
        getFolder("112", "111", "name2"),
        getFolder("113", "112", "name3")
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 3ul);
    EXPECT_EQ(folders.getPath(folders.at("111")).toString(), "name1");
    EXPECT_EQ(folders.getPath(folders.at("112")).toString(), "name1|name2");
    EXPECT_EQ(folders.getPath(folders.at("113")).toString(), "name1|name2|name3");
}

TEST_F(FoldersConverterTest, normalize_forFoldersInReverseOrder_updatesThem) {
    std::vector<macs::Folder> fs = {
        getFolder("113", "112", "name3"),
        getFolder("112", "111", "name2"),
        getFolder("111", macs::Folder::noParent, "name1")
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 3ul);
    EXPECT_EQ(folders.getPath(folders.at("111")).toString(), "name1");
    EXPECT_EQ(folders.getPath(folders.at("112")).toString(), "name1|name2");
    EXPECT_EQ(folders.getPath(folders.at("113")).toString(), "name1|name2|name3");
}

TEST_F(FoldersConverterTest, normalize_forSystemFolders_updatesThem) {
    std::vector<macs::Folder> fs = {
        getFolder("112", "111", "arch2", Type::system),
        getFolder("111", macs::Folder::noParent, "arch1", Type::system)
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 2ul);
    EXPECT_EQ(folders.getPath(folders.at("111")).toString(), "arch1");
    EXPECT_EQ(folders.getPath(folders.at("112")).toString(), "arch1|arch2");
}

TEST_F(FoldersConverterTest, normalize_forValidAndNonvalidFolders_removesNonvalidAndSetsNamesForValid) {
    std::vector<macs::Folder> fs = {
        getFolder("111", "113", "cicle1"),
        getFolder("112", "111", "cicle2"),
        getFolder("113", "112", "cicle3"),
        getFolder("114", "113", "cicle4"),
        getFolder("212", "211", "unknown_parent1"),
        getFolder("213", "212", "unknown_parent2"),
        getFolder("311", macs::Folder::noParent, "name1"),
        getFolder("312", "311", "name2"),
        getFolder("313", "312", "name3"),
        getFolder("411", macs::Folder::noParent, "name"),
    };
    const auto folders = macs::pg::normalize(fs, nullptr, "");

    EXPECT_EQ(folders.size(), 4ul);
    EXPECT_EQ(folders.getPath(folders.at("311")).toString(), "name1");
    EXPECT_EQ(folders.getPath(folders.at("312")).toString(), "name1|name2");
    EXPECT_EQ(folders.getPath(folders.at("313")).toString(), "name1|name2|name3");
    EXPECT_EQ(folders.getPath(folders.at("411")).toString(), "name");
}

} // namespace
