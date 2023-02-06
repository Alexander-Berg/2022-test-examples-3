#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/handlers/restore.h>


using namespace ::testing;

namespace barbet::tests {

TEST(MakeBackupFoldersPaths, shouldFormCorrectMap) {
    using macs::BackupFolder;
    using Path = macs::Folder::Path;

    std::vector<BackupFolder> backupFolders {
        BackupFolder{.fid="1", .name="inbox", .parentFid=std::nullopt},
        BackupFolder{.fid="2", .name="spam", .parentFid=""},
        BackupFolder{.fid="3", .name="trash", .parentFid="0"},
        BackupFolder{.fid="4", .name="root", .parentFid=""},
        BackupFolder{.fid="5", .name="child1", .parentFid="4"},
        BackupFolder{.fid="6", .name="child2", .parentFid="4"},
        BackupFolder{.fid="7", .name="grandchild", .parentFid="5"},
        BackupFolder{.fid="8", .name="child3", .parentFid="13"}
    };

    auto map = detail::makeBackupFoldersPaths(std::move(backupFolders));
    EXPECT_THAT(map, UnorderedElementsAre(
        Pair("1", Path(std::vector<std::string>{"inbox"})),
        Pair("2", Path(std::vector<std::string>{"spam"})),
        Pair("3", Path(std::vector<std::string>{"trash"})),
        Pair("4", Path(std::vector<std::string>{"root"})),
        Pair("5", Path(std::vector<std::string>{"root", "child1"})),
        Pair("6", Path(std::vector<std::string>{"root", "child2"})),
        Pair("7", Path(std::vector<std::string>{"root", "child1", "grandchild"})),
        Pair("8", Path(std::vector<std::string>{"child3"}))
    ));
}

}
