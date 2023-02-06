#include <catch.hpp>
#include <fakes/call.h>
#include <src/balancing_call_op_impl.h>

using namespace ymod_httpclient;
using namespace ymod_httpclient::detail;
using std::vector;

static const unsigned stats_period = 5;

struct t_balancing_call_op
{
    using balancing_call_op = balancing_call_op_impl<fakes::call>;

    t_balancing_call_op() : client(make_shared<fakes::call>())
    {
    }

    void run(request req, continuation_ptr cont = nullptr)
    {
        err = boost::system::error_code();
        resp = response();
        if (!call_op)
        {
            stat = std::make_shared<request_stat>(stats_period, false, settings.nodes.size());
            call_op = std::make_shared<balancing_call_op>(
                io, std::make_shared<balancing_settings>(settings), stat, client);
        }

        (*call_op)(
            boost::make_shared<yplatform::task_context>(),
            req,
            {},
            cont,
            [this](boost::system::error_code err, response resp, continuation_ptr cont) {
                this->err = err;
                this->resp = resp;
                this->cont = cont;
                if (resp.status == 200)
                {
                    stat->count_successfull_request(0, cont->last_selected_node);
                }
                else
                {
                    stat->count_failed_request(err, 0, cont->last_selected_node);
                }
            });
        io.run();
        io.reset();
    }

    boost::asio::io_service io;
    balancing_settings settings;
    shared_ptr<fakes::call> client;
    shared_ptr<request_stat> stat;
    shared_ptr<balancing_call_op> call_op;
    boost::system::error_code err;
    response resp;
    continuation_ptr cont;
};

TEST_CASE_METHOD(t_balancing_call_op, "simple_call")
{
    settings.nodes = { "status200" };
    run(request::GET("/"));
    REQUIRE(!err);
    REQUIRE(resp.status == 200);
    REQUIRE(client->journal == vector<string>{ "status200/" });
}

TEST_CASE_METHOD(t_balancing_call_op, "fallback")
{
    settings.nodes = { "status500", "status200" };
    auto req = request::GET("/");
    run(req);
    req.attempt++;
    run(req, cont);
    REQUIRE(!err);
    REQUIRE(resp.status == 200);
    REQUIRE(client->journal == vector<string>({ "status500/", "status200/" }));
}

TEST_CASE_METHOD(t_balancing_call_op, "wrs")
{
    settings.nodes = { "status500", "status200" };
    settings.select_strategy = balancing_settings::weighted_random;
    for (int i = 0; i < 1000; ++i)
    {
        run(request::GET("/"));
    }
    int cnt200 = 0, cnt500 = 0;
    for (auto& url : client->journal)
    {
        REQUIRE((url == "status200/" || url == "status500/"));
        if (url == "status200/")
        {
            ++cnt200;
        }
        else
        {
            ++cnt500;
        }
    }
    REQUIRE(cnt200 > 5 * cnt500);
}
