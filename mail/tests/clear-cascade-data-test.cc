#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/folder_factory.h>
#include <internal/folder/clear_cascade_data.h>

namespace {

using namespace testing;
using namespace macs::pg;
using macs::Folder;

struct ClearCascadeDataTest : public Test {
    macs::FoldersMap foldersData;
    macs::FolderSet folders;

    ClearCascadeDataTest() {
        fill();
        folders = macs::FolderSet(foldersData);
    }

    void addFolder(const std::string& id, const std::string& name,
            const std::string& parentId, const macs::Folder::Type& type) {
        macs::FolderFactory factory;
        Folder folder = factory.fid(id).name(name).parentId(parentId).type(type);
        foldersData.insert(make_pair(folder.fid(), folder));
    }

    void fill() {
        addFolder("1", "inbox", macs::Folder::noParent, Folder::Type::system);
        addFolder("2", "trash", macs::Folder::noParent, Folder::Type::system);
        addFolder("3", "spam", macs::Folder::noParent, Folder::Type::system);
        addFolder("4", "deletedFolder", "1", Folder::Type::user);
        addFolder("5", "child_system1", "4", Folder::Type::system);
        addFolder("6", "child", "4", Folder::Type::user);
        addFolder("7", "child_of_child", "6", Folder::Type::user);
        addFolder("8", "child_of_child_of_child", "7", Folder::Type::user);
        addFolder("9", "child_system2", "8", Folder::Type::system);
    }
};

TEST_F(ClearCascadeDataTest, foldersToMove_forCleanedFolderTree_returnsAllSystemFoldersFromIt) {
    ClearCascadeData clearData(folders, "4");

    const auto foldersToMove = clearData.foldersToMove();
    ASSERT_THAT(foldersToMove, ElementsAre(
            Property(&Folder::fid, AnyOf("5", "9")),
            Property(&Folder::fid, AnyOf("5", "9"))));
}

TEST_F(ClearCascadeDataTest, foldersToDelete_forCleanedFolderTree_returnsAllUserFoldersFromIt_InOrderFromChildToParent) {
    ClearCascadeData clearData(folders, "4");

    const auto foldersToDelete = clearData.foldersToDelete();
    ASSERT_THAT(foldersToDelete,
            ElementsAre(Property(&Folder::fid, "8"),
                        Property(&Folder::fid, "7"),
                        Property(&Folder::fid, "6"),
                        Property(&Folder::fid, "4")));
}

} // namespace

