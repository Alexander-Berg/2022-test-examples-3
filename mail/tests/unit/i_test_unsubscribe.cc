#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/unsubscribe.h>
#include <macs_pg/subscription/factory.h>
#include <macs_pg/subscribed_folders/factory.h>
#include <yplatform/reactor/io_pool.h>

using namespace testing;

namespace york {
namespace tests {

using operations::FidVec;
using operations::SubsFolderVec;
using operations::SubscriptionVec;

struct UnsubscribeIntegrationTest: public Test {
    std::shared_ptr<ContextMock> ctx = std::make_shared<ContextMock>();
    MacsMock<coro> macsMock;
    boost::asio::io_service ios;
    boost::shared_ptr<yplatform::reactor> old;

    const std::string subscriberUid {"suid"};
    const std::string ownerUid {"ouid"};
    const std::string sharedFolderFid {"fid"};
    const std::string requestId {"requestId"};

    macs::FoldersMap fs;
    FidVec sharFids;
    SubsFolderVec subFs;
    SubscriptionVec subsSync;
    SubscriptionVec subsTerm;
    macs::UnsubscribeTask task;

    UnsubscribeIntegrationTest() {
        auto iop = std::make_shared<yplatform::io_pool>(ios, 1);
        old = boost::make_shared<yplatform::reactor>(iop);
        std::swap(old, yplatform::global_net_reactor);

        EXPECT_CALL(*ctx, getOptionalArg("subscriber_uid")).WillOnce(Return(boost::optional<std::string>(subscriberUid)));
        EXPECT_CALL(*ctx, getOptionalArg("owner_uid")).WillOnce(Return(boost::optional<std::string>(ownerUid)));
        EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>(sharedFolderFid)));

        fs.insert({"1", macs::FolderFactory().fid("1").name("bbs").parentId("").messages(0)});

        sharFids.push_back(sharedFolderFid);
        subFs.push_back(macs::SubscribedFolderFactory()
                        .fid("1")
                        .ownerUid(ownerUid)
                        .ownerFid(sharedFolderFid)
                        .release());

        using SubState = macs::pg::SubscriptionState;
        subsSync.push_back(macs::SubscriptionFactory()
                           .subscriptionId(1)
                           .state(SubState::sync)
                           .fid(sharedFolderFid)
                           .release());
        subsTerm.push_back(macs::SubscriptionFactory()
                           .subscriptionId(1)
                           .state(SubState::terminated)
                           .fid(sharedFolderFid)
                           .release());

        task = macs::UnsubscribeTaskFactory()
                .taskRequestId(requestId)
                .ownerUid(ownerUid)
                .ownerFids({sharedFolderFid})
                .subscriberUid(subscriberUid)
                .rootSubscriberFid("1")
                .release();
    }
    ~UnsubscribeIntegrationTest() {
        std::swap(old, yplatform::global_net_reactor);
    }

    void doUnsubscribe() {
        auto handler = makeUnsubscribeHandler(nullptr,
            [this](const std::string&, auto&, auto){ return &macsMock; });
        handler(ctx, log::none);
        ios.run();
    }
};

TEST_F(UnsubscribeIntegrationTest, macsGetAllFoldersThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, macsGetFoldersByOwnerThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, macsGetByFidsThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(subFs));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, macsTransitStateThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(subFs));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _)).WillOnce(Return(subsSync));
    EXPECT_CALL(macsMock, transitState(1, _, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, macsAddUnsubscribeTaskThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(subFs));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _)).WillOnce(Return(subsSync));
    EXPECT_CALL(macsMock, transitState(1, _, _)).Times(1);
    EXPECT_CALL(*ctx, requestId()).WillOnce(Return(requestId));
    EXPECT_CALL(macsMock, addUnsubscribeTask(task, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, positiveCase) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(subFs));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _)).WillOnce(Return(subsSync));
    EXPECT_CALL(macsMock, transitState(1, _, _)).Times(1);
    EXPECT_CALL(*ctx, requestId()).WillOnce(Return(requestId));
    EXPECT_CALL(macsMock, addUnsubscribeTask(task, _)).WillOnce(Return(task));
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::UnsubscribeResult>())).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, onSingleSubscription_macsGetByFidsThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(SubsFolderVec{}));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, onSingleSubscription_macsRemoveChunkThrows_response500) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(SubsFolderVec{}));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _)).WillOnce(Return(subsTerm));
    EXPECT_CALL(macsMock, removeChunk(std::vector<macs::SubscriptionId>{1}, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);

    doUnsubscribe();
}

TEST_F(UnsubscribeIntegrationTest, onSingleSubscription_positiveCase) {
    InSequence seq;
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(fs));
    EXPECT_CALL(macsMock, getFoldersByOwner(ownerUid, _)).WillOnce(Return(SubsFolderVec{}));
    EXPECT_CALL(macsMock, getByFids(subscriberUid, sharFids, _)).WillOnce(Return(subsTerm));
    EXPECT_CALL(macsMock, removeChunk(std::vector<macs::SubscriptionId>{1}, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::UnsubscribeResult>())).Times(1);

    doUnsubscribe();
}

} //namespace tests
} //namespace york
