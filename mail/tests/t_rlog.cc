#include "test_messenger.h"
#include "test_database.h"
#include "test_caller.h"
#include "common.h"
#include "../src/rlog/rlog.h"
#include "../../ymod_paxos/src/acceptor_module.h"
#include <ymod_paxos/error.h>
#include <ymod_paxos/caller.h>
#include <ymod_paxos/operation.h>
#include <ymod_paxos/types.h>
#include <yplatform/log.h>
#include <yplatform/reactor.h>
#include <catch.hpp>
#include <boost/log/utility/setup/from_stream.hpp>
#include <thread>
#include <iostream>
#include <unistd.h>
#include <vector>

using std::vector;
using std::string;
using ymod_paxos::milliseconds;
using ymod_paxos::seconds;
using ymod_paxos::operation;

#define FAKE_CTX boost::make_shared<yplatform::task_context>()

class context : public yplatform::task_context
{
public:
    const std::string& get_name() const override
    {
        static const std::string& NAME = "test-context";
        return NAME;
    }
};

class test_rlog_t : public ymod_paxos::rlog_t
{
public:
    template <typename... Args>
    test_rlog_t(Args&&... args) : rlog_t(std::forward<Args>(args)...)
    {
    }

    size_t saved_ount()
    {
        return callers_.size();
    }

    size_t redirecting_ount()
    {
        return redirects_->size();
    }
};

class standalone_rlog
{
public:
    std::shared_ptr<test_rlog_t> rlog;
    std::shared_ptr<ymod_paxos::test_database> database;
    std::shared_ptr<test_messenger> messenger;
    yplatform::reactor_ptr reactor;
    std::shared_ptr<ymod_paxos::acceptor_module> acceptor;
    bool is_stopped;
    int peer_id;

public:
    standalone_rlog(int peer_id, string netch_name)
        : database(new ymod_paxos::test_database())
        , messenger(new test_messenger(netch_name))
        , reactor(new yplatform::reactor())
        , acceptor(new ymod_paxos::acceptor_module())
        , is_stopped(true)
        , peer_id(peer_id)
    {
        reactor->init(2, 2);
        // rlog message codes must be identical with acceptor
        auto paxos_network =
            std::make_shared<ymod_paxos::paxos_network>(messenger, DEFAULT_PAXOS_NETWORK_BASE);
        ymod_paxos::rlog_settings st;
        st.default_redirect_timeout = milliseconds(150);
        rlog.reset(new test_rlog_t(paxos_network, reactor, st));
        acceptor->init(messenger);
    }

    void start(bool open = true)
    {
        if (open) rlog->open(peer_id, database);
        is_stopped = false;
    }

    void stop()
    {
        is_stopped = true;
        rlog->close();
        reactor->stop();
        reactor->fini();
    }

    ~standalone_rlog()
    {
        if (!is_stopped) stop();
    }
};

class T_ymod_paxos_rlog
{
public:
    vector<std::shared_ptr<standalone_rlog>> rlogs;
    caller_counter counter;
    // std::shared_ptr<ymod_paxos::test_database> database;

public:
    T_ymod_paxos_rlog()
    //    : database(new ymod_paxos::test_database())
    {
        enable_boost_log();
        //      disable_boost_log();
        test_pool::clear();
    }

    void init(int count = 2)
    {
        rlogs = vector<std::shared_ptr<standalone_rlog>>(2);
        for (long long int i = 0; i < count; i++)
        {
            rlogs[i] = std::make_shared<standalone_rlog>(i, "netch" + std::to_string(i));

            rlogs[i]->start();
            for (size_t j = 0U; j < rlogs.size(); j++)
            {
                rlogs[i]->messenger->connect("netch" + std::to_string(j));
            }
            rlogs[i]->reactor->run();
        }
    }

    void stop()
    {
        for (auto i = 0U; i < rlogs.size(); ++i)
        {
            rlogs[i]->stop();
        }
    }

    ~T_ymod_paxos_rlog()
    {
        stop();
    }

    static void enable_boost_log()
    {
        // TODO rename and implement log init
    }

    static void disable_boost_log()
    {
        std::stringstream s;
        s << "[Core]\nDisableLogging=True\n";
        boost::log::init_from_stream(s);
    }

