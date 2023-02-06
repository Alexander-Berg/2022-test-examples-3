#pragma once

#include <common/folder_list.h>
#include <common/folder.h>
#include <common/message_data.h>

#include <backend/backend.h>

#include <boost/property_tree/json_parser.hpp>
#include <memory>
#include <string>
#include <map>
#include <unordered_map>

using namespace yimap;
using namespace yimap::backend;

using MapUidMessage = std::map<uint32_t, MessageData>;
using MapFolderMockMessages = std::map<std::string, MapUidMessage>;
using MapFolderMockMessagesPtr = std::shared_ptr<MapFolderMockMessages>;

struct FakeMailbox
{
    RawFolderList folders;
    MapFolderMockMessages messages;
};

inline FakeMailbox createFakeMailboxFromJsonFile(const std::string& path)
{
    RawFolderList folders;
    MapFolderMockMessages messages;

    Ptree cfg;
    boost::property_tree::read_json(path, cfg);
    cfg = cfg.get_child("fake_mailbox");

    auto folderCfg = cfg.get_child_optional("folders");

    if (folderCfg)
    {
        uint64_t mid = 1;
        for (auto& fold : *folderCfg)
        {
            FullFolderInfoPtr new_folder = std::make_shared<FullFolderInfo>();

            new_folder->name = fold.second.get("name", "");
            new_folder->fid = fold.second.get("fid", "");
            new_folder->symbol = fold.second.get("symbol", "");
            new_folder->subscribed = fold.second.get("subscribed", true);
            new_folder->messageCount = fold.second.get("messageCount", 0u);
            new_folder->uidNext = 1;
            new_folder->uidValidity = std::numeric_limits<uint32_t>::max();

            if (!new_folder->name.empty())
            {
                auto messCfg = fold.second.get_child_optional("messages");
                uint32_t maxUid = 0;

                if (messCfg)
                {
                    uint32_t num = 1;

                    for (auto& mess : *messCfg)
                    {
                        uint32_t uid = mess.second.get("uid", 0u);
                        uint32_t size = mess.second.get("size", 300);
                        std::string stid = mess.second.get("stid", "");
                        auto flags = mess.second.get_child_optional("flags");

                        if (uid && !stid.empty())
                        {
                            MessageData msg;
                            msg.uid = uid;
                            msg.num = num++;
                            msg.mid = mid++;
                            if (flags)
                            {
                                for (auto& flag : *flags)
                                {
                                    msg.flags.setFlag(flag.second.get_value<string>());
                                }
                            }
                            msg.setDetails(std::to_string(msg.mid), 0, size);

                            messages[new_folder->fid][uid] = msg;
                            maxUid = std::max(uid, maxUid);
                        }
                    }
                }
                if (maxUid >= 1)
                {
                    new_folder->uidNext = static_cast<uint64_t>(++maxUid);
                }
            }
            folders.push_back(new_folder);
        }
    }
    else
    {
        throw std::runtime_error("MockBackend cfg: folders is empty");
    }
    return FakeMailbox{ folders, messages };
}
