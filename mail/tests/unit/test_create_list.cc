#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include <internal/server/handlers/create_list.h>
#include <macs/folders_repository.h>
#include <macs/io.h>

using namespace testing;

namespace york {
namespace tests {

struct CreateListMacsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macs;

    using Type = macs::Folder::ArchivationType;
    const FolderArchivation cfg {"archive", 1000, 30};

    void execute(boost::optional<std::string> name) {
        server::handlers::executeMacsCreateList(
                    macs, ctx, name, cfg,
                    log::none,
                    macs::io::use_sync);
    }
};

TEST_F(CreateListMacsTest, folderExistsAndIsNotEmpty_responses400) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);
    macs::FoldersMap fs;
    fs.insert({"1", f});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(macs::FolderSet(fs)));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute(boost::optional<std::string>("a"));
}

TEST_F(CreateListMacsTest, folderExistsAndIsEmpty_createsSharedFolderAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);
    macs::FoldersMap fs;
    fs.insert({"1", f});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(macs::FolderSet(fs)));
    EXPECT_CALL(macs, createSharedFolderWithArchivation(
                    "1", Type{Type::fromString(cfg.type)}, cfg.keep_days, cfg.max_folder_size, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::CreateListResult>())).Times(1);

    execute(boost::optional<std::string>("a"));
}

TEST_F(CreateListMacsTest, folderDoesNotExist_createsFolderAndCreatesSharedFolderAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macs, createFolder("a", _)).WillOnce(Return(f));
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(std::vector<macs::Fid>()));
    EXPECT_CALL(macs, createSharedFolderWithArchivation(
                    "1", Type{Type::fromString(cfg.type)}, cfg.keep_days, cfg.max_folder_size, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::CreateListResult>())).Times(1);

    execute(boost::optional<std::string>("a"));
}

TEST_F(CreateListMacsTest, folderNotSet_usesInboxAndCreatesSharedFolderAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("inbox").parentId(macs::Folder::noParent).messages(0).symbol(macs::Folder::Symbol::inbox);
    macs::FoldersMap fs;
    fs.insert({"1", f});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(macs::FolderSet(fs)));
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(std::vector<macs::Fid>()));
    EXPECT_CALL(macs, createSharedFolderWithArchivation(
                    "1", Type{Type::fromString(cfg.type)}, cfg.keep_days, cfg.max_folder_size, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::CreateListResult>())).Times(1);

    execute(boost::none);
}

TEST_F(CreateListMacsTest, sharedFolderExists_ResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);
    macs::FoldersMap fs;
    fs.insert({"1", f});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(macs::FolderSet(fs)));
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(std::vector<macs::Fid>{"1"}));
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::CreateListResult>())).Times(1);

    execute(boost::optional<std::string>("a"));
}

} //namespace tests
} //namespace york
