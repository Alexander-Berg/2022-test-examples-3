#include <yplatform/module.h>
#include <yplatform/module_registration.h>
#include <ymod_webserver/server.h>
#include <ymod_messenger/ymod_messenger.h>
#include <ymod_paxos/network.h>
#include <multipaxos/ts_agent.h>
#include "../src/ylogger.h"
#include <boost/chrono.hpp>

namespace ypaxos_test {

using clock = boost::chrono::steady_clock;
using time_point = clock::time_point;
using duration = clock::duration;
using ymod_paxos::mutex_t;
using ymod_paxos::lock_t;
using ymod_paxos::milliseconds;

namespace http_codes = ymod_webserver::codes;

#define CTX_LEN 12

typedef multipaxos::ts_agent<ymod_paxos::paxos_network, ymod_paxos::ylogger> paxos_agent;

class mod : public yplatform::module
{
public:
    void init(const yplatform::ptree& data)
    {
        auto netch = yplatform::find<ymod_messenger::module, std::shared_ptr>("netch");
        auto global_reactor = yplatform::global_net_reactor;
        paxos_agent_.reset(
            new paxos_agent(ymod_paxos::ylogger(logger(), ymod_paxos::ylogger::level::info)));
        paxos_agent_->init_network(std::make_shared<ymod_paxos::paxos_network>(
            netch, data.get<unsigned>("paxos_message_base_type")));
        netch->bind_events(
            [this](const ymod_messenger::address_t&, const ymod_messenger::event_notification& e) {
                if (e.event == ymod_messenger::event_CONNECTED)
                    std::cout << "slave is connected" << std::endl;
            });

        multipaxos::deliver_value_function_t f1 = boost::bind(&mod::paxos_deliver, this, _1, _2);
        multipaxos::drop_function_t f2 = boost::bind(&mod::paxos_drop, this, _1);
        multipaxos::report_function_t f3;
        multipaxos::algorithm_settings_t alg;
        alg.max_parallel_accepts = data.get<unsigned>("parallel_accepts", 100);
        paxos_agent_->init(alg, 0, 6000, 0, f1, f2, f3);
        paxos_agent_->start();
        paxos_agent_->set_master();

        timer_interval_ = milliseconds(data.get<unsigned>("timer_interval_ms"));
        timer_.reset(new boost::asio::deadline_timer(*yplatform::global_net_reactor->io()));
        timer_func(boost::system::error_code());

        yplatform::find<ymod_webserver::server, std::shared_ptr>("web_server")
            ->bind("front", { "/" }, boost::bind(&mod::execute, this, _1));
        yplatform::find<ymod_messenger::module, std::shared_ptr>("netch")->connect(
            data.get<std::string>("slave"));
    }

    void stop()
    {
        timer_->cancel();
        paxos_agent_->stop();
        for (int i = 0; i < 128; ++i)
            streams[i].clear();
        paxos_agent_.reset();
    }

    void fini()
    {
        timer_.reset();
    }

    void execute(ymod_webserver::http::stream_ptr stream)
    {
        auto id = push_stream(stream);
        paxos_agent_->submit(multipaxos::buffer_t::create_from(id));

        //    lock_t g(q_mutex);
        //    q.push_back(id);
    }

private:
    void paxos_deliver(ymod_paxos::iid_t, multipaxos::value_t v)
    {
        auto size = v.size() / CTX_LEN;
        for (std::size_t i = 0; i < size; ++i)
        {
            std::string id(v.data() + i * CTX_LEN, CTX_LEN);
            auto val = counter_.fetch_add(1);
            auto stream = pop_stream(id);
            if (stream) stream->result(http_codes::ok, std::to_string(val + 1));
        }
    }

    void paxos_drop(multipaxos::value_t v)
    {
        auto stream = pop_stream(std::string(v.data(), v.size()));
        if (stream) stream->result(http_codes::internal_server_error, "");
    }

    std::string push_stream(ymod_webserver::http::stream_ptr stream)
    {
        auto id = stream->request()->context->uniq_id();
        int shard = id[0] % 128;
        lock_t g(mutex[shard]);
        streams[shard][id] = { stream, clock::now() };
        return id;
    }

    ymod_webserver::http::stream_ptr pop_stream(const std::string& id)
    {
        int shard = id[0] % 128;
        lock_t g(mutex[shard]);
        auto stream_data = streams[shard][id];
        //    std::cout << clock::now()-stream_data.time << std::endl;
        streams[shard].erase(id); // XXX not optimal
        return stream_data.stream;
    }

    void timer_func(const boost::system::error_code& e)
    {
        if (e) return;

        std::deque<std::string> q_copy;
        {
            lock_t g(q_mutex);
            q_copy.swap(q);
        }

        if (q_copy.size())
        {
            multipaxos::value_t value = multipaxos::buffer_t::allocate(CTX_LEN * q_copy.size());
            for (auto& i : q_copy)
            {
                memcpy(value.data(), i.data(), i.size());
            }
            paxos_agent_->submit(value);
        }

        timer_->expires_from_now(timer_interval_);
        timer_->async_wait(boost::bind(&mod::timer_func, this, _1));
    }

    struct stream_data
    {
        ymod_webserver::http::stream_ptr stream;
        time_point time;
    };

    std::shared_ptr<paxos_agent> paxos_agent_;
    std::map<std::string, stream_data> streams[128];
    mutex_t mutex[128];
    mutex_t q_mutex;
    std::deque<std::string> q;
    std::shared_ptr<boost::asio::deadline_timer> timer_;
    time_duration timer_interval_;
    std::atomic<uint64_t> counter_ = { 0 };
};

}

DEFINE_SERVICE_OBJECT(ypaxos_test::mod);
