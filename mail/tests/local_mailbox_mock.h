#pragma once

#include <mailbox/common.h>
#include <mailbox/local/store_message_response.h>
#include <mailbox/data_types/store_request.h>

#include <macs/errors.h>
#include <yplatform/ptree.h>

#include <unordered_set>
#include <unordered_map>

namespace xeno::mailbox::local {
class loc_mailbox_mock;
using loc_mailbox_mock_ptr = std::shared_ptr<loc_mailbox_mock>;

class loc_mailbox_mock
{
public:
    loc_mailbox_mock() = default;
    loc_mailbox_mock(const yplatform::ptree& cfg);
    virtual ~loc_mailbox_mock() = default;

    void get_folder_vector(const folder_vector_cb& cb);

    void get_messages_info_by_id(
        const fid_t& fid,
        const imap_id_vector& ids,
        msg_info_type info_type,
        const messages_vector_cb& cb);
    void get_messages_info_chunk_by_id(
        const fid_t& fid,
        imap_id_t highest_imap_id,
        size_t count,
        const messages_vector_cb& cb);
    void get_messages_info_top(const fid_t& fid, size_t num, const messages_vector_cb& cb);
    void get_messages_info_without_flags_by_mid(
        const mid_vector& mids,
        const messages_vector_cb& cb);
    virtual void get_not_downloaded_messages(uint32_t num, const messages_vector_cb& cb);

    void get_mids_by_tids(const tid_vector& tids, const mid_vector_cb& cb);

    void get_flags_by_lids(const lid_vector& /*lids*/, const flags_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    virtual void delete_messages_by_id(
        const fid_t& fid,
        const imap_id_vector& ids,
        const without_data_cb& cb);
    void delete_messages_by_mid(const mid_vector& mids, const without_data_cb& cb);

    void move_messages(
        const fid_t& src_fid,
        const fid_t& dst_fid,
        tab_opt dst_tab,
        const move_coordinates_vec& coords,
        const without_data_cb& cb);

    virtual void create_folder(
        const folder& folder,
        const fid_t& fid,
        const std::string& symbol,
        const folder_vector_cb& cb);
    virtual void update_folder(
        const folder& folder,
        const fid_t_opt& new_parent,
        const without_data_cb& cb);
    virtual void delete_mailish_folder_entry(const fid_vector& fids, const without_data_cb& cb);
    virtual void delete_folder(const fid_t& fid, const without_data_cb& cb);
    virtual void clear_folder(const fid_t& fid, const without_data_cb& cb);
    void set_folder_symbol(
        const fid_t& /*fid*/,
        const std::string& /*symbol*/,
        const without_data_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    };
    void set_folders_order(
        const fid_t& /*fid*/,
        const fid_t& /*prev_fid*/,
        const without_data_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void update_flags(const fid_t& fid, mid_t mid, const flags_t flags, const without_data_cb& cb);

    void change_flags(
        const mid_vector& mids,
        const flags_t& add,
        const flags_t& del,
        const without_data_cb& cb);

    void update_downloaded_range(
        const fid_t& fid,
        const imap_range& range,
        const without_data_cb& cb);

    virtual void store_message(
        uid_t uid,
        const std::string& email,
        const message& msg,
        string&& body,
        notification_type notify_type,
        const std::string& priority,
        const store_message_response_cb& cb);

    void check_spam(
        uid_t uid,
        const karma_t& karma,
        const std::string& from,
        const std::vector<std::string>& to,
        const std::string& client_ip,
        const std::string& request_id,
        string_ptr body,
        const without_data_cb& cb);

    void get_or_create_label(
        const std::string& /*name*/,
        const std::string& /*color*/,
        const std::string& /*type*/,
        bool /*force_create*/,
        const lid_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void get_or_create_label_by_symbol(
        const std::string& /*symbol*/,
        bool /*force_create*/,
        const lid_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void update_label(
        const lid& /*lid*/,
        const std::string& /*name*/,
        const std::string& /*color*/,
        const without_data_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void delete_label(const lid& /*lid*/, const without_data_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void get_send_operation_result(const std::string& operation_id, string_opt_cb cb);

    void save_send_operation_result(
        const std::string& operation_id,
        const std::string& result,
        without_data_cb cb);

    void clear(const without_data_cb& cb);

    void erase_security_locks(const without_data_cb& cb);

    void get_account(const account_cb& cb);

    void save_account(const account_t&, const without_data_cb&)
    {
        throw std::runtime_error("not implemented");
    }

    void invalidate_auth_data(const token_id_t&, const without_data_cb&)
    {
        throw std::runtime_error("not implemented");
    }

    void init_system_folder(const folder&, const without_data_cb&);

    virtual void increment_mailish_entry_errors_count(
        const fid_t& fid,
        imap_id_t ext_msg,
        uint32_t errors_count,
        const without_data_cb& cb);

    virtual void delete_mailish_entry(
        const fid_t& fid,
        const imap_id_vector& ids,
        const without_data_cb& cb);

    void compose_draft(
        uid_t /*uid*/,
        const std::string& /*user_ticket*/,
        store_request_ptr /*request*/,
        const json_cb& cb)
    {
        json::value rcpts = Json::arrayValue;
        rcpts.append("test@domain.tld");

        json::value res;
        res["to"] = rcpts;
        res["text"] = "i am composed message";
        cb(code::ok, res);
    }

    void compose_message(
        uid_t uid,
        const std::string& user_ticket,
        send_request_ptr request,
        const json_cb& cb)
    {
        compose_draft(uid, user_ticket, request, cb);
    }

    void get_attachments_sids(
        uid_t /*uid*/,
        const std::string& /*user_ticket*/,
        mid_t /*mid*/,
        const std::vector<std::string>& /*hids*/,
        const sids_cb& /*cb*/)
    {
        throw std::runtime_error("not implemented");
    }

    void update_last_sync_ts(std::time_t /*last_sync_ts*/, const without_data_cb& cb)
    {
        cb(code::ok);
    }

    void get_karma(uid_t uid, const karma_cb& cb);

    const message_vector& get_folder_messages(const fid_t& fid) const;

    std::string dump();

    uint32_t store_errors_count() const
    {
        return store_errors_count_;
    }

    void add_message(const fid_t& fid, const message& msg);

    size_t get_folders_count()
    {
        return folders.size();
    }

    void set_account(const account_t& account);

    void cancel()
    {
    }

protected:
    void read_folder_cfg(const yplatform::ptree& folder_cfg);

    virtual void delete_message_by_id(folder& folder_, message_vector& messages, imap_id_t id);

    void delete_message_by_mid(folder& folder_, message_vector& messages, mid_t mid);
    void move_message_by_mid(
        folder& fld_from,
        message_vector& msg_from,
        folder& fld_to,
        message_vector& msg_to,
        const move_coordinates& coords);

    void update_nums(message_vector& messages);

private:
    using folder_messages_pair = std::pair<folder, message_vector>;
    using fid_folders_messages_map = std::unordered_map<fid_t, folder_messages_pair>;
    using path_fid_map = std::unordered_map<path_t, fid_t>;
    using mid_fid_map = std::unordered_map<mid_t, fid_t>;

    fid_folders_messages_map folders;
    mid_fid_map mid_fid;
    path_fid_map path_fid;
    uint32_t fid_next{ 1 };
    mid_t mid_next{ 1 };

    uint32_t store_errors_count_ = 0;
    account_t account;

public:
    std::string last_store_priority;
    std::map<std::string, std::string> operation_results;
};

}
