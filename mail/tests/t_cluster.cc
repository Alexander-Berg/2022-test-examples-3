#include "catch.hpp"
#include "basic_cluster.h"
#include "mock_call.h"
#include <iostream>
using namespace ymod_pq;

static time_t fake_now = 100;
std::time_t fake_time(std::time_t*)
{
    return fake_now;
}

struct mock_replica_monitor
{
    struct replica
    {
        std::string host;
        yplatform::time_traits::duration lag;
    };

    std::vector<replica> get()
    {
        return replicas;
    }

    void start()
    {
    }

    void stop()
    {
    }

    std::vector<replica> replicas;
};

struct cluster_fixture
{
    using cluster = basic_cluster<mock_call, mock_replica_monitor, fake_time>;
    using cluster_ptr = std::shared_ptr<cluster>;
    using mock_replica_monitor_ptr = std::shared_ptr<mock_replica_monitor>;
    using response_handler_ptr = boost::shared_ptr<mock_response_handler>;
    using mock_call_ptr = std::shared_ptr<mock_call>;
    using seconds = yplatform::time_traits::seconds;
    using duration = yplatform::time_traits::duration;

    boost::asio::io_service io;
    cluster_ptr test_subject;
    cluster::settings settings;
    response_handler_ptr response_handler;
    mock_call_ptr master_pq;
    mock_call_ptr replica_pq;
    mock_call_ptr replica1_pq;
    mock_call_ptr replica2_pq;
    mock_call_ptr master_only_pq;
    mock_replica_monitor_ptr replica_monitor;
    yplatform::task_context_ptr ctx;

    cluster_fixture()
    {
        settings.master.max_conn = 30;
        settings.replica.max_conn = 20;
        settings.fallback.max_conn = 10;
        settings.auto_fallback_enabled = true;
        master_pq = std::make_shared<mock_call>();
        replica_pq = std::make_shared<mock_call>();
        replica1_pq = std::make_shared<mock_call>();
        replica2_pq = std::make_shared<mock_call>();
        master_only_pq = std::make_shared<mock_call>();
        replica_monitor = std::make_shared<mock_replica_monitor>();
        test_subject = make_cluster();
        response_handler = boost::make_shared<mock_response_handler>();
        ctx = boost::make_shared<yplatform::task_context>();
    }

    auto make_pq(const ymod_pq::settings& settings)
    {
        if (is_master_conninfo(settings.conninfo))
        {
            master_pq->settings = settings;
            return master_pq;
        }
        if (is_replica_conninfo(settings.conninfo))
        {
            replica_pq->settings = settings;
            return replica_pq;
        }
        if (is_replica1_conninfo(settings.conninfo))
        {
            replica1_pq->settings = settings;
            return replica1_pq;
        }
        if (is_replica2_conninfo(settings.conninfo))
        {
            replica2_pq->settings = settings;
            return replica2_pq;
        }
        if (is_master_only_conninfo(settings.conninfo))
        {
            master_only_pq->settings = settings;
            return master_only_pq;
        }
        throw std::runtime_error("unexpected conninfo in make_pq: '" + settings.conninfo + "'");
    }

    std::string master_conninfo()
    {
        return "host=replica1,Master,replica2 port=6432 user=test target_session_attrs=read-write";
    }

    bool is_master_conninfo(const std::string& s)
    {
        return s.find("host=replica1,Master,replica2") != std::string::npos &&
            s.find("target_session_attrs=read-write") != std::string::npos;
    }

    bool is_replica_conninfo(const std::string& s)
    {
        return s.find("host=replica1,Master,replica2") != std::string::npos &&
            s.find("target_session_attrs=read-write") == std::string::npos;
    }

    bool is_replica1_conninfo(const std::string& s)
    {
        return s.find("host=replica1") != std::string::npos &&
            s.find("target_session_attrs=read-write") == std::string::npos;
    }

    bool is_replica2_conninfo(const std::string& s)
    {
        return s.find("host=replica2") != std::string::npos &&
            s.find("target_session_attrs=read-write") == std::string::npos;
    }

    bool is_master_only_conninfo(const std::string& s)
    {
        return s.find("host=Master") != std::string::npos &&
            s.find("target_session_attrs=read-write") == std::string::npos;
    }

    cluster_ptr make_cluster()
    {
        auto pq_factory = std::bind(&cluster_fixture::make_pq, this, std::placeholders::_1);
        return std::make_shared<cluster>(master_conninfo(), pq_factory, replica_monitor, settings);
    }

    auto request(request_target t)
    {
        return test_subject->request(
            nullptr,
            "some_request",
            nullptr,
            response_handler,
            false,
            yplatform::time_traits::duration::max(),
            t);
    }

    auto execute(request_target t)
    {
        return test_subject->execute(
            nullptr, "some_request", nullptr, false, yplatform::time_traits::duration::max(), t);
    }

    auto update(request_target t)
    {
        return test_subject->update(
            nullptr, "some_request", nullptr, false, yplatform::time_traits::duration::max(), t);
    }

