#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct FoldersMoveFolderTest: public FoldersRepositoryTest {
    };

    //------------------------------ moveFolder --------------------------------

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToAnotherParent_callSyncModifyFolder) {
        Folder oldParent = folders.factory().fid("1").name("old-parent").symbol(Folder::Symbol::none);
        Folder newParent = folders.factory().fid("2").name("new-parent").symbol(Folder::Symbol::none);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        Folder modified = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("2");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));
        folders.moveFolder("3", "2");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToInbox_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("drafts").symbol(Folder::Symbol::drafts);
        Folder newParent = folders.factory().fid("2").name("inbox").symbol(Folder::Symbol::inbox);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        Folder modified = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("2");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
                .WillOnce(InvokeArgument<1>(macs::error_code(), modified));
        folders.moveFolder("3", "2");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToDrafts_callSyncModifyFolder) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("drafts").symbol(Folder::Symbol::drafts);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        Folder modified = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("2");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));
        folders.moveFolder("3", "2");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToSent_callSyncGetFolders) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("sent").symbol(Folder::Symbol::sent);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        Folder modified = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("2");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));
        folders.moveFolder("3", "2");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToTopLevel_callSyncModifyFolderWithChangedName) {
        Folder oldParent = folders.factory().fid("1").name("old-parent").symbol(Folder::Symbol::none);
        Folder newParent = folders.factory().fid("0").name("inbox").symbol(Folder::Symbol::inbox);
        Folder child = folders.factory().fid("2").name("child").symbol(Folder::Symbol::none).parentId("1");
        Folder modified = folders.factory().fid("2").name("child").symbol(Folder::Symbol::none).parentId("0");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));
        folders.moveFolder("2", "0");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveNonExistingFolder_throwNoSuchLabel) {
        Folder oldParent = folders.factory().fid("1").name("old-parent").symbol(Folder::Symbol::none);
        Folder newParent = folders.factory().fid("2").name("new-parent").symbol(Folder::Symbol::none);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("4", "2"), macs::error::noSuchFolder,
                "access to nonexistent folder '4': no such folder");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToNonExistingParent_throwNoSuchLabel) {
        Folder oldParent = folders.factory().fid("1").name("old-parent").symbol(Folder::Symbol::none);
        Folder newParent = folders.factory().fid("2").name("new-parent").symbol(Folder::Symbol::none);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "4"), macs::error::noSuchFolder,
                "access to nonexistent folder '4': no such folder");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveInsideItself_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("old-parent").symbol(Folder::Symbol::none);
        Folder newParent = folders.factory().fid("2").name("new-parent").symbol(Folder::Symbol::none);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "3"), macs::error::folderCantBeParent,
                "can't move folder 3 "
                "to itself: folder can not be parent");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToOutbox_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("outbox").symbol(Folder::Symbol::outbox);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "2"), macs::error::folderCantBeParent,
                "can't move folder 3 to 2"
                ": folder can not be parent");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToSpam_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("spam").symbol(Folder::Symbol::spam);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "2"), macs::error::folderCantBeParent,
                "can't move folder 3 to 2"
                ": folder can not be parent");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToArchive_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("archive").symbol(Folder::Symbol::archive);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "2"), macs::error::folderCantBeParent,
                        "can't move folder 3 to 2"
                        ": folder can not be parent");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToTemplates_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("templates").symbol(Folder::Symbol::template_);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "2"), macs::error::folderCantBeParent,
                        "can't move folder 3 to 2"
                        ": folder can not be parent");
    }

    TEST_F(FoldersMoveFolderTest, moveFolder_moveToDiscount_throwInvalidArgument) {
        Folder oldParent = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        Folder newParent = folders.factory().fid("2").name("discounts").symbol(Folder::Symbol::discount);
        Folder child = folders.factory().fid("3").name("child").symbol(Folder::Symbol::none).parentId("1");
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(oldParent, newParent, child));
        ASSERT_THROW_SYS(folders.moveFolder("3", "2"), macs::error::folderCantBeParent,
                        "can't move folder 3 to 2"
                        ": folder can not be parent");
    }
}