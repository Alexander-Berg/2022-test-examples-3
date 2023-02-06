#pragma once

#include <streamer/meta/repository.h>

using meta_repo = collectors::streamer::meta::repository;

struct fake_meta : public meta_repo
{
    using meta_repo::meta_repo;

    virtual ~fake_meta()
    {
    }

    void update_last_mid(const mid& last_mid, const no_data_cb& cb) override
    {
        collector_info->last_mid = last_mid;
        cb(code::ok);
    }

    void update_skipped_mids(const mids& skipped_mids, const no_data_cb& cb) override
    {
        collector_info->skipped_mids = skipped_mids;
        cb(code::ok);
    }

    void reset_token(const no_data_cb& cb) override
    {
        collector_info->auth_token = "";
        cb(code::ok);
    }

    void edit(
        const std::optional<std::string>& auth_token,
        const std::optional<fid>& root_folder_id,
        const std::optional<lid>& label_id,
        const no_data_cb& cb) override
    {
        collector_info->auth_token = auth_token ? *auth_token : collector_info->auth_token;
        collector_info->root_folder_id =
            root_folder_id ? *root_folder_id : collector_info->root_folder_id;
        collector_info->label_id = label_id ? *label_id : collector_info->label_id;
        cb(code::ok);
    }

    void update_state(collector_state state, const no_data_cb& cb) override
    {
        collector_info->state = state;
        cb(code::ok);
    }

    void update_migration_target_state(collector_state state, const no_data_cb& cb) override
    {
        collector_info->migration_target_state = state;
        cb(code::ok);
    }

    void reset_collector(
        const std::string& /*auth_token*/,
        const fid& /*root_folder_id*/,
        const lid& /*label_id*/,
        const no_data_cb& cb) override
    {
        cb(code::macs_error);
    }

    void delete_collector(const no_data_cb& cb) override
    {
        cb(code::ok);
    }

    void update_last_run_ts(std::time_t ts) override
    {
        collector_info->last_run_ts = ts;
    }

    void set_creation_ts(std::time_t value)
    {
        collector_info->creation_ts = value;
    }

    void set_root_folder(const fid& root_fid)
    {
        collector_info->root_folder_id = root_fid;
    }

    void set_ignore_folders_struct(bool ignore)
    {
        collector_info->ignore_folders_struct = ignore;
    }
};

using fake_meta_ptr = std::shared_ptr<fake_meta>;

struct fake_meta_err_update_skipped_mids : fake_meta
{
    using fake_meta::fake_meta;

    void update_skipped_mids(const mids& /*skipped_mids*/, const no_data_cb& cb) override
    {
        cb(code::macs_error);
    }
};

struct fake_meta_err_reset_token : fake_meta
{
    using fake_meta::fake_meta;

    void reset_token(const no_data_cb& cb) override
    {
        cb(code::macs_error);
    }
};

struct fake_meta_err_delete_collector : fake_meta
{
    using fake_meta::fake_meta;

    void delete_collector(const no_data_cb& cb) override
    {
        cb(code::macs_error);
    }
};

struct fake_meta_err_update_state : fake_meta
{
    using fake_meta::fake_meta;

    void update_state(collector_state /*state*/, const no_data_cb& cb) override
    {
        cb(code::macs_error);
    }
};

enum class fake_meta_type
{
    type_normal,
    type_err_update_skipped_mids,
    type_err_reset_token,
    type_err_delete_collector,
    type_err_update_state
};
