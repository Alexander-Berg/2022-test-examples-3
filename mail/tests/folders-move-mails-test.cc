#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace macs;
    using namespace testing;
    using namespace std;

    struct FoldersMoveMailsTest: public FoldersRepositoryTest {
    };

    TEST_F(FoldersMoveMailsTest, MovesBetweenFolders) {
        Folder from = folders.folder("1", "inbox", Folder::noParent);
        Folder to = folders.folder("2", "name", Folder::noParent);

        EXPECT_CALL(folders, syncMoveAll(matchFolder(from), matchFolder(to), _))
            .WillOnce(InvokeArgument<2>(macs::error_code(),
                    macs::OnUpdateMessages::second_argument_type{macs::Revision(), 0}));

        folders.moveAssociatedData(from, to);
    }
}
