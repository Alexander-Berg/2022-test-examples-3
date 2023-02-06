#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/algorithm/string/join.hpp>
#include <macs/folder_set.h>
#include <macs/folder_factory.h>
#include "throw-wmi-helper.h"

namespace {
using namespace ::testing;
using namespace ::macs;
using namespace ::std;

struct FolderSetTest : public Test {
    FoldersMap foldersData;
    FolderSet folders;
    FolderSetTest() {
        fill();
        folders = FolderSet(foldersData);
    }

    void addFolder(const std::string& id, const std::string& name,
            const std::string& parentId, const Folder::Symbol& symbol) {
        FolderFactory factory;
        Folder folder = factory.fid(id).name(name).parentId(parentId).symbol(symbol);
        foldersData.insert(make_pair(folder.fid(), folder));
    }

    void fill() {
        addFolder("1", "inbox", Folder::noParent, Folder::Symbol::inbox);
        addFolder("2", "trash", Folder::noParent, Folder::Symbol::trash);
        addFolder("3", "spam", Folder::noParent, Folder::Symbol::spam);

        addFolder("4", "user", "1", Folder::Symbol::none);
        addFolder("5", "user", "1", Folder::Symbol::none);
        addFolder("6", "user", "1", Folder::Symbol::none);

        addFolder("7", "user", "4", Folder::Symbol::none);
        addFolder("8", "user", "4", Folder::Symbol::none);
        addFolder("9", "user", "5", Folder::Symbol::none);

        addFolder("10", "user|with|separator", "1", Folder::Symbol::none);
    }