    operation create_operation(const string& params)
    {
        operation op(1, context().uniq_id(), multipaxos::value_t::create_from(params));
        return op;
    }

    std::shared_ptr<test_caller> create_caller()
    {
        return std::make_shared<test_caller>(&counter);
    }

    void perform_operation(int rlog_num, string event_text = "some event")
    {
        rlogs[rlog_num]->rlog->perform(FAKE_CTX, create_operation(event_text), create_caller());
    }

    void perform_operation(int rlog_num, int event_id)
    {
        string event_text = std::to_string(static_cast<long long int>(event_id));
        perform_operation(rlog_num, event_text);
    }

    void perform_operations_on(int rlog_num, int count = 1, int offset = 0)
    {
        for (int i = offset; i < offset + count; i++)
        {
            perform_operation(rlog_num, i);
        }
    }

    void set_master(int rlog_num, bool is_master = true)
    {
        if (is_master) rlogs[rlog_num]->rlog->set_master();
        else
            rlogs[rlog_num]->rlog->set_not_master();
    }

    void wait(int delay_sec = 1)
    {
        seconds sleep_time(delay_sec);
        std::this_thread::sleep_for(sleep_time);
    }

    void wait(double seconds)
    {
        milliseconds sleep_time(static_cast<long>(seconds * 1e3));
        std::this_thread::sleep_for(sleep_time);
    }

    std::shared_ptr<test_rlog_t> rlog(int rlog_num)
    {
        return rlogs[rlog_num]->rlog;
    }

