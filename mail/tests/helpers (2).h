#pragma once

#include <mailpusher/errors.h>
#include <mailpusher/task.h>
#include <mailpusher/notification.h>
#include <processor/format/destinations.h>
#include <yxiva/core/platforms.h>
#include <ymod_httpclient/cluster_client.h>
#include <ymod_ratecontroller/rate_controller.h>
#include <algorithm>
#include <functional>
#include <vector>
#include <string>

using namespace yxiva;
using namespace mailpusher;
using rc_error = ymod_ratecontroller::error;

struct fake_http_client
{
    struct clinet_impl
    {
        clinet_impl() = default;

        clinet_impl(const std::vector<yhttp::errc> errors)
        {
            responses.reserve(errors.size());
            for (auto e : errors)
            {
                responses.push_back({ e, {} });
            }
        }

        clinet_impl(const std::vector<yhttp::response> resps)
        {
            responses.reserve(resps.size());
            for (auto& r : resps)
            {
                responses.push_back({ yhttp::errc::success, r });
            }
        }

        template <typename Handler>
        void async_run(yhttp::request req, Handler&& handler)
        {
            requests.push_back(std::move(req));
            if (resp_id < responses.size())
            {
                auto& [err, resp] = responses[resp_id++];
                handler(make_error_code(err), resp);
            }
            else
            {
                handler(make_error_code(yhttp::errc::connect_error), yhttp::response{});
            }
        }

        std::vector<yhttp::request> requests;
        std::vector<std::pair<yhttp::errc, yhttp::response>> responses;
        size_t resp_id = 0;
    };

    fake_http_client() : impl(std::make_shared<clinet_impl>())
    {
    }

    fake_http_client(const std::vector<yhttp::errc> errors)
        : impl(std::make_shared<clinet_impl>(errors))
    {
    }

    fake_http_client(const std::vector<yhttp::response> resps)
        : impl(std::make_shared<clinet_impl>(resps))
    {
    }

    template <typename Handler>
    void async_run(yplatform::task_context_ptr /*ctx*/, yhttp::request req, Handler&& handler)
    {
        impl->async_run(req, handler);
    }

    const std::vector<yhttp::request>& requests() const
    {
        return impl->requests;
    }

    std::shared_ptr<clinet_impl> impl;
};

struct fake_rate_controller : ymod_ratecontroller::rate_controller
{
    fake_rate_controller() = default;
    fake_rate_controller(std::vector<ymod_ratecontroller::error> r) : results(std::move(r))
    {
    }

    std::vector<ymod_ratecontroller::error> results;
    size_t current_task = 0;
    size_t total_tasks = 0;
    size_t complete_tasks = 0;
    std::function<void()> cb = [this]() { ++complete_tasks; };

    void post(
        const task_type& task,
        const string& /*task_id*/,
        const yplatform::time_traits::time_point& /*deadline*/) override
    {
        ++total_tasks;
        task(
            make_error_code(current_task < results.size() ? results[current_task++] : rc_error::ok),
            cb);
    }

    void cancel(const string& /*task_id*/) override
    {
    }

    std::size_t max_concurrency() const override
    {
        return 1;
    }

    std::size_t queue_size() const override
    {
        return 0;
    }

    std::size_t running_tasks_count() const override
    {
        return 0;
    }
};

struct fake_rate_controller_module : ymod_ratecontroller::rate_controller_module
{
    using rc = ymod_ratecontroller::rate_controller;

    virtual std::shared_ptr<rc> get_controller(const std::string& path) override
    {
        if (!rc_map.count(path))
        {
            rc_map[path] = std::make_shared<fake_rate_controller>();
        }
        return rc_map[path];
    }

    std::map<std::string, std::shared_ptr<fake_rate_controller>> rc_map;
};

inline std::vector<error_code> to_error_codes(const std::vector<error::code>& errs)
{
    std::vector<error_code> error_codes;
    error_codes.reserve(errs.size());
    std::transform(errs.begin(), errs.end(), std::back_inserter(error_codes), error::make_error);
    return error_codes;
}

inline event make_event(action a, const std::string& item, const std::string& args = "{}")
{
    event e;
    e.action_type = a;
    e.ts = std::time(nullptr);
    auto json = json_parse(item, json_type::tarray);
    for (auto&& val : json.array_items())
    {
        e.items.push_back(val);
    }
    e.args = json_parse(args);
    return e;
}

inline std::string dump(const event& e)
{
    json_value val(json_type::tarray);
    for (auto& item : e.items)
    {
        val.push_back(item);
    }
    return json_write(val);
}

inline auto make_task()
{
    return make_shared<task>(yplatform::task_context{ "ctx_id" });
}

template <typename Impl>
class common_builder
{
public:
    common_builder(struct subscription::common& sub) : sub_(sub)
    {
    }
    auto& id(const string& v)
    {
        sub_.id = v;
        return static_cast<Impl&>(*this);
    }
    auto& extra(const string& v)
    {
        sub_.extra = v;
        return static_cast<Impl&>(*this);
    }
    auto& client(const string& v)
    {
        sub_.client = v;
        return static_cast<Impl&>(*this);
    }
    auto& filter(const string& v)
    {
        sub_.filter = v;
        return static_cast<Impl&>(*this);
    }

private:
    struct subscription::common& sub_;
};

class mobile_builder : public common_builder<mobile_builder>
{
public:
    mobile_builder() : common_builder(mobile_)
    {
        mobile_.id = "mob:id";
        mobile_.client = "test_mobile_client";
        mobile_.session = "test_mobile_session";
        mobile_.ttl = 100500;
        mobile_.app = "ru.yandex.mail";
        mobile_.uuid = "test_mobile_session";
        mobile_.platform = "fcm";
        mobile_.device = "some_sung";
    }

    subscription build()
    {
        return subscription(mobile_);
    }
    auto& platform(const string& v)
    {
        mobile_.platform = v;
        return *this;
    }
    auto& device(const string& v)
    {
        mobile_.device = v;
        return *this;
    }
    auto& app(const string& v)
    {
        mobile_.app = v;
        return *this;
    }

private:
    struct subscription::mobile mobile_;
};

class http_builder : public common_builder<http_builder>
{
public:
    http_builder() : common_builder(http_)
    {
        http_.id = "id";
        http_.client = "test_http_client";
        http_.session = "test_http_session";
        http_.ttl = 100;
        http_.url = "http://something";
    }

    subscription build()
    {
        return subscription(http_);
    }
    auto& url(const string& v)
    {
        http_.url = v;
        return *this;
    }

private:
    struct subscription::http http_;
};

class webpush_builder : public common_builder<webpush_builder>
{
public:
    webpush_builder() : common_builder(webpush_)
    {
        webpush_.id = "id";
        webpush_.client = "test_webpush_client";
        webpush_.session = "test_webpush_session";
        webpush_.ttl = 1050;
    }

    subscription build()
    {
        return subscription(webpush_);
    }

private:
    struct subscription::webpush webpush_;
};

inline auto make_http_clients(fake_http_client& fake_http, string environment = "a")
{
    http_clients<fake_http_client> http_clients;
    http_clients.emplace(environment, fake_http);
    return http_clients;
}

inline auto make_rate_controllers(
    std::shared_ptr<fake_rate_controller> fake_rc,
    string environment = "a")
{
    rate_controllers<ymod_ratecontroller::rate_controller_ptr> rate_controllers;
    rate_controllers.emplace(environment, fake_rc);
    return rate_controllers;
}