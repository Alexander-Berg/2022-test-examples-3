#include "wrap_mock.h"

#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/unsubscribe.h>
#include <macs/folders_repository.h>
#include <macs/io.h>
#include <macs_pg/subscription/factory.h>
#include <macs_pg/subscribed_folders/factory.h>

using namespace testing;

namespace york {
namespace tests {

struct UnsubscribeFunctionsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macsOwner;
    MacsMock<sync> macsSubscriber;
    LoggerMock logger;

    auto makeUnsubscribeOperation(server::handlers::UnsubscribeParams params) {
        auto typedLogger = log::make_log(log::none, &logger);
        return server::handlers::operations::UnsubscribeOperationImpl<
                MacsMock<sync>, ContextMock, decltype(typedLogger)>(
                    macsOwner, macsSubscriber, ctx, params, std::move(typedLogger));
    }

    using State = macs::pg::SubscriptionState;
    macs::Subscription subscription(State state) {
        return macs::SubscriptionFactory().state(state).release();
    }
    macs::Subscription subscription(macs::SubscriptionId id, State state, macs::Fid fid) {
        return macs::SubscriptionFactory().subscriptionId(id).state(state).fid(fid).release();
    }
};

using server::handlers::operations::FidVec;
using server::handlers::operations::FolderVec;
using server::handlers::operations::SubsFolderVec;
using server::handlers::operations::SubscriptionVec;
using server::handlers::UnsubscribeParams;

TEST_F(UnsubscribeFunctionsTest, getSubfolders_returnsEmptySubfolders_ifRootSubscribedNotFound) {
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("owner_uid", _)).WillOnce(Return(SubsFolderVec{}));

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    auto result = operation.getSubfolders("fid", macs::io::use_sync);

    ASSERT_THAT(result, IsEmpty());
}

TEST_F(UnsubscribeFunctionsTest, getSubfolders_returnsSubfolders) {
    macs::FoldersMap fs = {
        {"1", macs::FolderFactory().fid("1").name("bbs").parentId("")},
        {"2", macs::FolderFactory().fid("2").name("bbs|folder1").parentId("1")}};
    macs::FolderSet userFolders(fs);

    SubsFolderVec subFs {
        macs::SubscribedFolderFactory().fid("1").ownerUid("owner_uid").ownerFid("fid").release(),
        macs::SubscribedFolderFactory().fid("2").ownerUid("owner_uid").ownerFid("fid1").release()
    };

    InSequence seq;
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(userFolders));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("owner_uid", _)).WillOnce(Return(subFs));

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    auto result = operation.getSubfolders("fid", macs::io::use_sync);

    ASSERT_THAT(result, ElementsAreArray(subFs));
}

TEST_F(UnsubscribeFunctionsTest, getSubfolders_returnsOnlySubfolders) {
    macs::FoldersMap fs = {
        {"1", macs::FolderFactory().fid("1").name("bbs").parentId("")},
        {"2", macs::FolderFactory().fid("2").name("other").parentId("")}};
    macs::FolderSet userFolders(fs);

    SubsFolderVec subFs {
        macs::SubscribedFolderFactory().fid("1").ownerUid("owner_uid").ownerFid("fid").release(),
        macs::SubscribedFolderFactory().fid("2").ownerUid("owner_uid").ownerFid("fid1").release()
    };

    InSequence seq;
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(userFolders));
    EXPECT_CALL(macsSubscriber, getFoldersByOwner("owner_uid", _)).WillOnce(Return(subFs));

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    auto result = operation.getSubfolders("fid", macs::io::use_sync);

    ASSERT_THAT(result, ElementsAre(subFs[0]));
}

TEST_F(UnsubscribeFunctionsTest, operable_returnsFalseOnNotOperableSubscriptionStates) {
    SubscriptionVec subs {
        subscription(State::terminated),
        subscription(State::discontinued),
        subscription(State::clear),
        subscription(State::clearFail)
    };

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});

    for (const auto& s: subs) {
        EXPECT_FALSE(operation.operable(s));
    }
}

TEST_F(UnsubscribeFunctionsTest, operable_returnsTrueOnOperableSubscriptionStates) {
    SubscriptionVec subs {
        subscription(State::new_),
        subscription(State::init),
        subscription(State::sync),
        subscription(State::migrate),
        subscription(State::migrateFinished),
        subscription(State::initFail),
        subscription(State::syncFail),
        subscription(State::migrateFail)
    };

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});

    for (const auto& s: subs) {
        EXPECT_TRUE(operation.operable(s));
    }
}

