#pragma once

#include "fake_mailbox.h"

#include <src/backend/append/append.h>
#include <src/backend/backend.h>
#include <src/common/imap_context.h>
#include <src/common/settings.h>

#include <memory>

using namespace yimap;
using namespace yimap::backend;

class MockBackend
    : public MetaBackend
    , public std::enable_shared_from_this<MockBackend>
{
public:
    MockBackend(ImapContextPtr context, FakeMailbox mailbox);
    virtual ~MockBackend()
    {
    }

    Future<FolderListPtr> getFolders() override;
    FolderListPtr folderList();
    Future<FolderPtr> getFolder(const DBFolderId& folderId) override;
    Future<FolderInfo> getFolderInfo(const DBFolderId& folderId) override;

    // Loading messages
    Future<UidMapPtr> loadMessages(FolderRef mailbox, const seq_range& range, bool partial = false)
        override;
    Future<UidMapPtr> loadMessagesChunk(FolderRef mailbox, const seq_range& range) override;
    Future<UidMapPtr> loadMessagesDetails(FolderRef mailbox, const seq_range& range) override;

    Future<UidMapPtr> loadMessagesToDelete(FolderRef mailbox, const seq_range& range) override;
    Future<UidMapPtr> getMessagesByMessageId(FolderRef mailbox, const string& messageId) override;

    Future<BodyMetadataByMid> loadBodyMetadata(const SmidList& mids)
    {
        return makeFuture(bodyMetadata);
    }

    void setBodyMetadata(const BodyMetadataByMid& bodyMeta)
    {
        bodyMetadata = bodyMeta;
    }

    Future<MailboxDiffPtr> statusUpdate(FolderRef mailbox) override;

    void setStatusUpdateData(MailboxDiffPtr data)
    {
        statusUpdateFuture = makeFuture(data);
    }

    void setStatusUpdateFuture(Future<MailboxDiffPtr> future)
    {
        statusUpdateFuture = future;
    }

    // Working with message flags
    Future<MailboxDiffPtr> updateFlags(
        FolderRef mailbox,
        UidMapPtr uids,
        const Flags& del_flags,
        const Flags& add_flags) override;

    Future<void> expunge(FolderRef mailbox, UidMapPtr uids) override;

    Future<UidSequence> copyMessages(UidMap& uids, const FolderInfo& from, const FolderInfo& to)
        override;
    Future<UidSequence> moveMessages(UidMap& uids, const FolderInfo& from, const FolderInfo& to)
        override;

    Future<void> createFolder(const string& folderName) override;
    Future<void> deleteFolder(const DBFolderId& folderId) override;
    Future<void> renameFolder(const DBFolderId& folderId, const string& dstName) override;
    Future<void> subscribe(const DBFolderId& folderId, bool on) override;

    Future<void> dropFreshCounter() override;
    void journalAuth() override;

    auto& getMailbox()
    {
        return mailbox;
    }

    std::vector<uint64_t> getMidsByFid(const string& fid)
    {
        std::vector<uint64_t> res;
        auto messages = mailbox.messages.at(fid);
        for (auto&& [id, data] : messages)
        {
            res.push_back(data.mid);
        }
        return res;
    }

    Future<string> regenerateImapId(const std::string& /*mid*/) override
    {
        return makeFuture(std::to_string(nextRegeneratedImapId++));
    };

    uint64_t getNextRegeneratedImapId()
    {
        return nextRegeneratedImapId;
    }

    auto& expungedMids()
    {
        return expungeRequest;
    }

    auto& getCopyRequests()
    {
        return copyRequests;
    }

    auto& getMoveRequests()
    {
        return moveRequests;
    }

    bool allRequestsEmpty()
    {
        return expungeRequest.empty() && copyRequests.empty() && moveRequests.empty();
    }

    FolderInfo convertFolderInfo(FullFolderInfoPtr folder);

    bool faulty = false;
    ImapContextPtr context;
    FakeMailbox mailbox;
    uint64_t nextRegeneratedImapId = 5555555;
    std::vector<uint64_t> expungeRequest;
    std::vector<std::tuple<UidMap, FolderInfo, FolderInfo>> copyRequests;
    std::vector<std::tuple<UidMap, FolderInfo, FolderInfo>> moveRequests;
    std::optional<Future<MailboxDiffPtr>> statusUpdateFuture;
    BodyMetadataByMid bodyMetadata;
    Revision revision = 1;

    std::map<DBFolderId, FolderPtr> folderPtrs;
};

struct MockCallError : public runtime_error
{
    explicit MockCallError(const std::string& call)
        : runtime_error(std::string("You don't must call this function MockBackend::") + call)
    {
    }
};
