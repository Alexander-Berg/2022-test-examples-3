#include "mock_backend.h"
#include "test_folder.h"
#include "../src/backend/meta_common/helpers.h"

using namespace yimap;
using namespace yimap::backend;

MockBackend::MockBackend(ImapContextPtr context, FakeMailbox mailbox)
    : context(context), mailbox(mailbox)
{
}

Future<FolderListPtr> MockBackend::getFolders()
{
    return makeFuture(folderList());
}

FolderListPtr MockBackend::folderList()
{
    return std::make_shared<FolderList>(
        std::shared_ptr<RawFolderList>(shared_from(this), &mailbox.folders),
        LanguageConfigPtr{ new LanguageConfig() });
}

Future<FolderInfo> MockBackend::getFolderInfo(const DBFolderId& folderId)
{
    for (auto&& f : mailbox.folders)
    {
        if (f->fid == folderId.fid)
        {
            return makeFuture(convertFolderInfo(f));
        }
    }
    throw NoSuchFolderError(folderId.name, "fid:" + folderId.fid);
}

Future<FolderPtr> MockBackend::getFolder(const DBFolderId& folderId)
{
    if (folderPtrs.count(folderId))
    {
        return makeFuture(folderPtrs[folderId]);
    }

    UidMapData uidMapData;

    auto folderInfo = getFolderInfo(folderId).get();

    MessageVector messages;
    auto iter = mailbox.messages.find(folderInfo.fid);
    if (iter != mailbox.messages.end())
    {
        for (auto&& [_, msg] : iter->second)
        {
            messages.push_back(msg);
        }
    }

    auto folder = std::make_shared<TestFolder>(folderInfo);
    messages.sortByUid();
    folder->insertMessages(messages);

    auto res = FolderPtr(folder);
    folderPtrs[folderId] = res;
    return makeFuture(res);
}

Future<MailboxDiffPtr> MockBackend::statusUpdate(FolderRef folder)
{
    if (statusUpdateFuture)
    {
        auto res = *statusUpdateFuture;
        statusUpdateFuture.reset();
        return res;
    }
    else
    {
        return makeFuture(std::make_shared<MailboxDiff>(folder.info()));
    }
}

Future<UidMapPtr> MockBackend::loadMessages(FolderRef folder, const seq_range& range, bool)
{
    if (range.empty())
    {
        return makeFuture(folder.filterByRanges(range));
    }

    auto folderMap = mailbox.messages.find(folder.fid());

    if (folderMap == mailbox.messages.end())
    {
        return makeFuture(folder.filterByRanges(range));
    }

    const auto& fMap = folderMap->second;
    MessageVector result;

    for (auto&& r : range)
    {
        uint32_t begin = r.first;
        uint32_t end = r.second;
        for (auto i = begin; i <= end; ++i)
        {
            auto findByNum = [](auto&& map, auto&& num) {
                if (num >= map.size()) return map.end();
                return std::next(map.begin(), num - 1);
            };
            auto it = range.uidMode() ? fMap.find(i) : findByNum(fMap, i);
            if (it != fMap.end())
            {
                result.push_back(it->second);
            }
        }
    }

    folder.insertMessages(result);
    return makeFuture(folder.filterByRanges(range));
}

Future<UidMapPtr> MockBackend::loadMessagesChunk(FolderRef mailbox, const seq_range& range)
{
    return loadMessages(mailbox, range, true).then([](auto&& future) {
        auto data = future.get();
        if (data->empty())
        {
            return data;
        }

        auto message = *data->begin();
        auto res = std::make_shared<UidMap>();
        res->insert(message);
        return res;
    });
}

Future<UidMapPtr> MockBackend::loadMessagesDetails(FolderRef mailbox, const seq_range& range)
{
    return loadMessages(mailbox, range, false);
}

Future<UidMapPtr> MockBackend::loadMessagesToDelete(FolderRef folder, const seq_range& range)
{
    UidMapPtr ret = std::make_shared<UidMap>();
    for (auto& [imapId, message] : mailbox.messages[folder.fid()])
    {
        if (message.deleted && (range.empty() || range.contains(message)))
        {
            ret->insert(message);
        }
    }
    return makeFuture(ret);
}

Future<UidMapPtr> MockBackend::getMessagesByMessageId(FolderRef, const string&)
{
    throw MockCallError(__func__);
}

Future<MailboxDiffPtr> MockBackend::updateFlags(
    FolderRef folder,
    UidMapPtr messages,
    const Flags& flagsToDel,
    const Flags& flagsToAdd)
{
    auto res = MailboxDiffPtr(new MailboxDiff(folder.info()));

    auto it = mailbox.messages.find(folder.fid());
    if (it == mailbox.messages.end())
    {
        throw std::runtime_error("no messages in folder");
    }

    auto& folderMessages = it->second;
    for (auto& msg : *messages)
    {
        auto msgIt = folderMessages.find(msg.uid);
        if (msgIt == folderMessages.end())
        {
            throw std::runtime_error("message not found: " + std::to_string(msg.uid));
        }

        msgIt->second.flags.delFlags(flagsToDel);
        msgIt->second.flags.addFlags(flagsToAdd);
        res->changed.insert(msgIt->second);
    }

    return makeFuture(res);
}