    std::shared_ptr<ymod_paxos::test_database> database(int rlog_num)
    {
        return rlogs[rlog_num]->database;
    }
};

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/open-close", "")
{
    standalone_rlog rlog(0, "netch");
    rlog.start(true);
    REQUIRE(rlog.rlog->is_opened());
    REQUIRE(!rlog.rlog->is_master());

    REQUIRE_THROWS(rlog.rlog->open(rlog.peer_id, rlog.database));

    rlog.rlog->close();
    REQUIRE(!rlog.rlog->is_opened());

    // double close
    CHECK_NOTHROW(rlog.rlog->close());

    // when closed
    CHECK_NOTHROW(rlog.rlog->open(rlog.peer_id, rlog.database));
    CHECK_NOTHROW(rlog.rlog->close());

    rlog.stop();
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/set_master-not_master", "")
{
    standalone_rlog rlog(0, "netch");
    REQUIRE(!rlog.rlog->is_master());

    CHECK_THROWS(rlog.rlog->set_master());
    CHECK_NOTHROW(rlog.rlog->set_not_master());

    rlog.start(false);

    CHECK_THROWS(rlog.rlog->set_master());
    CHECK_NOTHROW(rlog.rlog->set_not_master());

    REQUIRE(!rlog.rlog->is_master());

    rlog.rlog->open(rlog.peer_id, rlog.database);

    rlog.rlog->set_master();
    REQUIRE(rlog.rlog->is_master());

    rlog.rlog->set_not_master();
    REQUIRE(!rlog.rlog->is_master());

    rlog.rlog->set_master();
    rlog.rlog->close();
    REQUIRE(!rlog.rlog->is_master());
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/ok", "")
{
    init();
    const int master_id = 0;

    set_master(master_id);
    perform_operations_on(master_id);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == 1);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/closed", "")
{
    init();
    const int master_id = 0;

    set_master(master_id);
    rlog(master_id)->close();
    perform_operations_on(master_id);
    wait(0.1);

    REQUIRE(counter.errors == 1);
    REQUIRE(counter.results == 0);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/no_master", "")
{
    init();
    const int slave_id = 0;
    perform_operations_on(slave_id);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == 0);
    REQUIRE(rlog(slave_id)->saved_ount() == 1);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 1);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/redirect", "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    set_master(master_id);
    perform_operations_on(slave_id);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == 1);
    REQUIRE(rlog(slave_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(
    T_ymod_paxos_rlog,
    "ymod_paxos/rlog/set_master_after_redirect_causes_redirect_timeout",
    "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    perform_operations_on(slave_id);
    set_master(master_id);
    wait(0.16);

    REQUIRE(counter.errors == 1);
    REQUIRE(counter.results == 0);
    REQUIRE(rlog(slave_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/duplicate", "")
{
    init();
    const int master_id = 0;
    set_master(master_id);
    auto op = create_operation("test");
    rlog(master_id)->perform(FAKE_CTX, op, create_caller());
    rlog(master_id)->perform(FAKE_CTX, op, create_caller());
    wait(0.1);

    REQUIRE(counter.errors == 1);
    REQUIRE(counter.results == 1);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/is_modifying1", "")
{
    init();
    const int master_id = 0;
    set_master(master_id);
    database(master_id)->events_are_modifying = true;

    perform_operations_on(master_id);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == 1);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/perform/is_modifying2", "")
{
    init();
    const int master_id = 0;
    set_master(master_id);
    database(master_id)->events_are_modifying = false;

    perform_operations_on(master_id);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == 1);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

/******************************************************************************/
/******************************************************************************/
/* Some functional tests                                                      */
/******************************************************************************/
/******************************************************************************/

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/on_master/perform_many_tasks", "")
{
    init();
    const int master_id = 0;
    const int count = 20;
    set_master(master_id);

    perform_operations_on(master_id, count);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == count);
}

TEST_CASE_METHOD(
    T_ymod_paxos_rlog,
    "ymod_paxos/rlog/on_slave/resend_quickly/perform_many_tasks",
    "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    const int count = 20;
    set_master(master_id);

    perform_operations_on(slave_id, count);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == count);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/on_slave/resend_slow/perform_many_tasks", "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    const int count = 20;
    set_master(master_id);

    perform_operations_on(slave_id, count);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == count);
}

TEST_CASE_METHOD(
    T_ymod_paxos_rlog,
    "ymod_paxos/rlog/combine_slave_master/resend_quickly/perform_many_tasks",
    "")
{
    init();
    const int master_id = 0;
    const int count = 20;
    set_master(master_id);

    for (int i = 0; i < count; i++)
        perform_operation(i % 2, i);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == count);
}

TEST_CASE_METHOD(
    T_ymod_paxos_rlog,
    "ymod_paxos/rlog/combine_slave_master/resend_slow/perform_many_tasks",
    "")
{
    init();
    const int master_id = 0;
    const int count = 20;
    set_master(master_id);

    for (int i = 0; i < count; i++)
        perform_operation(i % 2, i);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == count);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/on_slave/set_not_master/1", "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    const int executed_count = 14;
    const int waiting_count = 20;

    set_master(master_id);
    perform_operations_on(slave_id, executed_count);
    wait(0.2);

    set_master(master_id, false);
    perform_operations_on(slave_id, waiting_count);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == executed_count);
    REQUIRE(rlog(slave_id)->saved_ount() == waiting_count);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == waiting_count);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);

    set_master(master_id);
    wait(0.16);

    REQUIRE(counter.errors == waiting_count);
    REQUIRE(counter.results == executed_count);
    REQUIRE(rlog(slave_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}

TEST_CASE_METHOD(T_ymod_paxos_rlog, "ymod_paxos/rlog/on_slave/set_not_master/2", "")
{
    init();
    const int master_id = 0;
    const int slave_id = 1;
    const int executed_count = 14;
    const int waiting_count = 20;

    set_master(master_id);
    perform_operations_on(slave_id, executed_count);
    wait(0.2);

    set_master(master_id, false);
    perform_operations_on(slave_id, waiting_count, executed_count);
    wait(0.1);

    REQUIRE(counter.errors == 0);
    REQUIRE(counter.results == executed_count);
    REQUIRE(rlog(slave_id)->saved_ount() == waiting_count);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == waiting_count);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);

    set_master(slave_id);
    wait(0.3);

    REQUIRE(counter.errors == waiting_count);
    REQUIRE(counter.results == executed_count);
    REQUIRE(rlog(slave_id)->saved_ount() == 0);
    REQUIRE(rlog(master_id)->saved_ount() == 0);
    REQUIRE(rlog(slave_id)->redirecting_ount() == 0);
    REQUIRE(rlog(master_id)->redirecting_ount() == 0);
}