    std::vector<std::string> getFids(const std::vector<Folder>& folders) {
        std::vector<std::string> res(folders.size());
        std::transform(folders.begin(), folders.end(), res.begin(), [](const Folder& folder){return folder.fid();});
        return res;
    }
};

TEST_F(FolderSetTest, atFid_forNonExistingFolder_throwsException) {
    ASSERT_THROW_SYS(folders.at("666"),
                     macs::error::noSuchFolder,
                     "access to nonexistent folder '666': no such folder");
}

TEST_F(FolderSetTest, atFid_forExistingFolder_returnsIt) {
    ASSERT_EQ("1", folders.at("1").fid());
}

TEST_F(FolderSetTest, atName_forNonExistingFolder_throwsException) {
    ASSERT_THROW_SYS(folders.at("sent", Folder::noParent),
                     macs::error::noSuchFolder,
                     "access to nonexistent folder 'sent': no such folder");
}

TEST_F(FolderSetTest, atName_forWrongParent_throwsException) {
    ASSERT_THROW_SYS(folders.at("user", Folder::noParent),
                     macs::error::noSuchFolder,
                     "access to nonexistent folder 'user': no such folder");
}

TEST_F(FolderSetTest, atName_forExistingFolder_returnsIt) {
    ASSERT_EQ("4", folders.at("user", "1").fid());
}

TEST_F(FolderSetTest, atSymbol_forNonExistingFolder_throwsException) {
    ASSERT_THROW_SYS(folders.at(Folder::Symbol::archive),
                     macs::error::noSuchFolder,
                     "access to nonexistent folder 'archive': no such folder");
}

TEST_F(FolderSetTest, atSymbol_forExistingFolder_returnsIt) {
    ASSERT_EQ("3", folders.at(Folder::Symbol::spam).fid());
}

TEST_F(FolderSetTest, fidByName_forNonExistingFolder_returnsEmptyString) {
    ASSERT_EQ("", folders.fid("sent", Folder::noParent));
}

TEST_F(FolderSetTest, fidByName_forExistingFolder_returnsIt) {
    ASSERT_EQ("3", folders.fid("spam", Folder::noParent));
}

TEST_F(FolderSetTest, fidBySymbol_forNonExistingFolder_returnsEmptyString) {
    ASSERT_EQ("", folders.fid(Folder::Symbol::sent));
}

TEST_F(FolderSetTest, fidBySymbol_forExistingFolder_returnsIt) {
    ASSERT_EQ("3", folders.fid(Folder::Symbol::spam));
}

TEST_F(FolderSetTest, getChildren_forFolderWithChildren_returnDirectChildren) {
    const auto folder = folders.at("1");
    ASSERT_THAT(getFids(folders.getChildren(folder)), UnorderedElementsAre("4", "5", "6", "10"));
}

TEST_F(FolderSetTest, getChildrenRecursive_forFolderWithGrandChildren_returnAllChildrenRecursively) {
    const auto folder = folders.at("1");
    ASSERT_THAT(getFids(folders.getChildrenRecursive(folder)), UnorderedElementsAre("4", "5", "6", "7", "8", "9", "10"));
}

TEST_F(FolderSetTest, tooManyFolders_numberOkDepthOk_noEc) {
    ASSERT_EQ(macs::error_code(), folders.checkFolderLimits(Folder::noParent, 100, 100, false));
}

TEST_F(FolderSetTest, tooManyFolders_numberExceededDepthOk_ec) {
    ASSERT_EQ(error::foldersLimitExceeded, folders.checkFolderLimits(Folder::noParent, 5, 100, false));
}

TEST_F(FolderSetTest, tooManyFolders_numberExceededDepthOkButSymbol_ec) {
    ASSERT_EQ(macs::error_code(), folders.checkFolderLimits(Folder::noParent, 5, 100, true));
}

TEST_F(FolderSetTest, tooManyFolders_numberOkDepthExceeded_ec) {
    ASSERT_EQ(error::foldersLimitExceeded, folders.checkFolderLimits("9", 100, 2, false));
}


TEST_F(FolderSetTest, checkCanCreateFolder_should_return_error_for_existing_folder_name) {
    ASSERT_EQ(error::folderAlreadyExists, folders.checkCanCreateFolder(Folder::Name("user"), "1", false));
    ASSERT_EQ(error::folderAlreadyExists, folders.checkCanCreateFolder(Folder::Name("user|with|separator"), "1", false));
}


TEST_F(FolderSetTest, checkCanGetOrCreateFolder_should_return_error_for_not_existing_parent) {
    ASSERT_EQ(error::noSuchFolder, folders.checkCanGetOrCreateFolder("100500", false));
}

TEST_F(FolderSetTest, checkCanGetOrCreateFolder_should_not_return_error_for_zero_or_empty_parent) {
    ASSERT_EQ(macs::error_code(), folders.checkCanGetOrCreateFolder("", false));
    ASSERT_EQ(macs::error_code(), folders.checkCanGetOrCreateFolder("0", false));
}


TEST_F(FolderSetTest, checkCanRenameFolder_should_return_error_for_system_folder) {
    const auto original = FolderFactory().name("Inbox").fid("1").type(Folder::Type::system).product();
    const auto target = FolderFactory().name("Inbox").fid("1").parentId("2").type(Folder::Type::system).product();
    ASSERT_EQ(error::cantModifyFolder, folders.checkCanRenameFolder(target, original));
}

TEST_F(FolderSetTest, checkCanRenameFolder_should_return_error_for_empty_new_name) {
    const auto original = FolderFactory().name("user").fid("6").product();
    const auto target = FolderFactory().name("").fid("6").product();
    ASSERT_EQ(error::invalidArgument, folders.checkCanRenameFolder(target, original));
}

TEST_F(FolderSetTest, checkCanRenameFolder_should_return_error_for_too_long_new_name) {
    const auto original = FolderFactory().name("user").fid("6").product();
    const auto target = FolderFactory().name(std::string(Folder::maxFolderNameLength() + 1, 'A')).fid("6").product();
    ASSERT_EQ(error::invalidArgument, folders.checkCanRenameFolder(target, original));
}


TEST_F(FolderSetTest, checkCanMoveFolder_should_return_error_for_system_folder) {
    const auto original = FolderFactory().name("Inbox").fid("1").type(Folder::Type::system).product();
    const auto target = FolderFactory().name("Inbox").fid("1").parentId("2").type(Folder::Type::system).product();
    ASSERT_EQ(error::cantModifyFolder, folders.checkCanMoveFolder(target, original));
}

TEST_F(FolderSetTest, checkCanMoveFolder_should_return_error_for_move_folder_to_itself) {
    const auto original = FolderFactory().name("user").fid("10").product();
    const auto target = FolderFactory().name("user").fid("10").parentId("10").product();
    ASSERT_EQ(error::folderCantBeParent, folders.checkCanMoveFolder(target, original));
}

TEST_F(FolderSetTest, checkCanMoveFolder_should_return_error_for_move_folder_to_nonexistent_parent) {
    const auto original = FolderFactory().name("user").fid("10").product();
    const auto target = FolderFactory().name("user").fid("10").parentId("100500").product();
    ASSERT_EQ(error::folderCantBeParent, folders.checkCanMoveFolder(target, original));
}

TEST_F(FolderSetTest, checkCanMoveFolder_should_return_error_for_move_folder_to_childless_parent) {
    const auto original = FolderFactory().name("user").fid("10").product();
    const auto target = FolderFactory().name("user").fid("10").parentId("3").product();
    ASSERT_EQ(error::folderCantBeParent, folders.checkCanMoveFolder(target, original));
}

TEST_F(FolderSetTest, checkCanMoveFolder_should_not_return_error_for_move_folder_to_appropriate_parent) {
    const auto original = FolderFactory().name("test_folder").fid("6").product();
    const auto target = FolderFactory().name("test_folder").fid("6").parentId("4").product();
    ASSERT_EQ(macs::error_code(), folders.checkCanMoveFolder(target, original));
}


TEST_F(FolderSetTest, checkCanEraseFolder_should_return_error_for_system_folder) {
    const auto folder = FolderFactory().name("Inbox").type(Folder::Type::system).product();
    ASSERT_EQ(error::cantModifyFolder, folders.checkCanEraseFolder(folder));
}

TEST_F(FolderSetTest, checkCanEraseFolder_should_return_error_for_folder_with_messages) {
    const auto folder = FolderFactory().name("user").type(Folder::Type::user).messages(1).product();
    ASSERT_EQ(error::folderIsNotEmpty, folders.checkCanEraseFolder(folder));
}

TEST_F(FolderSetTest, checkCanEraseFolder_should_return_error_for_folder_with_subfolder) {
    ASSERT_EQ(error::folderIsNotEmpty, folders.checkCanEraseFolder(folders.at("1")));
}

TEST_F(FolderSetTest, checkCanEraseFolder_should_not_return_error_for_user_empty_folder) {
    const auto folder = FolderFactory().name("user").type(Folder::Type::user).product();
    ASSERT_EQ(macs::error_code(), folders.checkCanEraseFolder(folder));
}


TEST_F(FolderSetTest, checkCanUpdateFolder_should_not_return_error_when_folder_name_and_parent_id_have_not_changed) {
    const auto original = FolderFactory().name("name").parentId("4").product();
    const auto target = FolderFactory().name("name").parentId("4").product();
    ASSERT_EQ(macs::error_code(), folders.checkCanUpdateFolder(target, original));
}


TEST_F(FolderSetTest, find_should_return_iterator_for_existing_fid) {
    ASSERT_NE(folders.end(), folders.find(Fid("1")));
}

TEST_F(FolderSetTest, find_should_return_end_iterator_for_not_existing_fid) {
    ASSERT_EQ(folders.end(), folders.find(Fid("100500")));
    FolderSet emptyFolderSet;
    ASSERT_EQ(emptyFolderSet.end(), emptyFolderSet.find(Fid("100500")));
}

TEST_F(FolderSetTest, find_should_return_iterator_for_existing_path) {
    using PathValue = std::vector<Folder::Name>;
    ASSERT_NE(folders.end(), folders.find(Folder::Path(PathValue{"inbox", "user", "user"})));
}

TEST_F(FolderSetTest, find_should_return_end_iterator_for_not_existing_path) {
    using PathValue = std::vector<Folder::Name>;
    ASSERT_EQ(folders.end(), folders.find(Folder::Path(PathValue{"inbox", "foo", "bar"})));
    FolderSet emptyFolderSet;
    ASSERT_EQ(emptyFolderSet.end(), emptyFolderSet.find(Folder::Path(PathValue{"inbox", "foo", "bar"})));
}

TEST_F(FolderSetTest, find_should_return_iterator_for_existing_folder) {
    ASSERT_NE(folders.end(), folders.find(Folder::Name("inbox"), Folder::noParent));
    ASSERT_NE(folders.end(), folders.find(Folder::Name("user"), Fid("1")));
}

TEST_F(FolderSetTest, find_should_return_iterator_for_existing_folder_with_separator) {
    ASSERT_NE(folders.end(), folders.find(Folder::Name("user|with|separator"), Fid("1")));
}

TEST_F(FolderSetTest, find_should_return_end_iterator_for_not_existing_folder) {
    ASSERT_EQ(folders.end(), folders.find(Folder::Name("inbox"), Fid("1")));
    FolderSet emptyFolderSet;
    ASSERT_EQ(emptyFolderSet.end(), emptyFolderSet.find(Folder::Name("inbox"), Fid("1")));
}

TEST_F(FolderSetTest, find_should_return_end_iterator_for_not_existing_escaped_name) {
    ASSERT_EQ(folders.end(), folders.find(Folder::Name("foo|bar"), Fid("1")));
    FolderSet emptyFolderSet;
    ASSERT_EQ(emptyFolderSet.end(), emptyFolderSet.find(Folder::Name("foo|bar"), Fid("1")));
}

}
