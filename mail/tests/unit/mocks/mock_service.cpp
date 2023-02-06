#include "mock_service.h"

#include <boost/property_tree/json_parser.hpp>

using namespace yimap;
using namespace yimap::backend;

MockServicePtr MockService::createFromJSON(const std::string& path)
{
    Ptree cfg;
    boost::property_tree::read_json(path, cfg);
    auto fakeMailBox = cfg.get_child("fake_mailbox");
    return std::make_shared<MockService>(fakeMailBox);
}

MockService::MockService(Ptree& cfg)
    : folders(std::make_shared<RawFolderList>())
    , messages(std::make_shared<MapFolderMockMessages>())
{
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

                        if (uid && !stid.empty())
                        {
                            MessageData msg;
                            msg.uid = uid;
                            msg.num = num++;
                            msg.mid = mid++;
                            msg.setDetails(std::to_string(msg.mid), stid, 0, size);

                            (*messages)[new_folder->fid][uid] = msg;
                            maxUid = std::max(uid, maxUid);
                        }
                    }
                }
                if (maxUid >= 1)
                {
                    new_folder->uidNext = static_cast<uint64_t>(++maxUid);
                }
            }
            folders->push_back(new_folder);
        }
    }
    else
    {
        throw std::runtime_error("MockBackend cfg: folders is empty");
    }
}

FolderListPtr MockService::loadFolderList(LanguageConfigPtr langSettings)
{
    return std::make_shared<FolderList>(folders, langSettings);
}

FolderInfo MockService::deprecatedLoadFolderInfo(const DBFolderId& folderId)
{
    for (const auto& f : *folders)
    {
        if (f->fid == folderId.fid)
        {
            return convertFolderInfo(f);
        }
    }
    throw NoSuchFolderError(folderId.name, "fid:" + folderId.fid);
}

FolderInfo MockService::select(const DBFolderId& folderId, UidMapData& uidMap)
{
    auto folderInfo = deprecatedLoadFolderInfo(folderId);

    auto iter = messages->find(folderInfo.fid);

    if (iter != messages->end())
    {
        for (const auto& mess : iter->second)
        {
            UidMapEntry entry;
            entry.uid = mess.first;
            uidMap[entry.uid] = entry;
        }
    }

    return folderInfo;
}

void MockService::loadMessages(FolderRef mailbox, const seq_range& range)
{
    if (range.empty())
    {
        return;
    }

    auto folderMap = messages->find(mailbox.fid());

    if (folderMap == messages->end())
    {
        return;
    }

    const auto& fMap = folderMap->second;
    MessageVector result;

    for (const auto& r : range)
    {
        uint32_t begin = r.first;
        uint32_t end = r.second;
        for (auto uid = begin; uid <= end; ++uid)
        {
            auto mess = fMap.find(uid);

            if (mess != fMap.end())
            {
                result.push_back(mess->second);
            }
        }
    }

    mailbox.insertMessages(result);
}

FolderInfo MockService::convertFolderInfo(FullFolderInfoPtr folder)
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
