#pragma once

#include <mailbox/common.h>
#include <mailbox/external/statistics.h>

#include <yplatform/ptree.h>

#include <list>
#include <unordered_map>

namespace xeno::mailbox::external {
class ext_mailbox_mock;
using ext_mailbox_mock_ptr = std::shared_ptr<ext_mailbox_mock>;

class ext_mailbox_mock
{
public:
    ext_mailbox_mock() = default;
    ext_mailbox_mock(const yplatform::ptree& cfg);
    virtual ~ext_mailbox_mock() = default;

    void imap_authorize(
        const std::string& /*imap_login*/,
        const auth_data& /*data*/,
        const endpoint& /*imap_ep*/,
        const without_data_cb& /*cb*/){};

    void check_smtp_credentials(
        const std::string& /*smtp_login*/,
        const auth_data& /*data*/,
        const endpoint& /*smtp_ep*/,
        const std::string& /*email*/,
        const without_data_cb& cb)
    {
        cb(error());
    };

    void get_folder_vector(const folder_vector_cb& cb);
    void get_folder_info(const path_t& path, const folder_cb& cb);

    void get_messages_info_by_id(
        const path_t& path,
        const imap_range& range,
        const messages_vector_cb& cb);
    void get_messages_info_by_num(
        const path_t& path,
        num_t top,
        num_t bottom,
        const messages_vector_cb& cb);
    virtual void get_message_body(const path_t& path, imap_id_t id, const message_body_cb& cb);

    void delete_messages(const path_t& path, imap_id_vector_ptr ids, const without_data_cb& cb);
    void move_messages(
        const path_t& from,
        const path_t& to,
        imap_id_vector_ptr ids,
        const imap_ids_transform_cb& cb);
    void move_all_messages(const path_t& from, const path_t& to, const imap_ids_transform_cb& cb);

    virtual void create_folder(const path_t& path, const without_data_cb& cb);
    virtual void rename_folder(
        const path_t& old_path,
        const path_t& new_path,
        const without_data_cb& cb);
    virtual void delete_folder(const path_t& path, const without_data_cb& cb);
    virtual void clear_folder(const path_t& path, const without_data_cb& cb);

    void mark_flags(
        const path_t& path,
        imap_id_vector_ptr ids,
        const flags_t& add,
        const flags_t& del,
        const without_data_cb& cb);

    void append(
        const path_t& path,
        std::string body,
        const flags_t& flags,
        const std::string& date,
        const imap_id_and_uidvalidity_cb& cb);
    void send(
        const std::string& smtp_login,
        const auth_data& data,
        const endpoint& smtp_ep,
        const std::string& from,
        const std::vector<std::string>& to,
        std::shared_ptr<std::string> body,
        bool notify,
        const without_data_cb& cb);

    void change_uidvalidity(bool all = true, const path_vector& paths = {});

    bool authenticated()
    {
        return authenticated_;
    }

    void reset()
    {
    }

    void get_provider(const endpoint& imap_ep, const provider_cb& cb);

    std::string get_provider_unsafe()
    {
        return provider_;
    }

    void set_provider(const std::string& p)
    {
        provider_ = p;
    }

    void update_folder(
        const path_t& old_path,
        const path_t& new_path,
        uint64_t uidvalidity,
        const without_data_cb& cb);

    message_vector get_specific_messages(const path_t& path, const imap_id_vector& ids);
    const message_body_pairs_vector& get_folder_messages(const path_t& path);
    void make_folder_fake(const path_t& path);

    size_t get_folders_count()
    {
        return folders_.size();
    }

    statistics get_stats();

    int get_sent_count()
    {
        return sent_count_;
    }

    void set_authentication_flag(bool authenticated)
    {
        authenticated_ = authenticated;
    }

    void cancel()
    {
    }

protected:
    void read_folder_cfg(const yplatform::ptree& folder_cfg);

    void create_parent_folder(const path_t& path, const path_t& child);
    void delete_childs_folders(const path_t& path);

    void update_nums(message_body_pairs_vector& messages);

private:
    using folder_messages_pair = std::pair<folder, message_body_pairs_vector>;
    using path_folders_messages_map = std::unordered_map<path_t, folder_messages_pair>;
    path_folders_messages_map folders_;
    imap_id_t uidnext_{ 1 };
    std::string provider_ = "custom";
    bool authenticated_ = true;

    int sent_count_ = 0;
};

}
