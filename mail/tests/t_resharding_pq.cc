#include <yxiva/core/resharding/pq_migration_storage.h>
#include <yxiva/core/operation_result.h>
#include <yxiva/core/types.h>
#include <ymod_pq/bind_array.h>
#include <ymod_pq/response_handler.h>
#include <catch.hpp>
#include <boost/logic/tribool.hpp>

using namespace yxiva::resharding;
using namespace yxiva;

using future_result = yplatform::future::future<bool>;
using promise_result = yplatform::future::promise<bool>;

static void report_invalid_migration(ymod_pq::response_handler_ptr handler)
{
    handler->handle_row_begin(0);
    handler->handle_cell(0, 0, "asdf", false);
    handler->handle_cell(0, 1, "fdsa", false);
    handler->handle_row_end(0);
}

static void report_migration(const migration& m, ymod_pq::response_handler_ptr handler)
{
    handler->handle_row_begin(0);
    handler->handle_cell(0, 0, std::to_string(static_cast<int>(m.state)), false);
    handler->handle_cell(0, 1, std::to_string(m.end_gid), false);
    handler->handle_row_end(0);
}

class mock_pq
{
public:
    mock_pq(boost::asio::io_service& io) : io(io)
    {
    }
    virtual ~mock_pq()
    {
    }

    virtual future_result request(
        yplatform::task_context_ptr /*ctx*/,
        const std::string& db,
        const std::string& request,
        ymod_pq::bind_array_ptr bind_vars,
        ymod_pq::response_handler_ptr handler,
        bool /*log_timings*/ = false,
        const yplatform::time_traits::duration& /*deadline*/ =
            yplatform::time_traits::duration::max())
    {
        dbs.insert(db);
        requests.insert(request);
        vars[request] = bind_vars;

        auto prom = std::make_shared<promise_result>();

        if (throw_on_request)
        {
            prom->set_exception(std::runtime_error("mock_pq exception"));
        }
        else if (return_invalid_data)
        {
            io.post([prom, handler]() {
                report_invalid_migration(handler);
                prom->set(true);
            });
        }
        else
        {
            io.post([this, prom, handler]() {
                if (!return_empty)
                {
                    for (auto& m : migrations)
                    {
                        report_migration(m, handler);
                    }
                }

                prom->set(true);
            });
        }

        return *prom;
    }

    boost::asio::io_service& io;

    std::set<string> dbs;
    std::multiset<string> requests;
    std::map<string, ymod_pq::bind_array_ptr> vars;

    std::vector<migration> migrations;

    bool throw_on_request = false;
    bool return_invalid_data = false;
    bool return_empty = false;
};

namespace yxiva { namespace resharding {

std::ostream& operator<<(std::ostream& os, migration::state_type s)
{
    switch (s)
    {
    case migration::state_type::pending:
        os << "pending";
        break;
    case migration::state_type::ready:
        os << "ready";
        break;
    case migration::state_type::inprogress:
        os << "inprogress";
        break;
    case migration::state_type::finished:
        os << "finished";
        break;
    default:
        os << "unknown";
        break;
    }
    return os;
}

std::ostream& operator<<(std::ostream& os, const migration& m)
{
    os << "state: " << m.state << " end_gid: " << m.end_gid;
    return os;
}

class test_access
{
public:
    using subject = pq_migration_storage<mock_pq>::impl;
};

}}

using test_subject_t = yxiva::resharding::test_access::subject;

#define REQUIRE_REQUEST_EXECUTED(request, times_called)                                            \
    REQUIRE(pq->dbs.count("test_conninfo") == 1);                                                  \
    REQUIRE(pq->requests.count((request)) == (times_called));                                      \
    REQUIRE(pq->dbs.size() == 1);

#define REQUIRE_REQUEST_EXECUTED_MORE_OR_EQUAL(request, times_called)                              \
    REQUIRE(pq->dbs.count("test_conninfo") == 1);                                                  \
    REQUIRE(pq->requests.count((request)) >= (times_called));                                      \
    REQUIRE(pq->dbs.size() == 1);

