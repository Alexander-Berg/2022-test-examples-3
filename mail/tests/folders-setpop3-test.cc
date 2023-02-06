
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct FoldersSetPop3Test: public FoldersRepositoryTest {
    };

    TEST_F(FoldersSetPop3Test, setPop3_callWithRealFolders_callSyncSetPop3) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folders.folder("1", "a", Folder::noParent), 
				                                     folders.folder("2", "b", Folder::noParent)));
        EXPECT_CALL(folders, syncSetPop3(std::vector<std::string>{"1", "2"}, _, _))
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::NULL_REVISION));
        folders.setPop3({"1", "2"});
    }

    TEST_F(FoldersSetPop3Test, setPop3_callWithUnexistentFolders_throws) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folders.folder("1", "a", Folder::noParent),
				                                     folders.folder("2", "b", Folder::noParent)));
        ASSERT_THROW(folders.setPop3({"1", "3"}), std::exception);
    }

    TEST_F(FoldersSetPop3Test, setPop3_callNoFolders_callSyncSetPop3) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folders.folder("1", "a", Folder::noParent),
				                                     folders.folder("2", "b", Folder::noParent)));
        EXPECT_CALL(folders, syncSetPop3(std::vector<std::string>{}, _, _))
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::NULL_REVISION));
        folders.setPop3({});
    }

}
