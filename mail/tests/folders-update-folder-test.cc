#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct FoldersUpdateFolderTest: public FoldersRepositoryTest {
        FoldersUpdateFolderTest(void)
            : inbox(folders.folder("1", "inbox", Folder::noParent))
        {}

        Folder inbox;
    };

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderName) {
        FolderFactory factory(inbox);
        factory.name("name");
        Folder folder = factory.product();
        Folder modified = folders.factory().fid("1").name("name").symbol(Folder::Symbol::none).parentId(Folder::noParent);

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));

        folders.updateFolder(folder);
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderDirectly) {
        Folder parent = folders.folder("2", "name", Folder::noParent);
        FolderFactory factory(inbox);
        factory.name("name");
        factory.parentId("2");
        Folder folder = factory.product();
        Folder modified = folders.factory().fid("1").name("name").symbol(Folder::Symbol::none).parentId("2");

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, parent));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _)).WillOnce(InvokeArgument<1>(macs::error_code(), modified));

        folders.updateFolder(folder);
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesSystemFolderName) {
        Folder system = folders.system(folders.folder("1", "inbox", Folder::noParent));
        FolderFactory factory(inbox);
        factory.name("name");
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(system));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::cantModifyFolder,
                         "can't rename system folder 1: can not modify folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderNameToEmpty) {
        FolderFactory factory(inbox);
        factory.name("");
        Folder folder = factory.product();
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        ASSERT_THROW_SYS(folders.updateFolder(folder), macs::error::invalidArgument,
                         "can't rename folder 1 with empty name: invalid argument");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderNameToExistingName) {
        Folder exists = folders.folder("2", "name", Folder::noParent);
        FolderFactory factory(inbox);
        factory.name("name");
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox,
                                                                   exists));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderAlreadyExists,
                         "can't rename folder 1 with name \"name\": folder already exists");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentId) {
        Folder parent = folders.folder("2", "name", Folder::noParent);
        FolderFactory factory(inbox);
        factory.parentId("2");
        Folder folder = factory.product();
        Folder modified = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::none).parentId("2");

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox,
                                                                   parent));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));

        folders.updateFolder(folder);
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdToNoParent) {
        Folder source = folders.folder("1", "inbox", "2");
        Folder parent = folders.folder("2", "parent", Folder::noParent);
        FolderFactory factory(source);
        factory.parentId(Folder::noParent);
        Folder folder = factory.product();
        Folder modified = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::none).parentId(Folder::noParent);

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders({source, parent}));
        EXPECT_CALL(folders, syncModifyFolder(matchFolder(modified), _))
            .WillOnce(InvokeArgument<1>(macs::error_code(), modified));

        folders.updateFolder(folder);
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesSystemFolderParentId) {
        Folder system = folders.system(folders.folder("1", "inbox", Folder::noParent));
        Folder parent = folders.system(folders.folder("2", "name", Folder::noParent));
        FolderFactory factory(system);
        factory.parentId("2");
        Folder folder = factory.product();

        InSequence s;
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(system,
                                                                   parent));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::cantModifyFolder,
                         "can't move system folder 1: can not modify folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdToItself) {
        FolderFactory factory(inbox);
        factory.parentId(inbox.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 "
                         "to itself: folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdToNonExistingFolder) {
        FolderFactory factory(inbox);
        factory.parentId("2");
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdToSpam) {
        Folder spam = folders.folder("2", "spam", Folder::noParent)
            .symbol(Folder::Symbol::spam);
        FolderFactory factory(inbox);
        factory.parentId(spam.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, spam));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdToSent) {
        Folder outbox = folders.folder("2", "outbox", Folder::noParent)
            .symbol(Folder::Symbol::outbox);
        FolderFactory factory(inbox);
        factory.parentId(outbox.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox,
                                                                   outbox));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, updateFolder_setParentIdToArchive_throwInvalidArgument) {
        Folder archive = folders.folder("2", "archive", Folder::noParent).symbol(Folder::Symbol::archive);
        FolderFactory factory(inbox);
        factory.parentId(archive.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, archive));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, updateFolder_setParentIdToTemplates_throwInvalidArgument) {
        Folder template_ = folders.folder("2", "template", Folder::noParent).symbol(Folder::Symbol::template_);
        FolderFactory factory(inbox);
        factory.parentId(template_.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, template_));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, updateFolder_setParentIdToDiscounts_throwInvalidArgument) {
        Folder discounts = folders.folder("2", "discounts", Folder::noParent).symbol(Folder::Symbol::discount);
        FolderFactory factory(inbox);
        factory.parentId(discounts.fid());
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, discounts));

        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderCantBeParent,
                         "can't move folder 1 to 2"
                         ": folder can not be parent");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderParentIdAndMakeExistingFolder) {
        Folder exist = folders.folder("2", "inbox", Folder::noParent);
        Folder source = folders.folder("1", "inbox", "2");
        FolderFactory factory(source);
        factory.parentId(Folder::noParent);
        Folder folder = factory.product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(exist,
                                                                   source));
        ASSERT_THROW_SYS(folders.updateFolder(folder),
                         macs::error::folderAlreadyExists,
                         "can't move folder 1 "
                         "with name \"inbox\" to 0: folder already exists");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesNonexistingFolder) {
        std::vector<macs::Folder> empty;
        EXPECT_CALL(folders, syncGetFolders(_)).Times(AnyNumber())
            .WillRepeatedly(GiveFolders(empty));

        ASSERT_THROW_SYS(folders.updateFolder(folders.folder("1")),
                         macs::error::noSuchFolder,
                         "can't update folder 1: no such folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderWithLongName) {
        std::string longName(macs::Folder::maxFolderNameLength() + 1, 'x');
        FolderFactory factory(inbox);
        factory.name(longName);
        Folder folder = factory.product();
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox));
        ASSERT_THROW_SYS(folders.updateFolder(folder), macs::error::invalidArgument,
                         "can't rename folder 1 name is too long: invalid argument");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderWithInvalidName) {
        string invalidName = "мет\x85ка";
        FolderFactory factory(inbox);
        factory.name(invalidName);
        Folder folder = factory.product();

        ASSERT_EQ(folder.name(), "мет?ка");
    }

    TEST_F(FoldersUpdateFolderTest, UpdatesFolderWithSpacesInName) {
        FolderFactory factory(inbox);
        factory.name("  name  ");
        Folder folder = factory.product();

        ASSERT_EQ(folder.name(), "name");
    }

    TEST_F(FoldersUpdateFolderTest, UpdateSetSymbolWrongSymbol) {
        Folder folder = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        ASSERT_THROW_SYS(folders.setSymbol(inbox,Folder::Symbol::inbox),
                         macs::error::cantModifyFolder,
                         "can't change symbol to inbox for fid: 1"
                         ": can not modify folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdateResetSymbolNonChangebleSymbol) {
        Folder folder = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        ASSERT_THROW_SYS(folders.resetSymbol(folder),
                         macs::error::cantModifyFolder,
                         "can't change symbol name for fid: 1"
                         ": can't change system folder: can not modify folder");
    }


    TEST_F(FoldersUpdateFolderTest, UpdateResetSymbolWithoutSymbol) {
        Folder folder = folders.factory().fid("1").name("blabla").symbol(Folder::Symbol::none);
        ASSERT_THROW_SYS(folders.resetSymbol(folder),
                         macs::error::cantModifyFolder,
                         "can't change symbol name for fid: 1"
                         ": folder don't have symbol: can not modify folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdateSetSymbolToSystemFolder) {
        Folder folder = folders.factory().fid("1").name("inbox").symbol(Folder::Symbol::inbox);
        ASSERT_THROW_SYS(folders.setSymbol(folder,Folder::Symbol::archive),
                         macs::error::cantModifyFolder,
                         "can't change symbol name for fid: 1"
                         ": symbol already exist inbox: can not modify folder");
    }

    TEST_F(FoldersUpdateFolderTest, UpdateSetSymbolOk) {
        Folder archive = folders.system(folders.folder("2", "archive", Folder::noParent));
        EXPECT_CALL(folders, syncSetFolderSymbol(archive.fid(),Folder::Symbol::archive,_))
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::NULL_REVISION));
        folders.setSymbol(archive,Folder::Symbol::archive);
    }

    TEST_F(FoldersUpdateFolderTest, UpdateSetDiscountFolderSymbolOk) {
        Folder discounts = folders.system(folders.folder("2", "discounts", Folder::noParent));
        EXPECT_CALL(folders, syncSetFolderSymbol(discounts.fid(),Folder::Symbol::discount,_))
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::NULL_REVISION));
        folders.setSymbol(discounts,Folder::Symbol::discount);
    }
    TEST_F(FoldersUpdateFolderTest, UpdateRemoveDiscountFolderSymbolOk) {
        Folder discounts = folders.factory().fid("2").name("discounts").symbol(Folder::Symbol::discount);
        EXPECT_CALL(folders, syncSetFolderSymbol(discounts.fid(),Folder::Symbol::none,_))
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::NULL_REVISION));
        folders.resetSymbol(discounts);
    }

}