TEST_CASE("shards/pq_migration_storage", "")
{
    boost::asio::io_service io;
    boost::asio::io_service::work io_work(io);

    auto settings = pq_migration_storage<mock_pq>::settings{
        "test_conninfo", "get_migrations_range", "set_migration_state", time_traits::seconds(100)
    };

    auto pq = std::make_shared<mock_pq>(io);

    pq->migrations.push_back(migration{ migration::state_type::inprogress, 0, MAX_GID });

    auto test_subject = std::make_shared<test_subject_t>(io, settings, pq);

    SECTION("returns empty migration state before start")
    {
        REQUIRE(test_subject->get()->empty());
    }

    SECTION("queries migration state for all gids on start")
    {
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);
        REQUIRE(pq->vars["get_migrations_range"]->size() == 0);

        io.poll();
        auto migrations = test_subject->get();

        REQUIRE(migrations->at(0) == pq->migrations[0]);
    }

    SECTION("queries migration state for all gids periodically after start")
    {
        settings.update_interval = time_traits::milliseconds(5);

        test_subject = std::make_shared<test_subject_t>(io, settings, pq);
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        io.poll();

        pq->migrations[0] = migration{ migration::state_type::finished, 0, MAX_GID };

        std::this_thread::sleep_for(2 * settings.update_interval);

        io.poll();

        REQUIRE_REQUEST_EXECUTED_MORE_OR_EQUAL("get_migrations_range", 2);

        auto migrations = test_subject->get();

        REQUIRE(migrations->at(0) == pq->migrations[0]);
    }

    SECTION("does not modify cache state if the query fails")
    {
        settings.update_interval = time_traits::milliseconds(5);

        test_subject = std::make_shared<test_subject_t>(io, settings, pq);
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        io.poll();

        pq->throw_on_request = true;

        std::this_thread::sleep_for(2 * settings.update_interval);

        io.poll();

        REQUIRE_REQUEST_EXECUTED_MORE_OR_EQUAL("get_migrations_range", 2);

        auto migrations = test_subject->get();

        REQUIRE(migrations->at(0) == pq->migrations[0]);
    }

    SECTION("does not modify cache state if the query returns bad data")
    {
        settings.update_interval = time_traits::milliseconds(5);

        test_subject = std::make_shared<test_subject_t>(io, settings, pq);
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        io.poll();

        pq->return_invalid_data = true;

        std::this_thread::sleep_for(2 * settings.update_interval);

        io.poll();

        REQUIRE_REQUEST_EXECUTED_MORE_OR_EQUAL("get_migrations_range", 2);

        auto migrations = test_subject->get();

        REQUIRE(migrations->at(0) == pq->migrations[0]);
    }

    SECTION("queries migration state for all gids on demand")
    {
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        io.poll();

        pq->migrations[0] = migration{ migration::state_type::finished, 0, MAX_GID };
        boost::tribool update_successful;
        test_subject->fetch_migrations([&](const operation::result& fetch_result) {
            update_successful = fetch_result.success();
        });

        io.poll();
        REQUIRE(update_successful);

        auto migrations = test_subject->get();

        REQUIRE(migrations->at(0) == pq->migrations[0]);
    }

    SECTION("returns error if on-demand migration state query fails")
    {
        test_subject->start();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        io.poll();

        pq->throw_on_request = true;
        boost::tribool update_successful;
        test_subject->fetch_migrations([&](const operation::result& fetch_result) {
            update_successful = fetch_result.success();
        });

        io.poll();
        REQUIRE(!update_successful);
    }

    SECTION("cancels migration update timer on stop")
    {
        settings.update_interval = time_traits::milliseconds(5);

        test_subject = std::make_shared<test_subject_t>(io, settings, pq);
        test_subject->start();

        io.poll();

        test_subject->stop();

        std::this_thread::sleep_for(2 * settings.update_interval);

        io.poll();

        REQUIRE_REQUEST_EXECUTED_MORE_OR_EQUAL("get_migrations_range", 1);
    }

    const gid_t TEST_GID = 7;

    SECTION("migrates gid pending->ready")
    {
        pq->migrations[0] = migration{ migration::state_type::pending, 0, MAX_GID };

        test_subject->start();

        io.poll();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        boost::tribool set_successful;
        migration::state_type new_state = migration::state_type::pending;

        pq->migrations[0] = migration{ migration::state_type::finished, 0, MAX_GID };

        test_subject->set_migration_state(
            TEST_GID,
            migration::state_type::ready,
            [&](const operation::result& set_result, migration::state_type s, bool) {
                set_successful = set_result.success();
                new_state = s;
            });

        io.poll();

        REQUIRE(set_successful);
        REQUIRE(new_state == migration::state_type::finished);

        // require that local migration state got updated with pq result
        auto migrations = test_subject->get();
        REQUIRE(migrations->at(0) == (migration{ migration::state_type::finished, 0, MAX_GID }));

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);
        REQUIRE_REQUEST_EXECUTED("set_migration_state", 1);

        REQUIRE(pq->vars["set_migration_state"]->value<gid_t>(0) == TEST_GID);
        REQUIRE(
            pq->vars["set_migration_state"]->value<std::vector<int64_t>>(1) ==
            std::vector<int64_t>{ static_cast<int64_t>(migration::state_type::pending) });
        REQUIRE(
            pq->vars["set_migration_state"]->value<int>(2) ==
            static_cast<int>(migration::state_type::ready));
    }

    SECTION("returns error if set_migration returns empty state")
    {
        pq->migrations[0] = migration{ migration::state_type::pending, 0, MAX_GID };

        test_subject->start();

        io.poll();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        boost::tribool set_successful;
        pq->return_empty = true;

        test_subject->set_migration_state(
            TEST_GID,
            migration::state_type::ready,
            [&](const operation::result& set_result, migration::state_type, bool) {
                set_successful = set_result.success();
            });

        io.poll();

        REQUIRE(!set_successful);

        auto migrations = test_subject->get();
        REQUIRE(migrations->at(0) == (migration{ migration::state_type::pending, 0, MAX_GID }));

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);
        REQUIRE_REQUEST_EXECUTED("set_migration_state", 1);

        REQUIRE(pq->vars["set_migration_state"]->value<gid_t>(0) == TEST_GID);
        REQUIRE(
            pq->vars["set_migration_state"]->value<std::vector<int64_t>>(1) ==
            std::vector<int64_t>{ static_cast<int64_t>(migration::state_type::pending) });
        REQUIRE(
            pq->vars["set_migration_state"]->value<int>(2) ==
            static_cast<int>(migration::state_type::ready));
    }

    SECTION("sets bad state flag if set_migration returns a special tuple")
    {
        pq->migrations[0] = migration{ migration::state_type::pending, 0, MAX_GID };

        test_subject->start();

        io.poll();

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);

        boost::tribool set_successful;
        bool bad_migration_state = false;
        migration::state_type new_state = migration::state_type::pending;

        pq->migrations[0] = migration{ migration::state_type::finished, 0, MAX_GID };
        pq->migrations.push_back(migration{ migration::state_type::pending, MAX_GID, MAX_GID + 1 });

        test_subject->set_migration_state(
            TEST_GID,
            migration::state_type::ready,
            [&](const operation::result& set_result, migration::state_type s, bool bad_state) {
                set_successful = set_result.success();
                bad_migration_state = bad_state;
                new_state = s;
            });

        io.poll();

        REQUIRE(set_successful);
        REQUIRE(bad_migration_state);
        REQUIRE(new_state == migration::state_type::finished);

        auto migrations = test_subject->get();
        REQUIRE(migrations->at(0) == (migration{ migration::state_type::finished, 0, MAX_GID }));

        REQUIRE_REQUEST_EXECUTED("get_migrations_range", 1);
        REQUIRE_REQUEST_EXECUTED("set_migration_state", 1);

        REQUIRE(pq->vars["set_migration_state"]->value<gid_t>(0) == TEST_GID);
        REQUIRE(
            pq->vars["set_migration_state"]->value<std::vector<int64_t>>(1) ==
            std::vector<int64_t>{ static_cast<int64_t>(migration::state_type::pending) });
        REQUIRE(
            pq->vars["set_migration_state"]->value<int>(2) ==
            static_cast<int>(migration::state_type::ready));
    }
}