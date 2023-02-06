#include "catch.hpp"

#include "replica_monitor.h"
#include "mock_call.h"

using namespace ymod_pq;

struct replica_monitor_fixture
{
    boost::asio::io_service io;
    boost::asio::io_service::work w{ io };
    std::shared_ptr<mock_call> pq = std::make_shared<mock_call>();
    replica_monitor::settings settings{ "foo",
                                        yplatform::time_traits::milliseconds(10),
                                        yplatform::time_traits::seconds(10) };
    std::string request_text = "foo";
    std::shared_ptr<replica_monitor> test_subject = make_replica_monitor();

    std::shared_ptr<replica_monitor> make_replica_monitor()
    {
        return std::make_shared<replica_monitor>(
            yplatform::task_context_ptr(new yplatform::task_context()),
            yplatform::log::source{},
            io,
            "some_db",
            pq,
            settings);
    }

    void wait(yplatform::time_traits::duration d)
    {
        for (unsigned i = 0; i < 10; ++i)
        {
            std::this_thread::sleep_for(d / 10);
            io.poll();
        }
    }
};

using replica_row = std::tuple<std::string, std::string>;

inline auto make_response(const std::vector<replica_row>& replicas)
{
    return [replicas](const response_handler_ptr& handler) {
        std::size_t row = 0;
        for (auto& replica : replicas)
        {
            std::size_t col = 0;
            handler->handle_row_begin(row);
            handler->handle_cell(row, col++, std::get<0>(replica), std::get<0>(replica) == "null");
            handler->handle_cell(row, col++, std::get<1>(replica), std::get<1>(replica) == "null");
            handler->handle_row_end(row++);
        }
    };
}

TEST_CASE_METHOD(replica_monitor_fixture, "by default returns empty replica set", "")
{
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 0);
}

TEST_CASE_METHOD(replica_monitor_fixture, "queries pq after call to start", "")
{
    test_subject->start();
    io.poll();
    REQUIRE(pq->requests.size() == 1);
    REQUIRE(pq->requests[0].db == "some_db");
    REQUIRE(pq->requests[0].request_text == settings.query);
}

TEST_CASE_METHOD(
    replica_monitor_fixture,
    "after successful pq query returns replica list from query results",
    "")
{
    pq->default_response = make_response({ { "host1", "50" }, { "host2", "100" } });
    test_subject->start();
    io.poll();
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() >= 1);
    REQUIRE(replicas[0].host == "host1");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(50));
    REQUIRE(replicas.size() == 2);
    REQUIRE(replicas[1].host == "host2");
    REQUIRE(replicas[1].lag == yplatform::time_traits::microseconds(100));
}

TEST_CASE_METHOD(replica_monitor_fixture, "queries replicas periodically until told to stop", "")
{
    pq->default_response = make_response({ { "host1", "50" } });
    test_subject->start();
    io.poll();
    REQUIRE(pq->requests.size() == 1);
    wait(settings.poll_interval);
    REQUIRE(pq->requests.size() == 2);
    test_subject->stop();
    wait(settings.poll_interval);
    REQUIRE(pq->requests.size() == 2);
}

TEST_CASE_METHOD(replica_monitor_fixture, "ignores replicas with empty host", "")
{
    pq->default_response = make_response({ { "", "50" }, { "host2", "100" } });
    test_subject->start();
    io.poll();
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host2");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(100));
}

TEST_CASE_METHOD(replica_monitor_fixture, "ignores replicas with null host", "")
{
    pq->default_response = make_response({ { "null", "50" }, { "host2", "100" } });
    test_subject->start();
    io.poll();
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host2");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(100));
}

TEST_CASE_METHOD(replica_monitor_fixture, "ignores replicas with invalid lag", "")
{
    pq->default_response = make_response({ { "abc", "zyx" }, { "host2", "100" } });
    test_subject->start();
    io.poll();
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host2");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(100));
}

TEST_CASE_METHOD(replica_monitor_fixture, "ignores replicas with null lag", "")
{
    pq->default_response = make_response({ { "abc", "null" }, { "host2", "100" } });
    test_subject->start();
    io.poll();
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host2");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(100));
}

TEST_CASE_METHOD(replica_monitor_fixture, "failed pq query does not affect replica list", "")
{
    pq->default_response = make_response({
        { "host1", "50" },
    });
    test_subject->start();
    io.poll();
    REQUIRE(pq->requests.size() == 1);
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host1");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(50));
    pq->fail = true;
    wait(settings.poll_interval);
    REQUIRE(pq->requests.size() == 2);
    replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host1");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(50));
}

TEST_CASE_METHOD(
    replica_monitor_fixture,
    "replica list does not reset itself until ttl elapses",
    "")
{
    settings.replica_list_ttl = 2 * settings.poll_interval;
    test_subject = make_replica_monitor();

    pq->default_response = make_response({ { "host1", "50" } });
    test_subject->start();
    io.poll();
    REQUIRE(pq->requests.size() == 1);
    auto replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host1");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(50));

    pq->default_response = make_response({});
    wait(settings.poll_interval);
    REQUIRE(pq->requests.size() == 2);
    replicas = test_subject->get();
    REQUIRE(replicas.size() == 1);
    REQUIRE(replicas[0].host == "host1");
    REQUIRE(replicas[0].lag == yplatform::time_traits::microseconds(50));

    wait(settings.poll_interval);
    REQUIRE(pq->requests.size() == 3);
    replicas = test_subject->get();
    REQUIRE(replicas.size() == 0);
}
