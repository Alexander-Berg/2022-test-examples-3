#include "external_mailbox_mock.h"

#include <algorithm>
#include <boost/algorithm/string/join.hpp>
#include <boost/property_tree/json_parser.hpp>
#include <vector>

namespace xeno::mailbox::external {

ext_mailbox_mock::ext_mailbox_mock(const yplatform::ptree& cfg)
{
    auto folders_cfg = cfg.get_child_optional("folders");

    if (folders_cfg)
    {
        for (const auto& folder_cfg : *folders_cfg)
        {
            read_folder_cfg(folder_cfg.second);
        }
    }
}

void ext_mailbox_mock::read_folder_cfg(const yplatform::ptree& folder_cfg)
{
    folder folder_;
    path_t path = path_t(folder_cfg.get<std::string>("path"), '|');
    folder_.path = path;
    folder_.type = folder::type_from_string(folder_cfg.get("type", "user"));
    folder_.count = folder_cfg.get<num_t>("count", 0u);
    folder_.uidvalidity = folder_cfg.get<uint32_t>("uidvalidity", 0u);
    folder_.uidnext = folder_cfg.get<imap_id_t>("uidnext", 1u);

    auto childs_cfg = folder_cfg.get_child_optional("childs_path");
    if (childs_cfg)
    {
        for (const auto& child : *childs_cfg)
        {
            const std::string& child_path = child.second.data();
            if (!child_path.empty())
            {
                folder_.childs.emplace_back(path_t(child_path, '|'));
            }
        }
    }

    message_body_pairs_vector messages;
    auto messages_cfg = folder_cfg.get_child_optional("messages");
    if (messages_cfg)
    {
        for (const auto& m : *messages_cfg)
        {
            message msg;
            msg.num = m.second.get("num", 0);
            msg.id = m.second.get("imap_id", 0);
            msg.size = m.second.get("size", 0);
            auto flags = m.second.get_child_optional("flags");
            if (flags)
            {
                for (auto& flag : *flags)
                {
                    msg.flags.system_flags.insert(flags_t::from_string(flag.second.data()));
                }
            }
            std::string body = m.second.get("body", "");
            if (body.size() && !msg.size)
            {
                // adjust size according to body
                msg.size = body.size();
            }
            else if (msg.size && body.empty())
            {
                // fill body according to size
                body = std::string(msg.size, '1');
            }

            if (msg.id > uidnext_)
            {
                uidnext_ = msg.id + 1;
            }
            messages.emplace_back(
                std::make_pair<message, std::string>(std::move(msg), std::move(body)));
        }
    }
    folders_[path] =
        std::make_pair<folder, message_body_pairs_vector>(std::move(folder_), std::move(messages));
}

void ext_mailbox_mock::get_folder_vector(const folder_vector_cb& cb)
{
    auto folders_ptr = std::make_shared<folder_vector>();
    for (const auto& [path, folder_messages_pair] : folders_)
    {
        const auto& [folder, messages] = folder_messages_pair;
        folders_ptr->push_back(folder);
    }
    cb(code::ok, folders_ptr);
}

void ext_mailbox_mock::get_folder_info(const path_t& path, const folder_cb& cb)
{
    auto it = folders_.find(path);
    if (it != folders_.end())
    {
        return cb(code::ok, std::make_shared<folder>(it->second.first));
    }
    cb(code::imap_response_no, nullptr);
}

void ext_mailbox_mock::get_messages_info_by_id(
    const path_t& path,
    const imap_range& range,
    const messages_vector_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto result_vector = std::make_shared<message_vector>();
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (const auto& pair : msg_vector)
            {
                if (range.within(pair.first.id))
                {
                    result_vector->emplace_back(message(pair.first));
                }
            }
        }

        cb(code::ok, result_vector);
        return;
    }
    cb(code::imap_response_no, nullptr);
}

