#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>

namespace {
    using namespace ::std;
    using namespace ::macs;
    using namespace ::testing;

    struct FoldersUpdatePositionTest : public FoldersRepositoryTest {

    };

    TEST_F(FoldersUpdatePositionTest, UpdatePositionName) {
        Folder folder = folders.folder("1", "inbox", "2");
        ASSERT_EQ(Revision(0), folder.revision());

        InSequence s;
        EXPECT_CALL(folders, syncSetPosition(folder.fid(), folder.revision().value(), _))
                .WillOnce(InvokeArgument<2>(macs::error_code(), 1));

        folder = folders.updatePosition(folder);
        ASSERT_EQ(Revision(1), folder.revision());
    }
}
