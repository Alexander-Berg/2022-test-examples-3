#pragma once

#include <collector_ng/http/ms365_client.h>
#include <optional>

namespace yrpopper::mock {

using MS365FolderList = yrpopper::collector::MS365FolderList;
using MS365Message = yrpopper::collector::MS365Message;
using MS365MessageList = yrpopper::collector::MS365MessageList;

class MS365Client
{
public:
    MS365Client(
        rpop_context_ptr /*ctx*/,
        const std::string& /*accessToken*/,
        const std::string& /*baseUrl*/,
        std::uint32_t /*fetchChunkSize*/)
    {
    }

    MS365FolderList fetchFolders()
    {
        if (fetchFoldersException)
        {
            std::rethrow_exception(fetchFoldersException);
        }
        return folders;
    }

    MS365MessageList fetchMessages(
        const std::string& /*fid*/,
        std::time_t /*prevModified*/,
        std::time_t /*prevReceived*/,
        std::uint64_t /*count*/)
    {
        if (fetchMessagesException)
        {
            std::rethrow_exception(fetchMessagesException);
        }
        return messages;
    }

    std::string downloadMessage(const std::string& /*mid*/)
    {
        if (downloadMessageException)
        {
            std::rethrow_exception(downloadMessageException);
        }
        return messageContent;
    }

    void setFetchFoldersResult(const MS365FolderList& folders)
    {
        if (fetchFoldersException) fetchFoldersException = nullptr;
        this->folders = folders;
    }

    void setFetchFoldersResult(std::exception_ptr e)
    {
        this->fetchFoldersException = e;
    }

    void setFetchMessagesResult(const MS365MessageList& messages)
    {
        if (fetchMessagesException) fetchMessagesException = nullptr;
        this->messages = messages;
    }

    void setFetchMessagesResult(std::exception_ptr e)
    {
        this->fetchMessagesException = e;
    }

    void setDownloadMessageResult(const std::string& messageContent)
    {
        if (downloadMessageException) downloadMessageException = nullptr;
        this->messageContent = messageContent;
    }

    void setDownloadMessageResult(std::exception_ptr e)
    {
        this->downloadMessageException = e;
    }

private:
    MS365FolderList folders;
    MS365MessageList messages;
    std::string messageContent;
    std::exception_ptr fetchFoldersException;
    std::exception_ptr fetchMessagesException;
    std::exception_ptr downloadMessageException;
};

}