    auto ok_request(request_target t)
    {
        if (t == request_target::master || t == request_target::try_master)
        {
            master_pq->fail = false;
        }
        else
        {
            replica_pq->fail = false;
        }
        return request(t);
    }

    auto fail_request(request_target t)
    {
        if (t == request_target::master || t == request_target::try_master)
        {
            master_pq->fail = true;
        }
        else
        {
            replica_pq->fail = true;
        }
        return request(t);
    }
};

#define REQUIRE_OK(r) REQUIRE_NOTHROW(r.get())
#define REQUIRE_FAIL(r) REQUIRE_THROWS(r.get())
#define REQUIRE_FALLBACK(pq) REQUIRE(pq->settings.max_conn == settings.fallback.max_conn);
#define REQUIRE_NORMAL(pq) REQUIRE(pq->settings.max_conn != settings.fallback.max_conn);

TEST_CASE_METHOD(cluster_fixture, "forwards queries to master", "")
{
    request(request_target::master);
    REQUIRE(is_master_conninfo(master_pq->settings.conninfo));
    REQUIRE(master_pq->requests.size() == 1);
    execute(request_target::master);
    REQUIRE(is_master_conninfo(master_pq->settings.conninfo));
    REQUIRE(master_pq->executes.size() == 1);
    update(request_target::master);
    REQUIRE(is_master_conninfo(master_pq->settings.conninfo));
    REQUIRE(master_pq->updates.size() == 1);
}

TEST_CASE_METHOD(cluster_fixture, "forwards queries to replica", "")
{
    request(request_target::replica);
    REQUIRE(is_replica_conninfo(replica_pq->settings.conninfo));
    REQUIRE(replica_pq->requests.size() == 1);
    execute(request_target::replica);
    REQUIRE(is_replica_conninfo(replica_pq->settings.conninfo));
    REQUIRE(replica_pq->executes.size() == 1);
    update(request_target::replica);
    REQUIRE(is_replica_conninfo(replica_pq->settings.conninfo));
    REQUIRE(replica_pq->updates.size() == 1);
}

TEST_CASE_METHOD(cluster_fixture, "fallback for request_target master", "")
{
    settings.enable_fallback_error_rate = 0.49;
    settings.disable_fallback_error_rate = 0.26;
    test_subject = make_cluster();

    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_FAIL(fail_request(request_target::master));
    REQUIRE_NORMAL(master_pq);
    fake_now += 1; // <-- 1 error, 2 requests => rate 0.5 => enable fallback.
    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_FALLBACK(master_pq);
    fake_now += 1; // <-- 1 error, 3 requests => rate 0.33 => remain in fallback.
    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_FALLBACK(master_pq);
    fake_now += 1; // <-- 1 errors, 4 requests => rate 0.25 => exit fallback.
    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_NORMAL(master_pq);
}

TEST_CASE_METHOD(
    cluster_fixture,
    "redirects try_master requests to replicas when master is in fallback",
    "")
{
    test_subject->enable_fallback(ctx, request_target::master);
    REQUIRE_FALLBACK(master_pq);
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE_OK(ok_request(request_target::try_master));
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 1);
}

TEST_CASE_METHOD(
    cluster_fixture,
    "sends try_master probe requests to master when master is in fallback",
    "")
{
    settings.probe_rps = 0.5;
    settings.rate_accumulator_window = 2;
    test_subject = make_cluster();

    test_subject->enable_fallback(ctx, request_target::master);
    REQUIRE_FALLBACK(master_pq);
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE_FAIL(fail_request(request_target::try_master));
    REQUIRE(master_pq->requests.size() == 1);
    REQUIRE(replica_pq->requests.size() == 0);
    fake_now += 1;
    REQUIRE_OK(fail_request(request_target::try_master));
    REQUIRE(master_pq->requests.size() == 1);
    REQUIRE(replica_pq->requests.size() == 1);
}

TEST_CASE_METHOD(
    cluster_fixture,
    "redirects try_replica requests to master when replica is in fallback",
    "")
{
    test_subject->enable_fallback(ctx, request_target::replica);
    REQUIRE_FALLBACK(replica_pq);
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE_OK(ok_request(request_target::try_replica));
    REQUIRE(master_pq->requests.size() == 1);
    REQUIRE(replica_pq->requests.size() == 0);
}

TEST_CASE_METHOD(
    cluster_fixture,
    "sends try_replica probe requests to replica when replica is in fallback",
    "")
{
    settings.probe_rps = 0.5;
    settings.rate_accumulator_window = 2;
    test_subject = make_cluster();

    test_subject->enable_fallback(ctx, request_target::replica);
    REQUIRE_FALLBACK(replica_pq);
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE_FAIL(fail_request(request_target::try_replica));
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 1);
    fake_now += 1;
    REQUIRE_OK(fail_request(request_target::try_replica));
    REQUIRE(master_pq->requests.size() == 1);
    REQUIRE(replica_pq->requests.size() == 1);
}

