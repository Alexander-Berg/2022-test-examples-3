#pragma once

#include <mailbox/mailbox.h>

#include <yplatform/ptree.h>
#include <yplatform/util/uuid_generator.h>

#include <memory>

using namespace collectors;

struct fake_mailbox : public mailbox::mailbox
{
    fake_mailbox(yplatform::ptree ptree)
    {
        auto folders = ptree.get_child_optional("folders");
        if (folders)
        {
            for (const auto& folder : *folders)
            {
                folders_.push_back(read_folder(folder.second));
            }
        }

        auto labels = ptree.get_child_optional("labels");
        if (labels)
        {
            for (const auto& label : *labels)
            {
                labels_.push_back(read_label(label.second));
            }
        }

        auto messages = ptree.get_child_optional("messages");
        if (messages)
        {
            for (const auto& msg : *messages)
            {
                messages_.push_back(read_message(msg.second));
            }
        }
    }

    virtual ~fake_mailbox()
    {
    }

    void get_folders(const folders_cb& cb) override
    {
        cb(code::ok, folders_);
    }

    void get_folder_by_type(const std::string& title, const folder_cb& cb) override
    {
        for (auto& folder : folders_)
        {
            if (folder.symbol == title)
            {
                return cb({}, folder);
            }
        }
        cb(code::macs_error, {});
    }

    void get_labels(const labels_cb& cb) override
    {
        cb(code::ok, labels_);
    }

    void get_next_message_chunk(const mid& mid, uint64_t count, const messages_cb& cb) override
    {
        std::sort(messages_.begin(), messages_.end(), [](const auto& f, const auto& s) {
            return stoi(f.mid) < stoi(s.mid);
        });

        auto begin = messages_.begin();
        while (begin != messages_.end() && stoi(begin->mid) <= stoi(mid))
        {
            ++begin;
        }

        auto end = begin;
        uint64_t i = 0;
        while (end != messages_.end() && i < count)
        {
            ++end;
            ++i;
        }

        cb(code::ok, messages(begin, end));
    }

    void create_folder(const folder& new_folder, const folder_cb& cb) override
    {
        for (auto& folder : folders_)
        {
            bool found = std::tie(folder.name, folder.symbol, folder.parent_fid) ==
                std::tie(new_folder.name, new_folder.symbol, new_folder.parent_fid);
            if (found)
            {
                return cb(code::macs_error, new_folder);
            }
        }

        if (!is_empty_id(new_folder.parent_fid))
        {
            check_folder_exists(new_folder.parent_fid);
        }

        folders_.push_back(new_folder);
        folders_.back().fid = std::to_string(next_fid_++);

        cb(code::ok, folders_.back());
    }

    void create_label(const label& new_label, const label_cb& cb) override
    {
        for (auto& label : labels_)
        {
            bool found = std::tie(label.symbol, label.name, label.type) ==
                std::tie(new_label.symbol, new_label.name, new_label.type);
            if (found)
            {
                return cb(code::macs_error, new_label);
            }
        }
        labels_.push_back(new_label);
        labels_.back().lid = std::to_string(next_lid_++);

        cb(code::ok, labels_.back());
    }

    void store_message(
        const message& msg,
        const std::string& /*email*/,
        bool disable_push,
        bool /*skip_loop_prevention*/,
        const std::vector<std::string>& /*rpop_ids*/,
        const std::string& /*rpop_info*/,
        const mid_cb& cb) override
    {
        messages_.push_back(msg);
        auto new_mid = std::to_string(next_mid_++);
        mids_mapping_[new_mid] = messages_.back().mid;
        messages_.back().mid = new_mid;

        if (disable_push)
        {
            messages_wihout_push_.insert(messages_.back().mid);
        }

        cb(code::ok, new_mid);
    }

