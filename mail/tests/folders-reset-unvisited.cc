#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>

namespace {
    using namespace macs;
    using namespace testing;

    struct FoldersResetUnvisitedTest: public FoldersRepositoryTest {
    };

    TEST_F(FoldersResetUnvisitedTest, ResetsFolder) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent).unvisited(true);

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));
        EXPECT_CALL(folders, syncResetUnvisited(folder.fid(), _))
        .WillOnce(InvokeArgument<1>(macs::error_code(), macs::NULL_REVISION));

        folders.resetUnvisited(folder.fid());
    }

    TEST_F(FoldersResetUnvisitedTest, ResetsFolderWithNoUnvisited) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        folders.resetUnvisited(folder.fid());
    }

    TEST_F(FoldersResetUnvisitedTest, ResetsNonexistingFolder) {
        std::vector<macs::Folder> empty;
        EXPECT_CALL(folders, syncGetFolders(_)).Times(AnyNumber())
            .WillRepeatedly(GiveFolders(empty));

        folders.resetUnvisited("");
    }
}