TEST_F(UnsubscribeFunctionsTest, terminate_withTerminatedSubscription_doesNothing) {
    macs::SubscribedFolder folder = macs::SubscribedFolderFactory()
            .fid("1")
            .ownerUid("owner_uid")
            .ownerFid("fid")
            .release();
    macs::Subscription sub = subscription(1, State::terminated, "fid");

    EXPECT_CALL(macsOwner, getByFids("uid", FidVec{"fid"}, _)).WillOnce(Return(SubscriptionVec{sub}));

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.terminate(SubsFolderVec{folder}, macs::io::use_sync);
}

TEST_F(UnsubscribeFunctionsTest, terminate_terminatesSubscription) {
    macs::SubscribedFolder folder = macs::SubscribedFolderFactory()
            .fid("1")
            .ownerUid("owner_uid")
            .ownerFid("fid")
            .release();
    macs::Subscription sub = subscription(1, State::sync, "fid");

    InSequence seq;
    EXPECT_CALL(macsOwner, getByFids("uid", FidVec{"fid"}, _)).WillOnce(Return(SubscriptionVec{sub}));
    EXPECT_CALL(macsOwner, transitState(1, _, _)).Times(1);

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.terminate(SubsFolderVec{folder}, macs::io::use_sync);
}

TEST_F(UnsubscribeFunctionsTest, terminate_terminatesOnlyNotTerminatedSubscriptions) {
    SubsFolderVec folders {
        macs::SubscribedFolderFactory().fid("1").ownerUid("owner_uid").ownerFid("fid1").release(),
        macs::SubscribedFolderFactory().fid("2").ownerUid("owner_uid").ownerFid("fid2").release(),
    };
    macs::Subscription subTerm = subscription(1, State::terminated, "fid1");
    macs::Subscription subSync = subscription(2, State::sync, "fid2");

    InSequence seq;
    EXPECT_CALL(macsOwner, getByFids("uid", FidVec{"fid1", "fid2"}, _)).WillOnce(Return(SubscriptionVec{subTerm, subSync}));
    EXPECT_CALL(macsOwner, transitState(2, _, _)).Times(1);

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.terminate(folders, macs::io::use_sync);
}

TEST_F(UnsubscribeFunctionsTest, removeSubscription_getsSubscription_andRemovesIt) {
    macs::Subscription sub = macs::SubscriptionFactory()
            .subscriptionId(1)
            .state(macs::pg::SubscriptionState::terminated)
            .release();

    InSequence seq;
    EXPECT_CALL(macsOwner, getByFids("uid", macs::FidVec{"fid"}, _)).WillOnce(Return(SubscriptionVec{sub}));
    EXPECT_CALL(macsOwner, removeChunk(std::vector<macs::SubscriptionId>{1}, _)).Times(1);

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.removeSubscription("fid", macs::io::use_sync);
}

TEST_F(UnsubscribeFunctionsTest, removeSubscription_ifNoSubscriptions_doesNothing) {
    macs::Subscription sub = macs::SubscriptionFactory()
            .subscriptionId(1)
            .state(macs::pg::SubscriptionState::terminated)
            .release();

    InSequence seq;
    EXPECT_CALL(macsOwner, getByFids("uid", macs::FidVec{"fid"}, _)).WillOnce(Return(SubscriptionVec{}));

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.removeSubscription("fid", macs::io::use_sync);
}

TEST_F(UnsubscribeFunctionsTest, createUnsubscribeTask_addsUnsubscribeTask_logsInfo) {
    SubsFolderVec subFs {
        macs::SubscribedFolderFactory().fid("1").ownerUid("owner_uid").ownerFid("fid").release(),
        macs::SubscribedFolderFactory().fid("2").ownerUid("owner_uid").ownerFid("fid1").release()
    };
    macs::UnsubscribeTask task = macs::UnsubscribeTaskFactory()
            .taskRequestId("reqid")
            .ownerUid("owner_uid")
            .subscriberUid("uid")
            .ownerFids({"fid", "fid1"})
            .rootSubscriberFid("1")
            .release();

    InSequence seq;
    EXPECT_CALL(ctx, requestId()).WillOnce(Return("reqid"));
    EXPECT_CALL(macsOwner, addUnsubscribeTask(task, _)).WillOnce(Return(task));
    EXPECT_CALL(logger, notice(_)).Times(1);

    auto operation = makeUnsubscribeOperation(UnsubscribeParams{"uid", "owner_uid", boost::optional<std::string>("fid")});
    operation.createUnsubscribeTask(subFs, macs::io::use_sync);
}

} // namespace tests
} // namespace york
