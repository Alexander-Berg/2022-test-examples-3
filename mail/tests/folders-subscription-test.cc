#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>

namespace {
    using namespace macs;
    using namespace testing;

    struct FoldersSubscriptionTest: public FoldersRepositoryTest {
        FoldersSubscriptionTest(void)
            : sharedFoldersSuid("10000000")
            , yandex(folders.folder("1", "yandex", Folder::noParent))
            , sharedFolderNonSorted(folders.folder("2", "sharedFolderNonSorted", "1"))
            , sharedFolderSorted(folders.folder("3", "sharedFolderSorted", "1")
                    .position(100))
            , subscribedFolder(folders.folder("4", "subscribedFolder", "1"))
        {}

        std::string sharedFoldersSuid;

        macs::Folder yandex;
        macs::Folder sharedFolderNonSorted;
        macs::Folder sharedFolderSorted;
        macs::Folder subscribedFolder;

        FoldersMap getSharedFolders(const Folder &folder) {
            FoldersMap sharedFolders;
            sharedFolders.insert(make_pair(folder.fid(), folder));
            return sharedFolders;
        }
    };

    TEST_F(FoldersSubscriptionTest, subscribeToSharedFolders_whenSubscriptionAlreadyExists_returnsEmptyFoldersSet) {
        EXPECT_CALL(folders, syncSubscribeToSharedFolders(sharedFoldersSuid,_))
            .WillOnce(InvokeArgument<1>(macs::error_code(), FoldersMap()));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(Folder()));

        auto foldersSet = folders.subscribeToSharedFolders(sharedFoldersSuid);
        ASSERT_TRUE(foldersSet.empty());
    }

    TEST_F(FoldersSubscriptionTest, unsubscribeFromSharedFolders_whenSubscriptionNotExists_returnsEmptyFoldersSet) {
        EXPECT_CALL(folders, syncUnsubscribeFromSharedFolders(sharedFoldersSuid,_))
               .WillOnce(InvokeArgument<1>(macs::error_code(), FoldersMap()));

        auto foldersSet = folders.unsubscribeFromSharedFolders(sharedFoldersSuid);
        ASSERT_TRUE(foldersSet.empty());
    }

    TEST_F(FoldersSubscriptionTest,
            subscribeToSharedFolders_whenUserSharedFoldersManuallySorted_setsPositionForSubscribedFolder) {
        InSequence s;
        EXPECT_CALL(folders, syncSubscribeToSharedFolders(sharedFoldersSuid,_))
            .WillOnce(InvokeArgument<1>(macs::error_code(), getSharedFolders(subscribedFolder)));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(yandex,
                sharedFolderSorted, subscribedFolder));
        EXPECT_CALL(folders, syncSetPosition("4", 200, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));

        auto subscribedFolders = folders.subscribeToSharedFolders(sharedFoldersSuid);

        ASSERT_EQ( subscribedFolders["4"].position(), 200ul );
    }

    TEST_F(FoldersSubscriptionTest,
            subscribeToSharedFolders_whenUserSharedFoldersDefaultSorted_PositionForSubscribedFolderNotSet) {
        InSequence s;
        EXPECT_CALL(folders, syncSubscribeToSharedFolders(sharedFoldersSuid,_))
            .WillOnce(InvokeArgument<1>(macs::error_code(), getSharedFolders(subscribedFolder)));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(yandex,
                sharedFolderNonSorted, subscribedFolder));

        auto subscribedFolders = folders.subscribeToSharedFolders(sharedFoldersSuid);

        ASSERT_EQ( subscribedFolders["4"].position(), 0ul );
    }
}
