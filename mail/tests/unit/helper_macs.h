#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/folder_set.h>
#include <macs/io.h>
#include <macs_pg/subscription/subscription.h>
#include <macs_pg/subscribed_folders/subscribed_folder.h>
#include <macs_pg/subscription/subscription_action.h>
#include <macs_pg/subscription/unsubscribe_task.h>

namespace york {
namespace tests {

using sync = macs::io::sync_context;
using coro = decltype(macs::io::make_yield_context(
        std::declval<boost::asio::yield_context>()
));

template<typename SyncOrCoro>
struct MacsMock {
    const MacsMock& folders() const { return *this; }
    const MacsMock& sharedFolders() const { return *this; }
    const MacsMock& subscribedFolders() const { return *this; }
    const MacsMock& subscriptions() const { return *this; }
    const MacsMock& imapRepo() const { return *this; }

    MOCK_CONST_METHOD1_T(getAllFolders, macs::FolderSet(SyncOrCoro));
    MOCK_CONST_METHOD2_T(createFolder, macs::Folder(std::string, SyncOrCoro));
    MOCK_CONST_METHOD5_T(createSharedFolderWithArchivation,
                         void(macs::Fid, macs::Folder::ArchivationType, uint32_t, uint32_t, SyncOrCoro));
    MOCK_CONST_METHOD4_T(addFolder, void(std::string, std::string, std::string, SyncOrCoro));
    MOCK_CONST_METHOD3_T(addSubscriber, void(std::string, std::string, SyncOrCoro));
    MOCK_CONST_METHOD1_T(getAllSharedFolders, std::vector<macs::Fid>(SyncOrCoro));
    MOCK_CONST_METHOD2_T(createFolderByPath, macs::Folder(macs::Folder::Path, SyncOrCoro));
    MOCK_CONST_METHOD2_T(getFoldersByOwner, std::vector<macs::SubscribedFolder>(std::string, SyncOrCoro));
    MOCK_CONST_METHOD3_T(getByFids, std::vector<macs::Subscription>(std::string, std::vector<macs::Fid>, SyncOrCoro));
    MOCK_CONST_METHOD3_T(transitState, macs::Subscription(macs::SubscriptionId, macs::pg::SubscriptionAction, SyncOrCoro));
    MOCK_CONST_METHOD3_T(removeFolders, std::vector<macs::Fid>(std::string, std::vector<macs::Fid>, SyncOrCoro));
    MOCK_CONST_METHOD2_T(eraseCascade, macs::Revision(macs::Folder, SyncOrCoro));
    MOCK_CONST_METHOD2_T(removeChunk, std::vector<macs::Subscription>(std::vector<macs::SubscriptionId>, SyncOrCoro));
    MOCK_CONST_METHOD2_T(updateFolder, macs::Folder(macs::Folder, SyncOrCoro));
    MOCK_CONST_METHOD2_T(imapUnsubscribeFolder, macs::Revision(std::vector<std::string>, SyncOrCoro));
    MOCK_METHOD(void, resetFoldersCache, (), (const));

    MOCK_CONST_METHOD2_T(addUnsubscribeTask, macs::UnsubscribeTask(macs::UnsubscribeTask, SyncOrCoro));
    MOCK_CONST_METHOD2_T(removeSubscriptionsAndTask, std::vector<macs::Subscription>(macs::UnsubscribeTask, SyncOrCoro));

    MOCK_CONST_METHOD5_T(setArchivationRule, void(macs::Fid, macs::Folder::ArchivationType, uint32_t, uint32_t, SyncOrCoro));
    MOCK_CONST_METHOD2_T(removeArchivationRule, void(macs::Fid, SyncOrCoro));
};

template<typename SyncOrCoro>
struct ShardMock {
    const ShardMock& subscriptions() const { return *this; }

    MOCK_CONST_METHOD3_T(getUnsubscribeTasks, std::vector<macs::UnsubscribeTask>(
                             std::size_t, std::chrono::seconds, SyncOrCoro));
};

using PathValue = std::vector<macs::Folder::Name>;

} //namespace tests
} //namespace york

namespace macs {
inline bool operator==(const Subscription& lhs, const Subscription& rhs) {
    return lhs.subscriptionId() == rhs.subscriptionId() &&
           lhs.state() == rhs.state() &&
           lhs.fid() == rhs.fid();
}
inline bool operator==(const Folder& lhs, const Folder& rhs) {
    return lhs.fid() == rhs.fid() &&
           lhs.name() == rhs.name() &&
           lhs.parentId() == rhs.parentId();
}
inline bool operator==(const SubscribedFolder& lhs, const SubscribedFolder& rhs) {
    return lhs.fid() == rhs.fid() &&
           lhs.ownerFid() == lhs.ownerFid() &&
           lhs.ownerUid() == rhs.ownerUid();
}
inline bool operator==(const UnsubscribeTask& lhs, const UnsubscribeTask& rhs) {
    return lhs.taskRequestId() == rhs.taskRequestId() &&
           lhs.ownerUid() == rhs.ownerUid() &&
           lhs.subscriberUid() == rhs.subscriberUid() &&
           lhs.rootSubscriberFid() == rhs.rootSubscriberFid() &&
           lhs.ownerFids() == rhs.ownerFids();
}
}
