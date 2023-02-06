#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include <internal/server/handlers/subscribe.h>
#include <macs/folders_repository.h>
#include <macs/io.h>
#include <macs_pg/subscribed_folders/factory.h>
#include <macs_pg/subscription/factory.h>
#include "test_mocks.h"

using namespace testing;

namespace york {
namespace tests {

struct SubscribeMacsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macsOwner;
    MacsMock<sync> macsSubscriber;
    LoggerMock logMock;
    void execute(SubscribeParams params) {
        server::handlers::executeMacsSubscribe(
                    macsOwner, macsSubscriber, ctx, params,
                    log::make_log(log::none, &logMock), macs::io::use_sync);
    }
};

TEST_F(SubscribeMacsTest, sharedFolderFidNotFound_responses400) {
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderExistsAndIsNotEmpty_responses400) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(1);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(subFs));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderIsEmpty_subscribesItAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(subFs));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "fid", _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderSubscribedToSamePath_addSubscriberAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    macs::SubscribedFolder subf = macs::SubscribedFolderFactory()
            .fid(f.fid()).ownerUid("uid1").ownerFid(shared.fid()).release();
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(subFs));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{subf}));
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderSubscribedToDifferentPath_addSubscriberAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("b").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    macs::SubscribedFolder subf = macs::SubscribedFolderFactory()
            .fid(f.fid()).ownerUid("uid1").ownerFid(shared.fid()).release();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(subFs));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{subf}));
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderDoesNotExist_createsAndSubscribesItAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "fid", _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, folderDoesNotExist_createsAndSubscribesIt_withImapUnsubscribedYes_marksImapUnsubscribedAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    InSequence seq;
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "fid", _)).Times(1);
    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"a"}, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::optional<bool>(true)});
}

TEST_F(SubscribeMacsTest, sharedFidNotSet_usesInboxAndResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder i = macs::FolderFactory().fid("2").name("inbox").parentId("").messages(0).symbol(macs::Folder::Symbol::inbox);
    macs::FoldersMap sharFs;
    sharFs.insert({"2", i});

    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"2"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"2"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "2", _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("2", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>(boost::none), boost::none, boost::none});
}

TEST_F(SubscribeMacsTest, withRecursiveYes_getAllSharedSubfolders_folderExistsAndIsNotEmpty_responses400) {
    macs::FoldersMap subFs = {
        {"1", macs::FolderFactory().fid("2").name("bbs").parentId("").messages(0)},
        {"2", macs::FolderFactory().fid("2").name("folder2").parentId("1").messages(1)}};

    macs::FoldersMap sharFs = {
        {"fid", macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(0)},
        {"fid1", macs::FolderFactory().fid("fid1").name("folder1").parentId("fid").messages(0)},
        {"fid2", macs::FolderFactory().fid("fid2").name("child1").parentId("fid1").messages(0)},
        {"fid3", macs::FolderFactory().fid("fid3").name("folder2").parentId("fid").messages(0)}};

    auto fidRange = sharFs | boost::adaptors::map_keys;
    macs::FidVec fids(fidRange.begin(), fidRange.end());

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(fids));
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macsOwner, getByFids("uid2", UnorderedElementsAreArray(fids), _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(subFs));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid2", {"bbs"}, "uid1", boost::optional<std::string>("fid"), boost::optional<bool>(true), boost::none});
}

