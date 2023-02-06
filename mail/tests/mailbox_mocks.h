#pragma once

#include "external_mailbox_mock.h"
#include "local_mailbox_mock.h"

#include <src/xeno/rc/external_mailbox.h>
#include <src/xeno/rc/local_mailbox.h>
#include <src/common/types.h>

#include <ymod_ratecontroller/rate_controller.h>
#include <pgg/error.h>

#include <boost/property_tree/json_parser.hpp>

namespace core = xeno;
namespace mb = xeno::mailbox;
namespace ext_mb = mb::external;
namespace loc_mb = mb::local;
using code = xeno::code;
using error = xeno::error;

static const std::string EXTERNAL_MAILBOX_PATH{ "input_data/external_mailbox.json" };
static const std::string EXTERNAL_MAILBOX_WITH_TOP_DELETED_PATH{
    "input_data/external_mailbox_with_top_deleted.json"
};
static const std::string EXTERNAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH{
    "input_data/external_mailbox_for_redownload_messages_tests.json"
};
static const std::string EXTERNAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH{
    "input_data/external_mailbox_for_sync_oldest_flags_and_deleted_tests.json"
};
static const std::string EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH{
    "input_data/external_mailbox_for_turbo_sync.json"
};
static const std::string EXTERNAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS{
    "input_data/external_mailbox_for_changing_folder_type_tests.json"
};
static const std::string LOCAL_MAILBOX_PATH{ "input_data/local_mailbox.json" };
static const std::string LOCAL_MAILBOX_FOR_LOAD_FOLDER_TOP_TESTS_PATH{
    "input_data/local_mailbox_for_load_folder_top_tests.json"
};
static const std::string LOCAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH{
    "input_data/local_mailbox_for_redownload_messages_tests.json"
};
static const std::string LOCAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH{
    "input_data/local_mailbox_for_sync_oldest_flags_and_deleted_tests.json"
};
static const std::string LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH{
    "input_data/local_mailbox_for_turbo_sync.json"
};
static const std::string LOCAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS{
    "input_data/local_mailbox_for_changing_folder_type_tests.json"
};

static const int MAX_MESSAGE_SIZE = 50;

class loc_mailbox_err_store : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_store(const yplatform::ptree& cfg) : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void store_message(
        xeno::uid_t /*uid*/,
        const std::string& /*email*/,
        const mb::message& /*msg*/,
        std::string&& /*body*/,
        mb::notification_type /*notify_type*/,
        const std::string& /*priority*/,
        const mb::local::store_message_response_cb& cb) override
    {
        cb(code::store_message_error, mb::local::store_message_response());
    }
};

class loc_mailbox_err_delete : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_delete(const yplatform::ptree& cfg) : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void delete_messages_by_id(
        const mb::fid_t&,
        const mb::imap_id_vector&,
        const xeno::without_data_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
};

class loc_mailbox_err_increment_mailish_errors : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_increment_mailish_errors(const yplatform::ptree& cfg)
        : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void increment_mailish_entry_errors_count(
        const mb::fid_t&,
        mb::imap_id_t,
        uint32_t,
        const xeno::without_data_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
};

class loc_mailbox_err_get_not_downloaded_messages : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_get_not_downloaded_messages(const yplatform::ptree& cfg)
        : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void get_not_downloaded_messages(uint32_t /*num*/, const mb::messages_vector_cb& cb)
    {
        cb(code::local_mailbox_exception, mb::message_vector_ptr());
    }
};

class loc_mailbox_err_delete_mailish_entry : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_delete_mailish_entry(const yplatform::ptree& cfg)
        : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void delete_mailish_entry(
        const mb::fid_t& /*fid*/,
        const mb::imap_id_vector& /*ids*/,
        const xeno::without_data_cb& cb)
    {
        cb(code::local_mailbox_exception);
    }
};

class loc_mailbox_err_folder_operations : public loc_mb::loc_mailbox_mock
{
public:
    loc_mailbox_err_folder_operations(const yplatform::ptree& cfg) : loc_mb::loc_mailbox_mock(cfg)
    {
    }