void ext_mailbox_mock::get_messages_info_by_num(
    const path_t& path,
    num_t top,
    num_t bottom,
    const messages_vector_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto result_vector = std::make_shared<message_vector>();
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (const auto& pair : msg_vector)
            {
                if (pair.first.num >= bottom && pair.first.num <= top)
                {
                    result_vector->emplace_back(message(pair.first));
                }
            }
        }
        cb(code::ok, result_vector);
        return;
    }
    cb(code::imap_response_no, nullptr);
}

void ext_mailbox_mock::get_message_body(const path_t& path, imap_id_t id, const message_body_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (const auto& pair : msg_vector)
            {
                if (pair.first.id == id)
                {
                    std::string body = pair.second;
                    cb(code::ok, std::make_shared<std::string>(body));
                    return;
                }
            }
        }
        cb(code::imap_response_no, std::make_shared<std::string>());
        return;
    }
    cb(code::imap_response_no, std::make_shared<std::string>());
}

void ext_mailbox_mock::delete_messages(
    const path_t& path,
    imap_id_vector_ptr ids,
    const without_data_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (auto& id : *ids)
            {
                bool found = false;
                for (auto iter = msg_vector.begin(); iter != msg_vector.end(); ++iter)
                {
                    if (iter->first.id == id)
                    {
                        found = true;
                        msg_vector.erase(iter);
                        --folder->second.first.count;
                        update_nums(msg_vector);
                        break;
                    }
                }

                if (!found)
                {
                    cb(code::imap_response_no);
                    return;
                }
            }
            cb(code::ok);
            return;
        }
        cb(code::imap_response_no);
        return;
    }
    cb(code::imap_response_no);
}

void ext_mailbox_mock::update_nums(message_body_pairs_vector& messages)
{
    num_t num{ 1 };
    std::for_each(messages.begin(), messages.end(), [&num](message_body_pair& msg) {
        msg.first.num = num++;
    });
}

void ext_mailbox_mock::move_messages(
    const path_t& from,
    const path_t& to,
    imap_id_vector_ptr ids,
    const imap_ids_transform_cb& cb)
{
    auto folder_from = folders_.find(from);
    auto folder_to = folders_.find(to);
    if (folder_from != folders_.end() && folder_to != folders_.end())
    {
        imap_id_transform_map_ptr result_transform = std::make_shared<imap_id_transform_map>();
        for (auto& id : *ids)
        {
            auto& msg_vector_from = folder_from->second.second;
            auto& msg_vector_to = folder_to->second.second;
            if (!msg_vector_from.empty())
            {
                bool found = false;
                auto it = msg_vector_from.begin();
                while (it != msg_vector_from.end())
                {
                    if (it->first.id == id)
                    {
                        found = true;
                        it->first.num = folder_to->second.first.count++;
                        it->first.id = folder_to->second.first.uidnext++;
                        it->first.fid = folder_to->second.first.fid;
                        msg_vector_to.emplace_back(*it);
                        (*result_transform)[id] = it->first.id;
                        it = msg_vector_from.erase(it);
                        --folder_from->second.first.count;
                        continue;
                    }
                    ++it;
                }

                if (!found)
                {
                    return cb(code::imap_response_no, imap_id_transform_map_ptr());
                }
            }
            else
            {
                return cb(code::imap_response_no, imap_id_transform_map_ptr());
            }
        }
        return cb(code::ok, result_transform);
    }
    cb(code::imap_response_no, imap_id_transform_map_ptr());
}

void ext_mailbox_mock::move_all_messages(
    const path_t& /*from*/,
    const path_t& /*to*/,
    const imap_ids_transform_cb& cb)
{
    cb(code::imap_response_no, imap_id_transform_map_ptr());
}

