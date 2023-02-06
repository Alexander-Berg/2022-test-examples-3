#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/data/folder_position.h>
#include <macs/folder_factory.h>

namespace {
using namespace ::testing;
using namespace ::macs;
using namespace ::std;

struct FolderPositionTest : public Test {
    FoldersMap folders;

    void setPosition(Folder& folder, int position) {
        FolderFactory factory(std::move(folder));
        factory.position(static_cast<std::size_t>(position));
        folder = factory.product();
    }

    void addFolder(const std::string& id, const std::string& name,
            const std::string& parent, int position) {
        FolderFactory factory;
        Folder folder = factory.fid(id).name(name).parentId(parent);
        setPosition(folder, position);
        folders.insert(make_pair(folder.fid(), folder));
    }
};

TEST_F(FolderPositionTest, getFolderPosition_forFoldersWithNoPosition_returnsNoPosition) {
    addFolder("1", "name1", Folder::noParent, 0);
    addFolder("2", "name2", Folder::noParent, 0);
    addFolder("3", "name3", Folder::noParent, 0);
    ASSERT_EQ(0ul, GetFolderPosition(FolderSet(folders))(Folder::noParent));
}

TEST_F(FolderPositionTest, getFolderPosition_forFoldersWithPositions_returnsNextPosition) {
    addFolder("1", "name1", Folder::noParent, 0);
    addFolder("2", "name2", Folder::noParent, 100);
    addFolder("3", "name3", Folder::noParent, 200);
    ASSERT_EQ(300ul, GetFolderPosition(FolderSet(folders))(Folder::noParent));
}

}
