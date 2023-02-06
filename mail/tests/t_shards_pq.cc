#include <yxiva/core/shards/pq_updated_storage.h>
#include <catch.hpp>
#include <thread>

using namespace yxiva;
using namespace shard_config;

typedef yplatform::future::future<bool> future_result;
typedef yplatform::future::promise<bool> promise_result;

class mock_response_handler
{
public:
    virtual ~mock_response_handler()
    {
    }
    virtual void handle_cell(unsigned row, unsigned col, const string& value, bool is_null) = 0;
    virtual unsigned column_count() const = 0;
    virtual void handle_row_begin(unsigned /*row*/)
    {
    }
    virtual void handle_row_end(unsigned /*row*/)
    {
    }
};

typedef boost::shared_ptr<mock_response_handler> mock_response_handler_ptr;

static void report_shard(const shard& shard, const mock_response_handler_ptr& handler)
{
    // report master
    handler->handle_row_begin(0); // row number not used in pq_updater, may as well pass 0
    handler->handle_cell(0, 0, std::to_string(shard.id), false);
    handler->handle_cell(0, 1, "master", false);
    handler->handle_cell(0, 2, shard.master.conninfo, false);
    handler->handle_row_end(0);

    // report replicas
    for (const auto& replica : shard.replicas)
    {
        handler->handle_row_begin(0); // row number not used in pq_updater, may as well pass 0
        handler->handle_cell(0, 0, std::to_string(shard.id), false);
        handler->handle_cell(0, 1, "replica", false);
        handler->handle_cell(0, 2, replica.conninfo, false);
        handler->handle_row_end(0);
    }
}

static void report_invalid_shard(const mock_response_handler_ptr& handler)
{
    handler->handle_row_begin(0); // row number not used in pq_updater, may as well pass 0
    handler->handle_cell(0, 0, "", true);
    handler->handle_cell(0, 1, "", true);
    handler->handle_cell(0, 2, "", true);
    handler->handle_row_end(0);
}

class mock_pq_t
{
public:
    mock_pq_t(
        boost::asio::io_service& io,
        const shards& shards,
        std::size_t& num_requests,
        string& db_queried,
        string& query_executed)
        : io_(io)
        , shards_(shards)
        , num_requests_(num_requests)
        , db_queried_(db_queried)
        , query_executed_(query_executed)
    {
    }

    void throw_on_request(bool yes)
    {
        throw_on_request_ = yes;
    }
    void return_invalid_data(bool yes)
    {
        return_invalid_data_ = yes;
    }

    future_result request(
        yplatform::task_context_ptr /*ctx*/,
        const string& db,
        const string& query,
        void*,
        mock_response_handler_ptr handler,
        bool /*log_timings*/)
    {
        ++num_requests_;
        db_queried_ = db;
        query_executed_ = query;

        auto prom = std::make_shared<promise_result>();

        if (throw_on_request_)
        {
            prom->set_exception(std::runtime_error("mock_pq exception"));
        }
        else if (return_invalid_data_)
        {
            io_.post([prom, handler]() {
                report_invalid_shard(handler);
                prom->set(true);
            });
        }
        else
        {
            io_.post([this, prom, handler]() {
                for (const auto& shard : shards_)
                {
                    report_shard(shard, handler);
                }

                prom->set(true);
            });
        }

        return *prom;
    }

private:
    boost::asio::io_service& io_;
    const shards& shards_;

    std::size_t& num_requests_;
    string& db_queried_;
    string& query_executed_;

    bool throw_on_request_{ false };
    bool return_invalid_data_{ false };
};

using test_storage_t =
    pq_updated_storage<mock_pq_t, yplatform::future::future<bool>, mock_response_handler>;

class test_pq_updates : test_storage_t
{
public:
    using test_subject_t = test_storage_t::impl;
};

