#include <gtest/gtest.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::std;

    struct FoldersSaveFolderTest: public FoldersRepositoryTest {
        FoldersSaveFolderTest(void)
            : sent(folders.folder("1", "sent", macs::Folder::noParent).symbol(macs::Folder::Symbol::sent))
            , folder(folders.folder("2", "name", "1"))
            , folder2(folders.folder("4", "name2", "1"))
            , spam(folders.folder("3", "spam", macs::Folder::noParent).symbol(macs::Folder::Symbol::spam))
            , restored(folders.folder("5", "Restored", macs::Folder::noParent))
        {}

        macs::Folder sent;
        macs::Folder folder;
        macs::Folder folder2;
        macs::Folder spam;
        macs::Folder restored;
    };

    TEST_F(FoldersSaveFolderTest, CreatesFolder) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));
        EXPECT_CALL(folders, syncCreateFolder("name", "1", macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), folder));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent,
                                                                   folder));

        macs::Folder folder = folders.createFolder(string("name"), string("1"));

        ASSERT_EQ("name", folder.name());
        ASSERT_EQ("1", folder.parentId());
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithNoName) {
        ASSERT_THROW_SYS(folders.createFolder(string(""), macs::Folder::noParent),
                         macs::error::invalidArgument,
                         "Folder name is empty: invalid argument");
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithExistingName) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));

        ASSERT_THROW_SYS(folders.createFolder(string("sent"), macs::Folder::noParent),
                         macs::error::folderAlreadyExists,
                         "can't create folder "
                         "with name \"sent\" parent 0: folder already exists");
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithNonExistingParent) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));

        ASSERT_THROW_SYS(folders.createFolder(string("sent"), string("2")),
                         macs::error::noSuchFolder,
                         "can't create folder with parent 2"
                         ": no such folder");
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithLongName) {
        string longName(macs::Folder::maxFolderNameLength() + 1, 'x');
        ASSERT_THROW_SYS(folders.createFolder(longName),
                         macs::error::invalidArgument,
                         "Folder name is too large: invalid argument");
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithInvalidName) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));
        EXPECT_CALL(folders, syncCreateFolder("пап?ка", macs::Folder::noParent, macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(),
                                        folders.folder("2", "пап?ка", macs::Folder::noParent)));
        EXPECT_CALL(folders, syncGetFolders(_))
            .WillOnce(GiveFolders(sent, folders.folder("2", "пап?ка", macs::Folder::noParent)));

        const string invalidName = "пап\x85ка";
        folders.createFolder(invalidName, macs::Folder::noParent);
    }

    TEST_F(FoldersSaveFolderTest, CreateFolderWithSpacesInName) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));
        EXPECT_CALL(folders, syncCreateFolder("name", "1", macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), folder));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent,
                                                                   folder));

        folders.createFolder(string("   name    "), string("1"));
    }

    TEST_F(FoldersSaveFolderTest, createFolder_childlessParent_throwInvalidArgument) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(spam));
        ASSERT_THROW_SYS(folders.createFolder(string("name"), string("3")),
                         macs::error::folderCantBeParent,
                         "can't create folder with parent 3"
                         ": folder can not be parent");
    }

    TEST_F(FoldersSaveFolderTest, createFolder_inFoldersWithNoPosition_doesNotSetPosition) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent, folder));
        EXPECT_CALL(folders, syncCreateFolder("name2", "1", macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), folder2));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent, folder, folder2));

        macs::Folder createdFolder = folders.createFolder(string("name2"), string("1"));
        ASSERT_EQ(0ul, createdFolder.position());
    }

    TEST_F(FoldersSaveFolderTest, createFolder_inFoldersWithPositions_setPositionAfterLast) {
        macs::FolderFactory factory(folder);
        factory.position(100);
        folder = factory.product();

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent, folder));
        EXPECT_CALL(folders, syncCreateFolder("name2", "1", macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), folder2));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent, folder, folder2));
        EXPECT_CALL(folders, syncSetPosition("4", 200, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));

        macs::Folder createdFolder = folders.createFolder(string("name2"), string("1"));
        ASSERT_EQ(200ul, createdFolder.position());
    }

    TEST_F(FoldersSaveFolderTest, createFolderByPath_shouldReturnLast) {
        using PathValue = std::vector<macs::Folder::Name>;

        const auto a = macs::FolderFactory().name("a").product();
        const auto b = macs::FolderFactory().name("b").parentId(a.fid()).product();

        const InSequence s;

        EXPECT_CALL(folders, syncCreateFolderByPath(macs::Folder::Path(PathValue{"a", "b"}), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), b));

        const auto createdFolder = folders.createFolderByPath(macs::Folder::Path(PathValue{"a", "b"}));

        EXPECT_EQ(createdFolder.name(), "b");
    }

    TEST_F(FoldersSaveFolderTest, createFolderByPath_withEmptyPath_shouldThrowException) {
        const auto a = macs::FolderFactory().name("a").product();

        const InSequence s;

        ASSERT_THROW_SYS(folders.createFolderByPath(macs::Folder::Path()),
                         macs::error::invalidArgument,
                         "createFolderByPathInternal() path is empty: invalid argument");
    }

    TEST_F(FoldersSaveFolderTest, getOrCreateNotExistingFolder) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));
        EXPECT_CALL(folders, syncGetOrCreateFolder("name", "1", macs::Folder::Symbol::none, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), folder));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent, folder));

        macs::Folder folder = folders.getOrCreateFolder(string("name"), string("1"));

        ASSERT_EQ("name", folder.name());
        ASSERT_EQ("1", folder.parentId());
    }

    TEST_F(FoldersSaveFolderTest, getOrCreateExistingFolder) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));
        EXPECT_CALL(folders, syncGetOrCreateFolder("sent", macs::Folder::noParent, macs::Folder::Symbol::sent, _))
            .WillOnce(InvokeArgument<3>(macs::error_code(), sent));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));

        macs::Folder folder = folders.getOrCreateFolder(string("sent"), macs::Folder::noParent,
            macs::Folder::Symbol::sent);

        ASSERT_EQ("sent", folder.name());
        ASSERT_EQ(macs::Folder::noParent, folder.parentId());
    }

    TEST_F(FoldersSaveFolderTest, getOrCreateFolderBySymbolWithRandomizedName_shouldReturnsExistingFolderBySymbol) {
        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(sent));

        macs::Folder folder = macs::getOrCreateFolderBySymbolWithRandomizedName(folders, "sent", macs::Folder::noParent,
                macs::Folder::Symbol::sent, false);

        ASSERT_EQ("sent", folder.name());
        ASSERT_EQ(macs::Folder::noParent, folder.parentId());
    }

    TEST_F(FoldersSaveFolderTest, getOrCreateFolderBySymbolWithRandomizedName_shouldCreteNewFolderWhenNameIsBusy) {
        const macs::Folder folderWithNewName = macs::FolderFactory()
                .name("Restored_100500")
                .parentId(macs::Folder::noParent)
                .symbol(macs::Folder::Symbol::restored)
                .fid("100500")
                .product();

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(restored));
        EXPECT_CALL(folders, syncCreateFolder(_, macs::Folder::noParent, macs::Folder::Symbol::restored, _))
                .WillOnce(InvokeArgument<3>(macs::error_code(), folderWithNewName));
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folderWithNewName));

        macs::Folder folder = macs::getOrCreateFolderBySymbolWithRandomizedName(folders, "Restored", macs::Folder::noParent,
                macs::Folder::Symbol::restored, false);

        ASSERT_EQ(folderWithNewName.name(), folder.name());
        ASSERT_EQ(macs::Folder::noParent, folder.parentId());
        ASSERT_EQ(folderWithNewName.symbolicName(), folder.symbolicName());
    }
}
