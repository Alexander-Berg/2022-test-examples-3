#pragma once

#include "fake_mailbox.h"
#include "fake_meta.h"
#include "fake_passport.h"

#include <streamer/sync_state.h>
#include <streamer/settings.h>
#include <streamer/operations/operation.h>

#include <yplatform/task_context.h>
#include <boost/property_tree/json_parser.hpp>

using namespace collectors;
using namespace collectors::streamer;

using environment_ptr = operations::detail::environment_ptr;

static const collector_id COLLECTOR_ID = 1;
static const uid DST_UID = "1";
static const uid SRC_UID = "2";

namespace collectors {

inline bool operator==(const folder& f, const folder& s)
{
    return std::tie(f.fid, f.parent_fid, f.name, f.symbol) ==
        std::tie(s.fid, s.parent_fid, s.name, s.symbol);
}

inline bool operator==(const label& f, const label& s)
{
    return std::tie(f.lid, f.color, f.symbol, f.name, f.type) ==
        std::tie(s.lid, s.color, s.symbol, s.name, s.type);
}

template <typename Stream>
Stream& operator<<(Stream& stream, const folder& folder)
{
    stream << "folder: fid=" << folder.fid << ", parent_fid=" << folder.parent_fid << ", name=\""
           << folder.name << "\", symbol=" << folder.symbol;
    return stream;
}

template <typename Stream>
Stream& operator<<(Stream& stream, const label& label)
{
    stream << "label: lid=" << label.lid << ", symbol=" << label.symbol << ", name=\"" << label.name
           << "\", type=" << label.type << ", color=" << label.color;
    return stream;
}

template <typename Stream>
Stream& operator<<(Stream& stream, const code& error)
{
    stream << "error enum: value=" << static_cast<int>(error);
    return stream;
}

}

inline fake_mailbox_ptr make_mailbox(
    const std::string& path = "",
    fake_mailbox_type type = fake_mailbox_type::type_normal)
{
    yplatform::ptree ptree;
    if (path.size())
    {
        boost::property_tree::read_json(path, ptree);
    }
    switch (type)
    {
    case fake_mailbox_type::type_err_create_folder:
        return std::make_shared<fake_mailbox_err_create_folder>(ptree);
    case fake_mailbox_type::type_err_get_folders:
        return std::make_shared<fake_mailbox_err_get_folders>(ptree);
    case fake_mailbox_type::type_err_create_label:
        return std::make_shared<fake_mailbox_err_create_label>(ptree);
    case fake_mailbox_type::type_err_get_labels:
        return std::make_shared<fake_mailbox_err_get_labels>(ptree);
    case fake_mailbox_type::type_err_store_message:
        return std::make_shared<fake_mailbox_err_store_message>(ptree);
    default:
        return std::make_shared<fake_mailbox>(ptree);
    }
}

inline fake_passport_ptr make_passport(fake_passport_type type = fake_passport_type::type_normal)
{
    switch (type)
    {
    case fake_passport_type::type_err_get_userinfo:
        return std::make_shared<fake_passport_err_get_userinfo>();
    case fake_passport_type::type_internal_err_check_auth_token:
        return std::make_shared<fake_passport_internal_err_check_auth_token>();
    case fake_passport_type::type_err_invalid_auth_token:
        return std::make_shared<fake_passport_err_invalid_auth_token>();
    case fake_passport_type::type_err_alias_operations:
        return std::make_shared<fake_passport_err_alias_operations>();
    default:
        return std::make_shared<fake_passport>();
    }
}

inline meta::repository_ptr make_meta(
    boost::asio::io_context* io,
    const fid& root_folder_id = EMPTY_ID,
    const lid& label_id = EMPTY_ID,
    fake_meta_type type = fake_meta_type::type_normal)
{
    auto meta_data = std::make_shared<collector_info>(COLLECTOR_ID, DST_UID, SRC_UID);
    meta_data->root_folder_id = root_folder_id;
    meta_data->label_id = label_id;
    meta_data->last_mid = EMPTY_ID;
    switch (type)
    {
    case fake_meta_type::type_err_update_skipped_mids:
        return std::make_shared<fake_meta_err_update_skipped_mids>(meta_data, io);
    case fake_meta_type::type_err_reset_token:
        return std::make_shared<fake_meta_err_reset_token>(meta_data, io);
    case fake_meta_type::type_err_update_state:
        return std::make_shared<fake_meta_err_update_state>(meta_data, io);
    case fake_meta_type::type_err_delete_collector:
        return std::make_shared<fake_meta_err_delete_collector>(meta_data, io);
    default:
        return std::make_shared<fake_meta>(meta_data, io);
    }
}

inline environment_ptr make_default_env(boost::asio::io_context* io)
{
    auto res = std::make_shared<operations::detail::environment>();
    res->context = boost::make_shared<yplatform::task_context>();
    res->meta = make_meta(io);
    res->passport = std::make_shared<fake_passport>();
    res->state =
        std::make_shared<streamer::sync_state>(EMPTY_ID, std::vector<std::string>{ EMPTY_ID });
    res->settings = std::make_shared<streamer_settings>();
    res->io = io;
    return res;
}

template <typename Op, typename... Args>
error run_op(environment_ptr env, Args... args)
{
    error saved_ec = code::operation_exception; // in case of handler never called
    auto op = std::make_shared<Op>(env, [&saved_ec](auto ec, auto&&... /*res*/) { saved_ec = ec; });
    yplatform::spawn(env->io->get_executor(), op, std::forward<Args>(args)...);

    env->io->reset();
    env->io->run();
    return saved_ec;
}

template <typename... Args>
class callback
{
    using args_t = std::tuple<Args...>;

public:
    void operator()(const Args&... args)
    {
        *called_ = true;
        *args_ = std::tuple{ args... };
    }

    const auto& args()
    {
        REQUIRE(called());
        return *args_;
    }

    bool called()
    {
        return *called_;
    }

    void reset()
    {
        *called_ = false;
    }

private:
    std::shared_ptr<bool> called_ = std::make_shared<bool>(false);
    std::shared_ptr<args_t> args_ = std::make_shared<args_t>();
};
