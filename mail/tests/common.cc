#include "common.h"

using namespace xeno::mailbox;

bool folder_name_exist(const std::string& name, xeno::mailbox::folder_vector_ptr folders)
{
    auto it = std::find_if(folders->begin(), folders->end(), [name](const folder& folder) {
        return folder.path.to_string() == name;
    });

    return it != folders->end();
}

bool folder_has_child(const std::string& name, const xeno::mailbox::folder& folder)
{
    auto it = std::find_if(folder.childs.begin(), folder.childs.end(), [name](const path_t& path) {
        return path.to_string() == name;
    });
    return it != folder.childs.end();
}

xeno::mailbox::imap_id_message_map get_folder_top(
    xeno::mailbox::cache_mailbox_ptr cache_mailbox,
    const xeno::mailbox::path_t& path)
{
    auto it = cache_mailbox->sync_newest_state()->folders.find(path);
    if (it != cache_mailbox->sync_newest_state()->folders.end())
    {
        return it->second.messages_top;
    }
    return {};
}

void set_folder_top(
    xeno::mailbox::cache_mailbox_ptr cache_mailbox,
    const xeno::mailbox::path_t& path,
    const xeno::mailbox::message_vector_ptr& messages)
{
    auto& msg_top = cache_mailbox->sync_newest_state()->folders[path].messages_top;
    msg_top.clear();
    for (auto& msg : *messages)
    {
        msg_top[msg.id] = msg;
        msg_top[msg.id].status =
            (msg.mid ? message::status_t::ok : message::status_t::to_download_body);
    }
}
