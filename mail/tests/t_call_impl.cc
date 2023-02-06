#include "fakes/http_session.h"
#include "call_impl.h"
#include <catch.hpp>

using namespace ymod_httpclient;

using impl_type = ymod_httpclient::detail::call_impl<fakes::http_session>;

struct t_call_impl
{
    yplatform::task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    boost::asio::io_service io;
    settings st;
    boost::system::error_code err = ymod_httpclient::http_error::unknown_error;

    auto create_call_impl()
    {
        auto io_pool = make_shared<yplatform::io_pool>(io, 1);
        auto reactor = boost::make_shared<yplatform::reactor>(io_pool);
        auto call = make_shared<impl_type>();
        call->init(reactor, st);
        return call;
    }
};

TEST_CASE_METHOD(t_call_impl, "success")
{
    auto call = create_call_impl();
    auto req = yhttp::request::GET("http://yandex.ru");
    call->async_run(ctx, req, [&](auto&& e, auto&& /*resp*/) { err = e; });
    io.run();
    REQUIRE(!err);
}

TEST_CASE_METHOD(t_call_impl, "max_request_line_size")
{
    st.max_request_line_size = 10;
    auto req =
        yhttp::request::GET("http://yandex.ru/" + std::string(st.max_request_line_size + 1, 'x'));
    auto call = create_call_impl();
    call->async_run(ctx, req, [&](auto&& e, auto&& /*resp*/) { err = e; });
    io.run();
    REQUIRE(err == ymod_httpclient::http_error::request_uri_too_long);
}
