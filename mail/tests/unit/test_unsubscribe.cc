#include "wrap_mock.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/unsubscribe.h>
#include <macs/folders_repository.h>
#include <macs/io.h>
#include <macs_pg/subscribed_folders/factory.h>
#include <macs_pg/subscription/factory.h>

using namespace testing;

namespace york {
namespace tests {

using server::handlers::UnsubscribeParams;
using server::handlers::operations::FidVec;
using server::handlers::operations::FolderVec;
using server::handlers::operations::SubsFolderVec;
using server::handlers::operations::SubscriptionVec;
using server::handlers::operations::makeUnsubscribeOperationWithImpl;
using SubState = macs::pg::SubscriptionState;

template <typename SyncOrCoro>
struct UnsubscribeOperationMock {
    const MacsMock<SyncOrCoro>& ownerMailbox;
    const MacsMock<SyncOrCoro>& subscriberMailbox;
    ContextMock& ctx;
    UnsubscribeParams params;

    UnsubscribeOperationMock(const MacsMock<SyncOrCoro>& ownerMailbox_,
                             const MacsMock<SyncOrCoro>& subscriberMailbox_,
                             ContextMock& ctx_,
                             const UnsubscribeParams& params_)
            : ownerMailbox(ownerMailbox_)
            , subscriberMailbox(subscriberMailbox_)
            , ctx(ctx_)
            , params(params_) {}

    MOCK_CONST_METHOD2_T(getSubfolders, SubsFolderVec(const macs::Fid&,
                                                      SyncOrCoro));
    MOCK_CONST_METHOD2_T(terminate, void(const SubsFolderVec&,
                                         SyncOrCoro));
    MOCK_CONST_METHOD2_T(removeSubscription, void(const macs::Fid&,
                                             SyncOrCoro));
    MOCK_CONST_METHOD2_T(createUnsubscribeTask, void(const SubsFolderVec&,
                                                     SyncOrCoro));
};

struct UnsubscribeOperationTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macsOwner;
    MacsMock<sync> macsSubscriber;

    auto makeUnsubscribeMockPtr(const UnsubscribeParams& params) {
        using Impl = UnsubscribeOperationMock<sync>;
        return std::make_shared<Impl>(
            macsOwner, macsSubscriber, ctx, params
        );
    }

    macs::Subscription subscription(macs::SubscriptionId id, SubState state) const {
        return macs::SubscriptionFactory().subscriptionId(id).state(state).release();
    }
};

TEST_F(UnsubscribeOperationTest, execute_withNoSubscribedFoldersFound_removesSingleSubscription_responsesOk) {
    auto unsubscribe = makeUnsubscribeMockPtr(UnsubscribeParams{"suid", "ouid", boost::optional<std::string>("fid")});

    InSequence seq;
    EXPECT_CALL(*unsubscribe, getSubfolders("fid", _)).WillOnce(Return(SubsFolderVec{}));
    EXPECT_CALL(*unsubscribe, removeSubscription("fid", _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::UnsubscribeResult>())).Times(1);

    auto operation = makeUnsubscribeOperationWithImpl(unsubscribe);
    operation.execute(macs::io::use_sync);
}

TEST_F(UnsubscribeOperationTest, execute_withEmptySharedFolderFid_usesInbox) {
    macs::FoldersMap sharFs;
    sharFs.insert({"inbox", macs::FolderFactory().fid("inbox").name("Inbox").parentId("").messages(0).symbol(macs::Folder::Symbol::inbox)});

    auto unsubscribe = makeUnsubscribeMockPtr(UnsubscribeParams{"suid", "ouid", boost::none});

    InSequence seq;
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(*unsubscribe, getSubfolders("inbox", _)).WillOnce(Return(SubsFolderVec{}));
    EXPECT_CALL(*unsubscribe, removeSubscription("inbox", _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::UnsubscribeResult>())).Times(1);

    auto operation = makeUnsubscribeOperationWithImpl(unsubscribe);
    operation.execute(macs::io::use_sync);
}

TEST_F(UnsubscribeOperationTest, execute_discontinuesSubscriptions_responsesOk) {
    macs::Fid sharFid = "fid";
    SubsFolderVec subFs {
        macs::SubscribedFolderFactory().fid("1").ownerUid("ouid").ownerFid(sharFid).release()
    };

    auto unsubscribe = makeUnsubscribeMockPtr(UnsubscribeParams{"suid", "ouid", boost::optional<std::string>(sharFid)});

    InSequence seq;
    EXPECT_CALL(*unsubscribe, getSubfolders(sharFid, _)).WillOnce(Return(subFs));
    EXPECT_CALL(*unsubscribe, terminate(subFs, _)).Times(1);
    EXPECT_CALL(*unsubscribe, createUnsubscribeTask(subFs, _)).Times(1);
    EXPECT_CALL(ctx.resp, ok(An<server::handlers::UnsubscribeResult>())).Times(1);

    auto operation = makeUnsubscribeOperationWithImpl(unsubscribe);
    operation.execute(macs::io::use_sync);
}

} // namespace tests
} // namespace york

