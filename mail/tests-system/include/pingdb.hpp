#pragma once

#include <yplatform/reactor.h>

#include <include/context.hpp>
#include <include/common.hpp>
#include <include/expected.hpp>
#include <include/errors.hpp>
#include <include/logger.hpp>

#include <apq/connection_pool.hpp>

#include <boost/asio.hpp>

namespace {

template <typename CompletionToken>
apq::row_iterator async_request(
    apq::connection_pool& conn,
    const apq::query& query,
    const apq::time_traits::duration_type& request_timeout,
    CompletionToken&& token)
{
    boost::asio::
        async_completion<CompletionToken, void(boost::system::error_code, apq::row_iterator)>
            init(token);
    conn.async_request(
        apq::fake_task_context(),
        query,
        [handler = init.completion_handler](apq::result res, apq::row_iterator it) {
            if (res)
            {
                APQ_TESTER_LOG_ERROR(logdog::message = res.message());
                boost::asio::dispatch(std::bind(
                    std::move(handler),
                    make_error_code(apq_tester::Error::connectError),
                    std::move(it)));
            }
            else
            {
                boost::asio::dispatch(
                    std::bind(std::move(handler), boost::system::error_code{}, std::move(it)));
            }
        },
        apq::result_format_binary,
        request_timeout);
    return init.result.get();
}

}

namespace apq_tester::server::handlers {

struct PingDb
{

    expected<std::string> operator()(request_ptr request, response_ptr response, ContextPtr ctx)
        const
    {
        if (request->method != mth_get)
        {
            response->result(method_not_allowed);
            return {};
        }
        APQ_TESTER_LOG_DEBUG(logdog::message = "start connection with conninfo " + ctx->conninfo_);

        apq::connection_pool connection_pool{ *yplatform::global_net_reactor->io() };
        connection_pool.set_conninfo(ctx->conninfo_);
        connection_pool.set_limit(1);
        connection_pool.set_connect_timeout(std::chrono::seconds(3));
        connection_pool.set_queue_timeout(std::chrono::seconds(3));

        boost::system::error_code ec;
        auto result = async_request(
            connection_pool, apq::query("SELECT 1"), std::chrono::seconds(5), ctx->yield_[ec]);
        if (ec)
        {
            return make_unexpected(error_code(make_error_code(Error::connectError), ec.message()));
        }
        if (result->size() != 1)
        {
            return make_unexpected<mail_errors::error_code>(
                make_error_code(Error::sizeResultError));
        }

        std::int64_t value;
        result->at(0, value);

        if (value != 1)
        {
            return make_unexpected<mail_errors::error_code>(make_error_code(Error::resultError));
        }
        return std::string(R"({"result":"pongdb"})");
    }
};

}