    folder read_folder(yplatform::ptree ptree)
    {
        folder res;

        auto fid = ptree.get("fid", next_fid_);
        next_fid_ = std::max(fid + 1, next_fid_);
        res.fid = std::to_string(fid);
        res.parent_fid = ptree.get("parent_fid", EMPTY_ID);
        res.symbol = ptree.get("symbol", "");
        res.name = ptree.get<std::string>("name");
        return res;
    }

    label read_label(yplatform::ptree ptree)
    {
        label res;

        auto lid = ptree.get("lid", next_lid_);
        next_lid_ = std::max(lid + 1, next_lid_);
        res.lid = std::to_string(lid);

        res.color = ptree.get("color", "green");
        res.symbol = ptree.get("symbol", "");
        res.name = ptree.get<std::string>("name");
        res.type = ptree.get("type", "user");
        return res;
    }

    message read_message(yplatform::ptree ptree)
    {
        message res;

        auto mid = ptree.get("mid", next_mid_);
        next_mid_ = std::max(mid + 1, next_mid_);
        res.mid = std::to_string(mid);

        res.fid = ptree.get<std::string>("fid");
        check_folder_exists(res.fid);

        res.stid = ptree.get("stid", yplatform::util::string_uuid_generator()());
        res.date = ptree.get("date", std::time(nullptr) - 1000);
        auto lids = ptree.get_child_optional("lids");
        if (lids)
        {
            for (const auto& lid_ptree : *lids)
            {
                auto lid = lid_ptree.second.get_value<std::string>();
                auto label_it =
                    std::find_if(labels_.begin(), labels_.end(), [lid](const auto& label) {
                        return label.lid == lid;
                    });
                REQUIRE(label_it != labels_.end());
                res.labels.push_back(*label_it);
            }
        }

        return res;
    }

    void check_folder_exists(const fid& fid)
    {
        auto it = std::find_if(folders_.begin(), folders_.end(), [fid](const auto& folder) {
            return folder.fid == fid;
        });
        REQUIRE(it != folders_.end());
    }

    folders folders_;
    labels labels_;
    messages messages_;

    std::set<mid> messages_wihout_push_;

    std::map<mid, mid> mids_mapping_;

    std::uint32_t next_fid_ = 1;
    std::uint32_t next_lid_ = 1;
    std::uint32_t next_mid_ = 1;
};

using fake_mailbox_ptr = std::shared_ptr<fake_mailbox>;

struct fake_mailbox_err_create_folder : fake_mailbox
{
    using fake_mailbox::fake_mailbox;

    void create_folder(const folder& /*new_folder*/, const folder_cb& cb) override
    {
        return cb(code::macs_error, {});
    }
};

struct fake_mailbox_err_get_folders : fake_mailbox
{
    using fake_mailbox::fake_mailbox;

    void get_folders(const folders_cb& cb) override
    {
        cb(code::macs_error, {});
    }
};

struct fake_mailbox_err_create_label : fake_mailbox
{
    using fake_mailbox::fake_mailbox;

    void create_label(const label& /*new_label*/, const label_cb& cb) override
    {
        return cb(code::macs_error, {});
    }
};

struct fake_mailbox_err_get_labels : fake_mailbox
{
    using fake_mailbox::fake_mailbox;

    void get_labels(const labels_cb& cb) override
    {
        cb(code::macs_error, {});
    }
};

struct fake_mailbox_err_store_message : fake_mailbox
{
    using fake_mailbox::fake_mailbox;

    void store_message(
        const message& /*msg*/,
        const std::string& /*email*/,
        bool /*disable_push*/,
        bool /*skip_loop_prevention*/,
        const std::vector<std::string>& /*rpop_ids*/,
        const std::string& /*rpop_info*/,
        const mid_cb& cb) override
    {
        return cb(code::macs_error, "");
    }
};

enum class fake_mailbox_type
{
    type_normal,
    type_err_create_folder,
    type_err_get_folders,
    type_err_create_label,
    type_err_get_labels,
    type_err_store_message,
};
