#include <ymod_xconf/xconf.h>
#include <ymod_xconf/http_client.h>
#include <ymod_httpclient/errors.h>
#include "gmock/gmock.h"
#include "gtest/gtest.h"

using ::testing::Return;
using ::testing::Throw;
using ::testing::_;

using namespace ymod_xconf;
using namespace ymod_http_client;
using namespace std;

class mock_http_client : public yhttp::call
{
public:
    response run(task_context_ptr ctx, request req)
    {
        throw std::runtime_error("not implemented");
    }

    void async_run(task_context_ptr ctx, request req, callback_type callback)
    {
        throw std::runtime_error("not implemented");
    }

    response run(task_context_ptr ctx, request req, const yhttp::options&)
    {
        throw std::runtime_error("not implemented");
    }

    void async_run(task_context_ptr ctx, request req, const yhttp::options&, callback_type callback)
    {
        throw std::runtime_error("not implemented");
    }

    future_void_t get_url(
        yplatform::task_context_ptr context,
        response_handler_ptr handler,
        const remote_point_info_ptr host,
        const string& req,
        const string& headers)
    {
        return result_from_errcode(get_url_call_impl(req, headers));
    }

    future_void_t post_url(
        yplatform::task_context_ptr context,
        response_handler_ptr handler,
        const remote_point_info_ptr host,
        const string& req,
        const string_ptr& post,
        const string& headers,
        bool log_post_args)
    {
        return result_from_errcode(post_url_call_impl(req, *post, headers));
    }

    future_void_t mpost_url(
        yplatform::task_context_ptr context,
        response_handler_ptr handler,
        const remote_point_info_ptr host,
        const string& req,
        const std::list<post_chunk_ptr>& post,
        const string& headers,
        bool log_post_args)
    {
        return result_from_errcode(make_call_impl());
    }

    future_void_t head_url(
        yplatform::task_context_ptr context,
        response_handler_ptr handler,
        const remote_point_info_ptr host,
        const string& req,
        const string& headers) override
    {
        return result_from_errcode(make_call_impl());
    }

    MOCK_METHOD(remote_point_info_ptr, make_rm_info, (const string& host), (override));
    MOCK_METHOD(
        remote_point_info_ptr,
        make_rm_info,
        (const string& host, const timeouts& timeouts),
        (override));
    MOCK_METHOD(
        remote_point_info_ptr,
        make_rm_info,
        (const string& host, bool reuse_connection),
        (override));
    MOCK_METHOD(
        remote_point_info_ptr,
        make_rm_info,
        (const string& host, const timeouts& timeouts, bool reuse_connection),
        (override));

    // private:
    MOCK_METHOD(
        http_error::code,
        get_url_call_impl,
        (const string& req, const string& headers),
        (override));
    MOCK_METHOD(
        http_error::code,
        post_url_call_impl,
        (const string& req, const string& body, const string& headers),
        (override));
    MOCK_METHOD(http_error::code, make_call_impl, (void), (override));

private:
    future_void_t result_from_errcode(http_error::code code)
    {
        yplatform::future::promise<void> prom;
        if (code == http_error::code::success)
        {
            prom.set();
        }
        else
        {
            prom.set_exception(std::runtime_error("smth"));
        }
        return prom;
    }
};

TEST(xconfHttpImpl, InitOk)
{
    boost::shared_ptr<mock_http_client> mclient(new mock_http_client);
    EXPECT_CALL(*mclient, make_rm_info("", true));
    ASSERT_NO_THROW(http_client(mclient, ""));
}

TEST(xconfHttpImpl, InitError)
{
    boost::shared_ptr<mock_http_client> mclient(new mock_http_client);
    ON_CALL(*mclient, make_rm_info("", true))
        .WillByDefault(Throw(std::runtime_error("some bad http client config")));
    ASSERT_THROW((http_client(mclient, "")), std::runtime_error);
}

struct t_http_client
{
    t_http_client()
    {
        mclient.reset(new mock_http_client);
        EXPECT_CALL(*mclient, make_rm_info("", true));
        xconf.reset(new http_client(mclient, ""));
    }

    MOCK_METHOD(void, on_put, (const error::error_code& ec, revision_t), (override));
    MOCK_METHOD(void, on_list, (const error::error_code& ec, conf_list_ptr), (override));

    boost::shared_ptr<mock_http_client> mclient;
    std::unique_ptr<ymod_xconf::xconf> xconf;
};

