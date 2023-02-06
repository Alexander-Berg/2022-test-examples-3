#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/wmi/meta_access/metadata_helpers.h>

namespace {

using namespace ::testing;
using namespace ::hound::helpers;
using macs::FolderFactory;


inline auto makeFolder(const macs::Fid& fid, macs::Folder::Symbol symbol, const std::size_t newMsgs) {
    return std::make_pair(fid, FolderFactory()
            .fid(fid)
            .symbol(symbol)
            .newMessages(newMsgs)
            .product()
    );
}

TEST(GetUnreadUsefulFoldersTest, shouldReturnEmptyListForEmptyFolderSet) {
    EXPECT_THAT(getUnreadUsefulFolders(macs::FolderSet{}), IsEmpty());
}

TEST(GetUnreadUsefulFoldersTest, shouldNotReturnInboxWithoutNewMessages) {
    const macs::Fid fid = "1";
    const macs::FolderSet folders({
        makeFolder(fid, macs::Folder::Symbol::inbox, 0)
    });

    EXPECT_THAT(getUnreadUsefulFolders(folders), IsEmpty());
}

TEST(GetUnreadUsefulFoldersTest, shouldReturnInboxWithNewMessages) {
    const macs::Fid fid = "1";
    const macs::FolderSet folders({
        makeFolder(fid, macs::Folder::Symbol::inbox, 1)
    });

    EXPECT_EQ(getUnreadUsefulFolders(folders), macs::FidList{fid});
}

TEST(GetUnreadUsefulFoldersTest, shouldNotReturnFolderWithoutNewMessagesAndSymbol) {
    const macs::Fid fid = "10";
    const macs::FolderSet folders({
        makeFolder(fid, macs::Folder::Symbol::none, 0)
    });

    EXPECT_THAT(getUnreadUsefulFolders(folders), IsEmpty());
}

TEST(GetUnreadUsefulFoldersTest, shouldReturnFolderWithNewMessagesAndWithoutSymbol) {
    const macs::Fid fid = "10";
    const macs::FolderSet folders({
        makeFolder(fid, macs::Folder::Symbol::none, 1)
    });

    EXPECT_EQ(getUnreadUsefulFolders(folders), macs::FidList{fid});
}

TEST(GetUnreadUsefulFoldersTest, shouldNoReturnUnusefulFolder) {
    macs::FoldersMap folders;

    std::size_t fid = 2;
    for (auto symbol : macs::Folder::Symbol::getDict()) {
        if (macs::Folder::Symbol::inbox != *symbol && macs::Folder::Symbol::none != *symbol) {
            folders.insert(makeFolder(std::to_string(fid++), *symbol, 1));
            folders.insert(makeFolder(std::to_string(fid++), *symbol, 0));
        }
    }

    EXPECT_EQ(getUnreadUsefulFolders(macs::FolderSet(folders)), macs::FidList{});
}

TEST(FilterFoldersWithoutSymbolsTest, shouldReturnEmptyListForEmptyFolderSet) {
    macs::FidList fids;
    filterFoldersWithoutSymbols(macs::FolderSet{}, {}, fids);
    EXPECT_THAT(fids, IsEmpty());
}

TEST(FilterFoldersWithoutSymbolsTest, shouldReturnOnlyFoldersWithoutFilteredSymbols) {
    const macs::Fid draftsFid = "2";
    const macs::Fid spamFid = "4";
    const macs::FoldersMap folders = {
            {draftsFid, FolderFactory().fid(draftsFid).symbol(macs::Folder::Symbol::drafts).product()}
            , {spamFid, FolderFactory().fid(spamFid).symbol(macs::Folder::Symbol::spam).product()}
    };
    macs::FidList filteredFids{draftsFid, spamFid};
    const std::list<macs::Folder::Symbol> symbols {macs::Folder::Symbol::spam};

    filterFoldersWithoutSymbols(macs::FolderSet(folders), symbols, filteredFids);
    EXPECT_THAT(filteredFids, Contains(draftsFid));
}

} // anonymous namespace