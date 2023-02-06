#include <gtest/gtest.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct FoldersDeleteFolderTest: public FoldersRepositoryTest {
        FoldersDeleteFolderTest(void)
            : inbox(folders.folder("1", "inbox", Folder::noParent)),
              sysbox(folders.system(folders.folder("1", "inbox", Folder::noParent)))
        {}

        Folder inbox;
        Folder sysbox;
    };

    TEST_F(FoldersDeleteFolderTest, DeletesFolderDefault) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        EXPECT_CALL(folders, syncEraseFolder(inbox.fid(), A<macs::OnUpdate>()))
                .WillOnce(InvokeArgument<1>(macs::error_code(), macs::NULL_REVISION));

        folders.deleteFolder("1");
    }

    TEST_F(FoldersDeleteFolderTest, DeletesEmptyFolderNonforced) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        EXPECT_CALL(folders, syncEraseFolder(inbox.fid(), A<macs::OnUpdate>()))
                .WillOnce(InvokeArgument<1>(macs::error_code(), macs::NULL_REVISION));

        folders.deleteFolder("1", false);
    }

    TEST_F(FoldersDeleteFolderTest, NonEmptyFolderNonforcedThrowsException) {
        InSequence s;
        Folder inbox(folders.folder("1", "inbox", Folder::noParent, 10));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));

        ASSERT_THROW_SYS(folders.deleteFolder("1", false),
                         macs::error::folderIsNotEmpty,
                         "can not delete folder 1 with messages"
                         ": folder is not empty");
    }

    TEST_F(FoldersDeleteFolderTest, FolderWithChildrenNonforcedThrowsException) {
        InSequence s;
        Folder parent(folders.folder("1", "parent", Folder::noParent, 0));
        Folder child(folders.folder("2", "child", "1", 10));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(parent, child));

        ASSERT_THROW_SYS(folders.deleteFolder("1", false),
                         macs::error::folderIsNotEmpty,
                         "can not delete folder 1 with subfolders"
                         ": folder is not empty");
    }

    TEST_F(FoldersDeleteFolderTest, DeletesNonEmptyFolderForced) {
        InSequence s;
        Folder inbox(folders.folder("1", "inbox", Folder::noParent, 10));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        EXPECT_CALL(folders, syncClearFolderCascade(inbox.fid(), _, A<macs::OnUpdateMessages>()))
                .WillOnce(InvokeArgument<2>(macs::error_code(),
                        macs::OnUpdateMessages::second_argument_type{macs::Revision(), 10}));

        folders.deleteFolder("1", true);
    }

    TEST_F(FoldersDeleteFolderTest, DeletesFolderWithoutFid) {
        ASSERT_THROW_SYS(folders.deleteFolder(""), macs::error::invalidArgument,
                         "deleteFolder: empty fid: invalid argument");
    }

    TEST_F(FoldersDeleteFolderTest, DeletesSystemFolder) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sysbox));
        ASSERT_THROW_SYS(folders.deleteFolder("1"),
                         macs::error::cantModifyFolder,
                         "can't remove system folder 1: can not modify folder");
    }

}
