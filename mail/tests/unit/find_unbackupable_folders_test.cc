#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/handlers/settings.h>

using namespace ::testing;

namespace barbet::tests {

inline auto makeFolder(const macs::Fid& fid, const std::string& name, macs::Folder::Symbol symbol) {
    return std::make_pair(fid, macs::FolderFactory()
            .fid(fid)
            .name(name)
            .symbol(symbol)
            .product()
    );
}

struct ContainsUnbackupableFoldersTest : TestWithParam<std::pair<macs::FidVec, bool>> {
    const macs::FolderSet folderSet = macs::FolderSet({
        makeFolder("1", "inbox", macs::Folder::Symbol::inbox),
        makeFolder("11", "user", macs::Folder::Symbol::none),
        makeFolder("2", "spam", macs::Folder::Symbol::spam),
        makeFolder("3", "trash", macs::Folder::Symbol::trash),
        makeFolder("4", "hidden_trash", macs::Folder::Symbol::hidden_trash),
    });
};

INSTANTIATE_TEST_SUITE_P(fids, ContainsUnbackupableFoldersTest, Values(
        std::make_pair(macs::FidVec{}, false),
        std::make_pair(macs::FidVec{"1", "11"}, false),
        std::make_pair(macs::FidVec{"2"}, true),
        std::make_pair(macs::FidVec{"3"}, true),
        std::make_pair(macs::FidVec{"4"}, true),
        std::make_pair(macs::FidVec{"2", "3", "4"}, true)
));

TEST_P(ContainsUnbackupableFoldersTest, shouldReturnTrueWhenUnbackupableFolderExists) {
    const auto [fids, expected] = GetParam();
    EXPECT_EQ(findUnbackupableFolder(fids, folderSet), expected);
}

}