TEST_CASE("shards/pq_updater", "")
{
    boost::asio::io_service io;
    boost::asio::io_service::work io_work(io);

    std::size_t times_queried_shards = 0;
    const string conninfo_params = "parameters added to every conninfo";

    std::map<unsigned, gid_t> gid_mapping{ { { 100, 0 }, { 200, 1 } } };

    std::vector<shard> raw_shards = { { { 0, 0, 100, { "master" }, { { { "replica" } } } },
                                        { 1, 101, 200, { "master2" }, { { { "replica2" } } } } } };

    std::vector<shard> reference_shards = {
        { { 0, 0, 100, { "master " + conninfo_params }, { { { "replica " + conninfo_params } } } },
          { 1,
            101,
            200,
            { "master2 " + conninfo_params },
            { { { "replica2 " + conninfo_params } } } } }
    };

    string db_queried;
    string query_executed;

    auto pq = std::make_shared<mock_pq_t>(
        io, raw_shards, times_queried_shards, db_queried, query_executed);

    const test_storage_t::settings settings{
        milliseconds(10), "execute_me", conninfo_params, "query_me"
    };
    auto test_subject =
        std::make_shared<test_pq_updates::test_subject_t>(io, settings, gid_mapping, pq);

    test_subject->start();

    REQUIRE(test_subject->get()->empty());

    SECTION("after start queries pq and reports query result", "")
    {
        REQUIRE(times_queried_shards == 0);

        io.poll();

        REQUIRE(times_queried_shards >= 1);
        REQUIRE(*test_subject->get() == reference_shards);
    }

    SECTION("queries db specified in settings", "")
    {
        io.poll();

        REQUIRE(db_queried == settings.plproxy_conninfo);
    }

    SECTION("executes query specified in settings", "")
    {
        io.poll();

        REQUIRE(query_executed == settings.query);
    }

    SECTION("does not repeat query before update_interval elapses", "")
    {
        io.poll();

        auto prev_times_queried_shards = times_queried_shards;

        std::this_thread::sleep_for(settings.update_interval);
        io.poll();

        REQUIRE(times_queried_shards == prev_times_queried_shards + 1);
    }

    SECTION("reflects updates in pq", "")
    {
        io.poll();

        REQUIRE(*test_subject->get() == reference_shards);

        raw_shards.pop_back();
        reference_shards.pop_back();

        std::this_thread::sleep_for(settings.update_interval);
        io.poll();

        REQUIRE(*test_subject->get() == reference_shards);
    }

    SECTION("does not query shards after stop", "")
    {
        io.poll();

        auto prev_times_queried_shards = times_queried_shards;

        test_subject->stop();
        std::this_thread::sleep_for(settings.update_interval);
        io.poll();

        REQUIRE(times_queried_shards == prev_times_queried_shards);
    }

    SECTION("recovers after pq exception", "")
    {
        pq->throw_on_request(true);
        io.poll();

        REQUIRE(times_queried_shards == 1);

        std::this_thread::sleep_for(settings.update_interval);
        pq->throw_on_request(false);
        io.poll();

        REQUIRE(times_queried_shards >= 2);
        REQUIRE(*test_subject->get() == reference_shards);
    }

    SECTION("recovers after invalid data", "")
    {
        pq->return_invalid_data(true);
        io.poll();

        auto prev_times_queried_shards = times_queried_shards;

        std::this_thread::sleep_for(settings.update_interval);
        pq->return_invalid_data(false);
        io.poll();

        REQUIRE(times_queried_shards >= prev_times_queried_shards + 1);
        REQUIRE(*test_subject->get() == reference_shards);
    }
}

TEST_CASE("shards/pq_handler")
{
    const string conninfo_params = "added to every conninfo";

    pq_request_handler<mock_response_handler> test_subject(conninfo_params);

    SECTION("sets failure reason when id is null", "")
    {
        test_subject.handle_cell(0, 0, "", true);
        REQUIRE(!test_subject.failure_reason().empty());
    }

    SECTION("sets failure reason when role is null", "")
    {
        test_subject.handle_cell(0, 1, "", true);
        REQUIRE(!test_subject.failure_reason().empty());
    }

    SECTION("sets failure reason when conninfo is null", "")
    {
        test_subject.handle_cell(0, 2, "", true);
        REQUIRE(!test_subject.failure_reason().empty());
    }

    SECTION("sets failure reason when id is invalid", "")
    {
        test_subject.handle_cell(0, 0, "not a number", true);
        REQUIRE(!test_subject.failure_reason().empty());
    }

    SECTION("sets failure reason when role is invalid", "")
    {
        test_subject.handle_cell(0, 1, "not a valid role", true);
        REQUIRE(!test_subject.failure_reason().empty());
    }
}
