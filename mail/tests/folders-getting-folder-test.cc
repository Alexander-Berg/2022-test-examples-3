#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace macs;
    using namespace testing;
    using namespace std;

    struct FoldersGettingFolderTest: public FoldersRepositoryTest {
    };

    TEST_F(FoldersGettingFolderTest, FindsFolderByFid) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THAT(folders.getFolderByFid("1"), matchFolder(folder));
    }

    TEST_F(FoldersGettingFolderTest, FindsFoldersByFids) {
        Folder folder1 = folders.folder("1", "a", Folder::noParent);
        Folder folder2 = folders.folder("2", "b", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder1, folder2));

        std::vector<std::string> fids;
        fids.push_back("1");
        fids.push_back("2");
        std::vector<Folder> flds;
        folders.getFoldersByFids(fids.begin(), fids.end(), std::back_inserter(flds));
        ASSERT_THAT(flds, ElementsAre(matchFolder(folder1), matchFolder(folder2)));
    }

    TEST_F(FoldersGettingFolderTest, FindsNonexistingFoldersByFids) {
        Folder folder1 = folders.folder("1", "a", Folder::noParent);
        Folder folder2 = folders.folder("2", "b", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder1, folder2));

        std::vector<std::string> fids;
        fids.push_back("1");
        fids.push_back("2");
        fids.push_back("3");
        std::vector<Folder> flds;
        ASSERT_THROW_SYS(folders.getFoldersByFids(fids.begin(), fids.end(), std::back_inserter(flds)),
                         macs::error::noSuchFolder,
                         "access to nonexistent folder '3': no such folder");
    }

    TEST_F(FoldersGettingFolderTest, FindsNonexistingFolderByFid) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THROW_SYS(folders.getFolderByFid("2"),
                         macs::error::noSuchFolder,
                         "access to nonexistent folder '2': no such folder");
    }

    TEST_F(FoldersGettingFolderTest, FindsFolderFidBySymbol) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent)
            .symbol(Folder::Symbol::inbox);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_EQ("1", folders.getFolderFidBySymbol(Folder::Symbol::inbox));
    }

    TEST_F(FoldersGettingFolderTest, FindsFolderBySymbol) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent)
            .symbol(Folder::Symbol::inbox);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THAT(folders.getFolderBySymbol(Folder::Symbol::inbox),
                    matchFolder(folder));
    }

    TEST_F(FoldersGettingFolderTest, FindsNonexistingFolderBySymbol) {
        Folder folder = folders.folder("1", "inbox", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THROW_SYS(folders.getFolderBySymbol(Folder::Symbol::inbox),
                         macs::error::noSuchFolder,
                         "access to nonexistent folder 'inbox': no such folder");
    }

    TEST_F(FoldersGettingFolderTest, FindsNonexistingSymbol) {
        Folder folder = folders.folder("1", "folder", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_EQ("", folders.getFolderFidBySymbol(Folder::Symbol::inbox));
    }

    TEST_F(FoldersGettingFolderTest, FindsSymbolNone) {
        ASSERT_EQ("", folders.getFolderFidBySymbol(Folder::Symbol::none));
    }

    TEST_F(FoldersGettingFolderTest, GetsOnlyFolders) {
        Folder folder = folders.folder("2", "folder", Folder::noParent);

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THAT(folders.getAllFolders(),
                    ElementsAre(Field(&pair<string, Folder>::second,
                                      matchFolder(folder))));
    }

    TEST_F(FoldersGettingFolderTest, getAllFolders_skips_hidden_trash_folder) {
        const auto inbox = folders.folder("1").symbol(macs::Folder::Symbol::inbox).product();
        const auto hidden_trash = folders.folder("2").symbol(macs::Folder::Symbol::hidden_trash).product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders({inbox, hidden_trash}));

        ASSERT_THAT(folders.getAllFolders(),
                    ElementsAre(Field(&pair<string, Folder>::second, matchFolder(inbox))));
    }

    TEST_F(FoldersGettingFolderTest, getAllFoldersWithHidden_returns_hidden_trash_folder) {
        const auto inbox = folders.folder("1").symbol(macs::Folder::Symbol::inbox).product();
        const auto hidden_trash = folders.folder("2").symbol(macs::Folder::Symbol::hidden_trash).product();

        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders({inbox, hidden_trash}));

        ASSERT_THAT(folders.getAllFoldersWithHidden(),
                    ElementsAre(Field(&pair<string, Folder>::second, matchFolder(inbox)),
                                Field(&pair<string, Folder>::second, matchFolder(hidden_trash))));
    }

    struct MockFoldersRepositoryOriginalDefaultSymbols: public MockFoldersRepository {
        const macs::Folder::SymbolSet& defaultFoldersSymbols() const override {
            return macs::FoldersRepository::defaultFoldersSymbols();
        }
    };

    TEST_F(FoldersGettingFolderTest, get_folder_with_all_default_symbols_check_folder_range_should_succeed) {
        auto folders = std::make_shared<MockFoldersRepositoryOriginalDefaultSymbols>();

        const auto inbox = folders->folder("1").symbol(macs::Folder::Symbol::inbox).product();
        const auto sent = folders->folder("2").symbol(macs::Folder::Symbol::sent).product();
        const auto trash = folders->folder("3").symbol(macs::Folder::Symbol::trash).product();
        const auto spam = folders->folder("4").symbol(macs::Folder::Symbol::spam).product();
        const auto drafts = folders->folder("5").symbol(macs::Folder::Symbol::drafts).product();
        const auto outbox = folders->folder("6").symbol(macs::Folder::Symbol::outbox).product();

        EXPECT_CALL(*folders, syncGetFolders(_))
            .WillOnce(GiveFolders({inbox, sent, trash, spam, drafts, outbox}));

        ASSERT_THAT(folders->getAllFolders(),
                    ElementsAre(Field(&pair<string, Folder>::second, matchFolder(inbox)),
                                Field(&pair<string, Folder>::second, matchFolder(sent)),
                                Field(&pair<string, Folder>::second, matchFolder(trash)),
                                Field(&pair<string, Folder>::second, matchFolder(spam)),
                                Field(&pair<string, Folder>::second, matchFolder(drafts)),
                                Field(&pair<string, Folder>::second, matchFolder(outbox))));
    }

    TEST_F(FoldersGettingFolderTest, get_folder_without_all_default_symbols_check_folder_range_should_throw_exception) {
        auto folders = std::make_shared<MockFoldersRepositoryOriginalDefaultSymbols>();

        const auto folder = folders->folder("1").symbol(macs::Folder::Symbol::inbox).product();

        EXPECT_CALL(*folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        ASSERT_THROW(folders->getAllFolders(), macs::UserNotInitialized);
    }

    TEST_F(FoldersGettingFolderTest, get_folder_without_all_default_symbols_check_folder_range_should_throw_exception_with_specific_message) {
        auto folders = std::make_shared<MockFoldersRepositoryOriginalDefaultSymbols>();

        const auto folder = folders->folder("1").symbol(macs::Folder::Symbol::inbox).product();

        EXPECT_CALL(*folders, syncGetFolders(_)).WillOnce(GiveFolders(folder));

        std::string errorMessage;

        try {
            folders->getAllFolders();
        } catch (const macs::UserNotInitialized& error) {
            errorMessage = error.what();
        }

        ASSERT_EQ(errorMessage,
                  "folder list doesn't contains folders with "
                  "symbols=sent,trash,spam,draft,outbox"
                  ": user is not initialized in the database");
    }
}
