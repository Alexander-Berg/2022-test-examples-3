#include "local_mailbox_mock.h"

#include <yplatform/util/sstream.h>

#include <pgg/error.h>

#include <algorithm>
#include <boost/range/adaptor/reversed.hpp>
#include <boost/property_tree/json_parser.hpp>

namespace xeno::mailbox::local {

namespace {

void set_folder_info(folder& local_folder, const folder& folder_info)
{
    local_folder.path = folder_info.path;
    local_folder.uidvalidity = folder_info.uidvalidity;
    local_folder.uidnext = folder_info.uidnext;
    local_folder.top_id = folder_info.top_id;
    local_folder.downloaded_range = folder_info.downloaded_range;
    local_folder.importance = folder_info.importance;
}

}

loc_mailbox_mock::loc_mailbox_mock(const yplatform::ptree& cfg)
{
    auto folders_cfg = cfg.get_child_optional("folders");

    if (folders_cfg)
    {
        for (const auto& folder_cfg : *folders_cfg)
        {
            read_folder_cfg(folder_cfg.second);
        }
    }
    // we need a non-empty auth_data to successfully load the cache mailbox
    account.auth_data.emplace_back(auth_data());
}

void loc_mailbox_mock::read_folder_cfg(const yplatform::ptree& folder_cfg)
{
    folder folder_;
    fid_t fid = std::to_string(fid_next++);
    folder_.fid = fid;
    folder_.path = path_t(folder_cfg.get<std::string>("path", fid), '|');
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

    message_vector messages;
    auto messages_cfg = folder_cfg.get_child_optional("messages");
    if (messages_cfg)
    {
        for (const auto& m : *messages_cfg)
        {
            message msg;
            msg.num = m.second.get("num", 0);
            msg.id = m.second.get("imap_id", 0);
            msg.size = m.second.get("size", 0);
            msg.errors_count = m.second.get("error_count", 0);
            msg.saved_errors_count = msg.errors_count;
            msg.mid = m.second.get("mid", mid_next++);
            msg.fid = folder_.fid;
            auto flags = m.second.get_child_optional("flags");
            if (flags)
            {
                for (auto& flag : *flags)
                {
                    msg.flags.system_flags.insert(flags_t::from_string(flag.second.data()));
                }
            }

            messages.emplace_back(std::move(msg));
            mid_fid.insert(std::make_pair(msg.mid, folder_.fid));
        }
    }
    folders[fid] = std::make_pair<folder, message_vector>(std::move(folder_), std::move(messages));
    path_fid[folder_.path] = fid;
}

void loc_mailbox_mock::get_folder_vector(const folder_vector_cb& cb)
{
    auto folders_ptr = std::make_shared<folder_vector>();
    for (const auto& [fid, folder_messages_pair] : folders)
    {
        auto& [folder, messages] = folder_messages_pair;
        folders_ptr->push_back(folder);
        folders_ptr->back().count = 0;
    }
    cb(code::ok, folders_ptr);
}

void loc_mailbox_mock::get_messages_info_by_id(
    const fid_t& fid,
    const imap_id_vector& ids,
    msg_info_type /*with_flags*/,
    const messages_vector_cb& cb)
{
    auto folder = folders.find(fid);
    if (folder != folders.end())
    {
        auto result_vector = std::make_shared<message_vector>();
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (const auto& id : ids)
            {
                auto msg =
                    std::find_if(msg_vector.begin(), msg_vector.end(), [id](const message& msg) {
                        return id == msg.id;
                    });
                if (msg != msg_vector.end())
                {
                    result_vector->emplace_back(message(*msg));
                }
            }
        }
        cb(code::ok, result_vector);
        return;
    }
    cb(code::folder_not_found, nullptr);
}

void loc_mailbox_mock::get_messages_info_chunk_by_id(
    const fid_t& fid,
    imap_id_t imap_id_start,
    size_t count,
    const messages_vector_cb& cb)
{
    auto folder = folders.find(fid);
    if (folder != folders.end())
    {
        auto result_vector = std::make_shared<message_vector>();
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            for (auto& msg : boost::adaptors::reverse(msg_vector))
            {
                if (imap_id_start >= msg.id && result_vector->size() < count)
                {
                    result_vector->emplace_back(message(msg));
                }
            }
        }
        cb(code::ok, result_vector);
        return;
    }
    cb(code::folder_not_found, nullptr);
}