void ext_mailbox_mock::create_folder(const path_t& path, const without_data_cb& cb)
{
    auto folder_it = folders_.find(path);
    if (folder_it == folders_.end())
    {
        auto parent_path = path.get_parent_path();

        if (!parent_path.empty())
        {
            create_parent_folder(parent_path, path);
        }

        folder new_folder(path, "", static_cast<uint32_t>(rand()));
        folders_[path] = std::make_pair<folder, message_body_pairs_vector>(
            std::move(new_folder), message_body_pairs_vector());
    }
    else if (folder_it->second.first.status == folder::status_t::to_create_external)
    {
        folder_it->second.first.status = folder::status_t::ok;
    }
    else
    {
        return cb(code::imap_response_no);
    }
    cb(code::ok);
}

void ext_mailbox_mock::create_parent_folder(const path_t& path, const path_t& child)
{
    auto folder_it = folders_.find(path);
    if (folder_it != folders_.end())
    {
        folder_it->second.first.childs.emplace_back(child);
    }
    else
    {
        folder parent(path, "", static_cast<uint32_t>(rand()));
        parent.childs.emplace_back(child);
        folders_[path] = std::make_pair<folder, message_body_pairs_vector>(
            std::move(parent), message_body_pairs_vector());
        auto parent_path = path.get_parent_path();
        if (!parent_path.empty())
        {
            create_parent_folder(parent_path, path);
        }
    }
}

void ext_mailbox_mock::rename_folder(
    const path_t& old_path,
    const path_t& new_path,
    const without_data_cb& cb)
{
    auto it = folders_.find(old_path);
    if (it == folders_.end())
    {
        return cb(code::imap_response_no);
    }

    auto folder_messages_pair = it->second;
    auto& folder = folder_messages_pair.first;
    if (it->second.first.type != folder::type_t::user)
    {
        return cb(code::imap_response_no);
    }

    folders_.erase(it);

    folder.path = new_path;
    childs_path_vector new_children;
    for (auto& child : folder.childs)
    {
        auto new_child_path = new_path.make_child_path(child.get_name());
        rename_folder(child, new_child_path, [](error) {});

        new_children.push_back(new_child_path);
    }
    folder.childs = new_children;

    auto old_parent_path = old_path.get_parent_path();
    auto new_parent_path = new_path.get_parent_path();
    if (!old_parent_path.empty() && old_parent_path != new_parent_path)
    {
        auto old_parent_it = folders_.find(old_parent_path);
        if (old_parent_it != folders_.end())
        {
            auto& children = old_parent_it->second.first.childs;
            auto new_end = std::remove(children.begin(), children.end(), old_path);
            children.erase(new_end, children.end());
        }

        auto new_parent_it = folders_.find(old_parent_path);
        if (new_parent_it != folders_.end())
        {
            new_parent_it->second.first.childs.push_back(new_path);
        }
    }

    folders_[new_path] = folder_messages_pair;
    cb(code::ok);
}

void ext_mailbox_mock::delete_folder(const path_t& path, const without_data_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& folder_info = folder->second.first;
        if (folder_info.type == folder::type_t::user)
        {
            if (!folder_info.childs.empty())
            {
                for (const auto& child : folder_info.childs)
                {
                    delete_childs_folders(child);
                }
            }
            folders_.erase(path);
            cb(code::ok);
            return;
        }
        cb(code::imap_response_no);
        return;
    }
    cb(code::imap_response_no);
}

void ext_mailbox_mock::clear_folder(const path_t& path, const without_data_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder == folders_.end())
    {
        return cb(code::imap_response_no);
    }

    folder->second.second.clear();
    folder->second.first.count = 0;
    cb(code::ok);
}

void ext_mailbox_mock::delete_childs_folders(const path_t& path)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& childs = folder->second.first.childs;
        if (!childs.empty())
        {
            for (const auto& path_child : childs)
            {
                delete_childs_folders(path_child);
            }
        }
        folders_.erase(path);
    }
}