TEST_CASE_METHOD(
    cluster_fixture,
    "does not redirect try_ targets when both replica and master are in fallback",
    "")
{
    test_subject->enable_fallback(ctx, request_target::master);
    test_subject->enable_fallback(ctx, request_target::replica);
    REQUIRE_FALLBACK(master_pq);
    REQUIRE_FALLBACK(replica_pq);
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE_OK(ok_request(request_target::try_replica));
    REQUIRE(master_pq->requests.size() == 0);
    REQUIRE(replica_pq->requests.size() == 1);
    REQUIRE_OK(ok_request(request_target::try_master));
    REQUIRE(master_pq->requests.size() == 1);
    REQUIRE(replica_pq->requests.size() == 1);
}

TEST_CASE_METHOD(cluster_fixture, "fallback for request_target replica", "")
{
    settings.enable_fallback_error_rate = 0.9;
    settings.disable_fallback_error_rate = 0.4;
    test_subject = make_cluster();

    REQUIRE_FAIL(fail_request(request_target::replica));
    fake_now += 1; // <-- 1 request, 1 error => rate 1 => fallback.
    REQUIRE_OK(ok_request(request_target::replica));
    REQUIRE_FALLBACK(replica_pq);
    fake_now += 1; // <--- 2 requests, 1 error => rate 0.5 => remain in fallback.
    REQUIRE_OK(ok_request(request_target::replica));
    REQUIRE_FALLBACK(replica_pq);
    fake_now += 1; // <--- 3 requests, 1 error => rate 0.33 => exit fallback.
    REQUIRE_OK(ok_request(request_target::replica));
    REQUIRE_NORMAL(replica_pq);
}

TEST_CASE_METHOD(cluster_fixture, "enable_fallback forces fallback until disable_fallback")
{
    // Set disable rate to "always disable" to test forced fallback.
    settings.disable_fallback_error_rate = 1.1;
    test_subject = make_cluster();

    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_NORMAL(master_pq);
    test_subject->enable_fallback(ctx, request_target::master);
    REQUIRE_FALLBACK(master_pq);
    REQUIRE_OK(ok_request(request_target::master));
    fake_now += 1;
    // This should have disabled fallback, but mustn't have,
    // since fallback is forced.
    REQUIRE_OK(ok_request(request_target::master));
    REQUIRE_FALLBACK(master_pq);
    test_subject->disable_fallback(ctx, request_target::master);
    REQUIRE_NORMAL(master_pq);
}

TEST_CASE_METHOD(cluster_fixture, "chooses the least lagging replica", "")
{
    replica_monitor->replicas = { { "replica1", seconds(2) }, { "replica2", seconds(1) } };

    request(request_target::replica);
    REQUIRE(replica_pq->requests.size() == 0);
    REQUIRE(replica1_pq->requests.size() == 0);
    REQUIRE(replica2_pq->requests.size() == 1);
}

TEST_CASE_METHOD(cluster_fixture, "exports stats for all hosts", "")
{
    replica_monitor->replicas = { { "replica1", seconds(2) }, { "replica2", seconds(1) } };

    auto stats = test_subject->get_stats();
    REQUIRE(stats.get<std::string>("master.state") == "ok");
    REQUIRE(stats.get<double>("master.error_rate") == 0.0);
    REQUIRE(stats.get<std::string>("master.pq.foo") == "bar"); // from mock_pq
    REQUIRE(stats.get<double>("replica1.lag") == 2.0);
    REQUIRE(stats.get<std::string>("replica1.state") == "ok");
    REQUIRE(stats.get<double>("replica1.error_rate") == 0.0);
    REQUIRE(stats.get<std::string>("replica1.pq.foo") == "bar");
    REQUIRE(stats.get<double>("replica2.lag") == 1.0);
    REQUIRE(stats.get<std::string>("replica2.state") == "ok");
    REQUIRE(stats.get<double>("replica2.error_rate") == 0.0);
    REQUIRE(stats.get<std::string>("replica2.pq.foo") == "bar");
    REQUIRE(stats.get<double>("monitor.replicas.replica1") == 2.0);
    REQUIRE(stats.get<double>("monitor.replicas.replica2") == 1.0);
}

TEST_CASE_METHOD(cluster_fixture, "automatic fallback can be disabled", "")
{
    settings.enable_fallback_error_rate = 0.9;
    settings.disable_fallback_error_rate = 0.4;
    settings.auto_fallback_enabled = false;
    test_subject = make_cluster();

    REQUIRE_FAIL(fail_request(request_target::replica));
    REQUIRE_FAIL(fail_request(request_target::replica));
    REQUIRE_FAIL(fail_request(request_target::try_replica));
    REQUIRE_FAIL(fail_request(request_target::try_master));
    fake_now += 1; // <-- 2 requests, 2 error => rate 1 => should have been fallback.
    REQUIRE_FAIL(fail_request(request_target::replica));
    REQUIRE_FAIL(fail_request(request_target::replica));
    REQUIRE_FAIL(fail_request(request_target::try_replica));
    REQUIRE_FAIL(fail_request(request_target::try_master));
    REQUIRE_NORMAL(master_pq);
    REQUIRE_NORMAL(replica_pq);
}