Future<void> MockBackend::expunge(FolderRef, UidMapPtr messages)
{
    auto mids = *messages->toMidList();
    expungeRequest.insert(expungeRequest.begin(), mids.begin(), mids.end());
    return makeFuture();
}

////////////////////////////////////////////////////////////////////////////////

Future<UidSequence> MockBackend::copyMessages(
    UidMap& map,
    const FolderInfo& src,
    const FolderInfo& dst)
{
    copyRequests.emplace_back(map, src, dst);
    UidSequence res;
    MessagesVector messages;
    auto uid = dst.uidNext;
    auto num = dst.messageCount + 1;
    while (map.size())
    {
        res.push(uid);
        auto msg = map.pop();
        msg.modseq = revision;
        msg.uid = uid++;
        msg.num = num++;
        messages.push_back(msg);
    }
    revision++;
    setStatusUpdateData(
        std::make_shared<MailboxDiff>(dst, MessagesVector(), MessagesVector(), messages));
    return makeFuture(res);
}

Future<UidSequence> MockBackend::moveMessages(
    UidMap& map,
    const FolderInfo& src,
    const FolderInfo& dst)
{
    moveRequests.emplace_back(map, src, dst);
    UidSequence res;
    MessagesVector messages;
    auto uid = dst.uidNext;
    while (map.size())
    {
        res.push(uid++);
        auto msg = map.pop();
        msg.modseq = revision;
        messages.push_back(msg);
    }
    revision++;
    setStatusUpdateData(
        std::make_shared<MailboxDiff>(src, messages, MessagesVector(), MessagesVector()));
    return makeFuture(res);
}

////////////////////////////////////////////////////////////////////////////////

Future<void> MockBackend::createFolder(const string& name)
{
    Promise<void> promise;

    if (folderList()->hasFolder(name))
    {
        promise.set_exception(std::runtime_error("folder already exists"));
        return promise;
    }

    FullFolderInfoPtr new_folder = std::make_shared<FullFolderInfo>();
    new_folder->name = name;
    new_folder->fid = std::to_string(mailbox.folders.size() + 1);
    new_folder->symbol = "";
    new_folder->messageCount = 0;
    new_folder->uidNext = 1;
    new_folder->uidValidity = std::numeric_limits<uint32_t>::max();
    mailbox.folders.push_back(new_folder);

    promise.set();
    return promise;
}

Future<void> MockBackend::renameFolder(const DBFolderId& folderId, const string& name)
{
    Promise<void> promise;
    for (auto&& f : mailbox.folders)
    {
        if (f->fid == folderId.fid)
        {
            f->name = name;
            promise.set();
        }
    }

    promise.set_exception(std::runtime_error("no such folder to rename"));
    return promise;
}

Future<void> MockBackend::deleteFolder(const DBFolderId& folder)
{
    auto it =
        std::remove_if(mailbox.folders.begin(), mailbox.folders.end(), [folder](auto&& folderId) {
            return folderId->name == folder.name;
        });
    bool deleted = it != mailbox.folders.end();
    mailbox.folders.erase(it, mailbox.folders.end());

    Promise<void> promise;
    if (deleted)
    {
        promise.set();
    }
    else
    {
        promise.set_exception(std::runtime_error("no such folder to delete"));
    }
    return promise;
}

Future<void> MockBackend::subscribe(const DBFolderId& id, bool targetstateSubscribed)
{
    if (faulty) throw std::runtime_error("MockBackend fault");
    auto folder = folderList()->at(id.name);
    folder->subscribed = targetstateSubscribed;
    return makeFuture();
}

Future<void> MockBackend::dropFreshCounter()
{
    return makeFuture();
}

////////////////////////////////////////////////////////////////////////////////

void MockBackend::journalAuth()
{
    throw MockCallError(__func__);
}

FolderInfo MockBackend::convertFolderInfo(FullFolderInfoPtr folder)
{
    FolderInfo info;

    info.name = folder->name;
    info.fid = folder->fid;
    info.revision = static_cast<uint32_t>(folder->revision);
    info.messageCount = static_cast<uint32_t>(folder->messageCount);
    info.recentCount = static_cast<uint32_t>(folder->recentCount);
    info.unseenCount = static_cast<uint32_t>(folder->unseenCount);
    info.uidNext = static_cast<uint32_t>(folder->uidNext > 0 ? folder->uidNext : 0);
    info.uidValidity = static_cast<uint32_t>(folder->uidValidity);
    info.firstUnseen = static_cast<uint32_t>(folder->firstUnseen);

    return info;
}