TEST(xconfHttpImpl, PutOk)
{
    t_http_client t;
    auto data = boost::make_shared<string>("value");
    EXPECT_CALL(
        *t.mclient,
        post_url_call_impl("put?name=A&type=mobile&owner=B&token=C&environment=", *data, ""));
    t.xconf->put(
        config_type::MOBILE,
        "A",
        "B",
        "C",
        data,
        [](const error::error_code&, revision_t) {},
        yplatform::task_context_ptr());
}

TEST(xconfHttpImpl, PutOkEscapeArgs)
{
    t_http_client t;
    auto data = boost::make_shared<string>("value::..//&&");
    EXPECT_CALL(
        *t.mclient,
        post_url_call_impl(
            "put?name=A%3aa.A&type=mobile&owner=B.b%3aB&token=C%26C&environment=",
            "value::..//&&",
            ""));
    t.xconf->put(
        config_type::MOBILE,
        "A:a.A",
        "B.b:B",
        "C&C",
        data,
        [](const error::error_code&, revision_t) {},
        yplatform::task_context_ptr());
}

TEST(xconfHttpImpl, PutMissingArgs)
{
    t_http_client t;
    auto data = boost::make_shared<string>();
    auto callback = boost::bind(&t_http_client::on_put, &t, _1, _2);
    EXPECT_CALL(t, on_put(error::invalid_arg, INITIAL_REVISION)).Times(3);
    t.xconf->put(
        config_type::MOBILE, "", "owner", "", data, callback, yplatform::task_context_ptr());
    t.xconf->put(
        config_type::MOBILE, "name", "", "", data, callback, yplatform::task_context_ptr());
    t.xconf->put(
        config_type::MOBILE,
        "name",
        "owner",
        "",
        string_ptr(),
        callback,
        yplatform::task_context_ptr());
}

TEST(xconfHttpImpl, PutHandlerThrows)
{
    t_http_client t;
    auto data = boost::make_shared<string>();

    // check bag args error-handler
    ASSERT_NO_THROW(t.xconf->put(
        config_type::MOBILE,
        "",
        "owner",
        "",
        data,
        [](const error::error_code&, revision_t) { throw int{ 10 }; },
        yplatform::task_context_ptr()));

    EXPECT_CALL(*t.mclient, post_url_call_impl(_, _, _))
        .WillOnce(Return(http_error::success))
        .WillOnce(Return(http_error::connect_error));
    // check success-handler
    ASSERT_NO_THROW(t.xconf->put(
        config_type::MOBILE,
        "name",
        "owner",
        "",
        data,
        [](const error::error_code&, revision_t) { throw int{ 10 }; },
        yplatform::task_context_ptr()));
    // check error-handler
    ASSERT_NO_THROW(t.xconf->put(
        config_type::MOBILE,
        "name",
        "owner",
        "",
        data,
        [](const error::error_code&, revision_t) { throw int{ 10 }; },
        yplatform::task_context_ptr()));
}

TEST(xconfHttpImpl, ListHandlerThrows)
{
    t_http_client t;
    EXPECT_CALL(*t.mclient, get_url_call_impl(_, _))
        .WillOnce(Return(http_error::success))
        .WillOnce(Return(http_error::connect_error));

    // check success-handler
    ASSERT_NO_THROW(t.xconf->list(
        config_type::MOBILE,
        0,
        [](const error::error_code&, conf_list_ptr) { throw int{ 10 }; },
        yplatform::task_context_ptr()));
    // check error-handler
    ASSERT_NO_THROW(t.xconf->list(
        config_type::MOBILE,
        0,
        [](const error::error_code&, conf_list_ptr) { throw int{ 10 }; },
        yplatform::task_context_ptr()));
}

TEST(xconfHttpImpl, PutAnyNotAllowed)
{
    t_http_client t;
    testing::MockFunction<void(const error::error_code&, revision_t)> mock_cb;

    auto data = boost::make_shared<string>("value");
    EXPECT_CALL(mock_cb, Call(error::invalid_arg, INITIAL_REVISION));
    ASSERT_NO_THROW(t.xconf->put(
        config_type::ANY,
        "A",
        "B",
        "C",
        data,
        [&mock_cb](const error::error_code& e, revision_t r) { mock_cb.Call(e, r); },
        yplatform::task_context_ptr()););
}
