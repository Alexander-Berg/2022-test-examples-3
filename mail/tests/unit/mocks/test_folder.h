#include <common/types.h>
#include <common/folder.h>

using namespace yimap;
using namespace yimap::backend;

struct TestFolder : Folder
{
    MessagesVector messages;

    TestFolder(FolderInfo info) : Folder(info, false)
    {
    }

    virtual void update(MessagesVector& messages)
    {
        throw std::runtime_error("not implemented");
    }

    virtual MailboxDiffPtr updateToRevision(bool onlyChanged)
    {
        return nullptr;
    }

    virtual void insertMessages(MessagesVector& toInsert)
    {
        messages.insert(messages.end(), toInsert.begin(), toInsert.end());
    }

    virtual void filterPartialUids(const UidVector& uids, UidVector& result)
    {
        throw std::runtime_error("not implemented");
    }

    virtual UidMapPtr filterByRanges(const seq_range& ranges, MessageData::Predicate pred) const
    {
        auto result = std::make_shared<UidMap>();
        for (auto&& msg : messages)
        {
            if (ranges.contains(msg) && pred(msg))
            {
                result->insert(msg);
            }
        }
        return result;
    }

    UidMap filterContinuouslyCachedByRanges(const seq_range& range, size_t limit) const
    {
        throw std::runtime_error("not implemented");
    }

    virtual std::tuple<size_t, seq_range> uncachedRanges(const seq_range& ranges) const
    {
        throw std::runtime_error("not implemented");
    }

    virtual string dump() const
    {
        throw std::runtime_error("not implemented");
    }
};