void loc_mailbox_mock::get_messages_info_top(
    const fid_t& fid,
    size_t num,
    const messages_vector_cb& cb)
{
    auto folder = folders.find(fid);
    if (folder != folders.end())
    {
        auto result_vector = std::make_shared<message_vector>();
        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            auto size = msg_vector.size();
            if (num <= size)
            {
                result_vector->resize(num);
                std::copy_n(msg_vector.rbegin(), num, result_vector->begin());
            }
            else
            {
                result_vector->resize(size);
                std::copy_n(msg_vector.rbegin(), size, result_vector->begin());
            }
        }
        cb(code::ok, result_vector);
        return;
    }
    cb(code::folder_not_found, nullptr);
}

void loc_mailbox_mock::get_messages_info_without_flags_by_mid(
    const mid_vector& mids,
    const messages_vector_cb& cb)
{
    auto result_vector = std::make_shared<message_vector>();
    for (auto& mid : mids)
    {
        auto fid = mid_fid.find(mid);
        if (fid == mid_fid.end())
        {
            return cb(code::message_not_found, result_vector);
        }

        auto folder = folders.find(fid->second);
        if (folder == folders.end())
        {
            return cb(code::folder_not_found, result_vector);
        }

        const auto& msg_vector = folder->second.second;
        if (!msg_vector.empty())
        {
            auto msg =
                std::find_if(msg_vector.begin(), msg_vector.end(), [mid](const message& msg) {
                    return mid == msg.mid;
                });
            if (msg != msg_vector.end())
            {
                result_vector->emplace_back(message(*msg));
            }
        }
    }
    cb(code::ok, result_vector);
}

void loc_mailbox_mock::get_not_downloaded_messages(uint32_t num, const messages_vector_cb& cb)
{
    auto all_messages = std::make_shared<message_vector>();
    for (auto& [id, folder] : folders)
    {
        all_messages->insert(all_messages->end(), folder.second.begin(), folder.second.end());
    }
    std::sort(
        all_messages->begin(), all_messages->end(), [](const message& lhs, const message& rhs) {
            return lhs.errors_count < rhs.errors_count;
        });

    auto messages = std::make_shared<message_vector>();
    for (auto& message : *all_messages)
    {
        if (message.mid == 0)
        {
            messages->push_back(message);
            --num;
            if (!num) break;
        }
    }
    cb(code::ok, messages);
}

void loc_mailbox_mock::get_mids_by_tids(const tid_vector& tids, const mid_vector_cb& cb)
{
    if (tids.empty())
    {
        cb(code::ok, std::make_shared<mid_vector>());
    }
    else
    {
        cb(macs::error::make_error_code(macs::error::noSuchMessage), mid_vector_ptr());
    }
}

void loc_mailbox_mock::delete_messages_by_id(
    const fid_t& fid,
    const imap_id_vector& ids,
    const without_data_cb& cb)
{
    auto folder_it = folders.find(fid);
    if (folder_it != folders.end())
    {
        auto& [fid, folder_messages_pair] = *folder_it;
        auto& [folder, msg_vector] = folder_messages_pair;
        if (!msg_vector.empty())
        {
            for (const auto& id : ids)
            {
                delete_message_by_id(folder, msg_vector, id);
            }
            update_nums(msg_vector);
            cb(code::ok);
            return;
        }
        cb(code::message_not_found);
        return;
    }
    cb(code::folder_not_found);
}

void loc_mailbox_mock::delete_message_by_id(folder& folder_, message_vector& messages, imap_id_t id)
{
    for (auto iter = messages.begin(); iter != messages.end(); ++iter)
    {
        if (iter->id == id)
        {
            mid_fid.erase(iter->mid);
            messages.erase(iter);
            --folder_.count;
            break;
        }
    }
}

void loc_mailbox_mock::delete_messages_by_mid(const mid_vector& mids, const without_data_cb& cb)
{
    for (auto& mid : mids)
    {
        auto fid = mid_fid[mid];
        auto folder_it = folders.find(fid);
        if (folder_it != folders.end())
        {
            auto& [fid, folder_messages_pair] = *folder_it;
            auto& [folder, msg_vector] = folder_messages_pair;
            if (!msg_vector.empty())
            {
                delete_message_by_mid(folder, msg_vector, mid);
                update_nums(msg_vector);
            }
        }
        else
        {
            return cb(code::folder_not_found);
        }
    }
    cb(code::ok);
}