void ext_mailbox_mock::mark_flags(
    const path_t& path,
    imap_id_vector_ptr ids,
    const flags_t& add,
    const flags_t& del,
    const without_data_cb& cb)
{
    namespace mock = ::xeno::mailbox::mock_helpers;
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (auto& id : *ids)
            {
                bool found = false;
                for (auto& pair : msg_vector)
                {
                    if (pair.first.id == id)
                    {
                        mock::mark_flags(pair.first.flags, add, del);
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    cb(code::imap_response_no);
                    return;
                }
            }
            cb(code::ok);
            return;
        }
        cb(code::imap_response_no);
        return;
    }
    cb(code::imap_response_no);
}

void ext_mailbox_mock::append(
    const path_t& path,
    std::string body,
    const flags_t& flags,
    const std::string& /*date*/,
    const imap_id_and_uidvalidity_cb& cb)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& folder_info = folder->second.first;
        auto& msg_vector = folder->second.second;
        message msg;
        msg.num = ++folder->second.first.count;
        msg.id = uidnext_++;
        msg.flags = flags;

        msg_vector.emplace_back(
            std::make_pair<message, std::string>(std::move(msg), std::move(body)));
        cb(code::ok, imap_id_uidvalidity_pair(msg.id, folder_info.uidvalidity));
        return;
    }
    cb(code::imap_response_no, imap_id_uidvalidity_pair());
}

void ext_mailbox_mock::send(
    const std::string& /*smtp_login*/,
    const auth_data& /*data*/,
    const endpoint& /*smtp_ep*/,
    const std::string& /*from*/,
    const std::vector<std::string>& /*to*/,
    std::shared_ptr<std::string> /*body*/,
    bool /*notify*/,
    const without_data_cb& cb)
{
    sent_count_++;
    cb(code::ok);
}

void ext_mailbox_mock::change_uidvalidity(bool all, const path_vector& paths)
{
    if (all)
    {
        for (auto& folder_it : folders_)
        {
            auto& folder = folder_it.second.first;
            ++folder.uidvalidity;
        }
    }
    else
    {
        for (const auto& path : paths)
        {
            auto folder_it = folders_.find(path);
            if (folder_it != folders_.end())
            {
                auto& folder = folder_it->second.first;
                ++folder.uidvalidity;
            }
        }
    }
}

void ext_mailbox_mock::update_folder(
    const path_t& old_path,
    const path_t& new_path,
    uint64_t uidvalidity,
    const without_data_cb& cb)
{
    auto it = folders_.find(old_path);
    if (it != folders_.end())
    {
        auto folder_msg_pair = it->second;
        folders_.erase(it);
        auto& folder = folder_msg_pair.first;
        folder.path = new_path;
        folder.uidvalidity = uidvalidity;
        folders_[new_path] = folder_msg_pair;
        return cb(code::ok);
    }
    cb(code::imap_response_no);
}

message_vector ext_mailbox_mock::get_specific_messages(
    const path_t& path,
    const imap_id_vector& ids)
{
    message_vector result;

    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        const auto& msg_vector = folder->second.second;
        for (const auto& pair : msg_vector)
        {
            auto it = std::find(ids.begin(), ids.end(), pair.first.id);
            if (it != ids.end())
            {
                result.push_back(pair.first);
            }
        }
    }
    return result;
}

const message_body_pairs_vector& ext_mailbox_mock::get_folder_messages(const path_t& path)
{
    return folders_.at(path).second;
}

void ext_mailbox_mock::make_folder_fake(const path_t& path)
{
    auto folder = folders_.find(path);
    if (folder != folders_.end())
    {
        auto& actual_folder = folder->second.first;
        actual_folder.status = folder::status_t::to_create_external;
    }
}

void ext_mailbox_mock::get_provider(const endpoint& /*imap_ep*/, const provider_cb& cb)
{
    return cb(error(), "custom");
}

statistics ext_mailbox_mock::get_stats()
{
    return statistics();
}

}