TEST_F(SubscribeMacsTest, withRecursiveYes_getAllSharedSubfolders_createSubscribedFolders_AndResponsesOk) {
    macs::FoldersMap sharFs = {
        {"fid", macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(0)},
        {"fid1", macs::FolderFactory().fid("fid1").name("folder1").parentId("fid").messages(0)},
        {"fid2", macs::FolderFactory().fid("fid2").name("child1").parentId("fid1").messages(0)},
        {"fid3", macs::FolderFactory().fid("fid3").name("folder2").parentId("fid").messages(0)}};

    std::map<std::string, macs::Folder::Path> sharFsPath;
    sharFsPath["fid"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs"});
    sharFsPath["fid1"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder1"});
    sharFsPath["fid2"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder1", "child1"});
    sharFsPath["fid3"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder2"});

    auto fidRange = sharFs | boost::adaptors::map_keys;
    macs::FidVec fids(fidRange.begin(), fidRange.end());

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(fids));
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macsOwner, getByFids("uid2", UnorderedElementsAreArray(fids), _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    for (const auto& f: sharFs) {
        InSequence seq;
        auto fullName = sharFsPath[f.first];
        EXPECT_CALL(macsSubscriber, createFolderByPath(fullName, _))
                .WillOnce(Return(f.second));
        EXPECT_CALL(macsSubscriber, addFolder(f.first, "uid1", f.first, _)).Times(1);
        EXPECT_CALL(macsOwner, addSubscriber(f.first, "uid2", _)).Times(1);
    }

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"bbs"}, "uid1", boost::optional<std::string>("fid"), boost::optional<bool>(true), boost::none});
}

TEST_F(SubscribeMacsTest, withRecursiveYes_getAllSharedSubfolders_createSubscribedFolders_withImapUnsubscribedYes_marksImapUnsubscribed_AndResponsesOk) {
    macs::FoldersMap sharFs = {
        {"fid", macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(0)},
        {"fid1", macs::FolderFactory().fid("fid1").name("folder1").parentId("fid").messages(0)},
        {"fid2", macs::FolderFactory().fid("fid2").name("child1").parentId("fid1").messages(0)},
        {"fid3", macs::FolderFactory().fid("fid3").name("folder2").parentId("fid").messages(0)}};

    std::map<std::string, macs::Folder::Path> sharFsPath;
    sharFsPath["fid"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs"});
    sharFsPath["fid1"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder1"});
    sharFsPath["fid2"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder1", "child1"});
    sharFsPath["fid3"] = macs::Folder::Path(std::vector<macs::Folder::Name> {"bbs", "folder2"});

    auto fidRange = sharFs | boost::adaptors::map_keys;
    macs::FidVec fids(fidRange.begin(), fidRange.end());

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(fids));
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macsOwner, getByFids("uid2", UnorderedElementsAreArray(fids), _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    for (const auto& f: sharFs) {
        InSequence seq;
        auto fullName = sharFsPath[f.first];
        EXPECT_CALL(macsSubscriber, createFolderByPath(fullName, _))
                .WillOnce(Return(f.second));
        EXPECT_CALL(macsSubscriber, addFolder(f.first, "uid1", f.first, _)).Times(1);
        EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{fullName.begin(), fullName.end()}, _)).Times(1);
        EXPECT_CALL(macsOwner, addSubscriber(f.first, "uid2", _)).Times(1);
    }

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"bbs"}, "uid1", boost::optional<std::string>("fid"), boost::optional<bool>(true), boost::optional<bool>(true)});
}

TEST_F(SubscribeMacsTest, imapUnsubscribeThrows_logError_stillResponsesOk) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    InSequence seq;
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "fid", _)).Times(1);
    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"a"}, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(logMock, error(_)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::optional<bool>(true)});
}

TEST_F(SubscribeMacsTest, addOnlyFinalFolderInPathToImapUnsubscribe) {
    macs::Folder f = macs::FolderFactory().fid("1").name("k").parentId("par").messages(0);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    InSequence seq;
    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("uid1", _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"y", "o", "r", "k"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", "uid1", "fid", _)).Times(1);
    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"y", "o", "r", "k"}, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber("fid", "uid2", _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SubscribeResult>())).Times(1);

    execute({"uid2", {"y", "o", "r", "k"}, "uid1",boost::optional<std::string>("fid"), boost::none, boost::optional<bool>(true)});
}

TEST_F(SubscribeMacsTest, subscriptionExistsAndTerminating_responses400) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId("").messages(1);
    macs::FoldersMap subFs;
    subFs.insert({"1", f});

    macs::Folder shared = macs::FolderFactory().fid("fid").name("bbs").parentId("").messages(1);
    macs::FoldersMap sharFs;
    sharFs.insert({"fid", shared});

    macs::Subscription sub = macs::SubscriptionFactory()
            .subscriptionId(1)
            .state(macs::pg::SubscriptionState::clear)
            .release();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsOwner, getByFids("uid2", macs::FidVec{"fid"}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{sub}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid2", {"a"}, "uid1", boost::optional<std::string>("fid"), boost::none, boost::none});
}


} //namespace tests
} //namespace york