void loc_mailbox_mock::delete_message_by_mid(folder& folder_, message_vector& messages, mid_t mid)
{
    for (auto iter = messages.begin(); iter != messages.end(); ++iter)
    {
        if (iter->mid == mid)
        {
            mid_fid.erase(mid);
            messages.erase(iter);
            --folder_.count;
            break;
        }
    }
}

void loc_mailbox_mock::move_message_by_mid(
    folder& fld_from,
    message_vector& msg_from,
    folder& fld_to,
    message_vector& msg_to,
    const move_coordinates& coords)
{
    for (auto iter = msg_from.begin(); iter != msg_from.end(); ++iter)
    {
        if (iter->mid == coords.mid)
        {
            iter->num = fld_to.count++;
            iter->fid = fld_to.fid;
            iter->id = coords.new_imap_id;
            msg_to.emplace_back(*iter);
            msg_from.erase(iter);
            mid_fid[coords.mid] = fld_to.fid;
            --fld_from.count;
            break;
        }
    }
}

void loc_mailbox_mock::move_messages(
    const fid_t& src_fid,
    const fid_t& dst_fid,
    tab_opt /*dst_tab*/,
    const move_coordinates_vec& move_coords,
    const without_data_cb& cb)
{
    auto folder_from = folders.find(src_fid);
    auto folder_to = folders.find(dst_fid);
    if (folder_from != folders.end() && folder_to != folders.end())
    {
        auto& msg_vector_from = folder_from->second.second;
        auto& msg_vector_to = folder_to->second.second;
        if (!msg_vector_from.empty())
        {
            for (auto& coords : move_coords)
            {
                move_message_by_mid(
                    folder_from->second.first,
                    msg_vector_from,
                    folder_to->second.first,
                    msg_vector_to,
                    coords);
            }
            cb(code::ok);
            return;
        }
        cb(code::message_not_found);
        return;
    }
    cb(code::folder_not_found);
}

void loc_mailbox_mock::update_nums(message_vector& messages)
{
    num_t num{ 1 };
    std::for_each(messages.rbegin(), messages.rend(), [&num](message& msg) { msg.num = num++; });
}

void loc_mailbox_mock::create_folder(
    const folder& folder,
    const fid_t& parent_fid,
    const std::string& /*symbol*/,
    const folder_vector_cb& cb)
{
    auto created_folders = std::make_shared<folder_vector>();

    auto fid = path_fid.find(folder.path);
    if (fid == path_fid.end())
    {

        if (parent_fid.size())
        {
            auto parent = folders.find(parent_fid);
            if (parent == folders.end())
            {
                return cb(macs::error::make_error_code(macs::error::noSuchFolder), created_folders);
            }
            parent->second.first.childs.push_back(folder.path);
        }

        fid_t new_fid{ std::to_string(fid_next++) };
        mailbox::folder new_folder(folder.path, new_fid, static_cast<uint32_t>(rand()));
        created_folders->push_back(new_folder);
        folders[new_fid] = std::make_pair<mailbox::folder, message_vector>(
            std::move(new_folder), message_vector());
        path_fid[folder.path] = new_fid;
    }
    cb(code::ok, created_folders);
}

