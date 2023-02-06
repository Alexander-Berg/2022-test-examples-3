#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/subscribe.h>
#include <yplatform/reactor/io_pool.h>

using namespace testing;

namespace york {
namespace tests {

template <typename Context>
class ArgumentsExpectations {
public:
    ArgumentsExpectations(Context ctx)
            : ctx_(ctx)
            , subscriberUid_(boost::none)
            , destinationPath_(boost::none)
            , ownerUid_(boost::none)
            , sharedFolderFid_(boost::none)
            , recursive_(boost::none)
            , imapUnsubscribed_(boost::none) {}

    ArgumentsExpectations& subscriberUid(const std::string& suid) {
        subscriberUid_ = boost::make_optional(suid);
        return *this;
    }

    ArgumentsExpectations& destinationPath(const std::string& destPath) {
        destinationPath_ = boost::make_optional(destPath);
        return *this;
    }

    ArgumentsExpectations& ownerUid(const std::string& ouid) {
        ownerUid_ = boost::make_optional(ouid);
        return *this;
    }

    ArgumentsExpectations& sharedFolderFid(const std::string& sharedFid) {
        sharedFolderFid_ = boost::make_optional(sharedFid);
        return *this;
    }

    ArgumentsExpectations& recursive(const bool rec) {
        recursive_ = rec ? boost::make_optional(std::string("yes"))
                         : boost::make_optional(std::string("no"));
        return *this;
    }

    ArgumentsExpectations& imapUnsubscribed(const bool imapUnsubscribed) {
        imapUnsubscribed_ = imapUnsubscribed
                ? boost::make_optional(std::string("yes"))
                : boost::make_optional(std::string("no"));
        return *this;
    }

    void expect() {
        EXPECT_CALL(*ctx_, getOptionalArg("subscriber_uid")).WillOnce(Return(subscriberUid_));
        EXPECT_CALL(*ctx_, getOptionalArg("destination_folder_path")).WillOnce(Return(destinationPath_));
        EXPECT_CALL(*ctx_, getOptionalArg("owner_uid")).WillOnce(Return(ownerUid_));
        EXPECT_CALL(*ctx_, getOptionalArg("shared_folder_fid")).WillOnce(Return(sharedFolderFid_));
        EXPECT_CALL(*ctx_, getOptionalArg("recursive")).WillOnce(Return(recursive_));
        EXPECT_CALL(*ctx_, getOptionalArg("imap_unsubscribed")).WillOnce(Return(imapUnsubscribed_));
    }

private:
    Context ctx_;
    boost::optional<std::string> subscriberUid_;
    boost::optional<std::string> destinationPath_;
    boost::optional<std::string> ownerUid_;
    boost::optional<std::string> sharedFolderFid_;
    boost::optional<std::string> recursive_;
    boost::optional<std::string> imapUnsubscribed_;
};

struct SubscribeIntegrationTest: public Test {
    using ContextPtr = std::shared_ptr<StrictMock<ContextMock>>;
    ContextPtr ctx = std::make_shared<StrictMock<ContextMock>>();
    StrictMock<MacsMock<coro>> macsSubscriber;
    StrictMock<MacsMock<coro>> macsOwner;
    boost::asio::io_service ios;
    boost::shared_ptr<yplatform::reactor> old;
    const std::string subscriberUid {"subscriber_uid"};
    std::string ownerUid {"owner_uid"};
    std::string sharedFolderId {"shared_folder_fid"};
    std::string destinationFolderPath {R"json(["a"])json"};
    ArgumentsExpectations<ContextPtr> arguments{ctx};
    SubscribeIntegrationTest() {
        auto iop = std::make_shared<yplatform::io_pool>(ios, 1);
        old = boost::make_shared<yplatform::reactor>(iop);
        std::swap(old, yplatform::global_net_reactor);

        arguments.subscriberUid(subscriberUid)
                .ownerUid(ownerUid)
                .destinationPath(destinationFolderPath)
                .sharedFolderFid(sharedFolderId);
    }
    ~SubscribeIntegrationTest() {
        std::swap(old, yplatform::global_net_reactor);
    }
    void doSmth() {
        auto handler = makeSubscribeHandler(nullptr,
            [this](const std::string& uid, auto&, ConfigPtr){
                return uid == subscriberUid ? &macsSubscriber : &macsOwner;
            });
        handler(ctx, log::none);
        ios.run();
    }
};

TEST_F(SubscribeIntegrationTest, macsGetAllSharedFoldersThrows_responses500) {
    arguments.expect();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsGetByFidsThrows_responses500) {
    arguments.expect();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsGetAllFoldersThrows_responses500) {
    arguments.expect();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsGetFoldersByOwnerThrows_responses500) {
    arguments.expect();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsCreateFolderThrows_responses500) {
    arguments.expect();

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsAddFolderThrows_responses500) {
    arguments.expect();

    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", ownerUid, sharedFolderId, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsAddSubscriberThrows_responses500) {
    arguments.expect();

    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", ownerUid, sharedFolderId, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber(sharedFolderId, subscriberUid, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, macsImapUnsubscribeFolderThrows_responseOk) {
    arguments.imapUnsubscribed(true)
            .expect();

    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", ownerUid, sharedFolderId, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber(sharedFolderId, subscriberUid, _)).Times(1);
    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"a"}, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::SubscribeResult>())).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, positiveCase) {
    arguments.imapUnsubscribed(true)
            .expect();

    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", ownerUid, sharedFolderId, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber(sharedFolderId, subscriberUid, _)).Times(1);
    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"a"}, _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::SubscribeResult>())).Times(1);
    doSmth();
}

TEST_F(SubscribeIntegrationTest, recursive_positiveCase) {
    arguments.recursive(true)
            .expect();

    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(0);
    macs::Folder shared = macs::FolderFactory().fid(sharedFolderId).name("bbs").parentId(macs::Folder::noParent).messages(1);
    macs::FoldersMap fs;
    fs.insert({sharedFolderId, shared});

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{sharedFolderId}));
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsOwner, getByFids(subscriberUid, macs::FidVec{sharedFolderId}, _))
            .WillOnce(Return(std::vector<macs::Subscription>{}));
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner(ownerUid, _))
            .WillOnce(Return(std::vector<macs::SubscribedFolder>{}));
    EXPECT_CALL(macsSubscriber, createFolderByPath(macs::Folder::Path(PathValue{"a"}), _)).WillOnce(Return(f));
    EXPECT_CALL(macsSubscriber, addFolder("1", ownerUid, sharedFolderId, _)).Times(1);
    EXPECT_CALL(macsOwner, addSubscriber(sharedFolderId, subscriberUid, _)).Times(1);

    EXPECT_CALL(macsSubscriber, imapUnsubscribeFolder(PathValue{"Yandex"}, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::SubscribeResult>())).Times(1);
    doSmth();
}

} //namespace tests
} //namespace york
