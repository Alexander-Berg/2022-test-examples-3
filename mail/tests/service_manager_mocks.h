#pragma once

#include "service_manager/impl.h"
#include <catch.hpp>

using namespace yxiva;
using namespace ymod_xconf;

class fake_xconf : public local_conf_storage
{
    conf_list_ptr conf_;
    size_t req_count_;
    no_error_conf_list_handler_t handler_;

public:
    fake_xconf(const conf_list_ptr& conf)
        : conf_(conf ? conf : std::make_shared<conf_list>()), req_count_(0)
    {
    }

    void subscribe_updates_impl(config_type, const string&, no_error_conf_list_handler_t&& h)
        override
    {
        handler_ = std::move(h);
    }

    void put_impl(
        config_type type,
        const string& env,
        const string& name,
        const string& owner,
        const string& token,
        ymod_xconf::revision_t /*revision*/,
        string_ptr value,
        const put_handler_t& handler,
        task_context_ptr /*ctx*/) override
    {
        conf_->max_revision++;
        item i;
        std::tie(i.type, i.environment, i.name, i.owner, i.token, i.configuration, i.revision) =
            std::tie(type, env, name, owner, token, *value, conf_->max_revision);
        conf_->items.push_back(i);
        handler(error::success, conf_->max_revision);
    }

    void list_impl(
        config_type,
        const string&,
        revision_t /*r*/,
        const conf_list_handler_t& h,
        task_context_ptr) override
    {
        ++req_count_;
        h(error::success, conf_);
    }

    void test_do_update()
    {
        if (handler_)
        {
            handler_(conf_);
        }
    }

    conf_list_ptr test_list()
    {
        return conf_;
    }

    void reset_count()
    {
        req_count_ = 0;
    }

    size_t get_count()
    {
        return req_count_;
    }
};

class service_manager_mock : public service_manager_impl
{
    std::shared_ptr<boost::asio::io_service> io_;
    boost::shared_ptr<fake_xconf> xconf_;

public:
    service_manager_mock(
        std::shared_ptr<boost::asio::io_service> io,
        boost::shared_ptr<fake_xconf> xconf,
        const string& environment)
        : service_manager_impl(xconf, *io, environment), io_(io), xconf_(xconf)
    {
    }

    static std::shared_ptr<service_manager_mock> create_manager(
        const conf_list_ptr conf = nullptr,
        const string& environment = "sandbox")
    {
        auto xconf = boost::make_shared<fake_xconf>(conf);
        auto manager = std::make_shared<service_manager_mock>(
            std::make_shared<boost::asio::io_service>(), xconf, environment);
        return manager;
    }

    boost::shared_ptr<fake_xconf> get_xconf()
    {
        return xconf_;
    }

    void run()
    {
        io_->run();
        io_->reset();
    }

    void add_conf(std::vector<item> items)
    {
        auto conf = std::make_shared<conf_list>();
        for (auto& item : items)
        {
            if (item.revision > conf->max_revision)
            {
                conf->max_revision = item.revision;
            }
        }
        std::swap(conf->items, items);
        load_configs(conf);
    }

    template <typename Handler>
    void test_schedule_xconf_update(Handler&& h)
    {
        schedule_xconf_update(std::forward<Handler>(h));
    }

    std::pair<operation::result, services_type> test_find_services_by_owner(const string& owner)
    {
        services_type svc;
        std::shared_ptr<operation::result> res;
        bool callback_received = false;
        find_services_by_owner(
            boost::make_shared<task_context>(),
            owner,
            [&svc, &res, &callback_received](const operation::result& r, services_type s) {
                callback_received = true;
                std::swap(svc, s);
                res = std::make_shared<operation::result>(r.error_reason);
            });

        run();

        CHECK(callback_received);
        return std::make_pair(res ? *res : "cb not called", svc);
    }
};

struct service_properties_builder
{
    service_properties props;

    auto build() const
    {
        return props;
    }

    auto& owner_id(const string& v)
    {
        props.owner_id = v;
        return *this;
    }
    auto& name(const string& v)
    {
        props.name = v;
        return *this;
    }
    auto& is_stream(const bool& v)
    {
        props.is_stream = v;
        return *this;
    }
    auto& tvm_publishers(const std::map<string, std::set<tvm_app_info>>& v)
    {
        props.tvm_publishers = v;
        return *this;
    }
    auto& tvm_subscribers(const std::map<string, std::set<tvm_app_info>>& v)
    {
        props.tvm_subscribers = v;
        return *this;
    }
};

class item_builder
{
    item conf_item;
    size_t revision = 0;

    config_type type_from_data(const service_properties&)
    {
        return config_type::SERVICE;
    }
    config_type type_from_data(const send_token_properties&)
    {
        return config_type::SEND_TOKEN;
    }
    config_type type_from_data(const listen_token_properties&)
    {
        return config_type::LISTEN_TOKEN;
    }
    config_type type_from_data(const application_config&)
    {
        return config_type::MOBILE;
    }

    string env_from_data(const service_properties&)
    {
        return string{};
    }
    string env_from_data(const send_token_properties&)
    {
        return get_environment_name(config_environment::SANDBOX);
    }
    string env_from_data(const listen_token_properties&)
    {
        return get_environment_name(config_environment::SANDBOX);
    }
    string env_from_data(const application_config&)
    {
        return string{};
    }

public:
    item build()
    {
        // Legal xconf revisions start from 1.
        conf_item.revision = ++revision;
        return conf_item;
    }

    item_builder& type(config_type type)
    {
        conf_item.type = type;
        return *this;
    }

    item_builder& env(string env)
    {
        conf_item.environment = std::move(env);
        return *this;
    }

    template <typename DataType>
    item_builder& data(const DataType& data)
    {
        conf_item.configuration = pack(data);
        type(type_from_data(data));
        env(env_from_data(data));
        return *this;
    }

    item_builder& ltoken_data(
        const string& service,
        const string& name,
        bool revoked = false,
        const string& client = "",
        const std::set<string>& allowed_services = {})
    {
        listen_token_properties d;
        d.service = service;
        d.name = name;
        d.revoked = revoked;
        d.client = client;
        d.allowed_services = allowed_services;
        return owner(service).name(service + ":" + name).data(d);
    }

    item_builder& stoken_data(const string& service, const string& name, bool revoked = false)
    {
        send_token_properties d;
        d.service = service;
        d.name = name;
        d.revoked = revoked;
        return owner(service).name(service + ":" + name).data(d);
    }

    item_builder& svc_data(
        const string& service,
        const string& full_owner,
        bool revoked = false,
        bool is_stream = false)
    {
        service_properties data;
        data.name = service;
        data.owner_id = full_owner;
        data.revoked = revoked;
        data.is_stream = is_stream;
        return owner(full_owner).name(service).data(data);
    }

    item_builder& svc_data(const service_properties& props)
    {
        return owner(props.owner_id).name(props.name).data(props);
    }

    item_builder& name(const string& name)
    {
        conf_item.name = name;
        return *this;
    }

    item_builder& owner(const string& owner)
    {
        conf_item.owner = owner;
        return *this;
    }

    item_builder& token(const string& token)
    {
        conf_item.token = token;
        return *this;
    }
};