void loc_mailbox_mock::update_folder(
    const folder& new_folder,
    const fid_t_opt& new_parent,
    const without_data_cb& cb)
{
    auto it = folders.find(new_folder.fid);
    if (it == folders.end())
    {
        return cb(code::folder_not_found);
    }

    auto& folder = it->second.first;
    // update child vector in parent
    for (auto& old_parent_it : folders)
    {
        auto& old_parent_folder = old_parent_it.second.first;
        auto& old_parent_childs = old_parent_folder.childs;
        auto child_it = std::find(old_parent_childs.begin(), old_parent_childs.end(), folder.path);
        if (child_it != old_parent_childs.end())
        {
            old_parent_childs.erase(child_it);
            if (new_parent && *new_parent != old_parent_folder.fid)
            {
                auto new_parent_it = folders.find(*new_parent);
                if (new_parent_it != folders.end())
                {
                    auto& new_parent_folder = new_parent_it->second.first;
                    auto& new_parent_childs = new_parent_folder.childs;
                    new_parent_childs.push_back(new_folder.path);
                }
            }
            else
            {
                old_parent_childs.push_back(new_folder.path);
            }
        }
    }
    folder.path = new_folder.path;
    folder.uidvalidity = new_folder.uidvalidity;

    auto child_pathes = folder.childs;
    for (auto& child_path : child_pathes)
    {
        for (auto& entry : folders)
        {
            auto& child_folder_ref = entry.second.first;
            if (child_folder_ref.path == child_path)
            {
                auto child_folder = child_folder_ref;
                child_folder.path = new_folder.path.make_child_path(child_path.get_name());
                update_folder(child_folder, fid_t_opt(), [](error) {});
                break;
            }
        }
    }

    cb(code::ok);
}