    void create_folder(
        const mb::folder& /*folder*/,
        const mb::fid_t& /*fid*/,
        const std::string& /*symbol*/,
        const mb::folder_vector_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction),
           mb::folder_vector_ptr());
    }
    void update_folder(
        const mb::folder& /*folder*/,
        const mb::fid_t_opt& /*new_parent*/,
        const xeno::without_data_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
    void delete_folder(const mb::fid_t& /*fid*/, const xeno::without_data_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }

    void clear_folder(const mb::fid_t& /*fid*/, const xeno::without_data_cb& cb) override
    {
        cb(pgg::error::make_error_code(pgg::error::SqlErrors::read_only_sql_transaction));
    }
};

enum class local_mock_type
{
    type_normal,
    type_err_store,
    type_err_increment_mailish_errors,
    type_err_del,
    type_err_get_not_downloaded_messages,
    type_err_delete_mailish_entry,
    type_err_folder_operations,
};

inline loc_mb::loc_mailbox_mock_ptr create_from_json(const std::string& path, local_mock_type type)
{
    yplatform::ptree cfg;
    boost::property_tree::read_json(path, cfg);
    switch (type)
    {
    case local_mock_type::type_err_del:
        return std::make_shared<loc_mailbox_err_delete>(cfg);
    case local_mock_type::type_err_store:
        return std::make_shared<loc_mailbox_err_store>(cfg);
    case local_mock_type::type_err_increment_mailish_errors:
        return std::make_shared<loc_mailbox_err_increment_mailish_errors>(cfg);
    case local_mock_type::type_err_folder_operations:
        return std::make_shared<loc_mailbox_err_folder_operations>(cfg);
    case local_mock_type::type_err_get_not_downloaded_messages:
        return std::make_shared<loc_mailbox_err_get_not_downloaded_messages>(cfg);
    case local_mock_type::type_err_delete_mailish_entry:
        return std::make_shared<loc_mailbox_err_delete_mailish_entry>(cfg);
    default:
        return std::make_shared<loc_mb::loc_mailbox_mock>(cfg);
    }
}

class ext_mailbox_err_get_body : public ext_mb::ext_mailbox_mock
{
public:
    ext_mailbox_err_get_body(const yplatform::ptree& cfg) : ext_mb::ext_mailbox_mock(cfg)
    {
    }

    void get_message_body(
        const mb::path_t& /*path*/,
        mb::imap_id_t /*id*/,
        const mb::message_body_cb& cb) override
    {
        cb(code::imap_response_no, mb::string_ptr());
    }
};

class ext_mailbox_err_folder_operations : public ext_mb::ext_mailbox_mock
{
public:
    ext_mailbox_err_folder_operations(const yplatform::ptree& cfg) : ext_mb::ext_mailbox_mock(cfg)
    {
    }

    void create_folder(const mb::path_t& /*path*/, const xeno::without_data_cb& cb) override
    {
        cb(code::imap_response_no);
    }

    void rename_folder(
        const mb::path_t& /*old*/,
        const mb::path_t& /*new_*/,
        const xeno::without_data_cb& cb) override
    {
        cb(code::imap_response_no);
    }

    void delete_folder(const mb::path_t& /*path*/, const xeno::without_data_cb& cb) override
    {
        cb(code::imap_response_no);
    }

    void clear_folder(const mb::path_t& /*path*/, const xeno::without_data_cb& cb) override
    {
        cb(code::imap_response_no);
    }
};

enum class external_mock_type
{
    type_normal,
    type_err_download_body,
    type_err_folder_operations,
};

inline ext_mb::ext_mailbox_mock_ptr create_from_json(
    const std::string& path,
    external_mock_type type)
{
    yplatform::ptree cfg;
    boost::property_tree::read_json(path, cfg);
    switch (type)
    {
    case external_mock_type::type_err_download_body:
        return std::make_shared<ext_mailbox_err_get_body>(cfg);
    case external_mock_type::type_err_folder_operations:
        return std::make_shared<ext_mailbox_err_folder_operations>(cfg);
    default:
        return std::make_shared<ext_mb::ext_mailbox_mock>(cfg);
    }
}