void loc_mailbox_mock::delete_folder(const fid_t& fid, const without_data_cb& cb)
{
    auto folder = folders.find(fid);
    if (folder == folders.end())
    {
        return cb(code::folder_not_found);
    }

    auto& folder_info = folder->second.first;
    if (!folder_info.childs.empty())
    {
        return cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }

    if (!folder->second.second.empty())
    {
        return cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }

    if (folder_info.type != folder::type_t::user)
    {
        return cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
    for (auto& parent_folder : folders)
    {
        auto& parent_folder_info = parent_folder.second.first;
        auto it = std::find_if(
            parent_folder_info.childs.begin(),
            parent_folder_info.childs.end(),
            [&folder_info](const path_t& child_path) { return folder_info.path == child_path; });

        if (it != parent_folder_info.childs.end())
        {
            parent_folder_info.childs.erase(it);
            break;
        }
    }
    path_fid.erase(folder_info.path);
    folders.erase(fid);
    return cb(code::ok);
}

void loc_mailbox_mock::clear_folder(const fid_t& fid, const without_data_cb& cb)
{
    auto folder = folders.find(fid);
    if (folder != folders.end())
    {
        auto& messages = folder->second.second;
        for (auto& message : messages)
        {
            mid_fid.erase(message.mid);
        }
        messages.clear();
        folder->second.first.count = 0;
        cb(code::ok);
        return;
    }
    cb(code::folder_not_found);
}

void loc_mailbox_mock::update_flags(
    const fid_t& fid,
    mid_t mid,
    const flags_t flags,
    const without_data_cb& cb)
{
    auto folder_it = folders.find(fid);
    if (folder_it != folders.end())
    {
        auto& msg_vector = folder_it->second.second;
        if (!msg_vector.empty())
        {
            for (auto& msg : msg_vector)
            {
                if (msg.mid == mid)
                {
                    msg.flags = flags;
                    cb(code::ok);
                    return;
                }
            }
        }
        cb(code::message_not_found);
        return;
    }
    cb(code::folder_not_found);
}

void loc_mailbox_mock::change_flags(
    const mid_vector& mids,
    const flags_t& add,
    const flags_t& del,
    const without_data_cb& cb)
{
    for (auto& mid : mids)
    {
        auto fid = mid_fid.at(mid);
        auto& msg_vector = folders.at(fid).second;
        bool found = false;
        for (auto& msg : msg_vector)
        {
            if (msg.mid == mid)
            {
                found = true;

                msg.flags.system_flags.insert(add.system_flags.begin(), add.system_flags.end());
                msg.flags.user_flags.insert(add.user_flags.begin(), add.user_flags.end());

                for (auto& flag : del.system_flags)
                {
                    msg.flags.system_flags.erase(flag);
                }

                for (auto& flag : del.user_flags)
                {
                    msg.flags.user_flags.erase(flag);
                }
            }
        }

        if (!found)
        {
            cb(code::message_not_found);
            return;
        }
    }
    cb(code::ok);
}

void loc_mailbox_mock::update_downloaded_range(
    const fid_t& fid,
    const imap_range& range,
    const without_data_cb& cb)
{
    auto folder_it = folders.find(fid);
    if (folder_it != folders.end())
    {
        auto& folder = folder_it->second.first;
        folder.downloaded_range = range;
        cb(code::ok);
        return;
    }
    cb(code::folder_not_found);
}

void loc_mailbox_mock::store_message(
    uid_t /*uid*/,
    const std::string& /*email*/,
    const message& ext_msg,
    string&& /*body*/,
    notification_type /*notify_type*/,
    const std::string& priority,
    const store_message_response_cb& cb)
{
    last_store_priority = priority;

    auto it = folders.find(ext_msg.fid);
    if (it == folders.end())
    {
        ++store_errors_count_;
        return cb(code::folder_not_found, store_message_response());
    }

    auto& folder = it->second.first;
    auto& messages = it->second.second;

    auto msg_it =
        std::find_if(messages.begin(), messages.end(), [id = ext_msg.id](const auto& msg) {
            return msg.id == id;
        });
    if (msg_it != messages.end())
    {
        if (msg_it->mid)
        {
            ++store_errors_count_;
            return cb(code::message_is_duplicate, store_message_response());
        }
        else
        {
            mid_t mid{ mid_next++ };
            msg_it->mid = mid;
            return cb(code::ok, { mid, 0 });
        }
    }

    message msg;
    msg.num = ++folder.count;
    msg.id = ext_msg.id;
    mid_t mid{ mid_next++ };
    msg.mid = mid;

    mid_fid[mid] = ext_msg.fid;

    messages.emplace_back(std::move(msg));
    cb(code::ok, { mid, 0 });
}

void loc_mailbox_mock::check_spam(
    uid_t /*uid*/,
    const karma_t& /*karma*/,
    const std::string& /*from*/,
    const std::vector<std::string>& /*to*/,
    const std::string& /*client_ip*/,
    const std::string& /*request_id*/,
    string_ptr /*body*/,
    const without_data_cb& cb)
{
    cb(code::ok);
}

void loc_mailbox_mock::get_send_operation_result(const std::string& operation_id, string_opt_cb cb)
{
    if (operation_id.empty() || !operation_results.count(operation_id)) return cb({}, {});
    cb({}, operation_results[operation_id]);
}

void loc_mailbox_mock::save_send_operation_result(
    const std::string& operation_id,
    const std::string& result,
    without_data_cb cb)
{
    if (operation_results.count(operation_id))
    {
        return cb(code::local_mailbox_exception);
    }
    if (operation_id.size())
    {
        operation_results[operation_id] = result;
    }
    cb({});
}

void loc_mailbox_mock::clear(const without_data_cb& cb)
{
    std::vector<fid_t> fid_to_delete;
    for (auto& folder_it : folders)
    {
        auto& old_folder = folder_it.second.first;
        if (old_folder.type != folder::type_t::user)
        {
            auto& messages = folder_it.second.second;
            messages.clear();
            folder new_folder(old_folder.path, old_folder.fid);
            new_folder.type = old_folder.type;
            old_folder = new_folder;
        }
        else
        {
            fid_to_delete.emplace_back(old_folder.fid);
        }
    }
    for (const auto& fid : fid_to_delete)
    {
        folders.erase(fid);
    }
    mid_fid.clear();
    path_fid.clear();
    cb(code::ok);
}

void loc_mailbox_mock::erase_security_locks(const without_data_cb& cb)
{
    cb(code::ok);
}

void loc_mailbox_mock::get_account(const account_cb& cb)
{
    cb(code::ok, account);
}

void loc_mailbox_mock::init_system_folder(const folder& system_folder, const without_data_cb& cb)
{
    if (system_folder.type == folder::type_t::user)
    {
        return cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
    auto folder_it = std::find_if(
        folders.begin(),
        folders.end(),
        [type = system_folder.type](const std::pair<fid_t, folder_messages_pair>& fid_folder) {
            return fid_folder.second.first.type == type;
        });
    if (folder_it != folders.end() && !folder_it->second.first.path.empty())
    {
        return cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }

    folder_it = std::find_if(
        folders.begin(),
        folders.end(),
        [fid = system_folder.fid](const std::pair<fid_t, folder_messages_pair>& fid_folder) {
            return fid_folder.second.first.fid == fid;
        });
    if (folder_it == folders.end())
    {
        folders[system_folder.fid] = { system_folder, message_vector() };
        path_fid[system_folder.path] = system_folder.fid;
    }
    else
    {
        auto& folder_messages = folder_it->second.second;
        if (folder_messages.size())
        {
            return cb(
                pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
        }
        auto& folder = folder_it->second.first;
        if (!folder.path.empty())
        {
            return cb(
                pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
        }
        if (folder.type != system_folder.type)
        {
            return cb(
                pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
        }
        set_folder_info(folder, system_folder);
    }
    cb(code::ok);
}

void loc_mailbox_mock::delete_mailish_folder_entry(
    const fid_vector& fids,
    const without_data_cb& cb)
{
    for (auto& fid : fids)
    {
        auto folder_it = folders.find(fid);
        if (folder_it == folders.end())
        {
            return cb(code::folder_not_found);
        }

        auto& folder_messages = folder_it->second.second;
        if (folder_messages.size())
        {
            return cb(
                pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
        }
        auto& folder = folder_it->second.first;
        set_folder_info(folder, {});
    }
    return cb(code::ok);
}

void loc_mailbox_mock::increment_mailish_entry_errors_count(
    const fid_t& fid,
    imap_id_t ext_msg,
    uint32_t errors_count,
    const without_data_cb& cb)
{
    auto folder_it = folders.find(fid);
    if (folder_it == folders.end())
    {
        return cb(code::folder_not_found);
    }

    auto& msgs = folder_it->second.second;
    auto msg_it = std::find_if(
        msgs.begin(), msgs.end(), [ext_msg](const message& msg) { return msg.id == ext_msg; });

    if (msg_it == msgs.end())
    {
        msgs.emplace_back(message{ ext_msg, fid, errors_count });
        msg_it = std::prev(msgs.end());
    }
    else
    {
        msg_it->errors_count += errors_count;
    }
    // sync errors_count and saved_errors_count
    msg_it->saved_errors_count = msg_it->errors_count;
    cb(code::ok);
}

void loc_mailbox_mock::delete_mailish_entry(
    const fid_t& fid,
    const imap_id_vector& ids,
    const without_data_cb& cb)
{
    auto folder_it = folders.find(fid);
    if (folder_it == folders.end())
    {
        return cb(code::folder_not_found);
    }

    for (auto& ext_msg : ids)
    {
        auto& msgs = folder_it->second.second;
        auto msg_it = std::find_if(
            msgs.begin(), msgs.end(), [ext_msg](const message& msg) { return msg.id == ext_msg; });

        if (msg_it == msgs.end())
        {
            cb(code::message_not_found);
        }
        else
        {
            msgs.erase(msg_it);
        }
    }
    cb(code::ok);
}

const message_vector& loc_mailbox_mock::get_folder_messages(const fid_t& fid) const
{
    return folders.at(fid).second;
}

void loc_mailbox_mock::add_message(const fid_t& fid, const message& msg)
{
    auto& folder = folders.at(fid).first;
    auto& messages = folders.at(fid).second;

    folder.count++;
    messages.push_back(msg);
}

std::string loc_mailbox_mock::dump()
{
    yplatform::ptree dump;

    if (folders.size())
    {
        yplatform::ptree folders_tree;
        for (auto& folder_pair : folders)
        {
            yplatform::ptree folder_tree;

            auto& folder = folder_pair.second.first;
            folder_tree.put_child("folder", folder.dump());

            auto& messages = folder_pair.second.second;
            if (messages.size())
            {
                yplatform::ptree msg_tree;
                for (auto& msg : messages)
                {
                    msg_tree.push_back(std::make_pair("message", msg.dump()));
                }
                folder_tree.put_child("messages", msg_tree);
            }
            else
            {
                folder_tree.put<std::string>("messages", "empty");
            }
            folders_tree.push_back(std::make_pair("folder", folder_tree));
        }
        dump.put_child("folders", folders_tree);
    }
    else
    {
        dump.put<std::string>("folders", "empty");
    }

    std::stringstream result;
    boost::property_tree::write_json(result, dump);
    return result.str();
}

void loc_mailbox_mock::set_account(const account_t& account)
{
    this->account = account;
}

void loc_mailbox_mock::get_karma(uid_t /*uid*/, const karma_cb& cb)
{
    cb(code::ok, karma_t());
}

}
